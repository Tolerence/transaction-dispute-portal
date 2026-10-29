package com.disputeportal.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.oauth2.jwt.Jwt;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DisputeApiTest {

    private static final String VALID_DESCRIPTION = "I did not authorise this card payment.";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long aliceTransactionId;
    private long bobTransactionId;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM dispute_events");
        jdbc.update("DELETE FROM disputes");
        jdbc.update("DELETE FROM transactions");

        Timestamp bookedAt = Timestamp.from(Instant.now().minus(5, ChronoUnit.DAYS));
        aliceTransactionId = insertTransaction("alice", "TXN-A-1", bookedAt);
        bobTransactionId = insertTransaction("bob", "TXN-B-1", bookedAt);
    }

    @Test
    void listingRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerOnlySeesOwnTransactions() throws Exception {
        mvc.perform(get("/api/transactions").with(customer("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].reference").value("TXN-A-1"))
                .andExpect(jsonPath("$.content[0].disputable").value(true));
    }

    @Test
    void anotherCustomersTransactionIsNotFound() throws Exception {
        mvc.perform(get("/api/transactions/" + bobTransactionId).with(customer("alice")))
                .andExpect(status().isNotFound());
    }

    @Test
    void customerCanDisputeAndSeeItInHistory() throws Exception {
        mvc.perform(post("/api/transactions/" + aliceTransactionId + "/disputes")
                        .with(customer("alice"))
                        .contentType("application/json")
                        .content("""
                                {"reason": "UNAUTHORISED_TRANSACTION", "description": "%s"}
                                """.formatted(VALID_DESCRIPTION)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.events", hasSize(1)));

        mvc.perform(get("/api/disputes").with(customer("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].transaction.reference").value("TXN-A-1"));

        mvc.perform(get("/api/transactions/" + aliceTransactionId).with(customer("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disputable").value(false))
                .andExpect(jsonPath("$.ineligibilityReason").value("This transaction already has an open dispute"));
    }

    @Test
    void secondOpenDisputeOnSameTransactionIsRejected() throws Exception {
        submitDispute("alice", aliceTransactionId).andExpect(status().isCreated());

        submitDispute("alice", aliceTransactionId)
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void withdrawnDisputeAllowsANewOne() throws Exception {
        String location = submitDispute("alice", aliceTransactionId)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(post(location + "/withdraw").with(customer("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"))
                .andExpect(jsonPath("$.closedAt").isNotEmpty())
                .andExpect(jsonPath("$.events", hasSize(2)));

        submitDispute("alice", aliceTransactionId).andExpect(status().isCreated());

        mvc.perform(get("/api/disputes").param("status", "WITHDRAWN").with(customer("alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void invalidDescriptionIsRejectedWithFieldErrors() throws Exception {
        mvc.perform(post("/api/transactions/" + aliceTransactionId + "/disputes")
                        .with(customer("alice"))
                        .contentType("application/json")
                        .content("{\"reason\": \"OTHER\", \"description\": \"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").exists());
    }

    @Test
    void disputingAnotherCustomersTransactionIsNotFound() throws Exception {
        submitDispute("alice", bobTransactionId)
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownSortPropertyIsBadRequest() throws Exception {
        mvc.perform(get("/api/transactions").param("sort", "password").with(customer("alice")))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.ResultActions submitDispute(String customerId,
                                                                             long transactionId) throws Exception {
        return mvc.perform(post("/api/transactions/" + transactionId + "/disputes")
                .with(customer(customerId))
                .contentType("application/json")
                .content("""
                        {"reason": "UNAUTHORISED_TRANSACTION", "description": "%s"}
                        """.formatted(VALID_DESCRIPTION)));
    }

    private long insertTransaction(String customerId, String reference, Timestamp bookedAt) {
        jdbc.update("""
                INSERT INTO transactions (customer_id, reference, merchant_name, amount, currency, status, booked_at)
                VALUES (?, ?, 'Test Merchant', 100.00, 'ZAR', 'POSTED', ?)
                """, customerId, reference, bookedAt);
        return jdbc.queryForObject("SELECT id FROM transactions WHERE reference = ?", Long.class, reference);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor customer(String subject) {
        return jwt().jwt((Jwt.Builder builder) -> builder.subject(subject));
    }
}
