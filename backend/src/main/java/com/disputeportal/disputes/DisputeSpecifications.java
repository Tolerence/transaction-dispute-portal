package com.disputeportal.disputes;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

final class DisputeSpecifications {

    private DisputeSpecifications() {
    }

    /** All disputes, optionally narrowed to one status. Used by reviewers. */
    static Specification<Dispute> withStatus(DisputeStatus status) {
        return (root, query, cb) -> status == null
                ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    static Specification<Dispute> forCustomer(String customerId, DisputeStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("customerId"), customerId));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
