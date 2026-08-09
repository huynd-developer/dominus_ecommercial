package org.example.datn_sd69.modules.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.order.dto.request.AdminCancelOrderRequest;
import org.example.datn_sd69.modules.order.dto.request.MarkDeliveryCompletedRequest;
import org.example.datn_sd69.modules.order.dto.request.MarkDeliveryFailedRequest;
import org.example.datn_sd69.modules.order.dto.request.RejectReturnRequest;
import org.example.datn_sd69.modules.order.dto.response.AdminOrderResponse;
import org.example.datn_sd69.modules.order.service.AdminOrderService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
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
     * Admin xác nhận đơn hàng.
     * Khi chuyển từ Chờ xác nhận sang Đã xác nhận thì mới trừ tồn kho.
     *
     * PATCH /api/admin/orders/{orderId}/confirm
     */
    @PatchMapping({"/{orderId}/confirm", "/{orderId}/confirm/"})
    public ResponseEntity<?> confirmOrder(@PathVariable Integer orderId) {
        return ResponseEntity.ok(
                adminOrderService.confirmOrder(orderId)
        );
    }

    /**
     * Admin hủy đơn khi đơn còn ở trạng thái chờ xác nhận.
     * Bắt buộc có lý do hủy để admin/khách xem lại lịch sử.
     *
     * PATCH /api/admin/orders/{orderId}/cancel
     */
    @PatchMapping({"/{orderId}/cancel", "/{orderId}/cancel/"})
    public ResponseEntity<?> cancelOrder(
            @PathVariable Integer orderId,
            @Valid @RequestBody AdminCancelOrderRequest request
    ) {
        return ResponseEntity.ok(
                adminOrderService.cancelOrder(orderId, request)
        );
    }

    /**
     * Giao hàng thành công.
     * Chỉ áp dụng cho đơn đang giao hàng và bắt buộc có ảnh minh chứng.
     *
     * PATCH /api/admin/orders/{orderId}/delivery-completed
     */
    @PatchMapping(
            value = {"/{orderId}/delivery-completed", "/{orderId}/delivery-completed/"},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> markDeliveryCompleted(
            @PathVariable Integer orderId,
            @ModelAttribute MarkDeliveryCompletedRequest request
    ) {
        return ResponseEntity.ok(
                adminOrderService.markDeliveryCompleted(orderId, request)
        );
    }

    /**
     * Giao hàng thất bại.
     * Bắt buộc có lý do. Nếu chọn Khác thì bắt buộc mô tả.
     *
     * PATCH /api/admin/orders/{orderId}/delivery-failed
     */
    @PatchMapping(
            value = {"/{orderId}/delivery-failed", "/{orderId}/delivery-failed/"},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> markDeliveryFailed(
            @PathVariable Integer orderId,
            @ModelAttribute MarkDeliveryFailedRequest request
    ) {
        return ResponseEntity.ok(
                adminOrderService.markDeliveryFailed(orderId, request)
        );
    }

    /**
     * Admin xác nhận đã hoàn tiền thực tế cho đơn giao thất bại.
     * Chỉ dùng cho đơn giao thất bại đã thanh toán trước và đã có thông tin ngân hàng của khách.
     *
     * PATCH /api/admin/orders/{orderId}/delivery-refunded
     */
    @PatchMapping({"/{orderId}/delivery-refunded", "/{orderId}/delivery-refunded/"})
    public ResponseEntity<?> markDeliveryRefunded(@PathVariable Integer orderId) {
        return ResponseEntity.ok(
                adminOrderService.markDeliveryRefunded(orderId)
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

    /**
     * API ADMIN XÁC NHẬN ĐÃ CHUYỂN KHOẢN ĐƠN HỦY
     */
    @RequestMapping(value = "/{id}/cancel-refund/confirm", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<?> confirmCancelRefund(
            @PathVariable("id") Integer orderId,
            @RequestParam(name = "restoreStock", defaultValue = "false") boolean restoreStock) {

        AdminOrderResponse response = adminOrderService.confirmCancelRefund(orderId, restoreStock);
        return ResponseEntity.ok(response);
    }
}