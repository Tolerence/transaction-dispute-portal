package com.disputeportal.transactions;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * @param ineligibilityReason why the transaction cannot be disputed right now, or null when it can
 */
public record TransactionResponse(
        Long id,
        String reference,
        String merchantName,
        BigDecimal amount,
        String currency,
        TransactionStatus status,
        OffsetDateTime bookedAt,
        boolean disputable,
        String ineligibilityReason) {

    public static TransactionResponse of(Transaction transaction, String ineligibilityReason) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getReference(),
                transaction.getMerchantName(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                transaction.getBookedAt(),
                ineligibilityReason == null,
                ineligibilityReason);
    }
}
