package org.example.datn_sd69.modules.order.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.datn_sd69.entity.Customer;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderMailService {

    private static final Locale VIETNAM_LOCALE = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String TONE_SUCCESS = "SUCCESS";
    private static final String TONE_INFO = "INFO";
    private static final String TONE_WARNING = "WARNING";
    private static final String TONE_DANGER = "DANGER";

    private final JavaMailSender javaMailSender;
    private final OrderItemRepository orderItemRepository;
    private final OrderMailTemplateService orderMailTemplateService;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.shop-name:LUXORA PERFUME}")
    private String shopName;

    @Value("${app.mail.from:}")
    private String mailFrom;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public void sendOrderPlaced(Order order) {
        String paymentMethod = normalizeText(order == null ? null : order.getPaymentMethod());
        String message;

        if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
            message = "Đơn hàng của bạn đã được tạo. Vui lòng hoàn tất thanh toán VNPay để shop tiếp nhận xử lý.";
        } else if ("VIETQR".equalsIgnoreCase(paymentMethod)) {
            message = "Đơn hàng của bạn đã được tạo. Vui lòng chuyển khoản hoặc báo thanh toán để shop tiếp nhận xử lý.";
        } else {
            message = "Đơn hàng của bạn đã được tạo thành công và đang chờ shop xác nhận.";
        }

        sendOrderMail(
                order,
                "Đặt hàng thành công - " + resolveOrderCode(order),
                "Đặt hàng thành công",
                message,
                "Chờ xác nhận",
                TONE_WARNING,
                null,
                true
        );
    }

    public void sendPaymentSuccess(Order order) {
        sendOrderMail(
                order,
                "Thanh toán VNPay thành công - " + resolveOrderCode(order),
                "Thanh toán thành công",
                "Shop đã ghi nhận thanh toán VNPay thành công. Đơn hàng vẫn đang ở trạng thái Chờ xác nhận và sẽ được shop xử lý sớm.",
                "Đã thanh toán",
                TONE_SUCCESS,
                null,
                false
        );
    }

    public void sendOrderConfirmed(Order order) {
        sendOrderMail(
                order,
                "Đơn hàng đã được xác nhận - " + resolveOrderCode(order),
                "Đơn hàng đã được xác nhận",
                "Shop đã xác nhận đơn hàng của bạn và bắt đầu chuẩn bị hàng.",
                "Đã xác nhận",
                TONE_SUCCESS,
                null,
                false
        );
    }

    public void sendOrderCancelled(Order order, String reason) {
        boolean isAwaitingRefund = order != null && Integer.valueOf(8).equals(order.getStatus());

        String subject = isAwaitingRefund ? "Đã hủy & Chờ hoàn tiền - " : "Đơn hàng đã bị hủy - ";
        String title = isAwaitingRefund ? "Đã hủy & Chờ hoàn tiền" : "Đơn hàng đã bị hủy";
        String mainMessage = isAwaitingRefund
                ? "Đơn hàng của bạn đã bị hủy. Vui lòng cập nhật số tài khoản ngân hàng trong lịch sử đơn hàng để shop tiến hành hoàn tiền."
                : "Đơn hàng của bạn đã được cập nhật sang trạng thái Đã hủy.";
        String badgeText = isAwaitingRefund ? "Chờ hoàn tiền" : "Đã hủy";
        String tone = isAwaitingRefund ? TONE_WARNING : TONE_DANGER;

        sendOrderMail(
                order,
                subject + resolveOrderCode(order),
                title,
                mainMessage,
                badgeText,
                tone,
                "Lý do hủy: " + normalizeFallback(reason, "Không có"),
                false
        );
    }

    public void sendOrderAutoCancelled(Order order) {
        sendOrderMail(
                order,
                "Đơn hàng đã tự động hủy - " + resolveOrderCode(order),
                "Đơn hàng đã tự động hủy",
                "Đơn hàng của bạn đã quá thời gian thanh toán hoặc xác nhận thanh toán nên hệ thống tự động hủy.",
                "Tự động hủy",
                TONE_DANGER,
                null,
                false
        );
    }

    public void sendDeliveryCompleted(Order order) {
        sendOrderMail(
                order,
                "Giao hàng thành công - " + resolveOrderCode(order),
                "Giao hàng thành công",
                "Đơn hàng của bạn đã được xác nhận giao hàng thành công. Cảm ơn bạn đã mua hàng tại " + resolveShopName() + ".",
                "Hoàn thành",
                TONE_SUCCESS,
                null,
                false
        );
    }

    public void sendDeliveryFailed(Order order) {
        String extra = "Lý do giao thất bại: " + normalizeFallback(order == null ? null : order.getDeliveryFailedReason(), "Không có");
        String description = normalizeText(order == null ? null : order.getDeliveryFailedDescription());

        if (description != null) {
            extra += "\nMô tả: " + description;
        }

        sendOrderMail(
                order,
                "Giao hàng thất bại - " + resolveOrderCode(order),
                "Giao hàng thất bại",
                "Đơn hàng của bạn đã được cập nhật là giao hàng thất bại.",
                "Giao thất bại",
                TONE_DANGER,
                extra,
                false
        );
    }

    public void sendDeliveryRefunded(Order order) {
        sendOrderMail(
                order,
                "Đã hoàn tiền đơn giao thất bại - " + resolveOrderCode(order),
                "Đã hoàn tiền",
                "Shop đã xác nhận hoàn tiền cho đơn giao hàng thất bại.",
                "Đã hoàn tiền",
                TONE_SUCCESS,
                "Số tiền hoàn: " + formatMoney(order == null ? null : order.getDeliveryRefundAmount()),
                false
        );
    }

    // Gửi mail khi admin đã xác nhận hoàn tiền cho đơn hủy Online
    public void sendCancelRefunded(Order order) {
        sendOrderMail(
                order,
                "Đã hoàn tiền đơn hủy - " + resolveOrderCode(order),
                "Đã hoàn tiền",
                "Shop đã xác nhận hoàn tiền thành công cho đơn hàng bị hủy của bạn. Tiền sẽ sớm về tài khoản ngân hàng.",
                "Đã hoàn tiền",
                TONE_SUCCESS,
                "Số tiền hoàn: " + formatMoney(order == null ? null : order.getDeliveryRefundAmount()),
                false
        );
    }

    public void sendDeliveryRefundBankSubmitted(Order order) {
        sendOrderMail(
                order,
                "Đã nhận thông tin hoàn tiền - " + resolveOrderCode(order),
                "Đã nhận thông tin hoàn tiền",
                "Shop đã nhận thông tin tài khoản ngân hàng của bạn. Shop sẽ kiểm tra và hoàn tiền thủ công trong thời gian sớm nhất.",
                "Đã tiếp nhận",
                TONE_INFO,
                null,
                false
        );
    }

    public void sendReturnRequested(Order order) {
        sendOrderMail(
                order,
                "Đã gửi yêu cầu hoàn hàng - " + resolveOrderCode(order),
                "Yêu cầu hoàn hàng đã được gửi",
                "Shop đã nhận yêu cầu hoàn hàng/hoàn tiền của bạn. Yêu cầu đang chờ shop kiểm tra và xử lý.",
                "Chờ xử lý",
                TONE_WARNING,
                null,
                false
        );
    }

    public void sendReturnRequestCancelled(Order order) {
        sendOrderMail(
                order,
                "Đã hủy yêu cầu hoàn hàng - " + resolveOrderCode(order),
                "Đã hủy yêu cầu hoàn hàng",
                "Yêu cầu hoàn hàng của bạn đã được hủy. Đơn hàng đã quay lại trạng thái Hoàn thành.",
                "Đã hủy yêu cầu",
                TONE_INFO,
                null,
                false
        );
    }

    public void sendReturnAccepted(Order order) {
        sendOrderMail(
                order,
                "Yêu cầu hoàn hàng đã được chấp nhận - " + resolveOrderCode(order),
                "Yêu cầu hoàn hàng đã được chấp nhận",
                "Shop đã chấp nhận yêu cầu hoàn hàng của bạn. Shop sẽ tiếp tục xử lý bước hoàn tiền theo phương án đã chọn.",
                "Đã chấp nhận",
                TONE_SUCCESS,
                null,
                false
        );
    }

    public void sendReturnRejected(Order order, String reason) {
        sendOrderMail(
                order,
                "Yêu cầu hoàn hàng bị từ chối - " + resolveOrderCode(order),
                "Yêu cầu hoàn hàng bị từ chối",
                "Shop đã từ chối yêu cầu hoàn hàng của bạn.",
                "Đã từ chối",
                TONE_DANGER,
                "Lý do từ chối: " + normalizeFallback(reason, "Không có"),
                false
        );
    }

    public void sendReturnRefunded(Order order) {
        sendOrderMail(
                order,
                "Đã hoàn tiền đơn hoàn hàng - " + resolveOrderCode(order),
                "Đã hoàn tiền hoàn hàng",
                "Shop đã xác nhận hoàn tiền cho yêu cầu hoàn hàng của bạn.",
                "Đã hoàn tiền",
                TONE_SUCCESS,
                null,
                false
        );
    }

    private void sendOrderMail(
            Order order,
            String subject,
            String title,
            String mainMessage,
            String badgeText,
            String tone,
            String extraMessage,
            boolean createdTemplate
    ) {
        if (!mailEnabled) {
            return;
        }

        String recipient = resolveCustomerEmail(order);
        if (recipient == null) {
            return;
        }

        String from = resolveFromEmail();
        if (from == null) {
            log.warn("Không gửi mail đơn hàng {} vì chưa cấu hình app.mail.from hoặc spring.mail.username", resolveOrderCode(order));
            return;
        }

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            helper.setFrom(from);
            helper.setTo(recipient);
            helper.setSubject("[" + resolveShopName() + "] " + subject);

            OrderMailTemplateService.OrderMailTemplateModel model = buildMailModel(
                    order,
                    title,
                    mainMessage,
                    badgeText,
                    tone,
                    extraMessage
            );

            String htmlContent = createdTemplate
                    ? orderMailTemplateService.renderOrderCreated(model)
                    : orderMailTemplateService.renderOrderStatus(model);

            helper.setText(htmlContent, true);
            javaMailSender.send(mimeMessage);
        } catch (Exception exception) {
            log.warn("Không gửi được mail đơn hàng {} đến {}: {}", resolveOrderCode(order), recipient, exception.getMessage());
        }
    }

    private OrderMailTemplateService.OrderMailTemplateModel buildMailModel(
            Order order,
            String title,
            String mainMessage,
            String badgeText,
            String tone,
            String extraMessage
    ) {
        return new OrderMailTemplateService.OrderMailTemplateModel(
                escapeHtml(resolveShopName()),
                escapeHtml(normalizeFallback(mainMessage, "Đơn hàng của bạn đã được cập nhật.")),
                escapeHtml(normalizeFallback(title, "Cập nhật đơn hàng")),
                escapeHtml(normalizeFallback(mainMessage, "Đơn hàng của bạn đã được cập nhật.")),
                escapeHtml(normalizeFallback(badgeText, formatOrderStatus(order == null ? null : order.getStatus()))),
                resolveBadgeBackground(tone),
                resolveBadgeTextColor(tone),
                escapeHtml(resolveOrderCode(order)),
                escapeHtml(normalizeFallback(order == null ? null : order.getCustomerName(), "-")),
                escapeHtml(normalizeFallback(order == null ? null : order.getCustomerPhone(), "-")),
                escapeHtml(normalizeFallback(order == null ? null : order.getShippingAddress(), "-")),
                escapeHtml(formatPaymentMethod(order == null ? null : order.getPaymentMethod())),
                escapeHtml(formatOrderStatus(order == null ? null : order.getStatus())),
                escapeHtml(formatCreatedAt(order)),
                escapeHtml(formatMoney(order == null ? null : order.getTotalAmount())),
                escapeHtml(formatMoney(order == null ? null : order.getDiscountAmount())),
                escapeHtml(formatMoney(order == null ? null : order.getShippingFee())),
                escapeHtml(formatMoney(order == null ? null : order.getFinalAmount())),
                buildOrderItemsHtml(order),
                buildExtraBlock(extraMessage, tone),
                escapeHtml(resolveOrderDetailUrl()),
                escapeHtml("Nếu có thắc mắc, vui lòng phản hồi email này hoặc liên hệ shop để được hỗ trợ."),
                escapeHtml("Email này được gửi tự động từ hệ thống " + resolveShopName() + ". Vui lòng không chia sẻ thông tin đơn hàng cho người khác.")
        );
    }

    private String buildExtraBlock(String extraMessage, String tone) {
        String cleanExtraMessage = normalizeText(extraMessage);

        if (cleanExtraMessage == null) {
            return "";
        }

        String borderColor = switch (normalizeText(tone) == null ? "" : tone.toUpperCase(Locale.ROOT)) {
            case TONE_DANGER -> "#fecaca";
            case TONE_WARNING -> "#fcd34d";
            case TONE_SUCCESS -> "#bbf7d0";
            default -> "#bfdbfe";
        };

        String backgroundColor = switch (normalizeText(tone) == null ? "" : tone.toUpperCase(Locale.ROOT)) {
            case TONE_DANGER -> "#fef2f2";
            case TONE_WARNING -> "#fffbeb";
            case TONE_SUCCESS -> "#f0fdf4";
            default -> "#eff6ff";
        };

        String textColor = switch (normalizeText(tone) == null ? "" : tone.toUpperCase(Locale.ROOT)) {
            case TONE_DANGER -> "#991b1b";
            case TONE_WARNING -> "#92400e";
            case TONE_SUCCESS -> "#166534";
            default -> "#1e40af";
        };

        return """
                <tr>
                    <td style=\"padding:22px 32px 0 32px;\">
                        <div style=\"background:%s;border:1px solid %s;border-radius:16px;padding:15px 16px;color:%s;font-size:14px;line-height:1.7;font-weight:700;\">
                            %s
                        </div>
                    </td>
                </tr>
                """.formatted(
                backgroundColor,
                borderColor,
                textColor,
                escapeHtml(cleanExtraMessage).replace("\n", "<br>")
        );
    }

    private String buildOrderItemsHtml(Order order) {
        if (order == null || order.getId() == null) {
            return buildEmptyOrderItemRow();
        }

        List<OrderItem> items = orderItemRepository.findDetailByOrderId(order.getId());
        if (items == null || items.isEmpty()) {
            return buildEmptyOrderItemRow();
        }

        StringBuilder builder = new StringBuilder();

        for (OrderItem item : items) {
            if (item == null) {
                continue;
            }

            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            BigDecimal unitPrice = defaultMoney(item.getFinalPrice());
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(Math.max(quantity, 0)));
            String productDetail = buildProductDetail(item);

            builder.append("""
                    <tr>
                        <td style=\"padding:14px;border-bottom:1px solid #e5e7eb;vertical-align:top;\">
                            <div style=\"font-size:14px;color:#111827;font-weight:800;line-height:1.45;\">%s</div>
                            <div style=\"font-size:12px;color:#6b7280;line-height:1.6;margin-top:4px;\">%s</div>
                        </td>
                        <td align=\"center\" style=\"padding:14px;border-bottom:1px solid #e5e7eb;vertical-align:top;font-size:14px;color:#111827;font-weight:800;\">%d</td>
                        <td align=\"right\" style=\"padding:14px;border-bottom:1px solid #e5e7eb;vertical-align:top;font-size:14px;color:#111827;font-weight:700;white-space:nowrap;\">%s</td>
                        <td align=\"right\" style=\"padding:14px;border-bottom:1px solid #e5e7eb;vertical-align:top;font-size:14px;color:#b7791f;font-weight:900;white-space:nowrap;\">%s</td>
                    </tr>
                    """.formatted(
                    escapeHtml(resolveOrderItemName(item)),
                    escapeHtml(productDetail),
                    quantity,
                    escapeHtml(formatMoney(unitPrice)),
                    escapeHtml(formatMoney(lineTotal))
            ));
        }

        String html = builder.toString().trim();
        return html.isBlank() ? buildEmptyOrderItemRow() : html;
    }

    private String buildEmptyOrderItemRow() {
        return """
                <tr>
                    <td colspan=\"4\" style=\"padding:18px;text-align:center;color:#6b7280;font-size:14px;\">
                        Không có dữ liệu sản phẩm
                    </td>
                </tr>
                """;
    }

    private String buildProductDetail(OrderItem item) {
        String sku = normalizeFallback(item == null ? null : item.getSku(), "-");
        String capacity = normalizeFallback(item == null ? null : item.getCapacityName(), "-");
        String bottleType = normalizeFallback(item == null ? null : item.getBottleTypeName(), "-");

        return "SKU: " + sku + " • " + capacity + " • " + bottleType;
    }

    private String resolveOrderItemName(OrderItem item) {
        String snapshotName = normalizeText(item == null ? null : item.getProductName());
        if (snapshotName != null) {
            return snapshotName;
        }

        ProductVariant variant = item == null ? null : item.getProductVariant();
        if (variant != null && variant.getProduct() != null) {
            String productName = normalizeText(variant.getProduct().getName());
            if (productName != null) {
                return productName;
            }
        }

        return "Sản phẩm";
    }

    private String resolveCustomerEmail(Order order) {
        Customer customer = order == null ? null : order.getCustomer();
        User user = customer == null ? null : customer.getUser();

        return normalizeText(user == null ? null : user.getEmail());
    }

    private String resolveOrderCode(Order order) {
        if (order == null || order.getId() == null) {
            return "#N/A";
        }

        return "#" + order.getId();
    }

    private String resolveShopName() {
        return normalizeFallback(shopName, "LUXORA PERFUME");
    }

    private String resolveFromEmail() {
        String from = normalizeText(mailFrom);
        if (from != null) {
            return from;
        }

        return normalizeText(mailUsername);
    }

    private String resolveOrderDetailUrl() {
        String baseUrl = normalizeFallback(frontendUrl, "http://localhost:5173");

        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return baseUrl + "/customer/profile?tab=orders";
    }

    private String resolveBadgeBackground(String tone) {
        String normalizedTone = normalizeText(tone) == null ? "" : tone.toUpperCase(Locale.ROOT);

        return switch (normalizedTone) {
            case TONE_SUCCESS -> "#dcfce7";
            case TONE_WARNING -> "#fef3c7";
            case TONE_DANGER -> "#fee2e2";
            default -> "#dbeafe";
        };
    }

    private String resolveBadgeTextColor(String tone) {
        String normalizedTone = normalizeText(tone) == null ? "" : tone.toUpperCase(Locale.ROOT);

        return switch (normalizedTone) {
            case TONE_SUCCESS -> "#166534";
            case TONE_WARNING -> "#92400e";
            case TONE_DANGER -> "#991b1b";
            default -> "#1e40af";
        };
    }

    private String formatCreatedAt(Order order) {
        if (order == null || order.getCreatedAt() == null) {
            return "-";
        }

        return order.getCreatedAt().format(DATE_TIME_FORMATTER);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private String normalizeFallback(String value, String fallback) {
        String text = normalizeText(value);
        return text == null ? fallback : text;
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String formatMoney(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(VIETNAM_LOCALE).format(defaultMoney(value));
    }

    private String formatPaymentMethod(String paymentMethod) {
        String method = normalizeText(paymentMethod);
        if (method == null) {
            return "Không xác định";
        }

        String upper = method.toUpperCase(Locale.ROOT);

        if (upper.equals("COD")) {
            return "Thanh toán khi nhận hàng";
        }

        if (upper.equals("VNPAY")) {
            return "VNPay";
        }

        if (upper.equals("VIETQR")) {
            return "Chuyển khoản VietQR";
        }

        if (upper.contains("MIXED")) {
            return "Thanh toán hỗn hợp";
        }

        if (upper.contains("CASH")) {
            return "Tiền mặt";
        }

        return method;
    }

    private String formatOrderStatus(Integer status) {
        if (status == null) {
            return "Chờ xác nhận";
        }

        return switch (status) {
            case 0 -> "Chờ xác nhận";
            case 1 -> "Đã xác nhận / Đang chuẩn bị hàng";
            case 2 -> "Đang giao hàng";
            case 3 -> "Hoàn thành";
            case 4 -> "Đã hủy";
            case 5 -> "Giao hàng thất bại";
            case 6 -> "Yêu cầu hoàn hàng / đổi trả";
            case 7 -> "Hoàn hàng / đổi trả hoàn tất";
            case 8 -> "Đã hủy / Chờ hoàn tiền";
            default -> "Không xác định";
        };
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}