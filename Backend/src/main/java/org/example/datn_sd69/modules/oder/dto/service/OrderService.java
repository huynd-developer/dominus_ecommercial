package org.example.datn_sd69.modules.oder.service;

import jakarta.transaction.Transactional;
import org.example.datn_sd69.entity.Cart;
import org.example.datn_sd69.entity.CartItem;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.modules.oder.dto.OrderRequest;
import org.example.datn_sd69.repository.CartRepository;
import org.example.datn_sd69.repository.ProductVariantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class OrderService {
    @Autowired
    private CartRepository cartRepo;
    @Autowired private OrderRepository orderRepo;
    @Autowired private OrderItemRepository orderItemRepo;
    @Autowired private ProductVariantRepository variantRepo;

    @Transactional
    public Order placeOrder(Integer customerId, OrderRequest request) {
        // 1. Lấy giỏ hàng
        Cart cart = cartRepo.findByCustomerUserId(customerId)
                .orElseThrow(() -> new RuntimeException("Giỏ hàng trống"));

        if (cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng không có sản phẩm nào");
        }

        // 2. Tính toán tiền & Kiểm tra tồn kho
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem item : cart.getCartItems()) {
            ProductVariant variant = item.getProductVariant();

            // Kiểm tra tồn kho trước khi đặt
            if (variant.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Sản phẩm " + variant.getSku() + " chỉ còn " + variant.getStockQuantity() + " trong kho!");
            }

            // Trừ tồn kho tạm thời
            variant.setStockQuantity(variant.getStockQuantity() - item.getQuantity());
            variantRepo.save(variant);

            // Tính tiền
            BigDecimal lineTotal = variant.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
        }

        // 3. Tạo Order
        Order order = new Order();
        order.setCustomerId(cart.getCustomer().getUserId()); // Giả định CustomerId là UserID
        order.setOrderType("ONLINE");
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setShippingAddress(request.getShippingAddress());
        order.setTotalAmount(totalAmount);
        order.setFinalAmount(totalAmount); // Hiện tại không có giảm giá
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus(0); // Chờ xác nhận
        order = orderRepo.save(order);

        // 4. Tạo OrderItems
        for (CartItem item : cart.getCartItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductVariant(item.getProductVariant());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setOriginalPrice(item.getProductVariant().getPrice());
            orderItem.setFinalPrice(item.getProductVariant().getPrice());
            orderItemRepo.save(orderItem);
        }

        // 5. Xóa giỏ hàng sau khi đặt thành công
        // (M viết hàm clearCart trong CartService hoặc xử lý ở đây)
        cart.getCartItems().clear();
        cartRepo.save(cart);

        return order;
    }
}
