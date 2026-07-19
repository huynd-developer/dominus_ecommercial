package org.example.datn_sd69.modules.voucher.controller;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Voucher;
import org.example.datn_sd69.modules.voucher.service.VoucherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/vouchers")
@RequiredArgsConstructor
public class CustomerVoucherController {

    private final VoucherService voucherService;

    /**
     * API áp voucher cho khách mua online.
     *
     * GET /api/v1/customer/vouchers/apply?code=SALE10&orderTotal=650000
     */
    @GetMapping("/apply")
    public ResponseEntity<?> applyVoucher(
            @RequestParam String code,
            @RequestParam BigDecimal orderTotal
    ) {
        try {
            return ResponseEntity.ok(voucherService.applyVoucher(code, orderTotal));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    /**
     * API lấy voucher đang hoạt động cho khách hàng.
     *
     * GET /api/v1/customer/vouchers
     */
    @GetMapping
    public ResponseEntity<?> getActiveVouchers() {
        try {
            LocalDateTime now = LocalDateTime.now();

            List<Voucher> activeVouchers = voucherService.getAllVouchers()
                    .stream()
                    .filter(voucher -> voucher != null)
                    .filter(voucher -> valueOrZero(voucher.getStatus()) == 1)
                    .filter(voucher -> voucher.getStartDate() == null || !voucher.getStartDate().isAfter(now))
                    .filter(voucher -> voucher.getEndDate() == null || voucher.getEndDate().isAfter(now))
                    .filter(voucher -> {
                        int usageLimit = valueOrZero(voucher.getUsageLimit());
                        int usedCount = valueOrZero(voucher.getUsedCount());

                        return usageLimit <= 0 || usedCount < usageLimit;
                    })
                    .toList();

            return ResponseEntity.ok(activeVouchers);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}