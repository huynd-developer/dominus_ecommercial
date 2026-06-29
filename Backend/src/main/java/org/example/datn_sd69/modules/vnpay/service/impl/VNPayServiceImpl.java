package org.example.datn_sd69.modules.vnpay.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.common.config.VNPayConfig;
import org.example.datn_sd69.modules.vnpay.service.VNPayService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VNPayServiceImpl implements VNPayService {

    private final VNPayConfig vnPayConfig;

    @Override
    public String createPaymentUrl(Integer orderId, BigDecimal amount, String clientIp) {

        // VNPay yêu cầu amount * 100 (VND, không dùng thập phân)
        long vnpAmount = amount.multiply(BigDecimal.valueOf(100)).longValue();

        // Mã giao dịch: ghép orderId + random 6 số → dễ parse lại khi callback
        String txnRef = orderId + vnPayConfig.getRandomNumber(6);

        // Thời gian theo GMT+7
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss");
        String createDate = fmt.format(cld.getTime());
        cld.add(Calendar.MINUTE, 15); // Hết hạn QR sau 15 phút
        String expireDate = fmt.format(cld.getTime());

        // Tham số PHẢI sắp xếp theo alphabet — TreeMap tự xử lý
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version",    vnPayConfig.getVnp_Version());
        params.put("vnp_Command",    "pay");
        params.put("vnp_TmnCode",    vnPayConfig.getVnp_TmnCode());
        params.put("vnp_Amount",     String.valueOf(vnpAmount));
        params.put("vnp_CurrCode",   "VND");
        params.put("vnp_TxnRef",     txnRef);
        params.put("vnp_OrderInfo",  "Thanh toan don hang " + orderId);
        params.put("vnp_OrderType",  "billpayment");
        params.put("vnp_Locale",     "vn");
        params.put("vnp_ReturnUrl",  vnPayConfig.getVnp_ReturnUrl());
        params.put("vnp_IpAddr",     clientIp);
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", expireDate);

        // Build chuỗi hash data và query string
        StringBuilder hashData = new StringBuilder();
        StringBuilder queryStr = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String encodedKey   = URLEncoder.encode(entry.getKey(),   StandardCharsets.US_ASCII);
            String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII);

            hashData.append(entry.getKey()).append('=').append(encodedValue).append('&');
            queryStr.append(encodedKey).append('=').append(encodedValue).append('&');
        }

        // Xóa dấu '&' thừa cuối chuỗi
        hashData.deleteCharAt(hashData.length() - 1);
        queryStr.deleteCharAt(queryStr.length() - 1);

        // Ký HMAC-SHA512
        String secureHash = vnPayConfig.hmacSHA512(vnPayConfig.getSecretKey(), hashData.toString());
        queryStr.append("&vnp_SecureHash=").append(secureHash);

        return vnPayConfig.getVnp_PayUrl() + "?" + queryStr;
    }

    @Override
    public boolean verifyCallback(Map<String, String> params) {
        // Tách secureHash ra trước — KHÔNG đưa vào chuỗi ký
        String receivedHash = params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");

        // Sắp xếp alphabet và build lại chuỗi hash
        Map<String, String> sorted = new TreeMap<>(params);
        StringBuilder hashData = new StringBuilder();

        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII);
            hashData.append(entry.getKey()).append('=').append(encodedValue).append('&');
        }
        hashData.deleteCharAt(hashData.length() - 1);

        String calculatedHash = vnPayConfig.hmacSHA512(vnPayConfig.getSecretKey(), hashData.toString());
        return calculatedHash.equalsIgnoreCase(receivedHash);
    }
}