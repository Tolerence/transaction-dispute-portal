package com.disputeportal.disputes;

import com.disputeportal.common.BusinessException;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operations review of disputes. Reviewers may act on any customer's dispute except their own, and every
 * decision is written to the dispute timeline and to the audit log.
 */
@Service
public class DisputeReviewService {

    private static final Logger AUDIT = LoggerFactory.getLogger("audit.dispute-review");

    private final DisputeRepository disputes;
    private final DisputeEventRepository events;
    private final DisputeMetrics metrics;
    private final Clock clock;

    public DisputeReviewService(DisputeRepository disputes, DisputeEventRepository events,
                                DisputeMetrics metrics, Clock clock) {
        this.disputes = disputes;
        this.events = events;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<DisputeResponse> queue(DisputeStatus status, Pageable pageable) {
        return disputes.findAll(DisputeSpecifications.withStatus(status), pageable)
                .map(DisputeResponse::reviewerSummary);
    }

    @Transactional(readOnly = true)
    public DisputeResponse get(long disputeId) {
        Dispute dispute = disputes.findById(disputeId)
                .orElseThrow(() -> BusinessException.notFound("Dispute not found"));
        return DisputeResponse.reviewerView(dispute, events.findByDisputeIdOrderByCreatedAtAscIdAsc(disputeId));
    }

    @Transactional
    public DisputeResponse startReview(String reviewerId, long disputeId) {
        Dispute dispute = lockAndCheck(reviewerId, disputeId);
        OffsetDateTime now = OffsetDateTime.now(clock);
        DisputeEvent event = dispute.transitionTo(DisputeStatus.UNDER_REVIEW, reviewerId, null, now);
        events.save(event);
        metrics.reviewStarted();
        AUDIT.info("action=start_review disputeId={} reviewer={} from={} to={}",
                disputeId, reviewerId, event.getFromStatus(), event.getToStatus());
        return reviewerResponse(dispute, disputeId);
    }

    @Transactional
    public DisputeResponse decide(String reviewerId, long disputeId, ReviewDecision decision, String note) {
        Dispute dispute = lockAndCheck(reviewerId, disputeId);
        DisputeStatus target = decision == ReviewDecision.ACCEPT ? DisputeStatus.ACCEPTED : DisputeStatus.REJECTED;
        OffsetDateTime now = OffsetDateTime.now(clock);
        DisputeEvent event = dispute.transitionTo(target, reviewerId, note.strip(), now);
        events.save(event);
        metrics.decided(target);
        AUDIT.info("action=decide disputeId={} reviewer={} decision={} from={} to={}",
                disputeId, reviewerId, decision, event.getFromStatus(), event.getToStatus());
        return reviewerResponse(dispute, disputeId);
    }

    private Dispute lockAndCheck(String reviewerId, long disputeId) {
        Dispute dispute = disputes.findForUpdate(disputeId)
                .orElseThrow(() -> BusinessException.notFound("Dispute not found"));
        if (dispute.getCustomerId().equals(reviewerId)) {
            metrics.selfReviewBlocked();
            AUDIT.warn("action=self_review_blocked disputeId={} reviewer={}", disputeId, reviewerId);
            throw BusinessException.forbidden("Reviewers cannot review their own disputes");
        }
        return dispute;
    }

    private DisputeResponse reviewerResponse(Dispute dispute, long disputeId) {
        return DisputeResponse.reviewerView(dispute, events.findByDisputeIdOrderByCreatedAtAscIdAsc(disputeId));
    }
}
