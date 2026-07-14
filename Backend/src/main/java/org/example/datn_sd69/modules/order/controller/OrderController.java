package org.example.datn_sd69.modules.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.modules.order.dto.request.OrderRequest;
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

    /**
     * Khách hàng đặt đơn online.
     *
     * POST /api/v1/orders/checkout
     */
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

    // Đã xóa API verifyVnPayReturn ở đây vì hệ thống đã chuyển sang dùng VietQR
    /**
     * VNPay redirect về FE /payment-return.
     * FE gọi API này để BE xác minh chữ ký VNPay.
     *
     * Không bắt đăng nhập vì VNPay redirect từ ngoài hệ thống về.
     */
    @GetMapping("/payment/vnpay-return")
    public ResponseEntity<?> verifyVnPayReturn(
            @RequestParam Map<String, String> params
    ) {
        Map<String, Object> result = orderService.verifyVnPayReturn(params);
        return ResponseEntity.ok(result);
    }

    private Integer getCustomerId(Principal principal) {
        if (principal == null
                || principal.getName() == null
                || principal.getName().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Bạn chưa đăng nhập"
            );
        }

        String email = principal.getName().trim();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Không tìm thấy tài khoản đăng nhập"
                ));

        return user.getId();
    }
}