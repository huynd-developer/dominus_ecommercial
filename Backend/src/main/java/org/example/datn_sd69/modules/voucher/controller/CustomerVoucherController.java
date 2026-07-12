package org.example.datn_sd69.modules.voucher.controller;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.voucher.service.VoucherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/customer/vouchers")
@RequiredArgsConstructor
public class CustomerVoucherController {

    private final VoucherService voucherService;

    @GetMapping("/apply")
    public ResponseEntity<?> applyVoucher(@RequestParam String code, @RequestParam BigDecimal orderTotal) {
        try {
            return ResponseEntity.ok(voucherService.applyVoucher(code, orderTotal));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    // THÊM ĐOẠN API NÀY VÀO TRONG CustomerVoucherController.java
    @GetMapping
    public ResponseEntity<?> getActiveVouchers() {
        try {
            // Tái sử dụng hàm lấy danh sách Voucher của nhóm m, lọc những cái đang Status = 1 và hạn sử dụng > Now
            java.util.List<org.example.datn_sd69.entity.Voucher> activeVouchers = voucherService.getAllVouchers().stream()
                    .filter(v -> v.getStatus() == 1 && v.getEndDate().isAfter(java.time.LocalDateTime.now()))
                    .toList();
            return ResponseEntity.ok(activeVouchers);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}