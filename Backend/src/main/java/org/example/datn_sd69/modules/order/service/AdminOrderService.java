package org.example.datn_sd69.modules.order.service;

import org.example.datn_sd69.modules.order.dto.request.AdminCancelOrderRequest;
import org.example.datn_sd69.modules.order.dto.request.MarkDeliveryCompletedRequest;
import org.example.datn_sd69.modules.order.dto.request.MarkDeliveryFailedRequest;
import org.example.datn_sd69.modules.order.dto.request.RejectReturnRequest;
import org.example.datn_sd69.modules.order.dto.response.AdminOrderResponse;
import org.example.datn_sd69.modules.order.dto.response.AdminOrderStatusCountResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface AdminOrderService {

    Page<AdminOrderResponse> getOrders(
            String keyword,
            Integer status,
            String orderType,
            String paymentMethod,
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            Pageable pageable
    );

    AdminOrderResponse getOrderDetail(Integer orderId);

    AdminOrderStatusCountResponse getStatusCounts(
            String keyword,
            String orderType,
            LocalDate fromDate,
            LocalDate toDate
    );

    AdminOrderResponse cancelOrder(Integer orderId, AdminCancelOrderRequest request);

    AdminOrderResponse markDeliveryCompleted(Integer orderId, MarkDeliveryCompletedRequest request);

    AdminOrderResponse markDeliveryFailed(Integer orderId, MarkDeliveryFailedRequest request);

    AdminOrderResponse markDeliveryRefunded(Integer orderId);

    AdminOrderResponse acceptReturnRequest(Integer orderId);

    AdminOrderResponse rejectReturnRequest(Integer orderId, RejectReturnRequest request);

    AdminOrderResponse markReturnRefunded(Integer orderId);
}