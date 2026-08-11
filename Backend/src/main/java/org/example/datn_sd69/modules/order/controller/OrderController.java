package org.example.datn_sd69.modules.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.modules.order.dto.request.OrderRequest;
import org.example.datn_sd69.modules.order.dto.response.AdminOrderResponse;
import org.example.datn_sd69.modules.order.service.OrderService;
import org.example.datn_sd69.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepo;

    @PostMapping("/checkout")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<?> checkout(
            Principal principal,
            @Valid @RequestBody OrderRequest request
    ) {
        Integer customerId = getCustomerId(principal);
        Map<String, Object> result = orderService.placeOrder(customerId, request);
        return ResponseEntity.ok(result);
    }

    /**
     * API để VNPay gọi về sau khi khách thanh toán xong
     */
    @GetMapping("/payment/vnpay-return")
    public ResponseEntity<?> verifyVnPayReturn(@RequestParam Map<String, String> params) {
        Map<String, Object> result = orderService.verifyVnPayReturn(params);
        return ResponseEntity.ok(result);
    }

    /**
     * BỔ SUNG: API để lấy lại link thanh toán VNPay cho đơn hàng cũ (Nút "Thanh toán lại")
     */
    @GetMapping("/{orderId}/vnpay-url")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<?> getVnPayUrl(@PathVariable Integer orderId) {
        Map<String, Object> result = orderService.generateVnPayUrl(orderId);
        return ResponseEntity.ok(result);
    }

    /**
     * BỔ SUNG: API báo cáo đã thanh toán từ phía Khách Hàng
     */
    @PostMapping("/{orderId}/report-payment")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<?> reportPayment(@PathVariable Integer orderId) {
        // Có thể thêm check xem đơn này có đúng của ông khách đang login không nếu cần bảo mật kỹ hơn
        orderService.reportPayment(orderId);
        return ResponseEntity.ok(Map.of("message", "Đã báo cáo thanh toán thành công"));
    }

    /**
     * SỬA LẠI API KHÁCH GỬI THÔNG TIN HOÀN TIỀN
     * Link: /api/v1/orders/{id}/cancel-bank-info
     */
    @PostMapping("/{id}/cancel-bank-info")
    @PreAuthorize("hasAuthority('USER')")
    public ResponseEntity<?> submitCancelBankInfo(
            Principal principal,
            @PathVariable("id") Integer orderId,
            @RequestBody OrderRequest.CancelRefundBankRequest request) {

        Integer customerId = getCustomerId(principal);

        // M viết thêm hàm này trong OrderService để lưu vào bảng OrderRefund nhé
        orderService.submitCancelRefundBankInfo(customerId, orderId, request);

        return ResponseEntity.ok(Map.of("message", "Gửi thông tin tài khoản hoàn tiền thành công"));
    }

    private Integer getCustomerId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn chưa đăng nhập");
        }
        String email = principal.getName().trim();
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản đăng nhập"));
        return user.getId();
    }
}