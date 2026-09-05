package be.kdg.backend.infrastructure.security;

import be.kdg.backend.domain.driver.DeliveryPerson;
import be.kdg.backend.domain.driver.DeliveryPersonRepository;
import be.kdg.backend.domain.shared.DeliveryPersonId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DriverGuardTest {

    @Mock DeliveryPersonRepository driverRepository;

    private final UUID driverId = UUID.randomUUID();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String sub, String... roles) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(sub)
                .claim("realm_access", Map.of("roles", roles.length == 0 ? List.of() : List.of(roles)))
                .build();
        var authorities = java.util.Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, authorities));
    }

    @Test
    void deniesWhenUnauthenticated() {
        DriverGuard guard = new DriverGuard(driverRepository);
        assertThat(guard.canAccessPayouts(driverId)).isFalse();
    }

    @Test
    void allowsMatchingSubjectWithRegisteredDriver() {
        given(driverRepository.findById(DeliveryPersonId.of(driverId)))
                .willReturn(Optional.of(new DeliveryPerson(DeliveryPersonId.of(driverId), "R", "BICYCLE", true)));
        authenticate(driverId.toString(), "driver");
        DriverGuard guard = new DriverGuard(driverRepository);

        assertThat(guard.canAccessPayouts(driverId)).isTrue();
    }

    @Test
    void deniesWhenSubjectDoesNotMatch() {
        authenticate(UUID.randomUUID().toString(), "driver");
        DriverGuard guard = new DriverGuard(driverRepository);

        assertThat(guard.canAccessPayouts(driverId)).isFalse();
    }

    @Test
    void allowsAdminRegardlessOfDriver() {
        authenticate(UUID.randomUUID().toString(), "admin");
        DriverGuard guard = new DriverGuard(driverRepository);

        assertThat(guard.canAccessPayouts(driverId)).isTrue();
    }
}
