package be.kdg.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Security — Keycloak JWT resource server. Mirrors restaurant-service {@code SecurityConfig}.
 *
 * Realm roles (Keycloak realm "keepdishesgoing"): user=customer, owner, driver, admin.
 * Identity (customerId) is always derived from the JWT subject, never from request bodies.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(mgmt -> mgmt.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers("/actuator/health").permitAll()
                        // Payment webhook is secured by shared-secret header (see PaymentWebhookController).
                        .requestMatchers("/api/payments/**").permitAll()
                        // Public catalogue — guests browse restaurants/prices without an account.
                        .requestMatchers(HttpMethod.GET, "/api/restaurants/**").permitAll()
                        // Owner console must be checked BEFORE the {orderId} catch-all below.
                        .requestMatchers(HttpMethod.GET, "/api/orders/restaurant/**").hasAuthority("owner")
                        // Read-only order views stay public for guests (US21/US33 progress screens);
                        // writes always carry the JWT identity of the customer.
                        // NOTE: literal identity-bound reads must precede the {orderId} catch-all,
                        // otherwise an anonymous call would reach a JwtAuthenticationToken controller.
                        .requestMatchers(HttpMethod.GET, "/api/orders/customer").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/orders/{orderId}/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );
        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter conv = new JwtAuthenticationConverter();
        conv.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return conv;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration().applyPermitDefaultValues();
        cfg.addAllowedMethod("*"); // default allows only GET/HEAD/POST — PATCH/PUT/DELETE needed
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
