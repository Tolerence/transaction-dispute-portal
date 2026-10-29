package com.disputeportal.disputes;

import com.disputeportal.transactions.Transaction;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * A dispute as returned by the API.
 *
 * <p>{@code events} is populated on detail views only. {@code customerId} is populated only on the reviewer view.
 * Customers see reviewer actions as {@code BANK} rather than a reviewer's identity.
 */
public record DisputeResponse(
        Long id,
        String customerId,
        TransactionSummary transaction,
        DisputeReason reason,
        String description,
        DisputeStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime closedAt,
        List<EventResponse> events) {

    public record TransactionSummary(
            Long id,
            String reference,
            String merchantName,
            BigDecimal amount,
            String currency,
            OffsetDateTime bookedAt) {
    }

    public record EventResponse(
            DisputeStatus fromStatus,
            DisputeStatus toStatus,
            String actor,
            String note,
            OffsetDateTime createdAt) {
    }

    public static DisputeResponse summary(Dispute dispute) {
        return build(dispute, null, false, false);
    }

    /** Queue entry for reviewers: includes the customer ID, no timeline. */
    public static DisputeResponse reviewerSummary(Dispute dispute) {
        return build(dispute, null, true, false);
    }

    /** What the customer sees: their own actions are labelled CUSTOMER, everything else BANK. */
    public static DisputeResponse customerView(Dispute dispute, List<DisputeEvent> events) {
        return build(dispute, events, false, true);
    }

    /** What an operations reviewer sees: full audit trail, including the acting reviewer. */
    public static DisputeResponse reviewerView(Dispute dispute, List<DisputeEvent> events) {
        return build(dispute, events, true, true);
    }

    private static DisputeResponse build(Dispute dispute, List<DisputeEvent> events, boolean reviewer,
                                         boolean withEvents) {
        Transaction tx = dispute.getTransaction();
        List<EventResponse> mapped = withEvents
                ? events.stream().map(e -> toEvent(e, dispute.getCustomerId(), reviewer)).toList()
                : null;
        return new DisputeResponse(
                dispute.getId(),
                reviewer ? dispute.getCustomerId() : null,
                new TransactionSummary(tx.getId(), tx.getReference(), tx.getMerchantName(), tx.getAmount(),
                        tx.getCurrency(), tx.getBookedAt()),
                dispute.getReason(),
                dispute.getDescription(),
                dispute.getStatus(),
                dispute.getCreatedAt(),
                dispute.getUpdatedAt(),
                dispute.getClosedAt(),
                mapped);
    }

    private static EventResponse toEvent(DisputeEvent event, String customerId, boolean reviewer) {
        String actor = reviewer
                ? event.getActor()
                : (event.getActor().equals(customerId) ? "CUSTOMER" : "BANK");
        return new EventResponse(event.getFromStatus(), event.getToStatus(), actor, event.getNote(),
                event.getCreatedAt());
    }
}
