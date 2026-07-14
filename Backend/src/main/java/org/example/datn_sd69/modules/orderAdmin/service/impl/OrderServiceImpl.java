package org.example.datn_sd69.modules.orderAdmin.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.ProductImage;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.example.datn_sd69.repository.OrderRepository;
import org.example.datn_sd69.repository.ProductImageRepository;
import org.example.datn_sd69.repository.ProductVariantRepository;
import org.example.datn_sd69.modules.orderAdmin.dto.request.UpdateOrderStatusRequest;
import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderDetailResponse;
import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderItemResponse;
import org.example.datn_sd69.modules.orderAdmin.dto.response.OrderResponse;
import org.example.datn_sd69.modules.orderAdmin.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    private final OrderItemRepository orderItemRepository;

    private final ProductVariantRepository productVariantRepository;

    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAll(
            String keyword,
            String orderType,
            Integer status,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(page, size);

        Page<Order> orderPage = orderRepository.searchAdminOrders(
                keyword,
                status,
                orderType,
                pageable
        );

        return orderPage.map(this::mapOrderResponse);
    }

    /**
     * Mapping Order -> OrderResponse
     */
    private OrderResponse mapOrderResponse(Order order) {

        OrderResponse response = new OrderResponse();

        response.setId(order.getId());

        response.setOrderType(order.getOrderType());

        response.setCustomerName(order.getCustomerName());

        response.setCustomerPhone(order.getCustomerPhone());

        response.setPaymentMethod(order.getPaymentMethod());

        response.setStatus(order.getStatus());

        response.setTotalAmount(order.getTotalAmount());

        response.setDiscountAmount(order.getDiscountAmount());

        response.setFinalAmount(order.getFinalAmount());

        response.setCreatedAt(order.getCreatedAt());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getById(Integer id) {

        Order order = orderRepository.findDetailById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        OrderDetailResponse response = new OrderDetailResponse();

        response.setId(order.getId());
        response.setOrderType(order.getOrderType());
        response.setCustomerName(order.getCustomerName());
        response.setCustomerPhone(order.getCustomerPhone());
        response.setShippingAddress(order.getShippingAddress());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setFinalAmount(order.getFinalAmount());
        response.setCreatedAt(order.getCreatedAt());

        if (order.getVoucher() != null) {
            response.setVoucherName(order.getVoucher().getCode());
        }

        response.setItems(
                orderItemRepository.findDetailByOrderId(id)
                        .stream()
                        .map(item -> {

                            OrderItemResponse dto = new OrderItemResponse();

                            dto.setId(item.getId());

                            dto.setProductVariantId(
                                    item.getProductVariant().getId()
                            );

                            dto.setProductName(
                                    item.getProductVariant()
                                            .getProduct()
                                            .getName()
                            );

                            dto.setSku(
                                    item.getProductVariant()
                                            .getSku()
                            );

                            dto.setCapacity(
                                    item.getProductVariant()
                                            .getCapacity()
                                            .getValue() + " ml"
                            );

                            dto.setBottleType(
                                    item.getProductVariant()
                                            .getBottleType()
                                            .getName()
                            );

                            dto.setQuantity(item.getQuantity());

                            dto.setOriginalPrice(item.getOriginalPrice());

                            dto.setDiscountAmount(item.getDiscountAmount());

                            dto.setFinalPrice(item.getFinalPrice());

                            dto.setNote(item.getNote());

                            dto.setImage(
                                    getPrimaryImage(
                                            item.getProductVariant()
                                                    .getProduct()
                                                    .getId()
                                    )
                            );

                            return dto;

                        })
                        .toList()
        );

        return response;

    }

    @Override
    public void updateStatus(
            Integer id,
            UpdateOrderStatusRequest request
    ) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy đơn hàng")
                );

        Integer currentStatus = order.getStatus();

        Integer newStatus = request.getStatus();

        // Không cho cập nhật đơn đã hủy
        if (currentStatus == 4) {
            throw new RuntimeException("Đơn hàng đã hủy.");
        }

        // Không cho cập nhật đơn đã hoàn thành
        if (currentStatus == 3) {
            throw new RuntimeException("Đơn hàng đã hoàn thành.");
        }

        // Chỉ cho phép tăng đúng 1 trạng thái
        if (newStatus != currentStatus + 1) {
            throw new RuntimeException(
                    "Chỉ được chuyển trạng thái theo luồng: "
                            + currentStatus + " -> " + (currentStatus + 1)
            );
        }

        order.setStatus(newStatus);

        // Nếu hoàn thành thì lưu thời gian hoàn thành
        if (newStatus == 3) {
            order.setCompletedAt(java.time.LocalDateTime.now());
        }

        orderRepository.save(order);

        validateStatusTransition(currentStatus, newStatus);
    }

    @Override
    public void cancel(Integer id) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy đơn hàng")
                );

        switch (order.getStatus()) {

            case 3 ->
                    throw new RuntimeException("Đơn hàng đã hoàn thành.");

            case 4 ->
                    throw new RuntimeException("Đơn hàng đã hủy.");

        }

        var items = orderItemRepository.findDetailByOrderId(id);

        for (var item : items) {

            var variant = item.getProductVariant();

            variant.setStockQuantity(
                    variant.getStockQuantity() + item.getQuantity()
            );

            productVariantRepository.save(variant);

        }

        order.setStatus(4);

        orderRepository.save(order);

    }

    private void validateStatusTransition(Integer currentStatus,
                                          Integer newStatus) {

        if (currentStatus == 4) {
            throw new RuntimeException("Đơn hàng đã hủy.");
        }

        if (currentStatus == 3) {
            throw new RuntimeException("Đơn hàng đã hoàn thành.");
        }

        if (!newStatus.equals(currentStatus + 1)) {
            throw new RuntimeException(
                    "Không thể chuyển trạng thái từ "
                            + currentStatus + " sang " + newStatus
            );
        }

    }

    private String getPrimaryImage(Integer productId){

        Optional<ProductImage> image =
                productImageRepository
                        .findFirstByProduct_IdAndIsPrimaryTrue(productId);

        if(image.isPresent()){
            return image.get().getImageUrl();
        }

        image = productImageRepository
                .findFirstByProduct_Id(productId);

        return image.map(ProductImage::getImageUrl)
                .orElse(null);

    }
}
