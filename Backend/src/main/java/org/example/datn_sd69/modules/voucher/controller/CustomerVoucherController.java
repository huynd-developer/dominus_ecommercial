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
     * <p>
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
     * <p>
     * GET /api/v1/customer/vouchers
     */
    @GetMapping
    public ResponseEntity<?> getActiveVouchers() {
        try {
            LocalDateTime now = LocalDateTime.now();

            List<Voucher> vouchers = voucherService.getAllVouchers()
                    .stream()
                    .filter(voucher -> voucher != null)

                    /*
                     * status = 1 nghĩa là Admin đang cho phép Voucher hoạt động.
                     *
                     * Voucher tương lai vẫn được trả xuống FE để FE biết
                     * startDate và tự đặt timer.
                     *
                     * Việc Voucher có được sử dụng thật hay không vẫn do
                     * API /apply kiểm tra startDate <= now < endDate.
                     */
                    .filter(voucher -> valueOrZero(voucher.getStatus()) == 1)

                    /*
                     * Voucher đã hết hạn thì không cần trả xuống FE.
                     */
                    .filter(voucher ->
                            voucher.getEndDate() == null
                                    || voucher.getEndDate().isAfter(now)
                    )

                    /*
                     * Voucher hết lượt thì không cần hiển thị.
                     */
                    .filter(voucher -> {
                        int usageLimit = valueOrZero(voucher.getUsageLimit());
                        int usedCount = valueOrZero(voucher.getUsedCount());

                        return usageLimit <= 0 || usedCount < usageLimit;
                    })
                    .toList();

            return ResponseEntity.ok(vouchers);

        } catch (Exception e) {
            return ResponseEntity
                    .internalServerError()
                    .body(e.getMessage());
        }
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}