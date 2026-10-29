package com.disputeportal.transactions;

import jakarta.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    static Specification<Transaction> forCustomer(String customerId, TransactionSearch search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("customerId"), customerId));
            if (search.status() != null) {
                predicates.add(cb.equal(root.get("status"), search.status()));
            }
            if (search.from() != null) {
                OffsetDateTime start = search.from().atStartOfDay().atOffset(ZoneOffset.UTC);
                predicates.add(cb.greaterThanOrEqualTo(root.<OffsetDateTime>get("bookedAt"), start));
            }
            if (search.to() != null) {
                OffsetDateTime endExclusive = search.to().plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
                predicates.add(cb.lessThan(root.<OffsetDateTime>get("bookedAt"), endExclusive));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
