package com.disputeportal.transactions;

import com.disputeportal.common.BusinessException;
import com.disputeportal.disputes.DisputePolicy;
import com.disputeportal.disputes.DisputeRepository;
import com.disputeportal.disputes.DisputeStatus;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final TransactionRepository transactions;
    private final DisputeRepository disputes;
    private final DisputePolicy policy;
    private final Clock clock;

    public TransactionService(TransactionRepository transactions, DisputeRepository disputes,
                              DisputePolicy policy, Clock clock) {
        this.transactions = transactions;
        this.disputes = disputes;
        this.policy = policy;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> list(String customerId, TransactionSearch search, Pageable pageable) {
        if (search.from() != null && search.to() != null && search.from().isAfter(search.to())) {
            throw BusinessException.badRequest("'from' must not be after 'to'");
        }
        Page<Transaction> page = transactions.findAll(
                TransactionSpecifications.forCustomer(customerId, search), pageable);

        List<Long> ids = page.getContent().stream().map(Transaction::getId).toList();
        Set<Long> disputed = openDisputedTransactionIds(customerId, ids);
        OffsetDateTime now = OffsetDateTime.now(clock);

        return page.map(tx -> TransactionResponse.of(tx,
                policy.ineligibilityReason(tx, disputed.contains(tx.getId()), now).orElse(null)));
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(String customerId, long id) {
        Transaction transaction = transactions.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> BusinessException.notFound("Transaction not found"));
        boolean disputed = !openDisputedTransactionIds(customerId, List.of(id)).isEmpty();
        OffsetDateTime now = OffsetDateTime.now(clock);
        return TransactionResponse.of(transaction,
                policy.ineligibilityReason(transaction, disputed, now).orElse(null));
    }

    private Set<Long> openDisputedTransactionIds(String customerId, Collection<Long> transactionIds) {
        if (transactionIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(disputes.findTransactionIdsWithDisputes(
                customerId, transactionIds, DisputeStatus.OPEN));
    }
}
