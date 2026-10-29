package com.disputeportal.transactions;

import com.disputeportal.common.BusinessException;
import com.disputeportal.disputes.CreateDisputeRequest;
import com.disputeportal.disputes.DisputeResponse;
import com.disputeportal.disputes.DisputeService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final DisputeService disputeService;

    public TransactionController(TransactionService transactionService, DisputeService disputeService) {
        this.transactionService = transactionService;
        this.disputeService = disputeService;
    }

    @GetMapping
    public PagedModel<TransactionResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "bookedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TransactionResponse> page = transactionService.list(
                jwt.getSubject(), new TransactionSearch(status, from, to), pageable);
        return new PagedModel<>(page);
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return transactionService.get(jwt.getSubject(), id);
    }

    @PostMapping("/{id}/disputes")
    public ResponseEntity<DisputeResponse> dispute(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long id,
            @Valid @RequestBody CreateDisputeRequest request) {
        DisputeResponse created = disputeService.submit(jwt.getSubject(), id, request);
        return ResponseEntity.created(URI.create("/api/disputes/" + created.id())).body(created);
    }
}
