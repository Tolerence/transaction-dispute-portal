package com.disputeportal.disputes;

import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operations review endpoints. Access requires the DISPUTE_REVIEWER role; see SecurityConfig.
 */
@RestController
@RequestMapping("/api/review/disputes")
public class ReviewController {

    private final DisputeReviewService review;

    public ReviewController(DisputeReviewService review) {
        this.review = review;
    }

    /** Review queue, oldest first by default. Filter by status to see only open work. */
    @GetMapping
    public PagedModel<DisputeResponse> queue(
            @RequestParam(required = false) DisputeStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<DisputeResponse> page = review.queue(status, pageable);
        return new PagedModel<>(page);
    }

    @GetMapping("/{id}")
    public DisputeResponse get(@PathVariable long id) {
        return review.get(id);
    }

    @PostMapping("/{id}/start")
    public DisputeResponse startReview(@AuthenticationPrincipal Jwt reviewer, @PathVariable long id) {
        return review.startReview(reviewer.getSubject(), id);
    }

    @PostMapping("/{id}/decision")
    public DisputeResponse decide(@AuthenticationPrincipal Jwt reviewer,
                                  @PathVariable long id,
                                  @Valid @RequestBody ReviewDecisionRequest request) {
        return review.decide(reviewer.getSubject(), id, request.decision(), request.note());
    }
}
