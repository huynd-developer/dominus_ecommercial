package org.example.datn_sd69.modules.orderAdmin.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.orderAdmin.dto.request.OrderSearchRequest;
import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderAdminResponse;
import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderDetailAdminResponse;
import org.example.datn_sd69.modules.orderAdmin.service.OrderAdminService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Validated // Kích hoạt validate cho các tham số như @PathVariable
public class OrderAdminController {

    private final OrderAdminService orderAdminService;

    // Lấy danh sách đơn hàng (Đã đổi sang dùng Request Object đi kèm validate)
    @GetMapping
    public ResponseEntity<Page<OrderAdminResponse>> getOrders(@Valid OrderSearchRequest request) {
        return ResponseEntity.ok(orderAdminService.searchOrders(
                request.getKeyword(),
                request.getStatus(),
                request.getOrderType(),
                request.getPage(),
                request.getSize()
        ));
    }

    // Xem chi tiết đơn hàng (Validate ID phải lớn hơn 0)
    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailAdminResponse> getOrderDetail(
            @PathVariable @Min(value = 1, message = "ID đơn hàng phải lớn hơn 0") Integer id) {
        return ResponseEntity.ok(orderAdminService.getOrderDetail(id));
    }

    // Xử lý chuyển đổi trạng thái đơn
    @PutMapping("/{id}/next-status")
    public ResponseEntity<String> nextOrderStatus(
            @PathVariable @Min(value = 1, message = "ID đơn hàng phải lớn hơn 0") Integer id) {
        orderAdminService.nextOrderStatus(id);
        return ResponseEntity.ok("Cập nhật trạng thái đơn hàng thành công!");
    }

    // Logic Hủy đơn
    @PutMapping("/{id}/cancel")
    public ResponseEntity<String> cancelOrder(
            @PathVariable @Min(value = 1, message = "ID đơn hàng phải lớn hơn 0") Integer id) {
        orderAdminService.cancelOrder(id);
        return ResponseEntity.ok("Đã hủy đơn hàng và hoàn lại tồn kho thành công!");
    }
}