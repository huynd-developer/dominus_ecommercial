package org.example.datn_sd69.modules.orderStatus.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.modules.loyalty.service.LoyaltyPointService;
import org.example.datn_sd69.modules.orderStatus.service.AdminOrderStatusService;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.example.datn_sd69.repository.OrderRepository;
import org.example.datn_sd69.repository.ProductVariantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminOrderStatusServiceImpl implements AdminOrderStatusService {

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

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final LoyaltyPointService loyaltyPointService;

    @Override
    @Transactional
    public Order updateStatus(Integer orderId, Integer newStatus) {
        validateOrderId(orderId);
        validateNewStatus(newStatus);

        Order order = orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng"
                ));

        validateNotHoldOrder(order);

        Integer oldStatus = safeStatus(order);

        if (Objects.equals(oldStatus, newStatus)) {
            throw badRequest("Đơn hàng đã ở trạng thái này");
        }

        switch (newStatus) {
            case STATUS_CONFIRMED -> confirmOrder(order);
            case STATUS_SHIPPING -> shipOrder(order);
            case STATUS_COMPLETED -> completeOrder(order);
            case STATUS_CANCELLED -> cancelOrder(order);
            case STATUS_DELIVERY_FAILED -> markDeliveryFailed(order);
            case STATUS_RETURN_REQUESTED -> requestReturnExchange(order);
            case STATUS_RETURN_COMPLETED -> completeReturnExchange(order);
            default -> throw badRequest("Trạng thái đơn hàng không hợp lệ");
        }

        return order;
    }

    private void confirmOrder(Order order) {
        requireCurrentStatus(
                order,
                STATUS_PENDING,
                "Chỉ đơn chờ xác nhận mới được chuyển sang đã xác nhận"
        );

        order.setStatus(STATUS_CONFIRMED);
        orderRepository.save(order);
    }

    private void shipOrder(Order order) {
        requireCurrentStatus(
                order,
                STATUS_CONFIRMED,
                "Chỉ đơn đã xác nhận mới được chuyển sang đang giao hàng"
        );

        order.setStatus(STATUS_SHIPPING);
        orderRepository.save(order);
    }

    private void completeOrder(Order order) {
        Integer oldStatus = safeStatus(order);

        // CHO PHÉP ADMIN TỪ CHỐI YÊU CẦU HOÀN (Chuyển từ trạng thái 6 về lại 3)
        if (oldStatus == STATUS_RETURN_REQUESTED) {
            order.setStatus(STATUS_COMPLETED);
            orderRepository.save(order);
            return;
        }

        requireCurrentStatus(
                order,
                STATUS_SHIPPING,
                "Chỉ đơn đang giao hàng mới được chuyển sang hoàn thành"
        );

        order.setStatus(STATUS_COMPLETED);

        if (order.getCompletedAt() == null) {
            order.setCompletedAt(LocalDateTime.now());
        }

        orderRepository.save(order);

        loyaltyPointService.applyPointsWhenOrderCompleted(order);

        orderRepository.save(order);
    }

    private void cancelOrder(Order order) {
        Integer oldStatus = safeStatus(order);

        if (oldStatus == STATUS_COMPLETED) {
            throw badRequest(
                    "Đơn hàng đã hoàn thành, không thể hủy. Nếu khách trả hàng hãy chuyển sang yêu cầu hoàn hàng / đổi trả"
            );
        }

        if (oldStatus == STATUS_SHIPPING) {
            throw badRequest(
                    "Đơn hàng đang giao không nên hủy trực tiếp. Hãy chuyển sang giao hàng thất bại"
            );
        }

        if (isTerminalStatus(oldStatus)) {
            throw badRequest("Trạng thái hiện tại không cho phép hủy đơn");
        }

        if (oldStatus != STATUS_PENDING && oldStatus != STATUS_CONFIRMED) {
            throw badRequest("Chỉ đơn chờ xác nhận hoặc đã xác nhận mới được hủy");
        }

        restoreStock(order);

        order.setStatus(STATUS_CANCELLED);
        orderRepository.save(order);
    }

    private void markDeliveryFailed(Order order) {
        requireCurrentStatus(
                order,
                STATUS_SHIPPING,
                "Chỉ đơn đang giao hàng mới được chuyển sang giao hàng thất bại"
        );

        restoreStock(order);

        order.setStatus(STATUS_DELIVERY_FAILED);
        orderRepository.save(order);
    }

    private void requestReturnExchange(Order order) {
        requireCurrentStatus(
                order,
                STATUS_COMPLETED,
                "Chỉ đơn đã hoàn thành mới được yêu cầu hoàn hàng / đổi trả"
        );

        order.setStatus(STATUS_RETURN_REQUESTED);
        orderRepository.save(order);
    }

    private void completeReturnExchange(Order order) {
        requireCurrentStatus(
                order,
                STATUS_RETURN_REQUESTED,
                "Chỉ đơn đang yêu cầu hoàn hàng / đổi trả mới được chuyển sang hoàn tất"
        );

        restoreStock(order);

        order.setStatus(STATUS_RETURN_COMPLETED);
        orderRepository.save(order);
    }

    private void restoreStock(Order order) {
        if (order == null || order.getId() == null) {
            return;
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());

        if (orderItems == null || orderItems.isEmpty()) {
            return;
        }

        for (OrderItem item : orderItems) {
            if (item == null || item.getProductVariant() == null) {
                continue;
            }

            ProductVariant variant = item.getProductVariant();

            int currentStock = variant.getStockQuantity() == null
                    ? 0
                    : variant.getStockQuantity();

            int returnQuantity = item.getQuantity() == null
                    ? 0
                    : item.getQuantity();

            if (returnQuantity <= 0) {
                continue;
            }

            variant.setStockQuantity(currentStock + returnQuantity);
            productVariantRepository.save(variant);
        }
    }

    private void requireCurrentStatus(Order order, Integer expectedStatus, String message) {
        Integer oldStatus = safeStatus(order);

        if (isTerminalStatus(oldStatus)) {
            throw badRequest("Đơn hàng đã ở trạng thái kết thúc, không thể cập nhật tiếp");
        }

        if (!Objects.equals(oldStatus, expectedStatus)) {
            throw badRequest(message);
        }
    }

    private boolean isTerminalStatus(Integer status) {
        return status != null && (
                status == STATUS_CANCELLED ||
                        status == STATUS_DELIVERY_FAILED ||
                        status == STATUS_RETURN_COMPLETED
        );
    }

    private void validateOrderId(Integer orderId) {
        if (orderId == null || orderId <= 0) {
            throw badRequest("orderId phải là số nguyên dương");
        }
    }

    private void validateNewStatus(Integer newStatus) {
        if (newStatus == null) {
            throw badRequest("Trạng thái đơn hàng không được để trống");
        }

        if (!VALID_ORDER_STATUSES.contains(newStatus)) {
            throw badRequest("Trạng thái đơn hàng không hợp lệ");
        }

        if (newStatus == STATUS_PENDING) {
            throw badRequest("Không thể cập nhật đơn hàng quay về trạng thái chờ xác nhận");
        }
    }

    private Integer safeStatus(Order order) {
        return order.getStatus() == null ? STATUS_PENDING : order.getStatus();
    }

    private void validateNotHoldOrder(Order order) {
        if (isHoldOrder(order)) {
            throw badRequest(
                    "Đây là phiếu treo tại quầy. Vui lòng xử lý bằng chức năng POS, không xử lý ở màn quản lý đơn thường"
            );
        }
    }

    private boolean isHoldOrder(Order order) {
        String paymentMethod = order.getPaymentMethod() == null
                ? ""
                : order.getPaymentMethod().trim().toUpperCase(Locale.ROOT);

        String orderType = order.getOrderType() == null
                ? ""
                : order.getOrderType().trim().toUpperCase(Locale.ROOT);

        return "HOLD".equals(paymentMethod)
                && ("POS".equals(orderType) || "IN_STORE".equals(orderType));
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}