package org.example.datn_sd69.modules.adminReview.controller;

import jakarta.validation.Valid;
import org.example.datn_sd69.modules.adminReview.dto.request.RejectReviewRequest;
import org.example.datn_sd69.modules.adminReview.dto.response.AdminReviewResponse;
import org.example.datn_sd69.modules.adminReview.service.AdminReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {

    @Autowired
    private AdminReviewService adminReviewService;

    @GetMapping
    public ResponseEntity<Page<AdminReviewResponse>> getReviews(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean hasMedia,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<AdminReviewResponse> result = adminReviewService.getFilteredReviews(status, rating, hasMedia, keyword, page, size);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<String> approveReview(@PathVariable Integer id) {
        adminReviewService.approveReview(id);
        return ResponseEntity.ok("Đã duyệt đánh giá!");
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<String> rejectReview(
            @PathVariable Integer id,
            @Valid @RequestBody RejectReviewRequest request) {

        adminReviewService.rejectReview(id, request.getRejectedReason());
        return ResponseEntity.ok("Đã từ chối đánh giá!");
    }

    @PatchMapping("/{id}/hide")
    public ResponseEntity<String> hideReview(@PathVariable Integer id) {
        adminReviewService.hideReview(id);
        return ResponseEntity.ok("Đã ẩn đánh giá!");
    }
}