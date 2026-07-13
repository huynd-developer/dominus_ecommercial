package org.example.datn_sd69.modules.orderAdmin.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderResponse;
import org.example.datn_sd69.modules.orderAdmin.service.OrderService;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.example.datn_sd69.repository.OrderRepository;
import org.example.datn_sd69.repository.ProductVariantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Integer PENDING = 0;
    private static final Integer CONFIRMED = 1;
    private static final Integer SHIPPING = 2;
    private static final Integer COMPLETED = 3;
    private static final Integer CANCELLED = 4;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse.ListItem> getOrders(
            String keyword,
            Integer status,
            String orderType,
            Pageable pageable
    ) {

        Page<Order> page = orderRepository.searchAdminOrders(
                keyword,
                status,
                orderType,
                pageable
        );

        return page.map(order -> {

            OrderResponse.ListItem dto =
                    new OrderResponse.ListItem();

            dto.setOrderId(order.getId());
            dto.setOrderType(order.getOrderType());
            dto.setCustomerName(order.getCustomerName());
            dto.setCustomerPhone(order.getCustomerPhone());
            dto.setFinalAmount(order.getFinalAmount());
            dto.setStatus(order.getStatus());
            dto.setStatusName(getStatusName(order.getStatus()));
            dto.setCreatedAt(order.getCreatedAt());

            return dto;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse.Detail getOrderDetail(
            Integer orderId
    ) {

        Order order = orderRepository.findDetailById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy đơn hàng"));

        List<OrderItem> orderItems =
                orderItemRepository.findByOrderId(orderId);

        OrderResponse.Detail detail =
                new OrderResponse.Detail();

        detail.setOrderId(order.getId());
        detail.setOrderType(order.getOrderType());
        detail.setCustomerName(order.getCustomerName());
        detail.setCustomerPhone(order.getCustomerPhone());
        detail.setShippingAddress(order.getShippingAddress());
        detail.setPaymentMethod(order.getPaymentMethod());
        detail.setStatus(order.getStatus());
        detail.setStatusName(getStatusName(order.getStatus()));
        detail.setTotalAmount(order.getTotalAmount());
        detail.setDiscountAmount(order.getDiscountAmount());
        detail.setFinalAmount(order.getFinalAmount());
        detail.setCreatedAt(order.getCreatedAt());

        List<OrderResponse.OrderItem> itemResponses =
                orderItems.stream()
                        .map(item -> {

                            OrderResponse.OrderItem dto =
                                    new OrderResponse.OrderItem();

                            ProductVariant variant = item.getProductVariant();

                            dto.setProductVariantId(
                                    variant != null ? variant.getId() : null
                            );

                            dto.setProductName(
                                    variant != null && variant.getProduct() != null
                                            ? variant.getProduct().getName()
                                            : "Không xác định"
                            );

                            dto.setQuantity(
                                    item.getQuantity()
                            );

                            dto.setOriginalPrice(
                                    item.getOriginalPrice()
                            );

                            dto.setDiscountAmount(
                                    item.getDiscountAmount()
                            );

                            dto.setFinalPrice(
                                    item.getFinalPrice()
                            );

                            dto.setImage(
                                    item.getImage()
                            );

                            return dto;
                        })
                        .toList();

        detail.setItems(itemResponses);

        return detail;
    }

    @Override
    public OrderResponse.UpdateStatus nextStatus(
            Integer orderId
    ) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy đơn hàng"));

        Integer oldStatus = order.getStatus();

        if (CANCELLED.equals(oldStatus)) {
            throw new RuntimeException(
                    "Đơn hàng đã bị hủy"
            );
        }

        if (COMPLETED.equals(oldStatus)) {
            throw new RuntimeException(
                    "Đơn hàng đã hoàn thành"
            );
        }

        Integer newStatus = oldStatus + 1;

        order.setStatus(newStatus);

        orderRepository.save(order);

        OrderResponse.UpdateStatus response =
                new OrderResponse.UpdateStatus();

        response.setOrderId(order.getId());
        response.setOldStatus(oldStatus);
        response.setNewStatus(newStatus);
        response.setMessage(
                "Cập nhật trạng thái thành công"
        );

        return response;
    }

    @Override
    public OrderResponse.UpdateStatus cancelOrder(
            Integer orderId
    ) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy đơn hàng"));

        if (COMPLETED.equals(order.getStatus())) {
            throw new RuntimeException(
                    "Không thể hủy đơn đã hoàn thành"
            );
        }

        if (CANCELLED.equals(order.getStatus())) {
            throw new RuntimeException(
                    "Đơn hàng đã bị hủy"
            );
        }

        List<OrderItem> items =
                orderItemRepository.findAllByOrder_Id(
                        orderId
                );

        for (OrderItem item : items) {

            ProductVariant variant =
                    item.getProductVariant();

            variant.setStockQuantity(
                    variant.getStockQuantity()
                            + item.getQuantity()
            );

            productVariantRepository.save(
                    variant
            );
        }

        Integer oldStatus = order.getStatus();

        order.setStatus(CANCELLED);

        orderRepository.save(order);

        OrderResponse.UpdateStatus response =
                new OrderResponse.UpdateStatus();

        response.setOrderId(order.getId());
        response.setOldStatus(oldStatus);
        response.setNewStatus(CANCELLED);
        response.setMessage(
                "Hủy đơn hàng thành công"
        );

        return response;
    }

    private String getStatusName(
            Integer status
    ) {

        return switch (status) {

            case 0 -> "Chờ xác nhận";

            case 1 -> "Đã xác nhận";

            case 2 -> "Đang giao";

            case 3 -> "Hoàn thành";

            case 4 -> "Đã hủy";

            default -> "Không xác định";
        };
    }
}