package org.example.datn_sd69.modules.pos.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.pos.dto.request.PosCheckoutRequest;
import org.example.datn_sd69.modules.pos.dto.request.PosHeldOrderCheckoutRequest;
import org.example.datn_sd69.modules.pos.dto.request.PosHoldRequest;
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
     * Không dùng /api/admin/products để tránh phụ thuộc module quản trị sản phẩm.
     * API này trả đúng dữ liệu POS cần:
     * SKU, giá, tồn kho, NSX, HSD, trạng thái, sellable, lý do không bán được.
     */
    @GetMapping("/products")
    public ResponseEntity<List<ProductVariantPosResponse>> getProductsForPos() {
        return ResponseEntity.ok(posService.getProductsForPos());
    }

    /**
     * Quét SKU/barcode tại POS.
     * Nếu sản phẩm không bán được, BE trả lỗi luôn.
     */
    @GetMapping("/product")
    public ResponseEntity<ProductVariantPosResponse> findProductBySku(
            @RequestParam String sku
    ) {
        return ResponseEntity.ok(posService.findVariantBySku(sku));
    }

    @GetMapping("/customer")
    public ResponseEntity<?> findCustomerByPhone(
            @RequestParam String phone
    ) {
        CustomerPosResponse customer = posService.findCustomerByPhone(phone);

        if (customer == null) {
            return ResponseEntity.ok(Map.of(
                    "found", false,
                    "message", "Không tìm thấy khách hàng. Vui lòng nhập họ tên, số điện thoại và email để tạo khách mới."
            ));
        }

        return ResponseEntity.ok(Map.of(
                "found", true,
                "customer", customer
        ));
    }

    @PostMapping("/checkout")
    public ResponseEntity<PosOrderResponse> checkout(
            @Valid @RequestBody PosCheckoutRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String cashierEmail = authentication != null
                ? authentication.getName()
                : null;

        String clientIp = getClientIp(httpRequest);

        return ResponseEntity.ok(
                posService.checkout(request, cashierEmail, clientIp)
        );
    }

    @PostMapping("/hold")
    public ResponseEntity<PosOrderResponse> holdOrder(
            @Valid @RequestBody PosHoldRequest request,
            Authentication authentication
    ) {
        String cashierEmail = authentication != null
                ? authentication.getName()
                : null;

        return ResponseEntity.ok(
                posService.holdOrder(request, cashierEmail)
        );
    }

    @GetMapping("/held-orders")
    public ResponseEntity<List<PosHeldOrderResponse>> getHeldOrders(
            Authentication authentication
    ) {
        String cashierEmail = authentication != null
                ? authentication.getName()
                : null;

        return ResponseEntity.ok(
                posService.getHeldOrders(cashierEmail)
        );
    }

    @GetMapping("/held-orders/{orderId}")
    public ResponseEntity<PosOrderResponse> getHeldOrderDetail(
            @PathVariable Integer orderId,
            Authentication authentication
    ) {
        String cashierEmail = authentication != null
                ? authentication.getName()
                : null;

        return ResponseEntity.ok(
                posService.getHeldOrderDetail(orderId, cashierEmail)
        );
    }

    @PostMapping("/held-orders/{orderId}/checkout")
    public ResponseEntity<PosOrderResponse> checkoutHeldOrder(
            @PathVariable Integer orderId,
            @Valid @RequestBody PosHeldOrderCheckoutRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String cashierEmail = authentication != null
                ? authentication.getName()
                : null;

        String clientIp = getClientIp(httpRequest);

        return ResponseEntity.ok(
                posService.checkoutHeldOrder(orderId, request, cashierEmail, clientIp)
        );
    }

    @PatchMapping("/held-orders/{orderId}/transfer")
    public ResponseEntity<PosHeldOrderResponse> transferHeldOrder(
            @PathVariable Integer orderId,
            @Valid @RequestBody PosTransferHeldOrderRequest request,
            Authentication authentication
    ) {
        String cashierEmail = authentication != null
                ? authentication.getName()
                : null;

        return ResponseEntity.ok(
                posService.transferHeldOrder(orderId, request, cashierEmail)
        );
    }

    /**
     * API cho FE mở modal chọn nhân viên nhận phiếu.
     *
     * Không bắt thu ngân nhập UserId/EmployeeId thủ công.
     */
    @GetMapping("/transfer-targets")
    public ResponseEntity<List<PosTransferTargetResponse>> getTransferTargets(
            Authentication authentication
    ) {
        String cashierEmail = authentication != null
                ? authentication.getName()
                : null;

        return ResponseEntity.ok(
                posService.getTransferTargets(cashierEmail)
        );
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
}