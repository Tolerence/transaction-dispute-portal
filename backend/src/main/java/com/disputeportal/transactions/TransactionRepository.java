package com.disputeportal.transactions;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndCustomerId(Long id, String customerId);

    /**
     * Locks the transaction row so that two concurrent dispute submissions cannot both pass the
     * "no open dispute" check for the same transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Transaction t where t.id = :id and t.customerId = :customerId")
    Optional<Transaction> findForUpdate(@Param("id") Long id, @Param("customerId") String customerId);
}
