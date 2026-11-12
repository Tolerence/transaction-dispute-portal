package com.disputeportal.telemetry;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClientErrorControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void signedInUserCanReportAnError() throws Exception {
        mvc.perform(post("/api/client-errors")
                        .with(jwt().jwt(j -> j.subject("alice")))
                        .contentType("application/json")
                        .content("{\"source\":\"window-error\",\"message\":\"boom\",\"route\":\"/transactions\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    void anonymousReportIsRefused() throws Exception {
        mvc.perform(post("/api/client-errors")
                        .contentType("application/json")
                        .content("{\"source\":\"window-error\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownSourceIsRejected() throws Exception {
        mvc.perform(post("/api/client-errors")
                        .with(jwt().jwt(j -> j.subject("alice")))
                        .contentType("application/json")
                        .content("{\"source\":\"made-up\"}"))
                .andExpect(status().isBadRequest());
    }
}
