package org.example.datn_sd69.modules.order.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class OrderMailTemplateService {

    private static final String ORDER_CREATED_TEMPLATE = "classpath:templates/mail/order-created.html";
    private static final String ORDER_STATUS_TEMPLATE = "classpath:templates/mail/order-status.html";

    private final ResourceLoader resourceLoader;

    public OrderMailTemplateService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String renderOrderCreated(OrderMailTemplateModel model) {
        return render(ORDER_CREATED_TEMPLATE, model);
    }

    public String renderOrderStatus(OrderMailTemplateModel model) {
        return render(ORDER_STATUS_TEMPLATE, model);
    }

    private String render(String templatePath, OrderMailTemplateModel model) {
        String template = loadTemplate(templatePath);

        if (model == null) {
            return template;
        }

        Map<String, String> variables = model.toMap();

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            template = replaceVariable(
                    template,
                    entry.getKey(),
                    safe(entry.getValue())
            );
        }

        return template;
    }

    /**
     * Hỗ trợ cả 4 kiểu placeholder:
     * {{shopName}}
     * {{ shopName }}
     * {{shopName }}
     * {{ shopName}}
     */
    private String replaceVariable(String template, String key, String value) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        if (key == null || key.isBlank()) {
            return template;
        }

        String regex = "\\{\\{\\s*" + Pattern.quote(key) + "\\s*}}";

        return Pattern.compile(regex)
                .matcher(template)
                .replaceAll(Matcher.quoteReplacement(safe(value)));
    }

    private String loadTemplate(String templatePath) {
        try {
            Resource resource = resourceLoader.getResource(templatePath);

            if (!resource.exists()) {
                throw new IllegalStateException("Không tìm thấy template mail: " + templatePath);
            }

            try (InputStream inputStream = resource.getInputStream()) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception exception) {
            log.warn("Không đọc được template mail {}: {}", templatePath, exception.getMessage());
            throw new IllegalStateException("Không đọc được template mail: " + templatePath, exception);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public record OrderMailTemplateModel(
            String shopName,
            String preheader,
            String title,
            String mainMessage,
            String badgeText,
            String badgeBgColor,
            String badgeTextColor,
            String orderCode,
            String customerName,
            String customerPhone,
            String shippingAddress,
            String paymentMethod,
            String statusText,
            String createdAt,
            String totalAmount,
            String discountAmount,
            String shippingFee,
            String finalAmount,
            String itemRows,
            String extraBlock,
            String orderDetailUrl,
            String supportText,
            String footerText
    ) {
        private Map<String, String> toMap() {
            Map<String, String> variables = new LinkedHashMap<>();

            variables.put("shopName", shopName);
            variables.put("preheader", preheader);
            variables.put("title", title);
            variables.put("mainMessage", mainMessage);
            variables.put("badgeText", badgeText);
            variables.put("badgeBgColor", badgeBgColor);
            variables.put("badgeTextColor", badgeTextColor);
            variables.put("orderCode", orderCode);
            variables.put("customerName", customerName);
            variables.put("customerPhone", customerPhone);
            variables.put("shippingAddress", shippingAddress);
            variables.put("paymentMethod", paymentMethod);
            variables.put("statusText", statusText);
            variables.put("createdAt", createdAt);
            variables.put("totalAmount", totalAmount);
            variables.put("discountAmount", discountAmount);
            variables.put("shippingFee", shippingFee);
            variables.put("finalAmount", finalAmount);
            variables.put("itemRows", itemRows);
            variables.put("extraBlock", extraBlock);
            variables.put("orderDetailUrl", orderDetailUrl);
            variables.put("supportText", supportText);
            variables.put("footerText", footerText);

            return variables;
        }
    }
}