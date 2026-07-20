package org.example.datn_sd69.modules.vietqr.service;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.vietqr.config.VietQrProperties;
import org.example.datn_sd69.modules.vietqr.dto.VietQrResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class VietQrService {

    private final VietQrProperties properties;

    public VietQrResponse createPaymentQr(Integer orderId, BigDecimal amount) {
        if (orderId == null) {
            throw new RuntimeException("Mã hóa đơn không hợp lệ khi tạo VietQR.");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Số tiền tạo VietQR phải lớn hơn 0.");
        }

        String bankBin = cleanRequired(properties.getBankBin(), "Thiếu cấu hình vietqr.bank-bin");
        String accountNo = cleanRequired(properties.getAccountNo(), "Thiếu cấu hình vietqr.account-no");
        String accountName = cleanRequired(properties.getAccountName(), "Thiếu cấu hình vietqr.account-name");
        String bankCode = cleanRequired(properties.getBankCode(), "Thiếu cấu hình vietqr.bank-code");

        String template = properties.getTemplate() == null || properties.getTemplate().trim().isBlank()
                ? "compact2"
                : properties.getTemplate().trim();

        String transferContent = "POSDH" + orderId;
        String encodedContent = URLEncoder.encode(transferContent, StandardCharsets.UTF_8);
        String encodedAccountName = URLEncoder.encode(
                accountName.toUpperCase(Locale.ROOT),
                StandardCharsets.UTF_8
        );

        BigDecimal roundedAmount = amount.setScale(0, BigDecimal.ROUND_DOWN);

        /*
         * Link ảnh VietQR dạng public image.
         * Khách quét QR bằng app ngân hàng.
         */
        String qrImageUrl = "https://img.vietqr.io/image/"
                + bankBin
                + "-"
                + accountNo
                + "-"
                + template
                + ".png"
                + "?amount="
                + roundedAmount.toPlainString()
                + "&addInfo="
                + encodedContent
                + "&accountName="
                + encodedAccountName;

        return VietQrResponse.builder()
                .orderId(orderId)
                .bankCode(bankCode)
                .bankBin(bankBin)
                .accountNo(accountNo)
                .accountName(accountName)
                .amount(roundedAmount)
                .transferContent(transferContent)
                .qrImageUrl(qrImageUrl)
                .build();
    }

    private String cleanRequired(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new RuntimeException(message);
        }

        return value.trim();
    }
}