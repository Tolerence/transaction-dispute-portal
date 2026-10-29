package com.disputeportal.disputes;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DisputeStatusTest {

    @Test
    void customerCanWithdrawAnOpenDispute() {
        assertThat(DisputeStatus.SUBMITTED.canTransitionTo(DisputeStatus.WITHDRAWN)).isTrue();
        assertThat(DisputeStatus.UNDER_REVIEW.canTransitionTo(DisputeStatus.WITHDRAWN)).isTrue();
    }

    @Test
    void closedDisputesCannotMoveAnywhere() {
        for (DisputeStatus closed : new DisputeStatus[] {
                DisputeStatus.ACCEPTED, DisputeStatus.REJECTED, DisputeStatus.WITHDRAWN}) {
            for (DisputeStatus next : DisputeStatus.values()) {
                assertThat(closed.canTransitionTo(next)).as("%s -> %s", closed, next).isFalse();
            }
        }
    }

    @Test
    void submittedCannotBeDecidedWithoutReview() {
        assertThat(DisputeStatus.SUBMITTED.canTransitionTo(DisputeStatus.ACCEPTED)).isFalse();
        assertThat(DisputeStatus.SUBMITTED.canTransitionTo(DisputeStatus.REJECTED)).isFalse();
    }

    @Test
    void openStatusesAreSubmittedAndUnderReview() {
        assertThat(DisputeStatus.OPEN).containsExactlyInAnyOrder(DisputeStatus.SUBMITTED, DisputeStatus.UNDER_REVIEW);
        assertThat(DisputeStatus.ACCEPTED.isOpen()).isFalse();
    }
}
