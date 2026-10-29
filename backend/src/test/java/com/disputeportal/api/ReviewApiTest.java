package com.disputeportal.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewApiTest {

    private static final String CUSTOMER = "alice";
    private static final String OTHER_CUSTOMER = "carol";
    private static final String REVIEWER = "reviewer-1";
    private static final String NOTE = "Merchant confirmed the duplicate charge; refund is due.";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long aliceTransactionId;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM dispute_events");
        jdbc.update("DELETE FROM disputes");
        jdbc.update("DELETE FROM transactions");
        Timestamp bookedAt = Timestamp.from(Instant.now().minus(5, ChronoUnit.DAYS));
        aliceTransactionId = insertTransaction(CUSTOMER, "TXN-A-1", bookedAt);
        insertTransaction(OTHER_CUSTOMER, "TXN-C-1", bookedAt);
    }

    @Test
    void customerCannotUseTheReviewQueue() throws Exception {
        mvc.perform(get("/api/review/disputes").with(customer(CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void reviewerSeesAllCustomersOpenDisputesInTheQueue() throws Exception {
        submit(CUSTOMER, aliceTransactionId).andExpect(status().isCreated());

        mvc.perform(get("/api/review/disputes").param("status", "SUBMITTED").with(reviewer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].customerId").value(CUSTOMER));
    }

    @Test
    void decisionLifecycleIsRecordedAndHiddenReviewerIdentityFromCustomer() throws Exception {
        long disputeId = createDispute(CUSTOMER, aliceTransactionId);

        mvc.perform(post("/api/review/disputes/" + disputeId + "/start").with(reviewer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        mvc.perform(post("/api/review/disputes/" + disputeId + "/decision").with(reviewer())
                        .contentType("application/json")
                        .content("{\"decision\": \"ACCEPT\", \"note\": \"too short\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/review/disputes/" + disputeId + "/decision").with(reviewer())
                        .contentType("application/json")
                        .content("{\"decision\": \"ACCEPT\", \"note\": \"ok\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/review/disputes/" + disputeId + "/decision").with(reviewer())
                        .contentType("application/json")
                        .content("{\"decision\": \"ACCEPT\", \"note\": \"%s\"}".formatted(NOTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.events", hasSize(3)));

        mvc.perform(post("/api/review/disputes/" + disputeId + "/decision").with(reviewer())
                        .contentType("application/json")
                        .content("{\"decision\": \"REJECT\", \"note\": \"%s\"}".formatted(NOTE)))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/disputes/" + disputeId).with(customer(CUSTOMER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.events[*].actor", hasItems("CUSTOMER", "BANK")))
                .andExpect(content().string(not(containsString(REVIEWER))))
                .andExpect(jsonPath("$.customerId").doesNotExist());

        mvc.perform(get("/api/review/disputes/" + disputeId).with(reviewer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(CUSTOMER))
                .andExpect(jsonPath("$.events[*].actor", hasItems(REVIEWER)));
    }

    @Test
    void decisionBeforeReviewIsAConflict() throws Exception {
        long disputeId = createDispute(CUSTOMER, aliceTransactionId);

        mvc.perform(post("/api/review/disputes/" + disputeId + "/decision").with(reviewer())
                        .contentType("application/json")
                        .content("{\"decision\": \"REJECT\", \"note\": \"%s\"}".formatted(NOTE)))
                .andExpect(status().isConflict());
    }

    @Test
    void startingReviewTwiceIsAConflict() throws Exception {
        long disputeId = createDispute(CUSTOMER, aliceTransactionId);
        mvc.perform(post("/api/review/disputes/" + disputeId + "/start").with(reviewer()))
                .andExpect(status().isOk());

        mvc.perform(post("/api/review/disputes/" + disputeId + "/start").with(reviewer()))
                .andExpect(status().isConflict());
    }

    @Test
    void reviewerCannotReviewTheirOwnDispute() throws Exception {
        long disputeId = createDispute(REVIEWER, insertTransaction(REVIEWER, "TXN-R-1",
                Timestamp.from(Instant.now().minus(2, ChronoUnit.DAYS))));

        mvc.perform(post("/api/review/disputes/" + disputeId + "/start").with(reviewer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void reviewDecisionsRequireTheReviewerRole() throws Exception {
        long disputeId = createDispute(CUSTOMER, aliceTransactionId);

        mvc.perform(post("/api/review/disputes/" + disputeId + "/start").with(customer(OTHER_CUSTOMER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownDisputeIsNotFoundForReviewers() throws Exception {
        mvc.perform(get("/api/review/disputes/999999").with(reviewer()))
                .andExpect(status().isNotFound());
    }

    private long createDispute(String customerId, long transactionId) throws Exception {
        String location = submit(customerId, transactionId).andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private ResultActions submit(String customerId, long transactionId) throws Exception {
        return mvc.perform(post("/api/transactions/" + transactionId + "/disputes")
                .with(customer(customerId))
                .contentType("application/json")
                .content("""
                        {"reason": "DUPLICATE_CHARGE", "description": "Charged twice for the same purchase."}
                        """));
    }

    private long insertTransaction(String customerId, String reference, Timestamp bookedAt) {
        jdbc.update("""
                INSERT INTO transactions (customer_id, reference, merchant_name, amount, currency, status, booked_at)
                VALUES (?, ?, 'Test Merchant', 100.00, 'ZAR', 'POSTED', ?)
                """, customerId, reference, bookedAt);
        return jdbc.queryForObject("SELECT id FROM transactions WHERE reference = ?", Long.class, reference);
    }

    private static RequestPostProcessor customer(String subject) {
        return jwt().jwt((Jwt.Builder builder) -> builder.subject(subject));
    }

    private static RequestPostProcessor reviewer() {
        return jwt().jwt((Jwt.Builder builder) -> builder.subject(REVIEWER))
                .authorities(new SimpleGrantedAuthority("ROLE_DISPUTE_REVIEWER"));
    }
}
