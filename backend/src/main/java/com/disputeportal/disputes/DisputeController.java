package com.disputeportal.disputes;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/disputes")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    /**
     * Full dispute history for the customer, including closed disputes. Filter by status to narrow it down.
     */
    @GetMapping
    public PagedModel<DisputeResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) DisputeStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<DisputeResponse> page = disputeService.list(jwt.getSubject(), status, pageable);
        return new PagedModel<>(page);
    }

    @GetMapping("/{id}")
    public DisputeResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return disputeService.get(jwt.getSubject(), id);
    }

    @PostMapping("/{id}/withdraw")
    public DisputeResponse withdraw(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return disputeService.withdraw(jwt.getSubject(), id);
    }
}
