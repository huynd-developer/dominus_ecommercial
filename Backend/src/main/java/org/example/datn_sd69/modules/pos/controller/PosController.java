package org.example.datn_sd69.modules.pos.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.pos.dto.request.PosCheckoutRequest;
import org.example.datn_sd69.modules.pos.dto.request.PosHeldOrderCheckoutRequest;
import org.example.datn_sd69.modules.pos.dto.request.PosHoldRequest;
import org.example.datn_sd69.modules.pos.dto.request.PosSaveCustomerRequest;
import org.example.datn_sd69.modules.pos.dto.request.PosTransferHeldOrderRequest;
import org.example.datn_sd69.modules.pos.dto.response.CustomerPosResponse;
import org.example.datn_sd69.modules.pos.dto.response.PosHeldOrderResponse;
import org.example.datn_sd69.modules.pos.dto.response.PosOrderResponse;
import org.example.datn_sd69.modules.pos.dto.response.PosTransferTargetResponse;
import org.example.datn_sd69.modules.pos.dto.response.ProductVariantPosResponse;
import org.example.datn_sd69.modules.pos.service.PosService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/pos")
@RequiredArgsConstructor
public class PosController {

    private final PosService posService;

    /**
     * Danh sách sản phẩm/biến thể dành riêng cho POS.
     *
     * GET /api/admin/pos/products
     */
    @GetMapping("/products")
    public ResponseEntity<List<ProductVariantPosResponse>> getProductsForPos() {
        return ResponseEntity.ok(posService.getProductsForPos());
    }

    /**
     * Quét SKU/barcode tại POS.
     *
     * GET /api/admin/pos/product?sku=...
     */
    @GetMapping("/product")
    public ResponseEntity<ProductVariantPosResponse> findProductBySku(
            @RequestParam String sku
    ) {
        return ResponseEntity.ok(posService.findVariantBySku(sku));
    }

    /**
     * Tìm khách hàng theo SĐT.
     *
     * GET /api/admin/pos/customer?phone=...
     */
    @GetMapping("/customer")
    public ResponseEntity<Map<String, Object>> findCustomerByPhone(
            @RequestParam String phone
    ) {
        CustomerPosResponse customer = posService.findCustomerByPhone(phone);

        if (customer == null) {
            return ResponseEntity.ok(Map.of(
                    "found", false,
                    "message", "Không tìm thấy khách hàng. Vui lòng nhập họ tên, số điện thoại và email. Hệ thống sẽ lưu khách khi treo phiếu hoặc thanh toán."
            ));
        }

        return ResponseEntity.ok(Map.of(
                "found", true,
                "customer", customer
        ));
    }

