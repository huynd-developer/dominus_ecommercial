package org.example.datn_sd69.modules.customerOrder.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.customerOrder.service.CustomerOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
// LƯU Ý NHỎ: Nếu FE của m gọi '/api/v1/...' thì sửa chỗ này thành "/api/v1/customer/orders" nhé.
// T tạm giữ nguyên theo code cũ của m để không ảnh hưởng các API khác.
@RequestMapping("/api/customer/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER')")
@Validated
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;

    /**
     * Lấy danh sách đơn hàng của khách đang đăng nhập.
     *
     * GET /api/customer/orders
     */
    @GetMapping
    public ResponseEntity<?> getMyOrders() {
        return ResponseEntity.ok(customerOrderService.getMyOrders());
    }

    /**
     * Xem chi tiết 1 đơn hàng của khách đang đăng nhập.
     *
     * GET /api/customer/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderDetail(
            @PathVariable
            @Positive(message = "orderId phải là số nguyên dương")
            Integer orderId
    ) {
        return ResponseEntity.ok(customerOrderService.getOrderDetail(orderId));
    }

    /**
     * Khách hủy đơn.
     *
     * Chỉ cho hủy khi đơn đang ở trạng thái:
     * 0 = Chờ xác nhận
     *
     * PATCH /api/customer/orders/{orderId}/cancel
     */
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable
            @Positive(message = "orderId phải là số nguyên dương")
            Integer orderId
    ) {
        customerOrderService.cancelOrder(orderId);

        return ResponseEntity.ok(Map.of(
                "message", "Hủy đơn hàng thành công"
        ));
    }

    /**
     * KHÁCH YÊU CẦU HOÀN HÀNG (ĐÃ THÊM)
     *
     * Chỉ cho phép khi đơn ở trạng thái:
     * 3 = Hoàn thành
     *
     * PUT /api/customer/orders/{orderId}/request-return
     */
    @PutMapping("/{orderId}/request-return")
    public ResponseEntity<?> requestReturnOrder(
            @PathVariable
            @Positive(message = "orderId phải là số nguyên dương")
            Integer orderId,
            @RequestBody Map<String, String> payload // Dùng Map để hứng file JSON { "reason": "..." } từ FE
    ) {
        String reason = payload.get("reason");
        customerOrderService.requestReturnOrder(orderId, reason);

        return ResponseEntity.ok(Map.of(
                "message", "Gửi yêu cầu hoàn hàng thành công"
        ));
    }

    /**
     * KHÁCH HỦY YÊU CẦU HOÀN HÀNG (ĐÃ THÊM)
     *
     * Chỉ cho phép khi đơn ở trạng thái:
     * 6 = Yêu cầu hoàn hàng
     *
     * PUT /api/customer/orders/{orderId}/cancel-return
     */
    @PutMapping("/{orderId}/cancel-return")
    public ResponseEntity<?> cancelReturnRequest(
            @PathVariable
            @Positive(message = "orderId phải là số nguyên dương")
            Integer orderId
    ) {
        customerOrderService.cancelReturnRequest(orderId);

        return ResponseEntity.ok(Map.of(
                "message", "Đã hủy yêu cầu hoàn hàng thành công"
        ));
    }
}