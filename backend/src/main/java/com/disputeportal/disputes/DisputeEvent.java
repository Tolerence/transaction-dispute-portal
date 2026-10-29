package com.disputeportal.disputes;

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
import java.time.OffsetDateTime;

/**
 * Append-only audit trail entry for a dispute status change.
 */
@Entity
@Table(name = "dispute_events")
public class DisputeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dispute_id", nullable = false)
    private Dispute dispute;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private DisputeStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private DisputeStatus toStatus;

    @Column(nullable = false, length = 64)
    private String actor;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected DisputeEvent() {
    }

    public DisputeEvent(Dispute dispute, DisputeStatus fromStatus, DisputeStatus toStatus, String actor,
                        String note, OffsetDateTime createdAt) {
        this.dispute = dispute;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actor = actor;
        this.note = note;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public DisputeStatus getFromStatus() {
        return fromStatus;
    }

    public DisputeStatus getToStatus() {
        return toStatus;
    }

    public String getActor() {
        return actor;
    }

    public String getNote() {
        return note;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
