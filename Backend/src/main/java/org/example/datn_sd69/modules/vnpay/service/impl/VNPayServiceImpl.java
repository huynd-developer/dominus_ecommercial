package org.example.datn_sd69.modules.vnpay.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.common.config.VNPayConfig;
import org.example.datn_sd69.modules.vnpay.service.VNPayService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class VNPayServiceImpl implements VNPayService {

    private final VNPayConfig vnPayConfig;

    @Override
    public String createPaymentUrl(Integer orderId, BigDecimal amount, String clientIp) {
        if (orderId == null) {
            throw new RuntimeException("Mã đơn hàng không được để trống khi tạo thanh toán VNPay");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Số tiền thanh toán VNPay phải lớn hơn 0");
        }

        BigDecimal cleanAmount = amount.setScale(0, RoundingMode.DOWN);
        long vnpAmount = cleanAmount.multiply(BigDecimal.valueOf(100)).longValue();

        String txnRef = orderId + vnPayConfig.getRandomNumber(6);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss");
        fmt.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        String createDate = fmt.format(cld.getTime());

        cld.add(Calendar.MINUTE, 15);
        String expireDate = fmt.format(cld.getTime());

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", vnPayConfig.getVnp_Version());
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnPayConfig.getVnp_TmnCode());
        params.put("vnp_Amount", String.valueOf(vnpAmount));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", "Thanh toan don hang " + orderId);
        params.put("vnp_OrderType", "billpayment");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", vnPayConfig.getVnp_ReturnUrl());
        params.put("vnp_IpAddr", clientIp != null && !clientIp.isBlank() ? clientIp : "127.0.0.1");
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", expireDate);

        StringBuilder hashData = new StringBuilder();
        StringBuilder queryStr = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String encodedKey = URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII);
            String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII);

            hashData.append(entry.getKey()).append("=").append(encodedValue).append("&");
            queryStr.append(encodedKey).append("=").append(encodedValue).append("&");
        }

        hashData.deleteCharAt(hashData.length() - 1);
        queryStr.deleteCharAt(queryStr.length() - 1);

        String secureHash = vnPayConfig.hmacSHA512(
                vnPayConfig.getSecretKey(),
                hashData.toString()
        );

        queryStr.append("&vnp_SecureHash=").append(secureHash);

        return vnPayConfig.getVnp_PayUrl() + "?" + queryStr;
    }

    @Override
    public boolean verifyCallback(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return false;
        }

        String receivedHash = params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");

        if (receivedHash == null || receivedHash.isBlank()) {
            return false;
        }

        Map<String, String> sorted = new TreeMap<>(params);
        StringBuilder hashData = new StringBuilder();

        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII);
            hashData.append(entry.getKey()).append("=").append(encodedValue).append("&");
        }

        if (hashData.length() == 0) {
            return false;
        }

        hashData.deleteCharAt(hashData.length() - 1);

        String calculatedHash = vnPayConfig.hmacSHA512(
                vnPayConfig.getSecretKey(),
                hashData.toString()
        );

        return calculatedHash.equalsIgnoreCase(receivedHash);
    }
}