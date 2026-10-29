package com.disputeportal.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OidcJwtConfigTest {

    @Test
    void refusesToStartWithoutAnIssuer() {
        assertThatThrownBy(() -> new OidcJwtConfig().jwtDecoder("  ", "", "dispute-portal-api"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OIDC_ISSUER_URI");
    }
}
