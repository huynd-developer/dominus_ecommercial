package org.example.datn_sd69.modules.order.service;

import org.example.datn_sd69.modules.order.dto.response.AdminOrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminOrderService {

    Page<AdminOrderResponse> getOrders(
            String keyword,
            Integer status,
            String orderType,
            Pageable pageable
    );

    AdminOrderResponse getOrderDetail(Integer orderId);
}