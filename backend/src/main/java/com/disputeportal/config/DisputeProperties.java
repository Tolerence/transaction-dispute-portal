package com.disputeportal.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.disputes")
public record DisputeProperties(@Min(1) int windowDays) {
}
