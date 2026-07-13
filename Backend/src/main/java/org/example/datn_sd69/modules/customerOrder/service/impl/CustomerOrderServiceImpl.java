package org.example.datn_sd69.modules.customerOrder.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Brand;
import org.example.datn_sd69.entity.Customer;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.entity.Product;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.modules.customerOrder.dto.CustomerOrderItemResponse;
import org.example.datn_sd69.modules.customerOrder.dto.CustomerOrderResponse;
import org.example.datn_sd69.modules.customerOrder.service.CustomerOrderService;
import org.example.datn_sd69.repository.CustomerRepository;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.example.datn_sd69.repository.OrderRepository;
import org.example.datn_sd69.repository.ProductVariantRepository;
import org.example.datn_sd69.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerOrderServiceImpl implements CustomerOrderService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_CONFIRMED = 1;
    private static final int STATUS_SHIPPING = 2;
    private static final int STATUS_COMPLETED = 3;
    private static final int STATUS_CANCELLED = 4;

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerOrderResponse> getMyOrders() {
        Customer customer = getCurrentCustomer();

        return orderRepository.findByCustomer_UserIdOrderByCreatedAtDesc(customer.getUserId())
                .stream()
                .map(order -> {
                    List<OrderItem> items =
                            orderItemRepository.findByOrderId(order.getId());
                    return mapToOrderResponse(order, items);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerOrderResponse getOrderDetail(Integer orderId) {
        Customer customer = getCurrentCustomer();

        validateId(orderId, "orderId");

        Order order = orderRepository.findByIdAndCustomer_UserId(orderId, customer.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng hoặc đơn hàng không thuộc tài khoản của bạn"
                ));

        List<OrderItem> items =
                orderItemRepository.findByOrderId(order.getId());

        return mapToOrderResponse(order, items);
    }

    @Override
    @Transactional
    public void cancelOrder(Integer orderId) {
        Customer customer = getCurrentCustomer();

        validateId(orderId, "orderId");

        Order order = orderRepository.findByIdAndCustomer_UserId(orderId, customer.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng hoặc đơn hàng không thuộc tài khoản của bạn"
                ));

        if (!Integer.valueOf(STATUS_PENDING).equals(order.getStatus())) {
            throw badRequest("Chỉ được hủy đơn hàng khi đơn đang ở trạng thái chờ xác nhận");
        }

        List<OrderItem> items =
                orderItemRepository.findByOrderId(order.getId());

        if (items.isEmpty()) {
            throw badRequest("Đơn hàng không có sản phẩm, không thể hủy");
        }

        for (OrderItem item : items) {
            ProductVariant variant = item.getProductVariant();

            if (variant == null) {
                throw badRequest("Dữ liệu sản phẩm trong đơn hàng không hợp lệ");
            }

            Integer quantity = item.getQuantity();

            if (quantity == null || quantity <= 0) {
                throw badRequest("Số lượng sản phẩm trong đơn hàng không hợp lệ");
            }

            Integer currentStock = variant.getStockQuantity() == null
                    ? 0
                    : variant.getStockQuantity();

            variant.setStockQuantity(currentStock + quantity);

            productVariantRepository.save(variant);
        }

        order.setStatus(STATUS_CANCELLED);
        orderRepository.save(order);
    }

    private CustomerOrderResponse mapToOrderResponse(Order order, List<OrderItem> items) {
        List<CustomerOrderItemResponse> itemResponses = items.stream()
                .map(this::mapToOrderItemResponse)
                .toList();

        return new CustomerOrderResponse(
                order.getId(),
                order.getOrderType(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getShippingAddress(),
                moneyOrZero(order.getTotalAmount()),
                moneyOrZero(order.getDiscountAmount()),
                moneyOrZero(order.getFinalAmount()),
                order.getPaymentMethod(),
                order.getStatus(),
                getStatusText(order.getStatus()),
                Integer.valueOf(STATUS_PENDING).equals(order.getStatus()),
                order.getCreatedAt(),
                itemResponses
        );
    }

    private CustomerOrderItemResponse mapToOrderItemResponse(OrderItem item) {
        ProductVariant variant = item.getProductVariant();
        Product product = variant != null ? variant.getProduct() : null;
        Brand brand = product != null ? product.getBrand() : null;

        return new CustomerOrderItemResponse(
                item.getId(),
                variant != null ? variant.getId() : null,
                product != null ? product.getId() : null,
                product != null ? product.getName() : null,
                brand != null ? brand.getName() : null,
                variant != null ? variant.getSku() : null,
                item.getQuantity(),
                moneyOrZero(item.getOriginalPrice()),
                moneyOrZero(item.getDiscountAmount()),
                moneyOrZero(item.getFinalPrice()),
                item.getNote(),
                item.getImage()
        );
    }

    private String getStatusText(Integer status) {
        if (status == null) {
            return "Không xác định";
        }

        return switch (status) {
            case STATUS_PENDING -> "Chờ xác nhận";
            case STATUS_CONFIRMED -> "Đã xác nhận";
            case STATUS_SHIPPING -> "Đang giao hàng";
            case STATUS_COMPLETED -> "Hoàn thành";
            case STATUS_CANCELLED -> "Đã hủy";
            default -> "Không xác định";
        };
    }

    private Customer getCurrentCustomer() {
        User user = getCurrentUser();

        return customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Tài khoản hiện tại không phải khách hàng"
                ));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Bạn chưa đăng nhập"
            );
        }

        String email = authentication.getName();

        if (email == null || email.trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token không hợp lệ"
            );
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Không tìm thấy tài khoản đăng nhập"
                ));
    }

    private void validateId(Integer id, String fieldName) {
        if (id == null || id <= 0) {
            throw badRequest(fieldName + " phải là số nguyên dương");
        }
    }

    private BigDecimal moneyOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}