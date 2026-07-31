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

        /** Phí vận chuyển của đơn hàng. */
        BigDecimal shippingFee,

        /** Phí vận chuyển được hoàn trong yêu cầu hoàn hàng, nếu có. */
        BigDecimal returnShippingFee,

        String paymentMethod,

        Integer status,
        String statusText,

        Boolean canCancel,

        LocalDateTime createdAt,

        String cancelReason,
        LocalDateTime cancelledAt,

        String returnReason,
        String returnDescription,
        LocalDateTime returnRequestedAt,
        BigDecimal returnRefundAmount,
        List<String> returnMediaUrls,
        List<CustomerReturnItemResponse> returnItems,

        /**
         * PENDING / ACCEPTED / REJECTED / REFUNDED / PARTIAL / UNKNOWN.
         * Trả ra cho FE để hiển thị timeline hoàn hàng kiểu sàn TMĐT.
         */
        String returnProcessStatus,
        String returnProcessStatusText,
        String returnRejectReason,

        /**
         * Các field này optional. Nếu service chưa set thì FE vẫn fallback được.
         */
        String refundMethod,
        String bankName,
        String bankAccountNumber,
        String bankAccountHolder,
        LocalDateTime returnProcessedAt,
        LocalDateTime returnAcceptedAt,
        LocalDateTime returnRejectedAt,
        LocalDateTime returnRefundedAt,

        List<CustomerOrderItemResponse> items
) {

    /**
     * Giữ constructor cũ để không làm vỡ các chỗ đang new CustomerOrderResponse(...)
     * theo cấu trúc cũ.
     */
    public CustomerOrderResponse(
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
            String cancelReason,
            LocalDateTime cancelledAt,
            String returnReason,
            String returnDescription,
            LocalDateTime returnRequestedAt,
            BigDecimal returnRefundAmount,
            List<String> returnMediaUrls,
            List<CustomerReturnItemResponse> returnItems,
            List<CustomerOrderItemResponse> items
    ) {
        this(
                orderId,
                orderType,
                customerName,
                customerPhone,
                shippingAddress,
                totalAmount,
                discountAmount,
                finalAmount,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                paymentMethod,
                status,
                statusText,
                canCancel,
                createdAt,
                cancelReason,
                cancelledAt,
                returnReason,
                returnDescription,
                returnRequestedAt,
                returnRefundAmount,
                returnMediaUrls,
                returnItems,
                resolveReturnProcessStatus(status, returnItems),
                resolveReturnProcessStatusText(resolveReturnProcessStatus(status, returnItems)),
                resolveReturnRejectReason(returnItems),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                items
        );
    }

    private static String resolveReturnProcessStatus(
            Integer orderStatus,
            List<CustomerReturnItemResponse> returnItems
    ) {
        if (returnItems != null && !returnItems.isEmpty()) {
            boolean hasItemStatus = returnItems.stream()
                    .anyMatch(item -> item != null && item.status() != null);

            if (hasItemStatus) {
                boolean allPending = returnItems.stream().allMatch(item -> item != null && Integer.valueOf(0).equals(item.status()));
                boolean allAccepted = returnItems.stream().allMatch(item -> item != null && Integer.valueOf(1).equals(item.status()));
                boolean allRejected = returnItems.stream().allMatch(item -> item != null && Integer.valueOf(2).equals(item.status()));
                boolean allRefunded = returnItems.stream().allMatch(item -> item != null && Integer.valueOf(3).equals(item.status()));

                if (allPending) {
                    return "PENDING";
                }

                if (allAccepted) {
                    return "ACCEPTED";
                }

                if (allRejected) {
                    return "REJECTED";
                }

                if (allRefunded) {
                    return "REFUNDED";
                }

                return "PARTIAL";
            }
        }

        if (Integer.valueOf(7).equals(orderStatus)) {
            return "REFUNDED";
        }

        if (Integer.valueOf(6).equals(orderStatus)) {
            return "PENDING";
        }

        return "UNKNOWN";
    }

    private static String resolveReturnProcessStatusText(String processStatus) {
        if (processStatus == null) {
            return "Đang cập nhật";
        }

        return switch (processStatus) {
            case "PENDING" -> "Chờ shop xử lý";
            case "ACCEPTED" -> "Đã chấp nhận / Chờ hoàn tiền";
            case "REJECTED" -> "Đã từ chối hoàn hàng";
            case "REFUNDED" -> "Đã xử lý hoàn tiền";
            case "PARTIAL" -> "Đang xử lý một phần";
            default -> "Đang cập nhật";
        };
    }

    private static String resolveReturnRejectReason(List<CustomerReturnItemResponse> returnItems) {
        if (returnItems == null || returnItems.isEmpty()) {
            return null;
        }

        return returnItems.stream()
                .filter(item -> item != null && item.rejectReason() != null && !item.rejectReason().trim().isEmpty())
                .map(item -> item.rejectReason().trim())
                .findFirst()
                .orElse(null);
    }
}