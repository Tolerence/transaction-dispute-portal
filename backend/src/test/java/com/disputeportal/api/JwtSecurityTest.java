package com.disputeportal.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exercises the real JWT decoder (signature, expiry and audience), unlike the API tests which
 * bypass it with a pre-authenticated principal.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtSecurityTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtEncoder encoder;

    @Value("${app.security.audience}")
    private String audience;

    @Test
    void tokenIssuedForThisApiIsAccepted() throws Exception {
        mvc.perform(get("/api/transactions").header("Authorization", "Bearer " + token(audience, 300)))
                .andExpect(status().isOk());
    }

    @Test
    void tokenIssuedForAnotherApiIsRejected() throws Exception {
        mvc.perform(get("/api/transactions").header("Authorization", "Bearer " + token("some-other-api", 300)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        mvc.perform(get("/api/transactions").header("Authorization", "Bearer " + token(audience, -300)))
                .andExpect(status().isUnauthorized());
    }

    private String token(String aud, long secondsFromNow) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("demo-customer")
                .audience(List.of(aud))
                .issuedAt(now.minus(10, ChronoUnit.MINUTES))
                .expiresAt(now.plusSeconds(secondsFromNow))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
