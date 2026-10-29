package com.disputeportal.transactions;

import java.time.LocalDate;

/**
 * Optional filters for listing transactions. Dates are inclusive and interpreted in UTC.
 */
public record TransactionSearch(TransactionStatus status, LocalDate from, LocalDate to) {
}
