package com.disputeportal.disputes;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DisputeRepository extends JpaRepository<Dispute, Long>, JpaSpecificationExecutor<Dispute> {

    boolean existsByTransactionIdAndStatusIn(Long transactionId, Collection<DisputeStatus> statuses);

    @EntityGraph(attributePaths = "transaction")
    Optional<Dispute> findByIdAndCustomerId(Long id, String customerId);

    /** Locks the dispute for a review decision so concurrent reviewer actions are serialised. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "transaction")
    @Query("select d from Dispute d where d.id = :id")
    Optional<Dispute> findForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = "transaction")
    @Override
    Page<Dispute> findAll(Specification<Dispute> spec, Pageable pageable);

    @Query("""
            select d.transaction.id from Dispute d
            where d.customerId = :customerId
              and d.transaction.id in :transactionIds
              and d.status in :statuses
            """)
    List<Long> findTransactionIdsWithDisputes(@Param("customerId") String customerId,
                                              @Param("transactionIds") Collection<Long> transactionIds,
                                              @Param("statuses") Collection<DisputeStatus> statuses);
}
