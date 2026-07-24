package org.example.datn_sd69.modules.review.dto.response;

import java.time.LocalDateTime;
import java.util.List; // ĐÃ THÊM import này

public record PublicProductReviewResponse(
        Integer reviewId,
        Integer productId,
        Integer productVariantId,
        String productName,
        String brandName,
        String sku,

        String customerName,
        Integer rating,
        String comment,
        LocalDateTime createdAt,

        // ĐÃ THÊM: List chứa các link ảnh/video trả về cho Frontend
        List<String> mediaFiles
) {
}