package org.example.datn_sd69.modules.pos.service;

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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PosService {

    /**
     * Danh sách sản phẩm/biến thể dành riêng cho POS.
     */
    List<ProductVariantPosResponse> getProductsForPos();

    /**
     * Quét SKU/barcode tại POS.
     */
    ProductVariantPosResponse findVariantBySku(String sku);

    /**
     * Tìm khách hàng theo số điện thoại.
     *
     * API này chỉ tìm, không tạo khách.
     */
    CustomerPosResponse findCustomerByPhone(String phone);

    /**
     * Lưu khách hàng tại POS trước khi thanh toán/treo phiếu.
     *
     * Dùng cho nút "Lưu khách hàng" ở FE.
     */
    CustomerPosResponse saveCustomerForPos(PosSaveCustomerRequest request);

    /**
     * Thanh toán đơn POS.
     */
    PosOrderResponse checkout(
            PosCheckoutRequest request,
            String cashierEmail,
            String clientIp
    );

    /**
     * Treo phiếu POS.
     */
    PosOrderResponse holdOrder(
            PosHoldRequest request,
            String cashierEmail
    );

    /**
     * Danh sách phiếu treo.
     */
    List<PosHeldOrderResponse> getHeldOrders(String cashierEmail);

    /**
     * Chi tiết phiếu treo.
     */
    PosOrderResponse getHeldOrderDetail(
            Integer orderId,
            String cashierEmail
    );

    /**
     * Thanh toán phiếu treo.
     */
    PosOrderResponse checkoutHeldOrder(
            Integer orderId,
            PosHeldOrderCheckoutRequest request,
            String cashierEmail,
            String clientIp
    );

    /**
     * Chuyển phiếu treo cho nhân viên khác.
     */
    PosHeldOrderResponse transferHeldOrder(
            Integer orderId,
            PosTransferHeldOrderRequest request,
            String cashierEmail
    );

    /**
     * Danh sách nhân viên có thể nhận phiếu treo.
     *
     * FE dùng API này để hiển thị modal chọn nhân viên,
     * không bắt thu ngân nhập UserId/EmployeeId thủ công.
     */
    List<PosTransferTargetResponse> getTransferTargets(String cashierEmail);

    /**
     * Hủy phiếu treo.
     */
    Map<String, Object> cancelHeldOrder(
            Integer orderId,
            String cashierEmail
    );

    /**
     * Áp voucher tại POS.
     */
    Map<String, Object> applyVoucher(
            String voucherCode,
            BigDecimal totalAmount
    );
}