package com.disputeportal.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import com.disputeportal.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Issues short-lived tokens for local development. Only registered under the {@code dev} profile;
 * production must receive tokens from the bank's identity provider.
 */
@RestController
@RequestMapping("/api/dev")
@Profile("dev")
public class DevTokenController {

    private static final Duration TOKEN_LIFETIME = Duration.ofHours(1);
    private static final Set<String> ALLOWED_ROLES = Set.of("DISPUTE_REVIEWER");

    private final JwtEncoder encoder;
    private final Clock clock;
    private final String audience;

    public DevTokenController(JwtEncoder encoder, Clock clock, @Value("${app.security.audience}") String audience) {
        this.encoder = encoder;
        this.clock = clock;
        this.audience = audience;
    }

    @PostMapping("/token")
    public TokenResponse issue(@Valid @RequestBody TokenRequest request) {
        List<String> roles = request.roles() == null ? List.of() : request.roles();
        if (!ALLOWED_ROLES.containsAll(roles)) {
            throw BusinessException.badRequest("Unknown role requested. Allowed: " + ALLOWED_ROLES);
        }
        Instant now = clock.instant();
        Instant expiresAt = now.plus(TOKEN_LIFETIME);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(request.customerId())
                .audience(List.of(audience))
                .claim("roles", roles)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        String token = encoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenResponse(token, expiresAt);
    }

    public record TokenRequest(
            @NotBlank
            @Pattern(regexp = "[a-z0-9-]{3,64}", message = "customerId must be 3-64 lowercase letters, digits or hyphens")
            String customerId,
            List<String> roles) {
    }

    public record TokenResponse(String accessToken, Instant expiresAt) {
    }
}
