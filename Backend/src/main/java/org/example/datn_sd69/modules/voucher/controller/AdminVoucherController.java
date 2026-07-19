package org.example.datn_sd69.modules.voucher.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Voucher;
import org.example.datn_sd69.modules.voucher.dto.request.VoucherRequest;
import org.example.datn_sd69.modules.voucher.service.VoucherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/vouchers")
@RequiredArgsConstructor
public class AdminVoucherController {

    private final VoucherService voucherService;

    /**
     * API lấy danh sách Voucher cho màn quản lý Admin.
     *
     * GET /api/admin/vouchers
     */
    @GetMapping
    public ResponseEntity<?> getAllVouchers() {
        return ResponseEntity.ok(voucherService.getAllVouchers());
    }

    /**
     * API lấy danh sách voucher khả dụng cho POS.
     *
     * GET /api/admin/vouchers/available?orderTotal=650000
     *
     * BẮT BUỘC đặt trước @GetMapping("/{id}")
     * để Spring không hiểu "available" là id.
     */
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableVouchersForPos(
            @RequestParam(defaultValue = "0") BigDecimal orderTotal
    ) {
        try {
            BigDecimal safeOrderTotal = safeMoney(orderTotal);

            List<Map<String, Object>> result = voucherService.getAllVouchers()
                    .stream()
                    .filter(this::canShowVoucherInPos)
                    .map(voucher -> toPosVoucherResponse(voucher, safeOrderTotal))
                    .sorted(
                            Comparator
                                    .<Map<String, Object>, Boolean>comparing(
                                            item -> Boolean.TRUE.equals(item.get("eligible"))
                                    )
                                    .reversed()
                                    .thenComparing(
                                            item -> (BigDecimal) item.get("discountAmount"),
                                            Comparator.reverseOrder()
                                    )
                                    .thenComparing(
                                            item -> (BigDecimal) item.get("minOrderAmount")
                                    )
                    )
                    .toList();

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Không thể tải danh sách voucher POS: " + e.getMessage());
        }
    }

    /**
     * API áp voucher cho POS.
     *
     * GET /api/admin/vouchers/apply?code=SALE10&orderTotal=650000
     *
     * BẮT BUỘC đặt trước @GetMapping("/{id}")
     * để Spring không hiểu "apply" là id.
     */
    @GetMapping("/apply")
    public ResponseEntity<?> applyVoucherForPos(
            @RequestParam String code,
            @RequestParam BigDecimal orderTotal
    ) {
        try {
            String cleanCode = normalizeCode(code);
            BigDecimal safeOrderTotal = safeMoney(orderTotal);

            if (cleanCode.isBlank()) {
                return ResponseEntity.badRequest().body("Vui lòng nhập mã giảm giá.");
            }

            Voucher voucher = voucherService.getAllVouchers()
                    .stream()
                    .filter(v -> cleanCode.equalsIgnoreCase(normalizeCode(v.getCode())))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá không tồn tại."));

            Map<String, Object> response = toPosVoucherResponse(voucher, safeOrderTotal);

            if (!Boolean.TRUE.equals(response.get("eligible"))) {
                Object reason = response.get("ineligibleReason");

                return ResponseEntity.badRequest()
                        .body(reason == null
                                ? "Mã giảm giá không đủ điều kiện áp dụng."
                                : reason.toString());
            }

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Lỗi hệ thống khi áp voucher POS: " + e.getMessage());
        }
    }

