package org.example.datn_sd69.modules.orderAdmin.service;

import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderAdminResponse;
import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderDetailAdminResponse;
import org.springframework.data.domain.Page;

public interface OrderAdminService {
    // 1. Danh sách đơn hàng
    Page<OrderAdminResponse> searchOrders(String keyword, Integer status, String orderType, int page, int size);

    // 2. Chi tiết đơn hàng
    OrderDetailAdminResponse getOrderDetail(Integer orderId);

    // 3. Chuyển đổi trạng thái (Luồng dương)
    void nextOrderStatus(Integer orderId);

    // 4. Hủy đơn hàng và hoàn tồn kho
    void cancelOrder(Integer orderId);
}