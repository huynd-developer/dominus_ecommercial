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
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/pos")
@RequiredArgsConstructor
public class PosController {

    private final PosService posService;

    /**
     * GET /api/admin/pos/product?sku=ABC123
     * Quét barcode hoặc tìm thủ công → trả về thông tin biến thể + ảnh
     * Role được phép: CASHIER, MANAGER, OWNER
     */
    @GetMapping("/product")
    public ResponseEntity<?> findProductBySku(@RequestParam String sku) {
        try {
            ProductVariantPosResponse response = posService.findVariantBySku(sku);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * GET /api/admin/pos/customer?phone=0901234567
     * Tìm nhanh khách theo SĐT
     * found=true  → trả về thông tin khách
     * found=false → khách vãng lai, FE để CustomerId = null
     * Role được phép: CASHIER, MANAGER, OWNER
     */
    @GetMapping("/customer")
    public ResponseEntity<?> findCustomerByPhone(@RequestParam String phone) {
        CustomerPosResponse customer = posService.findCustomerByPhone(phone);
        if (customer == null) {
            return ResponseEntity.ok(Map.of(
                    "found", false,
                    "message", "Không tìm thấy khách hàng. Đơn sẽ tạo dưới dạng khách vãng lai."
            ));
        }
        return ResponseEntity.ok(Map.of("found", true, "customer", customer));
    }

    /**
     * POST /api/admin/pos/checkout
     * Thanh toán hóa đơn POS
     *
     * paymentMethod = "CASH"  → Status=3, trừ kho, tích điểm, trả data in hóa đơn ngay
     * paymentMethod = "VNPAY" → Status=0, trả vnpayPaymentUrl để FE render QR
     *
     * Role được phép: CASHIER, MANAGER, OWNER
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
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Lấy IP thực của client — xử lý cả khi chạy sau proxy/nginx
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