    /**
     * API thêm mới Voucher.
     *
     * POST /api/admin/vouchers
     */
    @PostMapping
    public ResponseEntity<?> createVoucher(@Valid @RequestBody VoucherRequest request) {
        try {
            voucherService.createVoucher(request);
            return ResponseEntity.ok().body("Thêm voucher thành công!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    /**
     * API lấy chi tiết 1 Voucher để fill vào form Sửa.
     *
     * GET /api/admin/vouchers/{id}
     *
     * API này phải nằm SAU /available và /apply.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getVoucher(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(voucherService.getVoucherById(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * API cập nhật Voucher.
     *
     * PUT /api/admin/vouchers/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateVoucher(
            @PathVariable Integer id,
            @Valid @RequestBody VoucherRequest request
    ) {
        try {
            voucherService.updateVoucher(id, request);
            return ResponseEntity.ok().body("Cập nhật voucher thành công!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    /**
     * API xóa Voucher.
     *
     * DELETE /api/admin/vouchers/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVoucher(@PathVariable Integer id) {
        try {
            voucherService.deleteVoucher(id);
            return ResponseEntity.ok().body("Xóa voucher thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private boolean canShowVoucherInPos(Voucher voucher) {
        if (voucher == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        if (valueOrZero(voucher.getStatus()) != 1) {
            return false;
        }

        if (voucher.getEndDate() != null && voucher.getEndDate().isBefore(now)) {
            return false;
        }

        int usageLimit = valueOrZero(voucher.getUsageLimit());
        int usedCount = valueOrZero(voucher.getUsedCount());

        if (usageLimit > 0 && usedCount >= usageLimit) {
            return false;
        }

        return true;
    }

    private Map<String, Object> toPosVoucherResponse(Voucher voucher, BigDecimal orderTotal) {
        BigDecimal safeOrderTotal = safeMoney(orderTotal);
        BigDecimal minOrderAmount = safeMoney(voucher.getMinOrderValue());
        BigDecimal maxDiscountAmount = safeMoney(voucher.getMaxDiscount());
        BigDecimal discountValue = safeMoney(voucher.getDiscountValue());

        String ineligibleReason = getIneligibleReason(voucher, safeOrderTotal);
        boolean eligible = ineligibleReason == null;

        BigDecimal discountAmount = eligible
                ? calculateDiscountAmount(voucher, safeOrderTotal)
                : BigDecimal.ZERO;

        int usageLimit = valueOrZero(voucher.getUsageLimit());
        int usedCount = valueOrZero(voucher.getUsedCount());
        Integer remainingQuantity = usageLimit > 0
                ? Math.max(usageLimit - usedCount, 0)
                : null;

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("id", voucher.getId());
        response.put("code", normalizeCode(voucher.getCode()));
        response.put("voucherCode", normalizeCode(voucher.getCode()));
        response.put("name", normalizeCode(voucher.getCode()));
        response.put("description", buildVoucherDescription(voucher));

        response.put("discountType", voucher.getDiscountType());
        response.put("discountValue", discountValue);
        response.put("discountPercent", isPercentVoucher(voucher) ? discountValue : BigDecimal.ZERO);

        response.put("minOrderAmount", minOrderAmount);
        response.put("minOrderValue", minOrderAmount);
        response.put("maxDiscountAmount", maxDiscountAmount);
        response.put("maxDiscount", maxDiscountAmount);

        response.put("discountAmount", discountAmount);
        response.put("orderTotal", safeOrderTotal);
        response.put("finalAmount", safeOrderTotal.subtract(discountAmount).max(BigDecimal.ZERO));

        response.put("startDate", voucher.getStartDate());
        response.put("endDate", voucher.getEndDate());
        response.put("status", voucher.getStatus());

        response.put("usageLimit", usageLimit);
        response.put("usedCount", usedCount);
        response.put("remainingQuantity", remainingQuantity);

        response.put("eligible", eligible);
        response.put("ineligibleReason", ineligibleReason);

        return response;
    }

    private String getIneligibleReason(Voucher voucher, BigDecimal orderTotal) {
        if (voucher == null) {
            return "Voucher không hợp lệ.";
        }

        LocalDateTime now = LocalDateTime.now();

        if (valueOrZero(voucher.getStatus()) != 1) {
            return "Voucher đã ngừng hoạt động.";
        }

        if (voucher.getStartDate() != null && voucher.getStartDate().isAfter(now)) {
            return "Voucher chưa đến thời gian sử dụng.";
        }

        if (voucher.getEndDate() != null && voucher.getEndDate().isBefore(now)) {
            return "Voucher đã hết hạn sử dụng.";
        }

        int usageLimit = valueOrZero(voucher.getUsageLimit());
        int usedCount = valueOrZero(voucher.getUsedCount());

        if (usageLimit > 0 && usedCount >= usageLimit) {
            return "Voucher đã hết lượt sử dụng.";
        }

        BigDecimal minOrderAmount = safeMoney(voucher.getMinOrderValue());

        if (orderTotal.compareTo(minOrderAmount) < 0) {
            return "Đơn tối thiểu " + formatMoney(minOrderAmount) + " ₫ để dùng voucher này.";
        }

        BigDecimal discountAmount = calculateDiscountAmount(voucher, orderTotal);

        if (discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return "Voucher không tạo ra giá trị giảm cho đơn hàng này.";
        }

        return null;
    }

    private BigDecimal calculateDiscountAmount(Voucher voucher, BigDecimal orderTotal) {
        BigDecimal safeOrderTotal = safeMoney(orderTotal);

        if (safeOrderTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discountValue = safeMoney(voucher.getDiscountValue());
        BigDecimal discountAmount;

        if (isPercentVoucher(voucher)) {
            discountAmount = safeOrderTotal
                    .multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else {
            discountAmount = discountValue;
        }

        BigDecimal maxDiscountAmount = safeMoney(voucher.getMaxDiscount());

        if (
                maxDiscountAmount.compareTo(BigDecimal.ZERO) > 0 &&
                        discountAmount.compareTo(maxDiscountAmount) > 0
        ) {
            discountAmount = maxDiscountAmount;
        }

        if (discountAmount.compareTo(safeOrderTotal) > 0) {
            discountAmount = safeOrderTotal;
        }

        return discountAmount.max(BigDecimal.ZERO);
    }

    private boolean isPercentVoucher(Voucher voucher) {
        String type = voucher.getDiscountType() == null
                ? ""
                : voucher.getDiscountType().trim();

        return "PERCENT".equalsIgnoreCase(type)
                || "PERCENTAGE".equalsIgnoreCase(type)
                || "%".equals(type);
    }

    private String buildVoucherDescription(Voucher voucher) {
        BigDecimal discountValue = safeMoney(voucher.getDiscountValue());

        if (isPercentVoucher(voucher)) {
            String percentText = discountValue.stripTrailingZeros().toPlainString();
            BigDecimal maxDiscountAmount = safeMoney(voucher.getMaxDiscount());

            if (maxDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
                return "Giảm " + percentText + "%, tối đa " + formatMoney(maxDiscountAmount) + " ₫";
            }

            return "Giảm " + percentText + "%";
        }

        return "Giảm " + formatMoney(discountValue) + " ₫";
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO);
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String formatMoney(BigDecimal value) {
        return String.format("%,.0f", safeMoney(value));
    }
}