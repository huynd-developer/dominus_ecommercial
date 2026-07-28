package org.example.datn_sd69.modules.orderAdmin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.orderAdmin.dto.request.UpdateOrderStatusRequest;
import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderAdminResponse;
import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderDetailResponse;
import org.example.datn_sd69.modules.orderAdmin.service.OrderAdminService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate; // ĐÃ ĐỔI TỪ LocalDateTime SANG LocalDate

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderAdminService orderAdminService;

    @GetMapping
    public ResponseEntity<Page<OrderAdminResponse>> searchOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderType,
            // ĐÃ SỬA: Dùng LocalDate và định dạng ISO.DATE để nhận "yyyy-MM-dd" từ Vue
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        return ResponseEntity.ok(
                orderAdminService.searchOrders(
                        keyword, status, orderType, startDate, endDate, page, size
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailResponse> getDetail(@PathVariable Integer id) {
        return ResponseEntity.ok(orderAdminService.getOrderDetail(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDetailResponse> updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return ResponseEntity.ok(orderAdminService.updateStatus(id, request.getStatus()));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<String> cancelOrder(@PathVariable Integer id) {
        orderAdminService.cancelOrder(id);
        return ResponseEntity.ok("Hủy đơn thành công.");
    }
}