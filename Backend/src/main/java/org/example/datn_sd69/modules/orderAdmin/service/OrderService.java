package org.example.datn_sd69.modules.orderAdmin.service;

import org.example.datn_sd69.modules.orderAdmin.dto.request.UpdateOrderStatusRequest;
import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderDetailResponse;
import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderResponse;
import org.springframework.data.domain.Page;

public interface OrderService {

    /**
     * Danh sách đơn hàng
     */
    Page<OrderResponse> getAll(
            String keyword,
            String orderType,
            Integer status,
            int page,
            int size
    );

    /**
     * Chi tiết đơn hàng
     */
    OrderDetailResponse getById(Integer id);

    /**
     * Cập nhật trạng thái
     *
     * 0 -> 1 -> 2 -> 3
     */
    void updateStatus(
            Integer id,
            UpdateOrderStatusRequest request
    );

    /**
     * Hủy đơn
     *
     * status = 4
     * hoàn lại tồn kho
     */
    void cancel(Integer id);

}