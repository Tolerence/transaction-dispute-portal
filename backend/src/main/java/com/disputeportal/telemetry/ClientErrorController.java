package com.disputeportal.telemetry;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives errors caught in the browser (crashes, unhandled promise rejections). Requires a signed-in user,
 * so anonymous traffic cannot fill the logs, and is covered by the API rate limit.
 */
@RestController
@RequestMapping("/api/client-errors")
public class ClientErrorController {

    private static final Logger LOG = LoggerFactory.getLogger("client.error");

    private final MeterRegistry registry;

    public ClientErrorController(MeterRegistry registry) {
        this.registry = registry;
        // Create each series at startup so it reads 0 (not "no data") before the first error.
        for (String source : List.of("error-boundary", "window-error", "unhandled-rejection")) {
            registry.counter("frontend.errors", "source", source);
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void report(@Valid @RequestBody ClientErrorReport report) {
        registry.counter("frontend.errors", "source", report.source()).increment();
        LOG.warn("source={} route={} message={}", report.source(), report.route(), report.message());
    }
}
