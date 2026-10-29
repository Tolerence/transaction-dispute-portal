package com.disputeportal.disputes;

import com.disputeportal.common.BusinessException;
import com.disputeportal.transactions.Transaction;
import com.disputeportal.transactions.TransactionRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DisputeService {

    private final TransactionRepository transactions;
    private final DisputeRepository disputes;
    private final DisputeEventRepository events;
    private final DisputePolicy policy;
    private final Clock clock;

    public DisputeService(TransactionRepository transactions, DisputeRepository disputes,
                          DisputeEventRepository events, DisputePolicy policy, Clock clock) {
        this.transactions = transactions;
        this.disputes = disputes;
        this.events = events;
        this.policy = policy;
        this.clock = clock;
    }

    @Transactional
    public DisputeResponse submit(String customerId, long transactionId, CreateDisputeRequest request) {
        Transaction transaction = transactions.findForUpdate(transactionId, customerId)
                .orElseThrow(() -> BusinessException.notFound("Transaction not found"));
        OffsetDateTime now = OffsetDateTime.now(clock);

        boolean hasOpenDispute = disputes.existsByTransactionIdAndStatusIn(transaction.getId(), DisputeStatus.OPEN);
        policy.ineligibilityReason(transaction, hasOpenDispute, now)
                .ifPresent(reason -> {
                    throw BusinessException.unprocessable(reason);
                });

        Dispute dispute = new Dispute(transaction, customerId, request.reason(),
                request.description().strip(), now);
        disputes.save(dispute);
        DisputeEvent submitted = events.save(
                new DisputeEvent(dispute, null, DisputeStatus.SUBMITTED, customerId, null, now));
        return DisputeResponse.customerView(dispute, List.of(submitted));
    }

    @Transactional(readOnly = true)
    public Page<DisputeResponse> list(String customerId, DisputeStatus status, Pageable pageable) {
        return disputes.findAll(DisputeSpecifications.forCustomer(customerId, status), pageable)
                .map(DisputeResponse::summary);
    }

    @Transactional(readOnly = true)
    public DisputeResponse get(String customerId, long disputeId) {
        Dispute dispute = findOwned(customerId, disputeId);
        return DisputeResponse.customerView(dispute, events.findByDisputeIdOrderByCreatedAtAscIdAsc(disputeId));
    }

    @Transactional
    public DisputeResponse withdraw(String customerId, long disputeId) {
        Dispute dispute = findOwned(customerId, disputeId);
        DisputeEvent event = dispute.transitionTo(DisputeStatus.WITHDRAWN, customerId,
                "Withdrawn by customer", OffsetDateTime.now(clock));
        events.save(event);
        return DisputeResponse.customerView(dispute, events.findByDisputeIdOrderByCreatedAtAscIdAsc(disputeId));
    }

    private Dispute findOwned(String customerId, long disputeId) {
        return disputes.findByIdAndCustomerId(disputeId, customerId)
                .orElseThrow(() -> BusinessException.notFound("Dispute not found"));
    }
}
