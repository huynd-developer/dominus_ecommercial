package org.example.datn_sd69.modules.orderAdmin.service;

import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    /**
     * Danh sách đơn hàng ONLINE + IN_STORE
     */
    Page<OrderResponse.ListItem> getOrders(
            String keyword,
            Integer status,
            String orderType,
            Pageable pageable
    );

    /**
     * Chi tiết đơn hàng
     */
    OrderResponse.Detail getOrderDetail(
            Integer orderId
    );

    /**
     * Chuyển trạng thái:
     * 0 -> 1 -> 2 -> 3
     */
    OrderResponse.UpdateStatus nextStatus(
            Integer orderId
    );

    /**
     * Hủy đơn:
     * status = 4
     * hoàn lại tồn kho
     */
    OrderResponse.UpdateStatus cancelOrder(
            Integer orderId
    );
}