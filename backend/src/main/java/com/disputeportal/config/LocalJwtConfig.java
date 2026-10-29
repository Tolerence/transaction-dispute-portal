package com.disputeportal.config;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Local HS256 token handling for the {@code dev} and {@code test} profiles only. Production validates
 * tokens issued by the identity provider (see {@link OidcJwtConfig}).
 */
@Configuration
@Profile({"dev", "test"})
public class LocalJwtConfig {

    private static final int MIN_SECRET_BYTES = 32;

    @Bean
    SecretKey localJwtKey(@Value("${app.security.jwt-secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.security.jwt-secret must be at least %d bytes; set JWT_SECRET".formatted(MIN_SECRET_BYTES));
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey localJwtKey, @Value("${app.security.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(localJwtKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(), new AudienceValidator(audience)));
        return decoder;
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey localJwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(localJwtKey));
    }
}
