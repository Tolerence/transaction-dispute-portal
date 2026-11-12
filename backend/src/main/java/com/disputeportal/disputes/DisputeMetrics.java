package com.disputeportal.disputes;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Business counters for disputes. Names are stable so Grafana panels and alert rules keep working.
 * Counters only record counts and outcomes, never customer or dispute details.
 */
@Component
public class DisputeMetrics {

    private final Counter submitted;
    private final Counter submissionRejected;
    private final Counter withdrawn;
    private final Counter reviewStarted;
    private final Counter accepted;
    private final Counter rejected;
    private final Counter selfReviewBlocked;

    public DisputeMetrics(MeterRegistry registry, DisputeRepository disputes) {
        this.submitted = Counter.builder("disputes.submitted")
                .description("Disputes submitted by customers").register(registry);
        this.submissionRejected = Counter.builder("disputes.submission.rejected")
                .description("Dispute submissions refused by the eligibility rules").register(registry);
        this.withdrawn = Counter.builder("disputes.withdrawn")
                .description("Disputes withdrawn by customers").register(registry);
        this.reviewStarted = Counter.builder("disputes.review.started")
                .description("Disputes a reviewer started reviewing").register(registry);
        this.accepted = Counter.builder("disputes.decisions")
                .tag("outcome", "accepted").description("Review decisions by outcome").register(registry);
        this.rejected = Counter.builder("disputes.decisions")
                .tag("outcome", "rejected").description("Review decisions by outcome").register(registry);
        this.selfReviewBlocked = Counter.builder("disputes.review.self_review_blocked")
                .description("Reviewer attempts to review their own dispute, refused").register(registry);

        Gauge.builder("disputes.open", disputes, repository -> repository.countByStatusIn(DisputeStatus.OPEN))
                .description("Disputes waiting for action (submitted or under review)")
                .register(registry);
    }

    public void submitted() {
        submitted.increment();
    }

    public void submissionRejected() {
        submissionRejected.increment();
    }

    public void withdrawn() {
        withdrawn.increment();
    }

    public void reviewStarted() {
        reviewStarted.increment();
    }

    public void decided(DisputeStatus outcome) {
        if (outcome == DisputeStatus.ACCEPTED) {
            accepted.increment();
        } else {
            rejected.increment();
        }
    }

    public void selfReviewBlocked() {
        selfReviewBlocked.increment();
    }
}
