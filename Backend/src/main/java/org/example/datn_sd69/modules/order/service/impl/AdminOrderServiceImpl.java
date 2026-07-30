package org.example.datn_sd69.modules.order.service.impl;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.modules.order.dto.response.AdminOrderItemResponse;
import org.example.datn_sd69.modules.order.dto.response.AdminOrderResponse;
import org.example.datn_sd69.modules.order.dto.response.AdminOrderStatusCountResponse;
import org.example.datn_sd69.modules.order.service.AdminOrderService;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.example.datn_sd69.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_CONFIRMED = 1;
    private static final int STATUS_SHIPPING = 2;
    private static final int STATUS_COMPLETED = 3;
    private static final int STATUS_CANCELLED = 4;
    private static final int STATUS_DELIVERY_FAILED = 5;
    private static final int STATUS_RETURN_REQUESTED = 6;
    private static final int STATUS_RETURN_COMPLETED = 7;

    private static final Set<Integer> VALID_ORDER_STATUSES = Set.of(
            STATUS_PENDING,
            STATUS_CONFIRMED,
            STATUS_SHIPPING,
            STATUS_COMPLETED,
            STATUS_CANCELLED,
            STATUS_DELIVERY_FAILED,
            STATUS_RETURN_REQUESTED,
            STATUS_RETURN_COMPLETED
    );

    private static final Set<String> SUPPORTED_ORDER_TYPES = Set.of(
            "ONLINE",
            "IN_STORE"
    );

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EntityManager entityManager;
    private record KeywordDateRange(
            String keyword,
            LocalDateTime fromDateTime,
            LocalDateTime toDateTime
    ) {
    }
    @Override
    @Transactional(readOnly = true)
    public Page<AdminOrderResponse> getOrders(
            String keyword,
            Integer status,
            String orderType,
            String paymentMethod,
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            Pageable pageable
    ) {
        validateAmountRange(minAmount, maxAmount);
        validateDateRange(fromDate, toDate);

        KeywordDateRange keywordDateRange = resolveKeywordDateRange(keyword);

        LocalDateTime requestFromDateTime =
                fromDate == null ? null : fromDate.atStartOfDay();

        LocalDateTime requestToDateTime =
                toDate == null ? null : toDate.plusDays(1).atStartOfDay();

        LocalDateTime finalFromDateTime =
                requestFromDateTime != null
                        ? requestFromDateTime
                        : keywordDateRange.fromDateTime();

        LocalDateTime finalToDateTime =
                requestToDateTime != null
                        ? requestToDateTime
                        : keywordDateRange.toDateTime();

        return orderRepository.searchAdminOrders(
                normalizeKeyword(keywordDateRange.keyword()),
                normalizeSearchStatus(status),
                normalizeOrderType(orderType),
                normalizePaymentMethod(paymentMethod),
                finalFromDateTime,
                finalToDateTime,
                minAmount,
                maxAmount,
                pageable
        ).map(order -> mapOrderToResponse(order, false));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderResponse getOrderDetail(Integer orderId) {
        Order order = findOrderOrThrow(orderId);
        return mapOrderToResponse(order, true);
    }

    private AdminOrderResponse mapOrderToResponse(Order order, boolean includeItems) {
        AdminOrderResponse response = new AdminOrderResponse();

        response.setOrderId(order.getId());
        response.setOrderCode(formatOrderCode(order.getId()));
        response.setOrderType(order.getOrderType());

        if (order.getCustomer() != null) {
            response.setCustomerId(order.getCustomer().getUserId());
        }

        response.setCustomerName(order.getCustomerName());
        response.setCustomerPhone(order.getCustomerPhone());
        response.setShippingAddress(order.getShippingAddress());

        if (order.getCashier() != null) {
            response.setCashierId(order.getCashier().getUserId());
        }

        if (order.getVoucher() != null) {
            String voucherCode = order.getVoucher().getCode();

            response.setVoucher(
                    new AdminOrderResponse.VoucherInfo(
                            order.getVoucher().getId(),
                            voucherCode,
                            voucherCode
                    )
            );
        }

        response.setTotalAmount(defaultMoney(order.getTotalAmount()));
        response.setDiscountAmount(defaultMoney(order.getDiscountAmount()));
        response.setFinalAmount(defaultMoney(order.getFinalAmount()));

        response.setPaymentMethod(order.getPaymentMethod());
        response.setStatus(order.getStatus());
        response.setStatusText(getStatusText(order.getStatus()));
        response.setCreatedAt(order.getCreatedAt());
        response.setCompletedAt(order.getCompletedAt());

        // BỔ SUNG TRƯỜNG NÀY ĐỂ TRẢ VỀ CHO VUE
        response.setIsPaymentReported(order.getIsPaymentReported() != null && order.getIsPaymentReported());

        // --- MAP THÔNG TIN HOÀN HÀNG CHO ADMIN ---
        response.setReturnReason(order.getReturnReason());
        response.setReturnImages(parseMediaString(order.getReturnImages()));
        response.setReturnVideos(parseMediaString(order.getReturnVideos()));

        if (includeItems) {
            var items = orderItemRepository.findDetailByOrderId(order.getId());

            var itemResponses = items
                    .stream()
                    .map(this::mapOrderItemToResponse)
                    .toList();

            response.setItems(itemResponses);
            response.setTotalQuantity(
                    itemResponses.stream()
                            .mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                            .sum()
            );
        } else {
            response.setItems(new ArrayList<>());
            response.setTotalQuantity(null);
        }

        return response;
    }

    // --- HÀM HỖ TRỢ PARSE CHUỖI ẢNH/VIDEO THÀNH LIST ---
    private List<String> parseMediaString(String mediaStr) {
        if (mediaStr == null || mediaStr.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String[] parts = mediaStr.split(",");
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            String cleaned = p.trim();
            if (!cleaned.isEmpty()) {
                list.add(cleaned);
            }
        }
        return list;
    }

    private AdminOrderItemResponse mapOrderItemToResponse(OrderItem item) {
        AdminOrderItemResponse response = new AdminOrderItemResponse();

        response.setOrderItemId(item.getId());
        response.setQuantity(item.getQuantity());
        response.setOriginalPrice(defaultMoney(item.getOriginalPrice()));
        response.setDiscountAmount(defaultMoney(item.getDiscountAmount()));
        response.setFinalPrice(defaultMoney(item.getFinalPrice()));
        response.setNote(item.getNote());

        BigDecimal lineTotal = defaultMoney(item.getFinalPrice())
                .multiply(BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity()));

        response.setLineTotal(lineTotal);

        ProductVariant variant = item.getProductVariant();

        if (variant == null) {
            response.setImageUrl(cleanImageUrl(item.getImage()));
            return response;
        }

        response.setProductVariantId(variant.getId());
        response.setSku(variant.getSku());

        if (variant.getProduct() != null) {
            response.setProductName(variant.getProduct().getName());
        }

        if (variant.getCapacity() != null && variant.getCapacity().getValue() != null) {
            response.setCapacity(formatCapacity(variant.getCapacity().getValue()));
        }

        if (variant.getBottleType() != null) {
            response.setBottleType(variant.getBottleType().getName());
        }

        response.setImageUrl(resolveOrderItemImage(item, variant));

        return response;
    }

    private String resolveOrderItemImage(OrderItem item, ProductVariant variant) {
        String savedImage = cleanImageUrl(item == null ? null : item.getImage());

        if (savedImage != null) {
            return savedImage;
        }

        return resolveProductImageByVariant(variant);
    }

    private String resolveProductImageByVariant(ProductVariant variant) {
        if (variant == null || variant.getProduct() == null || variant.getProduct().getId() == null) {
            return null;
        }

        try {
            return entityManager.createQuery(
                            """
                            SELECT img.imageUrl
                            FROM ProductImage img
                            WHERE img.product.id = :productId
                              AND img.imageUrl IS NOT NULL
                              AND img.imageUrl <> ''
                            ORDER BY img.id ASC
                            """,
                            String.class
                    )
                    .setParameter("productId", variant.getProduct().getId())
                    .setMaxResults(1)
                    .getResultStream()
                    .map(this::cleanImageUrl)
                    .filter(value -> value != null)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            System.out.println("=== LỖI QUERY ẢNH CHI TIẾT ĐƠN HÀNG: " + e.getMessage());
            return null;
        }
    }

    private String cleanImageUrl(String imageUrl) {
        if (imageUrl == null) {
            return null;
        }

        String cleanValue = imageUrl.trim();

        return cleanValue.isEmpty() ? null : cleanValue;
    }

    private Order findOrderOrThrow(Integer orderId) {
        if (orderId == null || orderId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã đơn hàng không hợp lệ"
            );
        }

        return orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng"
                ));
    }

    private Integer normalizeSearchStatus(Integer status) {
        if (status == null) {
            return null;
        }

        if (!VALID_ORDER_STATUSES.contains(status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trạng thái tìm kiếm không hợp lệ"
            );
        }

        return status;
    }

    private String normalizeOrderType(String orderType) {
        if (orderType == null || orderType.trim().isEmpty()) {
            return null;
        }

        String normalized = orderType.trim().toUpperCase(Locale.ROOT);

        if ("ALL".equals(normalized) || "TAT_CA".equals(normalized)) {
            return null;
        }

        if ("INSTORE".equals(normalized) || "IN-STORE".equals(normalized) || "OFFLINE".equals(normalized)) {
            normalized = "IN_STORE";
        }

        if (!SUPPORTED_ORDER_TYPES.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Loại đơn hàng không hợp lệ. Chỉ hỗ trợ ONLINE, IN_STORE hoặc POS"
            );
        }

        return normalized;
    }

    private String normalizePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            return null;
        }

        String normalized = paymentMethod.trim().toUpperCase(Locale.ROOT);

        if ("ALL".equals(normalized) || "TAT_CA".equals(normalized)) {
            return null;
        }

        return normalized;
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ngày bắt đầu không được sau ngày kết thúc"
            );
        }
    }

    private void validateAmountRange(BigDecimal minAmount, BigDecimal maxAmount) {
        if (minAmount != null && minAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số tiền tối thiểu không hợp lệ"
            );
        }

        if (maxAmount != null && maxAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số tiền tối đa không hợp lệ"
            );
        }

        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số tiền tối thiểu không được lớn hơn số tiền tối đa"
            );
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        String trimmed = keyword.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        String upper = trimmed.toUpperCase(Locale.ROOT);

        if (upper.matches("^DH0*\\d+$")) {
            String numberPart = upper.replaceFirst("^DH0*", "");

            return numberPart.isEmpty() ? trimmed : numberPart;
        }

        if (trimmed.matches("^#0*\\d+$")) {
            String numberPart = trimmed.replaceFirst("^#0*", "");

            return numberPart.isEmpty() ? trimmed : numberPart;
        }

        return trimmed;
    }

    private KeywordDateRange resolveKeywordDateRange(String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);

        if (normalizedKeyword == null) {
            return new KeywordDateRange(null, null, null);
        }

        String value = normalizedKeyword.trim();

        DateTimeFormatter dateTimeFormatter =
                DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

        DateTimeFormatter dateFormatterSlash =
                DateTimeFormatter.ofPattern("dd/MM/yyyy");

        DateTimeFormatter dateFormatterDash =
                DateTimeFormatter.ofPattern("dd-MM-yyyy");

        try {
            LocalDateTime dateTime = LocalDateTime.parse(value, dateTimeFormatter);

            return new KeywordDateRange(
                    null,
                    dateTime,
                    dateTime.plusSeconds(1)
            );
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalDate date = LocalDate.parse(value, dateFormatterSlash);

            return new KeywordDateRange(
                    null,
                    date.atStartOfDay(),
                    date.plusDays(1).atStartOfDay()
            );
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalDate date = LocalDate.parse(value, dateFormatterDash);

            return new KeywordDateRange(
                    null,
                    date.atStartOfDay(),
                    date.plusDays(1).atStartOfDay()
            );
        } catch (DateTimeParseException ignored) {
        }

        return new KeywordDateRange(normalizedKeyword, null, null);
    }

    private Integer safeStatus(Order order) {
        return order.getStatus() == null ? STATUS_PENDING : order.getStatus();
    }

    private String getStatusText(Integer status) {
        if (status == null) {
            return "Chờ xác nhận";
        }

        return switch (status) {
            case STATUS_PENDING -> "Chờ xác nhận";
            case STATUS_CONFIRMED -> "Đã xác nhận / Đang chuẩn bị hàng";
            case STATUS_SHIPPING -> "Đang giao hàng";
            case STATUS_COMPLETED -> "Hoàn thành";
            case STATUS_CANCELLED -> "Đã hủy";
            case STATUS_DELIVERY_FAILED -> "Giao hàng thất bại";
            case STATUS_RETURN_REQUESTED -> "Yêu cầu hoàn hàng / đổi trả";
            case STATUS_RETURN_COMPLETED -> "Hoàn hàng / đổi trả hoàn tất";
            default -> "Không xác định";
        };
    }

    private String formatOrderCode(Integer orderId) {
        if (orderId == null) {
            return "DH000000";
        }

        return "DH" + String.format("%06d", orderId);
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String formatCapacity(Double value) {
        if (value == null) {
            return "-";
        }

        if (value % 1 == 0) {
            return value.intValue() + "ml";
        }

        return value + "ml";
    }
    @Override
    @Transactional(readOnly = true)
    public AdminOrderStatusCountResponse getStatusCounts(
            String keyword,
            String orderType,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        validateDateRange(fromDate, toDate);

        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedOrderType = normalizeOrderType(orderType);

        var fromDateTime = fromDate == null ? null : fromDate.atStartOfDay();
        var toDateTime = toDate == null ? null : toDate.plusDays(1).atStartOfDay();

        return new AdminOrderStatusCountResponse(
                countOrderByStatus(normalizedKeyword, null, normalizedOrderType, fromDateTime, toDateTime),
                countOrderByStatus(normalizedKeyword, STATUS_PENDING, normalizedOrderType, fromDateTime, toDateTime),
                countOrderByStatus(normalizedKeyword, STATUS_CONFIRMED, normalizedOrderType, fromDateTime, toDateTime),
                countOrderByStatus(normalizedKeyword, STATUS_SHIPPING, normalizedOrderType, fromDateTime, toDateTime),
                countOrderByStatus(normalizedKeyword, STATUS_COMPLETED, normalizedOrderType, fromDateTime, toDateTime),
                countOrderByStatus(normalizedKeyword, STATUS_CANCELLED, normalizedOrderType, fromDateTime, toDateTime),
                countOrderByStatus(normalizedKeyword, STATUS_DELIVERY_FAILED, normalizedOrderType, fromDateTime, toDateTime),
                countOrderByStatus(normalizedKeyword, STATUS_RETURN_REQUESTED, normalizedOrderType, fromDateTime, toDateTime),
                countOrderByStatus(normalizedKeyword, STATUS_RETURN_COMPLETED, normalizedOrderType, fromDateTime, toDateTime)
        );
    }

    private long countOrderByStatus(
            String keyword,
            Integer status,
            String orderType,
            java.time.LocalDateTime fromDateTime,
            java.time.LocalDateTime toDateTime
    ) {
        Long count = orderRepository.countAdminOrdersForTabs(
                keyword,
                status,
                orderType,
                fromDateTime,
                toDateTime
        );

        return count == null ? 0L : count;
    }
}