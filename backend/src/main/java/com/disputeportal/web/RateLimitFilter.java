package com.disputeportal.web;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-client limits on the API. Runs before authentication, so unauthenticated floods are limited too.
 * Dispute submissions have a tighter limit of their own. Buckets are held in a bounded, expiring cache.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Pattern DISPUTE_SUBMIT = Pattern.compile("^/api/transactions/\\d+/disputes$");
    private static final String API_PREFIX = "/api/";

    private final int apiPerMinute;
    private final int disputePerMinute;
    private final boolean trustForwardedFor;
    private final Cache<String, Bucket> apiBuckets = newCache();
    private final Cache<String, Bucket> disputeBuckets = newCache();

    public RateLimitFilter(
            @Value("${app.rate-limit.api-per-minute}") int apiPerMinute,
            @Value("${app.rate-limit.dispute-submit-per-minute}") int disputePerMinute,
            @Value("${app.rate-limit.trust-forwarded-for}") boolean trustForwardedFor) {
        this.apiPerMinute = apiPerMinute;
        this.disputePerMinute = disputePerMinute;
        this.trustForwardedFor = trustForwardedFor;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(API_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String client = clientAddress(request);

        if (!apiBuckets.get(client, k -> bucket(apiPerMinute)).tryConsume(1)) {
            reject(response);
            return;
        }
        if (isDisputeSubmission(request)
                && !disputeBuckets.get(client, k -> bucket(disputePerMinute)).tryConsume(1)) {
            reject(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isDisputeSubmission(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && DISPUTE_SUBMIT.matcher(request.getRequestURI()).matches();
    }

    private String clientAddress(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", "60");
        response.setContentType("application/problem+json");
        response.getWriter().write("""
                {"type":"about:blank","title":"Too Many Requests","status":429,\
                "detail":"Too many requests. Try again in a minute."}""");
    }

    private static Bucket bucket(int perMinute) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(perMinute).refillGreedy(perMinute, Duration.ofMinutes(1)).build())
                .build();
    }

    private static Cache<String, Bucket> newCache() {
        return Caffeine.newBuilder()
                .maximumSize(100_000)
                .expireAfterAccess(Duration.ofMinutes(10))
                .build();
    }
}
