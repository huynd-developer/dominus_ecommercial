package org.example.datn_sd69.modules.orderAdmin.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderAdminResponse;
import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderDetailAdminResponse;
import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderItemAdminResponse;
import org.example.datn_sd69.modules.orderAdmin.service.OrderAdminService;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.example.datn_sd69.repository.OrderRepository;
import org.example.datn_sd69.repository.ProductVariantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderAdminServiceImpl implements OrderAdminService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    public Page<OrderAdminResponse> searchOrders(String keyword, Integer status, String orderType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orderPage = orderRepository.searchAdminOrders(keyword, status, orderType, pageable);

        return orderPage.map(order -> OrderAdminResponse.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .orderType(order.getOrderType())
                .totalAmount(order.getTotalAmount())
                .finalAmount(order.getFinalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .build());
    }

    @Override
    public OrderDetailAdminResponse getOrderDetail(Integer orderId) {
        Order order = orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        List<OrderItem> items = orderItemRepository.findDetailByOrderId(orderId);

        List<OrderItemAdminResponse> itemResponses = items.stream().map(item -> {
            ProductVariant variant = item.getProductVariant();
            String productName = variant.getProduct() != null ? variant.getProduct().getName() : "N/A";
            String capacity = variant.getCapacity() != null ? String.valueOf(variant.getCapacity().getValue()) : "";
            String bottleType = variant.getBottleType() != null ? variant.getBottleType().getName() : "";

            return OrderItemAdminResponse.builder()
                    .id(item.getId())
                    .productName(productName)
                    .capacity(capacity)
                    .bottleType(bottleType)
                    .quantity(item.getQuantity())
                    .originalPrice(item.getOriginalPrice())
                    .finalPrice(item.getFinalPrice())
                    .discountAmount(item.getDiscountAmount())
                    .image(item.getImage())
                    .build();
        }).collect(Collectors.toList());

        return OrderDetailAdminResponse.builder()
                .id(order.getId())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .shippingAddress(order.getShippingAddress())
                .orderType(order.getOrderType())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .paymentMethod(order.getPaymentMethod())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }

    @Override
    @Transactional
    public void nextOrderStatus(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        Integer currentStatus = order.getStatus();

        // Luồng dương: 0 -> 1 -> 2 -> 3
        if (currentStatus >= 3) {
            throw new RuntimeException("Đơn hàng đã hoàn thành hoặc bị hủy, không thể tiếp tục!");
        }

        order.setStatus(currentStatus + 1);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void cancelOrder(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        // Chỉ cho phép hủy nếu chưa hoàn thành hoặc chưa hủy
        if (order.getStatus() == 3 || order.getStatus() == 4) {
            throw new RuntimeException("Trạng thái đơn hàng không hợp lệ để hủy!");
        }

        // Cập nhật trạng thái = 4 (Đã hủy)
        order.setStatus(4);
        orderRepository.save(order);

        // Lấy danh sách sản phẩm và cộng lại tồn kho
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : orderItems) {
            ProductVariant variant = item.getProductVariant();
            if (variant != null) {
                int currentStock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
                variant.setStockQuantity(currentStock + item.getQuantity());
                productVariantRepository.save(variant);
            }
        }
    }
}