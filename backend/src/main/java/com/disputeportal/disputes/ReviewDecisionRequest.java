package com.disputeportal.disputes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The note is recorded in the dispute timeline and shown to the customer, so it must be written
 * as customer-facing text.
 */
public record ReviewDecisionRequest(
        @NotNull(message = "decision is required")
        ReviewDecision decision,

        @NotBlank(message = "note is required")
        @Size(min = 10, max = 500, message = "note must be between 10 and 500 characters")
        String note) {
}
