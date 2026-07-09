package org.example.datn_sd69.modules.review.dto.response;

public record ProductReviewSummaryResponse(
        Integer productId,
        Double averageRating,
        Long reviewCount,
        Long fiveStarCount,
        Long fourStarCount,
        Long threeStarCount,
        Long twoStarCount,
        Long oneStarCount
) {
}