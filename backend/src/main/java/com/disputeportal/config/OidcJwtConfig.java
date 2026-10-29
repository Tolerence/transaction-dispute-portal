package com.disputeportal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Production token validation against the identity provider. Checks signature, expiry, issuer and audience.
 * Active everywhere except the dev and test profiles.
 *
 * <p>Keys come from {@code OIDC_JWK_SET_URI} when set, which lets containers reach the provider on an internal
 * address while tokens still carry the public issuer. Otherwise keys are discovered from the issuer.
 */
@Configuration
@Profile("!dev & !test")
public class OidcJwtConfig {

    @Bean
    JwtDecoder jwtDecoder(@Value("${app.security.issuer-uri:}") String issuerUri,
                          @Value("${app.security.jwk-set-uri:}") String jwkSetUri,
                          @Value("${app.security.audience}") String audience) {
        if (issuerUri.isBlank()) {
            throw new IllegalStateException(
                    "app.security.issuer-uri must be set (OIDC_ISSUER_URI) unless the dev profile is active");
        }
        NimbusJwtDecoder decoder = jwkSetUri.isBlank()
                ? NimbusJwtDecoder.withIssuerLocation(issuerUri).build()
                : NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuerUri), new AudienceValidator(audience)));
        return decoder;
    }
}
