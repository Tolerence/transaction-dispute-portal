package com.disputeportal.disputes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDisputeRequest(
        @NotNull(message = "reason is required")
        DisputeReason reason,

        @NotBlank(message = "description is required")
        @Size(min = 10, max = 1000, message = "description must be between 10 and 1000 characters")
        String description) {
}
