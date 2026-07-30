package org.example.datn_sd69.modules.review.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.review.dto.request.CreateReviewRequest;
import org.example.datn_sd69.modules.review.service.CustomerReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/customer/reviews")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAuthority('USER')")
public class CustomerReviewController {

    private final CustomerReviewService customerReviewService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createReview(
            @Valid @ModelAttribute CreateReviewRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(customerReviewService.createReview(request));
    }

    @PatchMapping(value = "/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateReview(
            @PathVariable @Positive(message = "reviewId phải là số nguyên dương") Integer reviewId,
            @RequestParam @Min(value = 1, message = "Số sao đánh giá phải từ 1 đến 5")
            @Max(value = 5, message = "Số sao đánh giá phải từ 1 đến 5") Integer rating,
            @RequestParam(required = false) String comment,
            @RequestParam(name = "mediaFiles", required = false) List<MultipartFile> mediaFiles
    ) {
        return ResponseEntity.ok(
                customerReviewService.updateReview(reviewId, rating, comment, mediaFiles)
        );
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