package com.disputeportal.disputes;

import com.disputeportal.common.BusinessException;
import com.disputeportal.transactions.Transaction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "disputes")
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DisputeReason reason;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisputeStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Version
    private long version;

    protected Dispute() {
    }

    public Dispute(Transaction transaction, String customerId, DisputeReason reason, String description,
                   OffsetDateTime now) {
        this.transaction = transaction;
        this.customerId = customerId;
        this.reason = reason;
        this.description = description;
        this.status = DisputeStatus.SUBMITTED;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Moves the dispute to {@code next} and returns the audit event describing the change.
     */
    public DisputeEvent transitionTo(DisputeStatus next, String actor, String note, OffsetDateTime now) {
        if (!status.canTransitionTo(next)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "A dispute in status %s cannot be changed to %s".formatted(status, next));
        }
        DisputeStatus previous = status;
        status = next;
        updatedAt = now;
        if (!next.isOpen()) {
            closedAt = now;
        }
        return new DisputeEvent(this, previous, next, actor, note, now);
    }

    public Long getId() {
        return id;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public String getCustomerId() {
        return customerId;
    }

    public DisputeReason getReason() {
        return reason;
    }

    public String getDescription() {
        return description;
    }

    public DisputeStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }
}
