package com.disputeportal.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Maps role claims to Spring authorities. Accepts Keycloak's {@code realm_access.roles} and a plain
 * {@code roles} array (used by the dev token endpoint). Authorities are {@code ROLE_<name>}.
 */
public final class JwtAuthorities {

    private JwtAuthorities() {
    }

    public static Set<GrantedAuthority> rolesOf(Jwt jwt) {
        Set<String> roles = new java.util.HashSet<>();
        if (jwt.getClaim("realm_access") instanceof Map<?, ?> realmAccess
                && realmAccess.get("roles") instanceof Collection<?> realmRoles) {
            realmRoles.forEach(role -> roles.add(String.valueOf(role)));
        }
        Object flat = jwt.getClaim("roles");
        if (flat instanceof Collection<?> flatRoles) {
            flatRoles.forEach(role -> roles.add(String.valueOf(role)));
        }
        return roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toUnmodifiableSet());
    }
}
