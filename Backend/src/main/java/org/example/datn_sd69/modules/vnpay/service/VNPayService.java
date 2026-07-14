package org.example.datn_sd69.modules.vnpay.service;

import java.math.BigDecimal;
import java.util.Map;

public interface VNPayService {

    /**
     * Sinh URL thanh toán VNPay.
     *
     * Dùng cho:
     * - Online checkout
     * - POS nếu cần thanh toán qua VNPay
     *
     * amount truyền vào là tiền VND thật.
     * VNPay yêu cầu amount * 100.
     */
    String createPaymentUrl(Integer orderId, BigDecimal amount, String clientIp);

    /**
     * Xác thực chữ ký HMAC-SHA512 từ VNPay callback / return / ipn.
     *
     * Không được sửa trực tiếp params gốc.
     */
    boolean verifyCallback(Map<String, String> params);

    /**
     * Lấy orderId từ vnp_TxnRef.
     *
     * Format mới:
     * 132_20260714112030_123456
     *
     * Với format cũ không có dấu "_" thì trả null,
     * controller sẽ fallback lấy orderId từ vnp_OrderInfo để không phá logic cũ.
     */
    Integer extractOrderIdFromTxnRef(String txnRef);

    /**
     * Chuyển số tiền VND sang đơn vị VNPay.
     *
     * 420000 -> 42000000
     */
    long toVnpayAmountUnit(BigDecimal amount);

    /**
     * Chuyển vnp_Amount từ VNPay về VND.
     *
     * 42000000 -> 420000
     */
    BigDecimal parseVnpayAmountToVnd(String rawVnpAmount);
}