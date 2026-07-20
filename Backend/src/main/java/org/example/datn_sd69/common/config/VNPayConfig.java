package org.example.datn_sd69.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Configuration
public class VNPayConfig {

    @Value("${vnpay.tmnCode}")
    private String vnp_TmnCode;

    @Value("${vnpay.hashSecret}")
    private String secretKey;

    @Value("${vnpay.url}")
    private String vnp_PayUrl;

    /**
     * Return URL cũ, giữ lại cho POS hoặc code cũ.
     * Hiện tại route này là /payment/result.
     */
    @Value("${vnpay.returnUrl}")
    private String vnp_ReturnUrl;

    /**
     * Return URL riêng cho POS.
     */
    @Value("${vnpay.posReturnUrl:${vnpay.returnUrl}}")
    private String vnp_PosReturnUrl;

    /**
     * Return URL riêng cho online checkout của khách.
     */
    @Value("${vnpay.onlineReturnUrl}")
    private String vnp_OnlineReturnUrl;

    @Value("${vnpay.version}")
    private String vnp_Version;

    private final SecureRandom secureRandom = new SecureRandom();

    public String getVnp_TmnCode() {
        return vnp_TmnCode;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getVnp_PayUrl() {
        return vnp_PayUrl;
    }

    /**
     * Getter cũ, không xóa để tránh vỡ POS.
     */
    public String getVnp_ReturnUrl() {
        return vnp_ReturnUrl;
    }

    public String getVnp_PosReturnUrl() {
        return vnp_PosReturnUrl;
    }

    public String getVnp_OnlineReturnUrl() {
        return vnp_OnlineReturnUrl;
    }

    public String getVnp_Version() {
        return vnp_Version;
    }

    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                return "";
            }

            Mac hmac512 = Mac.getInstance("HmacSHA512");

            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA512"
            );

            hmac512.init(secretKeySpec);

            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder(result.length * 2);

            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }

            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    public String getRandomNumber(int len) {
        if (len <= 0) {
            return "";
        }

        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }

        return sb.toString();
    }

    public static String getIpAddress(jakarta.servlet.http.HttpServletRequest request) {
        String ipAdress;
        try {
            ipAdress = request.getHeader("X-FORWARDED-FOR");
            if (ipAdress == null) {
                ipAdress = request.getRemoteAddr();
            }
        } catch (Exception e) {
            ipAdress = "Invalid IP";
        }
        return ipAdress;
    }
}