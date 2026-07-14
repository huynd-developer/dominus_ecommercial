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

    private static final String VNPAY_COMMAND_PAY = "pay";
    private static final String VNPAY_CURRENCY_VND = "VND";
    private static final String VNPAY_LOCALE_VN = "vn";
    private static final String VNPAY_ORDER_TYPE = "billpayment";
    private static final String DEFAULT_CLIENT_IP = "127.0.0.1";
    private static final int PAYMENT_EXPIRE_MINUTES = 15;

    private final VNPayConfig vnPayConfig;

    @Override
    public String createPaymentUrl(Integer orderId, BigDecimal amount, String clientIp) {
        validateCreatePaymentInput(orderId, amount);
        validateConfig();

        String createDate = currentVnPayDate();
        String expireDate = expireVnPayDate(PAYMENT_EXPIRE_MINUTES);

        long vnpAmount = toVnpayAmountUnit(amount);

        /*
         * Format mới có dấu "_" để parse orderId an toàn.
         *
         * Ví dụ:
         * 132_20260714112030_123456
         *
         * Không ảnh hưởng logic cũ vì vnp_OrderInfo vẫn giữ:
         * "Thanh toan don hang 132"
         */
        String txnRef = orderId + "_" + createDate + "_" + vnPayConfig.getRandomNumber(6);

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", vnPayConfig.getVnp_Version());
        params.put("vnp_Command", VNPAY_COMMAND_PAY);
        params.put("vnp_TmnCode", vnPayConfig.getVnp_TmnCode());
        params.put("vnp_Amount", String.valueOf(vnpAmount));
        params.put("vnp_CurrCode", VNPAY_CURRENCY_VND);
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", "Thanh toan don hang " + orderId);
        params.put("vnp_OrderType", VNPAY_ORDER_TYPE);
        params.put("vnp_Locale", VNPAY_LOCALE_VN);
        params.put("vnp_ReturnUrl", vnPayConfig.getVnp_ReturnUrl());
        params.put("vnp_IpAddr", normalizeClientIp(clientIp));
        params.put("vnp_CreateDate", createDate);
        params.put("vnp_ExpireDate", expireDate);

        String hashData = buildHashData(params);
        String queryString = buildQueryString(params);

        String secureHash = vnPayConfig.hmacSHA512(
                vnPayConfig.getSecretKey(),
                hashData
        );

        return vnPayConfig.getVnp_PayUrl()
                + "?"
                + queryString
                + "&vnp_SecureHash="
                + secureHash;
    }

    @Override
    public boolean verifyCallback(Map<String, String> params) {
        try {
            if (params == null || params.isEmpty()) {
                return false;
            }

            /*
             * Không remove trực tiếp trên params gốc.
             * Controller còn cần dùng lại params để đọc orderId, amount, responseCode.
             */
            Map<String, String> fields = new TreeMap<>(params);

            String receivedHash = fields.remove("vnp_SecureHash");
            fields.remove("vnp_SecureHashType");

            if (receivedHash == null || receivedHash.trim().isEmpty()) {
                return false;
            }

            String hashData = buildHashData(fields);

            if (hashData.isBlank()) {
                return false;
            }

            String calculatedHash = vnPayConfig.hmacSHA512(
                    vnPayConfig.getSecretKey(),
                    hashData
            );

            return calculatedHash.equalsIgnoreCase(receivedHash);
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public Integer extractOrderIdFromTxnRef(String txnRef) {
        if (txnRef == null || txnRef.trim().isEmpty()) {
            return null;
        }

        String cleanTxnRef = txnRef.trim();

        /*
         * Format mới:
         * 132_20260714112030_123456
         *
         * Format cũ:
         * 132123456
         *
         * Với format cũ không đoán orderId vì có thể nhầm.
         * Controller sẽ fallback parse từ vnp_OrderInfo.
         */
        if (!cleanTxnRef.contains("_")) {
            return null;
        }

        String rawOrderId = cleanTxnRef.substring(0, cleanTxnRef.indexOf("_"));

        if (!rawOrderId.matches("\\d+")) {
            return null;
        }

        try {
            return Integer.parseInt(rawOrderId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    public long toVnpayAmountUnit(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }

        return amount
                .setScale(0, RoundingMode.DOWN)
                .multiply(BigDecimal.valueOf(100))
                .longValue();
    }

    @Override
    public BigDecimal parseVnpayAmountToVnd(String rawVnpAmount) {
        try {
            if (rawVnpAmount == null || rawVnpAmount.trim().isEmpty()) {
                return BigDecimal.ZERO;
            }

            return new BigDecimal(rawVnpAmount.trim())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private void validateCreatePaymentInput(Integer orderId, BigDecimal amount) {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Mã đơn hàng không hợp lệ khi tạo thanh toán VNPay");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán VNPay phải lớn hơn 0");
        }
    }

    private void validateConfig() {
        if (isBlank(vnPayConfig.getVnp_Version())) {
            throw new IllegalStateException("Thiếu cấu hình vnp_Version");
        }

        if (isBlank(vnPayConfig.getVnp_TmnCode())) {
            throw new IllegalStateException("Thiếu cấu hình vnp_TmnCode");
        }

        if (isBlank(vnPayConfig.getVnp_ReturnUrl())) {
            throw new IllegalStateException("Thiếu cấu hình vnp_ReturnUrl");
        }

        if (isBlank(vnPayConfig.getVnp_PayUrl())) {
            throw new IllegalStateException("Thiếu cấu hình vnp_PayUrl");
        }

        if (isBlank(vnPayConfig.getSecretKey())) {
            throw new IllegalStateException("Thiếu cấu hình secretKey VNPay");
        }
    }

    private String normalizeClientIp(String clientIp) {
        if (clientIp == null || clientIp.trim().isEmpty()) {
            return DEFAULT_CLIENT_IP;
        }

        return clientIp.trim();
    }

    private String currentVnPayDate() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        return formatter.format(calendar.getTime());
    }

    private String expireVnPayDate(int plusMinutes) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        calendar.add(Calendar.MINUTE, plusMinutes);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        return formatter.format(calendar.getTime());
    }

    private String buildHashData(Map<String, String> params) {
        StringBuilder hashData = new StringBuilder();

        for (Map.Entry<String, String> entry : new TreeMap<>(params).entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (isBlank(key) || isBlank(value)) {
                continue;
            }

            if (hashData.length() > 0) {
                hashData.append("&");
            }

            hashData.append(key)
                    .append("=")
                    .append(encode(value));
        }

        return hashData.toString();
    }

    private String buildQueryString(Map<String, String> params) {
        StringBuilder query = new StringBuilder();

        for (Map.Entry<String, String> entry : new TreeMap<>(params).entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (isBlank(key) || isBlank(value)) {
                continue;
            }

            if (query.length() > 0) {
                query.append("&");
            }

            query.append(encode(key))
                    .append("=")
                    .append(encode(value));
        }

        return query.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(
                value,
                StandardCharsets.US_ASCII
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}