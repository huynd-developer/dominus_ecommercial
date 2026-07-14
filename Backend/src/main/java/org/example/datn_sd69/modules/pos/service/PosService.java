package org.example.datn_sd69.modules.pos.service;

import org.example.datn_sd69.modules.pos.dto.request.PosCheckoutRequest;
import org.example.datn_sd69.modules.pos.dto.request.PosHeldOrderCheckoutRequest;
import org.example.datn_sd69.modules.pos.dto.request.PosHoldRequest;
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

    ProductVariantPosResponse findVariantBySku(String sku);

    CustomerPosResponse findCustomerByPhone(String phone);

    PosOrderResponse checkout(
            PosCheckoutRequest request,
            String cashierEmail,
            String clientIp
    );

    PosOrderResponse holdOrder(
            PosHoldRequest request,
            String cashierEmail
    );

    List<PosHeldOrderResponse> getHeldOrders(String cashierEmail);

    PosOrderResponse getHeldOrderDetail(
            Integer orderId,
            String cashierEmail
    );

    PosOrderResponse checkoutHeldOrder(
            Integer orderId,
            PosHeldOrderCheckoutRequest request,
            String cashierEmail,
            String clientIp
    );

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
    List<ProductVariantPosResponse> getProductsForPos();
    Map<String, Object> cancelHeldOrder(Integer orderId, String cashierEmail);
    Map<String, Object> applyVoucher(String voucherCode, BigDecimal totalAmount);
}