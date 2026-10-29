package com.disputeportal.disputes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.disputeportal.config.DisputeProperties;
import com.disputeportal.transactions.Transaction;
import com.disputeportal.transactions.TransactionStatus;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DisputePolicyTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-29T10:00:00Z");

    private DisputePolicy policy;

    @BeforeEach
    void setUp() {
        policy = new DisputePolicy(new DisputeProperties(120));
    }

    @Test
    void postedTransactionInsideWindowIsDisputable() {
        Transaction tx = transaction(TransactionStatus.POSTED, NOW.minusDays(30));

        assertThat(policy.ineligibilityReason(tx, false, NOW)).isEmpty();
    }

    @Test
    void transactionWithOpenDisputeIsNotDisputable() {
        Transaction tx = transaction(TransactionStatus.POSTED, NOW.minusDays(1));

        assertThat(policy.ineligibilityReason(tx, true, NOW)).hasValueSatisfying(
                reason -> assertThat(reason).contains("open dispute"));
    }

    @Test
    void pendingAndReversedTransactionsAreNotDisputable() {
        assertThat(policy.ineligibilityReason(transaction(TransactionStatus.PENDING, NOW), false, NOW))
                .isPresent();
        assertThat(policy.ineligibilityReason(transaction(TransactionStatus.REVERSED, NOW), false, NOW))
                .isPresent();
    }

    @Test
    void transactionOlderThanWindowIsNotDisputable() {
        Transaction tx = transaction(TransactionStatus.POSTED, NOW.minusDays(121));

        assertThat(policy.ineligibilityReason(tx, false, NOW)).hasValueSatisfying(
                reason -> assertThat(reason).contains("120-day"));
    }

    private static Transaction transaction(TransactionStatus status, OffsetDateTime bookedAt) {
        Transaction tx = mock(Transaction.class);
        when(tx.getStatus()).thenReturn(status);
        when(tx.getBookedAt()).thenReturn(bookedAt);
        return tx;
    }
}