    /**
     * Lưu khách hàng tại POS.
     *
     * POST /api/admin/pos/customer
     */
    @PostMapping("/customer")
    public ResponseEntity<Map<String, Object>> saveCustomerForPos(
            @Valid @RequestBody PosSaveCustomerRequest request
    ) {
        CustomerPosResponse customer = posService.saveCustomerForPos(request);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã lưu thông tin khách hàng.",
                "customer", customer
        ));
    }

    /**
     * Áp mã giảm giá tại POS.
     *
     * GET /api/admin/pos/voucher/apply?code=...&totalAmount=...
     *
     * API này chỉ giữ lại để tương thích code cũ.
     * FE mới nên dùng /api/admin/vouchers/apply.
     */
    @GetMapping("/voucher/apply")
    public ResponseEntity<Map<String, Object>> applyVoucher(
            @RequestParam String code,
            @RequestParam BigDecimal totalAmount
    ) {
        return ResponseEntity.ok(
                posService.applyVoucher(code, totalAmount)
        );
    }

    /**
     * Thanh toán đơn POS bình thường.
     *
     * POST /api/admin/pos/checkout
     */
    @PostMapping("/checkout")
    public ResponseEntity<PosOrderResponse> checkout(
            @Valid @RequestBody PosCheckoutRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String cashierEmail = getCurrentEmail(authentication);
        String clientIp = getClientIp(httpRequest);

        return ResponseEntity.ok(
                posService.checkout(request, cashierEmail, clientIp)
        );
    }

    /**
     * Treo phiếu mua hàng lần đầu.
     *
     * POST /api/admin/pos/hold
     *
     * Đúng nghiệp vụ:
     * - Tạo Order trạng thái phiếu treo
     * - PaymentMethod = HOLD
     * - Chưa trừ kho
     * - Chưa tăng lượt dùng voucher
     */
    @PostMapping("/hold")
    public ResponseEntity<PosOrderResponse> holdOrder(
            @Valid @RequestBody PosHoldRequest request,
            Authentication authentication
    ) {
        String cashierEmail = getCurrentEmail(authentication);

        return ResponseEntity.ok(
                posService.holdOrder(request, cashierEmail)
        );
    }

    /**
     * Cập nhật phiếu treo đang mở.
     *
     * PATCH /api/admin/pos/held-orders/{orderId}
     *
     * Đúng hướng nghiệp vụ mới:
     * - Được sửa sản phẩm
     * - Được sửa số lượng
     * - Được sửa voucher
     * - Không được sửa khách hàng
     * - Chưa trừ kho
     * - Chưa tăng usedCount voucher
     */
    @PatchMapping("/held-orders/{orderId}")
    public ResponseEntity<PosOrderResponse> updateHeldOrder(
            @PathVariable Integer orderId,
            @Valid @RequestBody PosHoldRequest request,
            Authentication authentication
    ) {
        String cashierEmail = getCurrentEmail(authentication);

        return ResponseEntity.ok(
                posService.updateHeldOrder(orderId, request, cashierEmail)
        );
    }

    /**
     * Danh sách phiếu treo.
     *
     * GET /api/admin/pos/held-orders
     */
    @GetMapping("/held-orders")
    public ResponseEntity<List<PosHeldOrderResponse>> getHeldOrders(
            Authentication authentication
    ) {
        String cashierEmail = getCurrentEmail(authentication);

        return ResponseEntity.ok(
                posService.getHeldOrders(cashierEmail)
        );
    }

    /**
     * Xem chi tiết phiếu treo.
     *
     * GET /api/admin/pos/held-orders/{orderId}
     */
    @GetMapping("/held-orders/{orderId}")
    public ResponseEntity<PosOrderResponse> getHeldOrderDetail(
            @PathVariable Integer orderId,
            Authentication authentication
    ) {
        String cashierEmail = getCurrentEmail(authentication);

        return ResponseEntity.ok(
                posService.getHeldOrderDetail(orderId, cashierEmail)
        );
    }

    /**
     * Thanh toán phiếu treo.
     *
     * POST /api/admin/pos/held-orders/{orderId}/checkout
     */
    @PostMapping("/held-orders/{orderId}/checkout")
    public ResponseEntity<PosOrderResponse> checkoutHeldOrder(
            @PathVariable Integer orderId,
            @Valid @RequestBody PosHeldOrderCheckoutRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String cashierEmail = getCurrentEmail(authentication);
        String clientIp = getClientIp(httpRequest);

        return ResponseEntity.ok(
                posService.checkoutHeldOrder(orderId, request, cashierEmail, clientIp)
        );
    }

    /**
     * Chuyển phiếu treo cho nhân viên khác.
     *
     * PATCH /api/admin/pos/held-orders/{orderId}/transfer
     */
    @PatchMapping("/held-orders/{orderId}/transfer")
    public ResponseEntity<PosHeldOrderResponse> transferHeldOrder(
            @PathVariable Integer orderId,
            @Valid @RequestBody PosTransferHeldOrderRequest request,
            Authentication authentication
    ) {
        String cashierEmail = getCurrentEmail(authentication);

        return ResponseEntity.ok(
                posService.transferHeldOrder(orderId, request, cashierEmail)
        );
    }

    /**
     * Hủy phiếu treo.
     *
     * PATCH /api/admin/pos/held-orders/{orderId}/cancel
     */
    @PatchMapping("/held-orders/{orderId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelHeldOrder(
            @PathVariable Integer orderId,
            Authentication authentication
    ) {
        String cashierEmail = getCurrentEmail(authentication);

        return ResponseEntity.ok(
                posService.cancelHeldOrder(orderId, cashierEmail)
        );
    }

    /**
     * Danh sách nhân viên nhận phiếu.
     *
     * GET /api/admin/pos/transfer-targets
     */
    @GetMapping("/transfer-targets")
    public ResponseEntity<List<PosTransferTargetResponse>> getTransferTargets(
            Authentication authentication
    ) {
        String cashierEmail = getCurrentEmail(authentication);

        return ResponseEntity.ok(
                posService.getTransferTargets(cashierEmail)
        );
    }

    private String getCurrentEmail(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");

        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        return request.getRemoteAddr();
    }
    @PostMapping("/orders/{orderId}/vietqr/confirm")
    public ResponseEntity<?> confirmVietQrPayment(
            @PathVariable Integer orderId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                posService.confirmVietQrPayment(orderId, authentication.getName())
        );
    }
}