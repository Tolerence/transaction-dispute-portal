package com.disputeportal.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtAuthoritiesTest {

    @Test
    void mapsKeycloakRealmRolesToAuthorities() {
        Jwt jwt = token(Map.of("realm_access", Map.of("roles", List.of("DISPUTE_REVIEWER", "offline_access"))));

        assertThat(authorities(jwt)).containsExactlyInAnyOrder("ROLE_DISPUTE_REVIEWER", "ROLE_offline_access");
    }

    @Test
    void mapsFlatRolesClaimToAuthorities() {
        Jwt jwt = token(Map.of("roles", List.of("DISPUTE_REVIEWER")));

        assertThat(authorities(jwt)).containsExactly("ROLE_DISPUTE_REVIEWER");
    }

    @Test
    void tokenWithoutRolesHasNoRoleAuthorities() {
        assertThat(authorities(token(Map.of()))).isEmpty();
    }

    private static List<String> authorities(Jwt jwt) {
        return JwtAuthorities.rolesOf(jwt).stream().map(GrantedAuthority::getAuthority).toList();
    }

    private static Jwt token(Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Jwt.Builder builder = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject("someone")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300));
        extraClaims.forEach(builder::claim);
        return builder.build();
    }
}
