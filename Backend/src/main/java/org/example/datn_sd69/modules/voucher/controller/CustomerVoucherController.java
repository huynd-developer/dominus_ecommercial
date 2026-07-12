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
}