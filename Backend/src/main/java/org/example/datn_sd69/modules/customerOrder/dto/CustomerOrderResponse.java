package org.example.datn_sd69.modules.customerOrder.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CustomerOrderResponse(
        Integer orderId,

        String orderType,

        String customerName,
        String customerPhone,
        String shippingAddress,

        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal finalAmount,

        String paymentMethod,

        Integer status,
        String statusText,

        Boolean canCancel,

        LocalDateTime createdAt,

        List<CustomerOrderItemResponse> items
) {
}