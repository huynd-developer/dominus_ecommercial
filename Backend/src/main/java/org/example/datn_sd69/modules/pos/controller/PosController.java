package org.example.datn_sd69.modules.pos.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.pos.dto.CustomerPosResponse;
import org.example.datn_sd69.modules.pos.dto.PosCheckoutRequest;
import org.example.datn_sd69.modules.pos.dto.PosOrderResponse;
import org.example.datn_sd69.modules.pos.dto.ProductVariantPosResponse;
import org.example.datn_sd69.modules.pos.service.PosService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/pos")
@RequiredArgsConstructor
public class PosController {

    private final PosService posService;

    /**
     * GET /api/admin/pos/product?sku=ABC123
     * Quét barcode hoặc tìm thủ công.
     */
    @GetMapping("/product")
    public ResponseEntity<?> findProductBySku(@RequestParam String sku) {
        try {
            String cleanSku = sku == null ? "" : sku.trim();

            if (cleanSku.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "SKU không được để trống."
                ));
            }

            ProductVariantPosResponse response = posService.findVariantBySku(cleanSku);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * GET /api/admin/pos/customer?phone=0901234567
     *
     * Nghiệp vụ:
     * - API này chỉ dùng khi FE có nhập SĐT.
     * - Nếu bán vãng lai thì FE không cần gọi API này.
     */
    @GetMapping("/customer")
    public ResponseEntity<?> findCustomerByPhone(@RequestParam String phone) {
        String cleanPhone = normalizePhone(phone);

        if (cleanPhone == null || cleanPhone.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Vui lòng nhập số điện thoại khách hàng."
            ));
        }

        if (!isValidVietnamPhone(cleanPhone)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Số điện thoại không hợp lệ. SĐT phải gồm 10 số và bắt đầu bằng 03, 05, 07, 08 hoặc 09."
            ));
        }

        CustomerPosResponse customer = posService.findCustomerByPhone(cleanPhone);

        if (customer == null) {
            return ResponseEntity.ok(Map.of(
                    "found", false,
                    "message", "Không tìm thấy khách hàng. Vui lòng nhập tên để tạo khách mới hoặc bỏ chọn để bán vãng lai."
            ));
        }

        return ResponseEntity.ok(Map.of(
                "found", true,
                "customer", customer
        ));
    }

    /**
     * POST /api/admin/pos/checkout
     *
     * paymentMethod = CASH  => hoàn tất đơn, trừ kho, tích điểm nếu có khách
     * paymentMethod = VNPAY => tạo đơn chờ thanh toán, trả URL VNPay
     */
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(
            @Valid @RequestBody PosCheckoutRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        try {
            String cashierEmail = authentication != null ? authentication.getName() : null;
            String clientIp = getClientIp(httpRequest);

            PosOrderResponse response = posService.checkout(request, cashierEmail, clientIp);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Trả message validate gọn cho FE.
     * Nếu không có hàm này, @Valid lỗi sẽ trả response mặc định khó đọc.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Dữ liệu không hợp lệ.");

        return ResponseEntity.badRequest().body(Map.of(
                "message", message
        ));
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }

        return phone.replaceAll("[\\s.-]", "").trim();
    }

    private boolean isValidVietnamPhone(String phone) {
        return phone != null && phone.matches("^(03|05|07|08|09)\\d{8}$");
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");

        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }
}