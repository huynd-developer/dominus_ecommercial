package org.example.datn_sd69.modules.order.service.impl;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.modules.order.dto.response.AdminOrderItemResponse;
import org.example.datn_sd69.modules.order.dto.response.AdminOrderResponse;
import org.example.datn_sd69.modules.order.service.AdminOrderService;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.example.datn_sd69.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
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

    @Override
    @Transactional(readOnly = true)
    public Page<AdminOrderResponse> getOrders(
            String keyword,
            Integer status,
            String orderType,
            Pageable pageable
    ) {
        return orderRepository.searchAdminOrders(
                normalizeKeyword(keyword),
                normalizeSearchStatus(status),
                normalizeOrderType(orderType),
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

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        String trimmed = keyword.trim();

        return trimmed.isEmpty() ? null : trimmed;
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
}