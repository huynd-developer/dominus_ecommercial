package org.example.datn_sd69.modules.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.datn_sd69.entity.Customer;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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

    private final JavaMailSender javaMailSender;
    private final OrderItemRepository orderItemRepository;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.shop-name:LUXORA PERFUME}")
    private String shopName;

    @Value("${app.mail.from:}")
    private String mailFrom;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public void sendOrderPlaced(Order order) {
        String paymentMethod = normalizeText(order == null ? null : order.getPaymentMethod());
        String message;

        if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
            message = "Đơn hàng của bạn đã được tạo. Vui lòng hoàn tất thanh toán VNPay để shop tiếp nhận xử lý.";
        } else if ("VIETQR".equalsIgnoreCase(paymentMethod)) {
            message = "Đơn hàng của bạn đã được tạo. Vui lòng chuyển khoản/báo thanh toán để shop tiếp nhận xử lý.";
        } else {
            message = "Đơn hàng của bạn đã được tạo thành công và đang chờ shop xác nhận.";
        }

        sendOrderMail(
                order,
                "Đặt hàng thành công - " + resolveOrderCode(order),
                "ĐẶT HÀNG THÀNH CÔNG",
                message,
                null
        );
    }

    public void sendPaymentSuccess(Order order) {
        sendOrderMail(
                order,
                "Thanh toán VNPay thành công - " + resolveOrderCode(order),
                "THANH TOÁN THÀNH CÔNG",
                "Shop đã ghi nhận thanh toán VNPay thành công. Đơn hàng vẫn đang ở trạng thái Chờ xác nhận và sẽ được shop xử lý sớm.",
                null
        );
    }

    public void sendOrderConfirmed(Order order) {
        sendOrderMail(
                order,
                "Đơn hàng đã được xác nhận - " + resolveOrderCode(order),
                "ĐƠN HÀNG ĐÃ ĐƯỢC XÁC NHẬN",
                "Shop đã xác nhận đơn hàng của bạn và bắt đầu chuẩn bị hàng.",
                null
        );
    }

    public void sendOrderCancelled(Order order, String reason) {
        sendOrderMail(
                order,
                "Đơn hàng đã bị hủy - " + resolveOrderCode(order),
                "ĐƠN HÀNG ĐÃ BỊ HỦY",
                "Đơn hàng của bạn đã bị hủy.",
                "Lý do hủy: " + normalizeFallback(reason, "Không có")
        );
    }

    public void sendOrderAutoCancelled(Order order) {
        sendOrderMail(
                order,
                "Đơn hàng đã tự động hủy - " + resolveOrderCode(order),
                "ĐƠN HÀNG ĐÃ TỰ ĐỘNG HỦY",
                "Đơn hàng của bạn đã quá thời gian thanh toán/xác nhận thanh toán nên hệ thống tự động hủy.",
                null
        );
    }

    public void sendDeliveryCompleted(Order order) {
        sendOrderMail(
                order,
                "Giao hàng thành công - " + resolveOrderCode(order),
                "GIAO HÀNG THÀNH CÔNG",
                "Đơn hàng của bạn đã được xác nhận giao hàng thành công. Cảm ơn bạn đã mua hàng tại " + resolveShopName() + ".",
                null
        );
    }

    public void sendDeliveryFailed(Order order) {
        String extra = "Lý do giao thất bại: " + normalizeFallback(order == null ? null : order.getDeliveryFailedReason(), "Không có");
        String description = normalizeText(order == null ? null : order.getDeliveryFailedDescription());

        if (description != null) {
            extra += System.lineSeparator() + "Mô tả: " + description;
        }

        sendOrderMail(
                order,
                "Giao hàng thất bại - " + resolveOrderCode(order),
                "GIAO HÀNG THẤT BẠI",
                "Đơn hàng của bạn đã được cập nhật là giao hàng thất bại.",
                extra
        );
    }

    public void sendDeliveryRefunded(Order order) {
        sendOrderMail(
                order,
                "Đã hoàn tiền đơn giao thất bại - " + resolveOrderCode(order),
                "ĐÃ HOÀN TIỀN",
                "Shop đã xác nhận hoàn tiền cho đơn giao hàng thất bại.",
                "Số tiền hoàn: " + formatMoney(order == null ? null : order.getDeliveryRefundAmount())
        );
    }

    public void sendDeliveryRefundBankSubmitted(Order order) {
        sendOrderMail(
                order,
                "Đã nhận thông tin hoàn tiền - " + resolveOrderCode(order),
                "ĐÃ NHẬN THÔNG TIN HOÀN TIỀN",
                "Shop đã nhận thông tin tài khoản ngân hàng của bạn cho đơn giao hàng thất bại. Shop sẽ kiểm tra và hoàn tiền thủ công trong thời gian sớm nhất.",
                null
        );
    }

    public void sendReturnRequested(Order order) {
        sendOrderMail(
                order,
                "Đã gửi yêu cầu hoàn hàng - " + resolveOrderCode(order),
                "YÊU CẦU HOÀN HÀNG ĐÃ ĐƯỢC GỬI",
                "Shop đã nhận yêu cầu hoàn hàng/hoàn tiền của bạn. Yêu cầu đang chờ shop kiểm tra và xử lý.",
                null
        );
    }

    public void sendReturnRequestCancelled(Order order) {
        sendOrderMail(
                order,
                "Đã hủy yêu cầu hoàn hàng - " + resolveOrderCode(order),
                "ĐÃ HỦY YÊU CẦU HOÀN HÀNG",
                "Yêu cầu hoàn hàng của bạn đã được hủy. Đơn hàng đã quay lại trạng thái Hoàn thành.",
                null
        );
    }

    public void sendReturnAccepted(Order order) {
        sendOrderMail(
                order,
                "Yêu cầu hoàn hàng đã được chấp nhận - " + resolveOrderCode(order),
                "YÊU CẦU HOÀN HÀNG ĐÃ ĐƯỢC CHẤP NHẬN",
                "Shop đã chấp nhận yêu cầu hoàn hàng của bạn. Shop sẽ tiếp tục xử lý bước hoàn tiền theo phương án đã chọn.",
                null
        );
    }

    public void sendReturnRejected(Order order, String reason) {
        sendOrderMail(
                order,
                "Yêu cầu hoàn hàng bị từ chối - " + resolveOrderCode(order),
                "YÊU CẦU HOÀN HÀNG BỊ TỪ CHỐI",
                "Shop đã từ chối yêu cầu hoàn hàng của bạn.",
                "Lý do từ chối: " + normalizeFallback(reason, "Không có")
        );
    }

    public void sendReturnRefunded(Order order) {
        sendOrderMail(
                order,
                "Đã hoàn tiền đơn hoàn hàng - " + resolveOrderCode(order),
                "ĐÃ HOÀN TIỀN HOÀN HÀNG",
                "Shop đã xác nhận hoàn tiền cho yêu cầu hoàn hàng của bạn.",
                null
        );
    }

    private void sendOrderMail(
            Order order,
            String subject,
            String title,
            String mainMessage,
            String extraMessage
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
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(recipient);
            message.setSubject("[" + resolveShopName() + "] " + subject);
            message.setText(buildMailBody(order, title, mainMessage, extraMessage));

            javaMailSender.send(message);
        } catch (Exception exception) {
            /*
             * Tuyệt đối không làm fail nghiệp vụ đặt hàng/đổi trạng thái chỉ vì lỗi gửi mail.
             * Lỗi được log để dev kiểm tra lại SMTP.
             */
            log.warn("Không gửi được mail đơn hàng {} đến {}: {}", resolveOrderCode(order), recipient, exception.getMessage());
        }
    }

    private String buildMailBody(Order order, String title, String mainMessage, String extraMessage) {
        StringBuilder builder = new StringBuilder();

        builder.append(resolveShopName()).append(System.lineSeparator());
        builder.append("==============================").append(System.lineSeparator());
        builder.append(title).append(System.lineSeparator()).append(System.lineSeparator());
        builder.append(normalizeFallback(mainMessage, "Đơn hàng của bạn đã được cập nhật.")).append(System.lineSeparator()).append(System.lineSeparator());

        if (extraMessage != null && !extraMessage.isBlank()) {
            builder.append(extraMessage.trim()).append(System.lineSeparator()).append(System.lineSeparator());
        }

        builder.append("THÔNG TIN ĐƠN HÀNG").append(System.lineSeparator());
        builder.append("Mã đơn: ").append(resolveOrderCode(order)).append(System.lineSeparator());
        builder.append("Khách hàng: ").append(normalizeFallback(order == null ? null : order.getCustomerName(), "-")).append(System.lineSeparator());
        builder.append("Số điện thoại: ").append(normalizeFallback(order == null ? null : order.getCustomerPhone(), "-")).append(System.lineSeparator());
        builder.append("Địa chỉ: ").append(normalizeFallback(order == null ? null : order.getShippingAddress(), "-")).append(System.lineSeparator());
        builder.append("Phương thức thanh toán: ").append(formatPaymentMethod(order == null ? null : order.getPaymentMethod())).append(System.lineSeparator());
        builder.append("Trạng thái hiện tại: ").append(formatOrderStatus(order == null ? null : order.getStatus())).append(System.lineSeparator());

        if (order != null && order.getCreatedAt() != null) {
            builder.append("Thời gian đặt: ").append(order.getCreatedAt().format(DATE_TIME_FORMATTER)).append(System.lineSeparator());
        }

        builder.append(System.lineSeparator());
        builder.append("SẢN PHẨM").append(System.lineSeparator());
        builder.append(buildOrderItemsText(order)).append(System.lineSeparator());

        builder.append(System.lineSeparator());
        builder.append("Tạm tính: ").append(formatMoney(order == null ? null : order.getTotalAmount())).append(System.lineSeparator());
        builder.append("Giảm giá: ").append(formatMoney(order == null ? null : order.getDiscountAmount())).append(System.lineSeparator());
        builder.append("Phí vận chuyển: ").append(formatMoney(order == null ? null : order.getShippingFee())).append(System.lineSeparator());
        builder.append("Thanh toán: ").append(formatMoney(order == null ? null : order.getFinalAmount())).append(System.lineSeparator());

        builder.append(System.lineSeparator());
        builder.append("Nếu có thắc mắc, vui lòng liên hệ shop để được hỗ trợ.").append(System.lineSeparator());
        builder.append("Cảm ơn bạn đã mua hàng tại ").append(resolveShopName()).append(".");

        return builder.toString();
    }

    private String buildOrderItemsText(Order order) {
        if (order == null || order.getId() == null) {
            return "- Không có dữ liệu sản phẩm";
        }

        List<OrderItem> items = orderItemRepository.findDetailByOrderId(order.getId());
        if (items == null || items.isEmpty()) {
            return "- Không có dữ liệu sản phẩm";
        }

        StringBuilder builder = new StringBuilder();
        int index = 1;

        for (OrderItem item : items) {
            if (item == null) {
                continue;
            }

            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            BigDecimal unitPrice = defaultMoney(item.getFinalPrice());
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(Math.max(quantity, 0)));

            builder.append(index++).append(". ")
                    .append(resolveOrderItemName(item))
                    .append(" | SKU: ").append(normalizeFallback(item.getSku(), "-"))
                    .append(" | ").append(normalizeFallback(item.getCapacityName(), "-"))
                    .append(" | ").append(normalizeFallback(item.getBottleTypeName(), "-"))
                    .append(" | SL: ").append(quantity)
                    .append(" | Giá: ").append(formatMoney(unitPrice))
                    .append(" | Thành tiền: ").append(formatMoney(lineTotal))
                    .append(System.lineSeparator());
        }

        String text = builder.toString().trim();
        return text.isBlank() ? "- Không có dữ liệu sản phẩm" : text;
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
            default -> "Không xác định";
        };
    }
}