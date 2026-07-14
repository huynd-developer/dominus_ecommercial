package org.example.datn_sd69.modules.review.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.review.service.PublicProductReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop/products")
@RequiredArgsConstructor
@Validated
public class PublicProductReviewController {

    private final PublicProductReviewService publicProductReviewService;

    @GetMapping("/{productId}/reviews")
    public ResponseEntity<?> getProductReviews(
            @PathVariable @Positive(message = "productId phải là số nguyên dương") Integer productId
    ) {
        return ResponseEntity.ok(publicProductReviewService.getReviewsByProduct(productId));
    }

    @GetMapping("/{productId}/reviews/summary")
    public ResponseEntity<?> getProductReviewSummary(
            @PathVariable @Positive(message = "productId phải là số nguyên dương") Integer productId
    ) {
        return ResponseEntity.ok(publicProductReviewService.getReviewSummary(productId));
    }
}