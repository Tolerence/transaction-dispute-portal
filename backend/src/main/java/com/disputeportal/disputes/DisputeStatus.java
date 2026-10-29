package com.disputeportal.disputes;

import java.util.List;

public enum DisputeStatus {
    SUBMITTED,
    UNDER_REVIEW,
    ACCEPTED,
    REJECTED,
    WITHDRAWN;

    /** Statuses in which a dispute still needs action and blocks a second dispute on the same transaction. */
    public static final List<DisputeStatus> OPEN = List.of(SUBMITTED, UNDER_REVIEW);

    public boolean isOpen() {
        return OPEN.contains(this);
    }

    public boolean canTransitionTo(DisputeStatus next) {
        return switch (this) {
            case SUBMITTED -> next == UNDER_REVIEW || next == WITHDRAWN;
            case UNDER_REVIEW -> next == ACCEPTED || next == REJECTED || next == WITHDRAWN;
            case ACCEPTED, REJECTED, WITHDRAWN -> false;
        };
    }
}
