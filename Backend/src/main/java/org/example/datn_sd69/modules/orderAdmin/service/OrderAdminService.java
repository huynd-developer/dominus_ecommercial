package org.example.datn_sd69.modules.orderAdmin.service;

import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderAdminResponse;
import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderDetailResponse;
import org.springframework.data.domain.Page;

public interface OrderAdminService {

    /**
     * Danh sách đơn hàng
     */
    Page<OrderAdminResponse> searchOrders(
            String keyword,
            Integer status,
            String orderType,
            int page,
            int size
    );

    /**
     * Chi tiết đơn hàng
     */
    OrderDetailResponse getOrderDetail(Integer orderId);

    /**
     * Chuyển trạng thái đơn hàng
     */
    OrderDetailResponse updateStatus(
            Integer orderId,
            Integer newStatus
    );

    /**
     * Hủy đơn
     */
    void cancelOrder(Integer orderId);

}