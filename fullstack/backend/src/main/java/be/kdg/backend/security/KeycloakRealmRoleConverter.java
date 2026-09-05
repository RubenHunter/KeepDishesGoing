package be.kdg.backend.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Maps Keycloak {@code realm_access.roles} → Spring {@link GrantedAuthority}.
 * Mirrors restaurant-service {@code KeycloakRealmRoleConverter}.
 */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object realmAccessObj = jwt.getClaims().get("realm_access");
        if (!(realmAccessObj instanceof Map<?, ?> realmAccess)) return List.of();

        Object rolesObj = realmAccess.get("roles");
        if (!(rolesObj instanceof List<?> raw)) return List.of();

        return raw.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(s -> !s.isBlank())
                .map(SimpleGrantedAuthority::new) // e.g. "user", "owner"
                .collect(Collectors.toList());
    }
}
