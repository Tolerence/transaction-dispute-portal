package com.disputeportal.disputes;

import com.disputeportal.config.DisputeProperties;
import com.disputeportal.transactions.Transaction;
import com.disputeportal.transactions.TransactionStatus;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Business rules that decide whether a transaction may be disputed.
 */
@Component
public class DisputePolicy {

    private final DisputeProperties properties;

    public DisputePolicy(DisputeProperties properties) {
        this.properties = properties;
    }

    /**
     * @return the reason the transaction cannot be disputed, or empty when it can
     */
    public Optional<String> ineligibilityReason(Transaction transaction, boolean hasOpenDispute,
                                                OffsetDateTime now) {
        if (hasOpenDispute) {
            return Optional.of("This transaction already has an open dispute");
        }
        if (transaction.getStatus() != TransactionStatus.POSTED) {
            return Optional.of("Only posted transactions can be disputed");
        }
        OffsetDateTime windowEnd = transaction.getBookedAt().plusDays(properties.windowDays());
        if (now.isAfter(windowEnd)) {
            return Optional.of("The %d-day dispute window for this transaction has closed"
                    .formatted(properties.windowDays()));
        }
        return Optional.empty();
    }
}
