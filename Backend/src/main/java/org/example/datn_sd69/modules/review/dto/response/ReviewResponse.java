package org.example.datn_sd69.modules.review.dto.response;

import java.time.LocalDateTime;
import java.util.List; // ĐÃ THÊM

public record ReviewResponse(
        Integer reviewId,
        Integer orderItemId,
        Integer orderId,

        Integer productVariantId,
        Integer productId,
        String productName,
        String brandName,
        String sku,
        String image,

        Integer rating,
        String comment,
        LocalDateTime createdAt,

        // ĐÃ THÊM: List để hứng danh sách URL ảnh từ Database ném ra
        List<String> mediaFiles
) {
}