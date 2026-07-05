package org.example.datn_sd69.modules.review.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.review.dto.request.CreateReviewRequest;
import org.example.datn_sd69.modules.review.service.CustomerReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/reviews")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAuthority('USER')")
public class CustomerReviewController {

    private final CustomerReviewService customerReviewService;

    @PostMapping
    public ResponseEntity<?> createReview(
            @Valid @RequestBody CreateReviewRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(customerReviewService.createReview(request));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyReviews() {
        return ResponseEntity.ok(customerReviewService.getMyReviews());
    }

    @GetMapping("/orders/{orderId}/items")
    public ResponseEntity<?> getReviewableItemsByOrder(
            @PathVariable @Positive(message = "orderId phải là số nguyên dương") Integer orderId
    ) {
        return ResponseEntity.ok(customerReviewService.getReviewableItemsByOrder(orderId));
    }
}
