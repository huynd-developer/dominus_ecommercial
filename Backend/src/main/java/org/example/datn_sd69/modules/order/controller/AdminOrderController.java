package org.example.datn_sd69.modules.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.order.dto.request.RejectReturnRequest;
import org.example.datn_sd69.modules.order.service.AdminOrderService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'CASHIER')")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping({"", "/"})
    public ResponseEntity<?> getOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String orderType,
            @RequestParam(required = false) String paymentMethod,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(safePage, safeSize);

        return ResponseEntity.ok(
                adminOrderService.getOrders(
                        keyword,
                        status,
                        orderType,
                        paymentMethod,
                        fromDate,
                        toDate,
                        minAmount,
                        maxAmount,
                        pageable
                )
        );
    }

    @GetMapping("/status-counts")
    public ResponseEntity<?> getStatusCounts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String orderType,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        return ResponseEntity.ok(
                adminOrderService.getStatusCounts(
                        keyword,
                        orderType,
                        fromDate,
                        toDate
                )
        );
    }

    @GetMapping({"/{orderId}", "/{orderId}/"})
    public ResponseEntity<?> getOrderDetail(@PathVariable Integer orderId) {
        return ResponseEntity.ok(
                adminOrderService.getOrderDetail(orderId)
        );
    }

    /**
     * Admin chấp nhận yêu cầu hoàn hàng.
     * Sau bước này mới được xác nhận đã hoàn tiền.
     *
     * PATCH /api/admin/orders/{orderId}/return-accepted
     */
    @PatchMapping({"/{orderId}/return-accepted", "/{orderId}/return-accepted/"})
    public ResponseEntity<?> acceptReturnRequest(@PathVariable Integer orderId) {
        return ResponseEntity.ok(
                adminOrderService.acceptReturnRequest(orderId)
        );
    }

    /**
     * Admin từ chối yêu cầu hoàn hàng.
     * Bắt buộc truyền lý do để khách có thể xem vì sao bị từ chối.
     *
     * PATCH /api/admin/orders/{orderId}/return-rejected
     */
    @PatchMapping({"/{orderId}/return-rejected", "/{orderId}/return-rejected/"})
    public ResponseEntity<?> rejectReturnRequest(
            @PathVariable Integer orderId,
            @Valid @RequestBody RejectReturnRequest request
    ) {
        return ResponseEntity.ok(
                adminOrderService.rejectReturnRequest(orderId, request)
        );
    }

    /**
     * Chuyển đơn hoàn hàng sang trạng thái đã hoàn tiền.
     * Chỉ được gọi sau khi yêu cầu hoàn hàng đã được chấp nhận.
     *
     * PATCH /api/admin/orders/{orderId}/return-refunded
     */
    @PatchMapping({"/{orderId}/return-refunded", "/{orderId}/return-refunded/"})
    public ResponseEntity<?> markReturnRefunded(@PathVariable Integer orderId) {
        return ResponseEntity.ok(
                adminOrderService.markReturnRefunded(orderId)
        );
    }
}