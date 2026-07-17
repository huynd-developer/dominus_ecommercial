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
     *
     * Nếu sản phẩm không bán được, BE trả lỗi luôn.
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
     * API này chỉ tìm, không tạo khách.
     *
     * Luồng đúng:
     * - Nếu tìm thấy: FE đổ thông tin khách vào form.
     * - Nếu chưa thấy: FE cho nhập họ tên + email.
     * - Sau đó FE gọi POST /api/admin/pos/customer để lưu khách trước khi thanh toán/treo phiếu.
     */
    @GetMapping("/customer")
    public ResponseEntity<Map<String, Object>> findCustomerByPhone(
            @RequestParam String phone
    ) {
        CustomerPosResponse customer = posService.findCustomerByPhone(phone);

        if (customer == null) {
            return ResponseEntity.ok(Map.of(
                    "found", false,
                    "message", "Không tìm thấy khách hàng. Vui lòng nhập họ tên, số điện thoại và email rồi bấm Lưu khách hàng."
            ));
        }

        return ResponseEntity.ok(Map.of(
                "found", true,
                "customer", customer
        ));
    }

    /**
     * Lưu khách hàng tại POS trước khi thanh toán/treo phiếu.
     *
     * Đúng nghiệp vụ thực tế:
     * - Thu ngân nhập SĐT, họ tên, email.
     * - Bấm "Lưu khách hàng".
     * - BE tạo/cập nhật Customer ngay.
     * - FE nhận lại customerId rồi mới cho thanh toán hoặc treo phiếu.
     *
     * Lưu ý:
     * DB hiện tại Customer.UserId là PK/FK tới Users.Id,
     * nên nếu khách mới hoàn toàn thì BE vẫn tạo Users role USER trước,
     * sau đó tạo Customer.
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
     * Chỉ kiểm tra điều kiện mã và tính số tiền giảm.
     * Chưa tăng usedCount voucher ở bước này.
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
     * CASH: hoàn thành ngay.
     * VNPAY/MIXED: tạo đơn chờ thanh toán VNPay.
     *
     * Về khách hàng:
     * FE nên bắt buộc lưu khách trước khi gọi checkout.
     * BE vẫn giữ kiểm tra/tạo-cập nhật khách trong service để chống dữ liệu lỗi
     * nếu FE reload, bỏ qua bước lưu, hoặc client khác gọi API trực tiếp.
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
     * Treo phiếu mua hàng.
     *
     * Đúng nghiệp vụ hiện tại:
     * - Tạo Order status = 0
     * - PaymentMethod = HOLD
     * - Chưa trừ kho
     * - Khi thanh toán phiếu treo mới re-check tồn kho và trừ kho
     *
     * FE nên bắt buộc lưu khách trước khi treo phiếu.
     * BE vẫn giữ kiểm tra/tạo-cập nhật khách trong service để tránh lỗi dữ liệu.
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
     * Danh sách phiếu treo.
     *
     * CASHIER: chỉ thấy phiếu của mình.
     * MANAGER/OWNER: thấy toàn bộ phiếu treo.
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
     * Phiếu treo chưa trừ kho, nên khi thanh toán phải:
     * - Re-check sản phẩm còn bán được không
     * - Re-check tồn kho
     * - Trừ kho sau khi hợp lệ
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
     * CASHIER: chỉ được chuyển phiếu của mình.
     * MANAGER/OWNER: được chuyển phiếu của nhân viên khác.
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
     * Không hard delete DB.
     * Chỉ đổi trạng thái phiếu sang đã hủy.
     *
     * Với logic hiện tại:
     * - Phiếu treo chưa trừ kho nên không cần hoàn kho
     * - Voucher chưa tăng lượt dùng nên không cần hoàn voucher
     * - Giữ lại Order/OrderItem để truy vết nghiệp vụ
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
     * API cho FE mở modal chọn nhân viên nhận phiếu.
     *
     * Không bắt thu ngân nhập UserId/EmployeeId thủ công.
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
}