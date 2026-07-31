package org.example.datn_sd69.modules.review.dto.response;

import java.time.LocalDateTime;
import java.util.List;

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

        Integer approvalStatus,
        String approvalStatusText,
        LocalDateTime approvedAt,
        LocalDateTime rejectedAt,
        String rejectedReason,

        /*
         * Giữ lại mediaUrls để không phá FE cũ đang đọc list URL.
         */
        List<String> mediaUrls,

        /*
         * mediaFiles có mediaId để FE có thể đánh dấu xóa ảnh/video cũ khi sửa đánh giá.
         */
        List<ReviewMediaResponse> mediaFiles,

        LocalDateTime editedAt,
        Integer editCount,
        Boolean canEdit,
        LocalDateTime editDeadline,
        String editMessage
) {
}