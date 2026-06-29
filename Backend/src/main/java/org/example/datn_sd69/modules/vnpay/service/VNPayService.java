package org.example.datn_sd69.modules.vnpay.service;

import java.math.BigDecimal;
import java.util.Map;

public interface VNPayService {

    /**
     * Sinh URL thanh toán VNPay (dùng cho cả POS và online)
     * FE dùng URL này để redirect hoặc render QR code
     */
    String createPaymentUrl(Integer orderId, BigDecimal amount, String clientIp);

    /**
     * Xác thực chữ ký HMAC-SHA512 từ VNPay gọi về
     * Trả về true nếu hợp lệ
     */
    boolean verifyCallback(Map<String, String> params);
}