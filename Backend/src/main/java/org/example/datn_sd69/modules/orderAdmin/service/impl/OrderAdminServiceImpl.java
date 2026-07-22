package org.example.datn_sd69.modules.orderAdmin.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Customer;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderAdminResponse;
import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderDetailResponse;
import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderItemResponse;
import org.example.datn_sd69.modules.orderAdmin.exception.InvalidOrderStatusException;
import org.example.datn_sd69.modules.orderAdmin.exception.OrderNotFoundException;
import org.example.datn_sd69.modules.orderAdmin.service.OrderAdminService;
import org.example.datn_sd69.repository.CustomerRepository;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.example.datn_sd69.repository.OrderRepository;
import org.example.datn_sd69.repository.ProductVariantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderAdminServiceImpl implements OrderAdminService {

    private final OrderRepository orderRepository;

    private final OrderItemRepository orderItemRepository;

    private final ProductVariantRepository productVariantRepository;

    private final CustomerRepository customerRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<OrderAdminResponse> searchOrders(
            String keyword,
            Integer status,
            String orderType,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(page, size);

        return orderRepository
                .searchAdminOrders(
                        keyword,
                        status,
                        orderType,
                        pageable
                )
                .map(this::mapToResponse);

    }

    private OrderAdminResponse mapToResponse(Order order) {

        OrderAdminResponse response = new OrderAdminResponse();

        response.setId(order.getId());

        response.setOrderType(order.getOrderType());

        response.setCustomerName(order.getCustomerName());

        response.setCustomerPhone(order.getCustomerPhone());

        response.setShippingAddress(order.getShippingAddress());

        response.setPaymentMethod(order.getPaymentMethod());

        response.setTotalAmount(order.getTotalAmount());

        response.setDiscountAmount(order.getDiscountAmount());

        response.setFinalAmount(order.getFinalAmount());

        response.setStatus(order.getStatus());

        response.setStatusName(getStatusName(order.getStatus()));

        response.setCreatedAt(order.getCreatedAt());

        response.setCompletedAt(order.getCompletedAt());

        if (order.getCashier() != null) {

            response.setCashierId(order.getCashier().getUserId());

            if (order.getCashier().getUser() != null) {

                response.setCashierName(
                        order.getCashier().getUser().getName()
                );

            }

        }

        if (order.getVoucher() != null) {

            response.setVoucherId(order.getVoucher().getId());

            response.setVoucherCode(order.getVoucher().getCode());

        }

        return response;

    }

    private String getStatusName(Integer status) {

        if (status == null) {
            return "";
        }

        return switch (status) {

            case 0 -> "Chờ xác nhận";

            case 1 -> "Đã xác nhận";

            case 2 -> "Đang giao hàng";

            case 3 -> "Hoàn thành";

            case 4 -> "Đã hủy";

            case 5 -> "Giao hàng thất bại";

            case 6 -> "Yêu cầu hoàn hàng";

            case 7 -> "Hoàn hàng hoàn tất";

            default -> "Không xác định";
        };

    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Integer orderId) {

        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException("Không tìm thấy đơn hàng."));

        List<OrderItem> orderItems =
                orderItemRepository.findDetailByOrderId(orderId);

        OrderDetailResponse response = new OrderDetailResponse();

        response.setId(order.getId());

        response.setOrderType(order.getOrderType());

        if (order.getCustomer() != null) {

            response.setCustomerId(order.getCustomer().getUserId());

        }

        response.setCustomerName(order.getCustomerName());

        response.setCustomerPhone(order.getCustomerPhone());

        response.setShippingAddress(order.getShippingAddress());

        if (order.getCashier() != null) {

            response.setCashierId(order.getCashier().getUserId());

            if (order.getCashier().getUser() != null) {

                response.setCashierName(
                        order.getCashier()
                                .getUser()
                                .getName()
                );

            }

        }

        if (order.getVoucher() != null) {

            response.setVoucherId(order.getVoucher().getId());

            response.setVoucherCode(order.getVoucher().getCode());

        }

        response.setPaymentMethod(order.getPaymentMethod());

        response.setTotalAmount(order.getTotalAmount());

        response.setDiscountAmount(order.getDiscountAmount());

        response.setFinalAmount(order.getFinalAmount());

        response.setStatus(order.getStatus());

        response.setStatusName(getStatusName(order.getStatus()));

        response.setCreatedAt(order.getCreatedAt());

        response.setCompletedAt(order.getCompletedAt());

        response.setLoyaltyPointsApplied(
                order.getLoyaltyPointsApplied()
        );

        response.setLoyaltyPointsEarned(
                order.getLoyaltyPointsEarned()
        );

        List<OrderItemResponse> items = new ArrayList<>();

        for (OrderItem item : orderItems) {

            items.add(mapOrderItem(item));

        }

        response.setItems(items);

        return response;

    }

    private OrderItemResponse mapOrderItem(OrderItem item) {

        OrderItemResponse response = new OrderItemResponse();

        response.setId(item.getId());

        response.setProductVariantId(
                item.getProductVariant().getId()
        );

        if (item.getProductVariant().getProduct() != null) {

            response.setProductId(
                    item.getProductVariant()
                            .getProduct()
                            .getId()
            );

            response.setProductName(
                    item.getProductVariant()
                            .getProduct()
                            .getName()
            );

        }

        response.setSku(
                item.getProductVariant().getSku()
        );

        if (item.getProductVariant().getCapacity() != null) {

            response.setCapacityId(
                    item.getProductVariant()
                            .getCapacity()
                            .getId()
            );

            response.setCapacityName(
                    String.valueOf(
                            item.getProductVariant()
                                    .getCapacity()
                                    .getValue()
                    )
            );

        }

        if (item.getProductVariant().getBottleType() != null) {

            response.setBottleTypeId(
                    item.getProductVariant()
                            .getBottleType()
                            .getId()
            );

            response.setBottleTypeName(
                    item.getProductVariant()
                            .getBottleType()
                            .getName()
            );

        }

        response.setImage(item.getImage());

        response.setQuantity(item.getQuantity());

        response.setOriginalPrice(item.getOriginalPrice());

        response.setDiscountAmount(item.getDiscountAmount());

        response.setFinalPrice(item.getFinalPrice());

        response.setNote(item.getNote());

        response.setLineTotal(
                item.getFinalPrice()
                        .multiply(
                                BigDecimal.valueOf(
                                        item.getQuantity()
                                )
                        )
        );

        return response;

    }

    @Override
    public OrderDetailResponse updateStatus(
            Integer orderId,
            Integer newStatus
    ) {

        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException("Không tìm thấy đơn hàng."));

        Integer currentStatus = order.getStatus();

        validateStatusTransition(currentStatus, newStatus);

        switch (newStatus) {

            case 1 -> {
                order.setStatus(1);
            }

            case 2 -> {
                order.setStatus(2);
            }

            case 3 -> {

                order.setStatus(3);

                order.setCompletedAt(LocalDateTime.now());

                applyLoyaltyPoints(order);

            }

            case 4 -> {

                restoreStock(order.getId());

                order.setStatus(4);

            }

            case 5 -> {

                restoreStock(order.getId());

                order.setStatus(5);

            }

            case 6 -> {

                order.setStatus(6);

            }

            case 7 -> {

                restoreStock(order.getId());

                order.setStatus(7);

            }

            default -> throw new RuntimeException("Trạng thái không hợp lệ.");

        }

        orderRepository.save(order);

        return getOrderDetail(order.getId());

    }

    private void validateStatusTransition(
            Integer currentStatus,
            Integer newStatus
    ) {

        if (currentStatus == null) {
            throw new RuntimeException("Trạng thái hiện tại không hợp lệ.");
        }

        switch (currentStatus) {

            case 0 -> {

                if (newStatus != 1 && newStatus != 4) {
                    throw new RuntimeException(
                            "Đơn chờ xác nhận chỉ được xác nhận hoặc hủy."
                    );
                }

            }

            case 1 -> {

                if (newStatus != 2) {
                    throw new RuntimeException(
                            "Đơn đã xác nhận chỉ được chuyển sang đang giao."
                    );
                }

            }

            case 2 -> {

                if (newStatus != 3 && newStatus != 5) {
                    throw new InvalidOrderStatusException(
                            "Đơn đang giao chỉ được hoàn thành hoặc giao thất bại."
                    );
                }

            }

            case 3 -> {

                if (newStatus != 6) {
                    throw new RuntimeException(
                            "Đơn hoàn thành chỉ được yêu cầu hoàn hàng."
                    );
                }

            }

            case 4 -> {

                throw new RuntimeException(
                        "Đơn đã hủy không thể cập nhật."
                );

            }

            case 5 -> {

                throw new RuntimeException(
                        "Đơn giao thất bại không thể cập nhật."
                );

            }

            case 6 -> {

                if (newStatus != 7) {

                    throw new RuntimeException(
                            "Đơn đang hoàn hàng chỉ được chuyển sang hoàn tất."
                    );

                }

            }

            case 7 -> {

                throw new RuntimeException(
                        "Đơn đã hoàn hàng hoàn tất không thể cập nhật."
                );

            }

            default -> throw new RuntimeException("Trạng thái không hợp lệ.");

        }

    }

    private void restoreStock(Integer orderId) {

        List<OrderItem> items =
                orderItemRepository.findByOrderId(orderId);

        for (OrderItem item : items) {

            ProductVariant variant =
                    item.getProductVariant();

            variant.setStockQuantity(
                    variant.getStockQuantity()
                            + item.getQuantity()
            );

            productVariantRepository.save(variant);

        }

    }

    private void applyLoyaltyPoints(Order order) {

        if (Boolean.TRUE.equals(order.getLoyaltyPointsApplied())) {
            return;
        }

        if (order.getCustomer() == null) {
            return;
        }

        Customer customer = order.getCustomer();

        int currentPoint = customer.getLoyaltyPoints() == null
                ? 0
                : customer.getLoyaltyPoints();

        int earnedPoint =
                order.getFinalAmount()
                        .divide(BigDecimal.valueOf(100000))
                        .intValue();

        customer.setLoyaltyPoints(
                currentPoint + earnedPoint
        );

        customerRepository.save(customer);

        order.setLoyaltyPointsEarned(
                earnedPoint
        );

        order.setLoyaltyPointsApplied(true);

    }

    @Override
    public void cancelOrder(Integer orderId) {

        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() ->
                        new OrderNotFoundException("Không tìm thấy đơn hàng."));

        if (order.getStatus() != 0) {

            throw new RuntimeException(
                    "Chỉ được hủy đơn đang chờ xác nhận."
            );

        }

        restoreStock(orderId);

        order.setStatus(4);

        orderRepository.save(order);

    }
}
