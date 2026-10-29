package com.disputeportal.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

    @Test
    void rejectsRequestsOverTheLimitWithProblemDetails() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(2, 10, false);

        for (int i = 0; i < 2; i++) {
            assertThat(send(filter, "10.0.0.1", "/api/transactions").getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse limited = send(filter, "10.0.0.1", "/api/transactions");

        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(limited.getHeader("Retry-After")).isEqualTo("60");
        assertThat(limited.getContentType()).isEqualTo("application/problem+json");
    }

    @Test
    void limitsEachClientSeparately() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 10, false);
        send(filter, "10.0.0.1", "/api/transactions");

        assertThat(send(filter, "10.0.0.2", "/api/transactions").getStatus()).isEqualTo(200);
    }

    @Test
    void doesNotLimitNonApiPaths() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 1, false);
        for (int i = 0; i < 5; i++) {
            assertThat(send(filter, "10.0.0.1", "/actuator/health").getStatus()).isEqualTo(200);
        }
    }

    @Test
    void tightLimitAppliesToDisputeSubmissions() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(100, 1, false);
        String path = "/api/transactions/7/disputes";

        assertThat(post(filter, "10.0.0.1", path).getStatus()).isEqualTo(200);
        assertThat(post(filter, "10.0.0.1", path).getStatus()).isEqualTo(429);
    }

    @Test
    void usesFirstForwardedAddressWhenTrusted() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 10, true);
        MockHttpServletRequest request = request("GET", "/api/transactions", "10.0.0.9");
        request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.9");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(send(filter, "203.0.113.5", "/api/transactions").getStatus()).isEqualTo(429);
    }

    private MockHttpServletResponse send(RateLimitFilter filter, String ip, String path) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("GET", path, ip), response, new MockFilterChain());
        return response;
    }

    private MockHttpServletResponse post(RateLimitFilter filter, String ip, String path) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("POST", path, ip), response, new MockFilterChain());
        return response;
    }

    private static MockHttpServletRequest request(String method, String path, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(ip);
        return request;
    }
}
