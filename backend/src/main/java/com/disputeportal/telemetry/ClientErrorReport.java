package com.disputeportal.telemetry;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * An error caught in the web app. The source is restricted to a fixed list so it is safe to use as a metric tag.
 */
public record ClientErrorReport(
        @NotBlank
        @Pattern(regexp = "error-boundary|window-error|unhandled-rejection",
                message = "source must be error-boundary, window-error or unhandled-rejection")
        String source,

        @Size(max = 1000)
        String message,

        @Size(max = 300)
        String route) {
}
