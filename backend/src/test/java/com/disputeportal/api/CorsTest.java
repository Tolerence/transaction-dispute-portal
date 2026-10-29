package com.disputeportal.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Browsers send an Origin header on every POST, including same-origin ones. An origin that is not in
 * CORS_ALLOWED_ORIGINS must be refused, and an allowed one must pass.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void allowedOriginIsAcceptedOnPost() throws Exception {
        mvc.perform(post("/api/transactions/1/disputes")
                        .with(jwt().jwt(j -> j.subject("alice")))
                        .header("Origin", "http://localhost:5173")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void unlistedOriginIsRefused() throws Exception {
        mvc.perform(get("/api/transactions")
                        .with(jwt().jwt(j -> j.subject("alice")))
                        .header("Origin", "https://evil.example"))
                .andExpect(status().isForbidden());
    }
}
