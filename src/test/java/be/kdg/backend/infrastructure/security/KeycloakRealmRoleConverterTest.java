package be.kdg.backend.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRealmRoleConverterTest {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    private Jwt jwtWithRoles(List<String> roles) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", roles))
                .build();
    }

    @Test
    void mapsRealmRolesToAuthorities() {
        Collection<GrantedAuthority> authorities = converter.convert(jwtWithRoles(List.of("driver", "admin")));
        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("driver", "admin");
    }

    @Test
    void returnsEmptyWhenNoRealmAccess() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("sub").build();
        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    void returnsEmptyWhenRolesNotAList() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", "not-a-list"))
                .build();
        assertThat(converter.convert(jwt)).isEmpty();
    }
}
