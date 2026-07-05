package org.example.datn_sd69.modules.review.dto.response;

public record ReviewableOrderItemResponse(
        Integer orderItemId,
        Integer orderId,

        Integer productVariantId,
        Integer productId,
        String productName,
        String brandName,
        String sku,
        String image,

        Integer orderStatus,
        Boolean reviewed,
        Boolean canReview,
        String message
) {
}
