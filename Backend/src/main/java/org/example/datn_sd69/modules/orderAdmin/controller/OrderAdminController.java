package org.example.datn_sd69.modules.orderAdmin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.orderAdmin.dto.request.UpdateOrderStatusRequest;
import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderAdminResponse;
import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderDetailResponse;
import org.example.datn_sd69.modules.orderAdmin.service.OrderAdminService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderAdminService orderAdminService;

    /**
     * Danh sách đơn hàng
     *
     * GET:
     * /api/admin/orders
     */
    @GetMapping
    public ResponseEntity<Page<OrderAdminResponse>> searchOrders(

            @RequestParam(required = false) String keyword,

            @RequestParam(required = false) Integer status,

            @RequestParam(required = false) String orderType,

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "10") int size
    ) {

        return ResponseEntity.ok(

                orderAdminService.searchOrders(
                        keyword,
                        status,
                        orderType,
                        page,
                        size
                )

        );

    }

    /**
     * Chi tiết đơn hàng
     *
     * GET:
     * /api/admin/orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailResponse> getDetail(
            @PathVariable Integer id
    ) {

        return ResponseEntity.ok(

                orderAdminService.getOrderDetail(id)

        );

    }

    /**
     * Cập nhật trạng thái
     *
     * PUT:
     * /api/admin/orders/{id}/status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDetailResponse> updateStatus(

            @PathVariable Integer id,

            @Valid
            @RequestBody
            UpdateOrderStatusRequest request

    ) {

        return ResponseEntity.ok(

                orderAdminService.updateStatus(
                        id,
                        request.getStatus()
                )

        );

    }

    /**
     * Hủy đơn
     *
     * PUT:
     * /api/admin/orders/{id}/cancel
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<String> cancelOrder(
            @PathVariable Integer id
    ) {

        orderAdminService.cancelOrder(id);

        return ResponseEntity.ok("Hủy đơn thành công.");

    }

}