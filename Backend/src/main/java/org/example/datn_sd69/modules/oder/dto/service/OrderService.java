package org.example.datn_sd69.modules.oder.dto.service;

import jakarta.transaction.Transactional;
import org.example.datn_sd69.entity.*;
import org.example.datn_sd69.modules.oder.dto.request.OrderRequest;
import org.example.datn_sd69.repository.*;
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

    // Đã bỏ chữ 'final' ở đây để không bị lỗi compile Java
    @Autowired private CartItemRepository cartItemRepository;

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

            int qty = item.getQuantity();
            BigDecimal price = item.getProductVariant().getPrice();

            orderItem.setQuantity(qty);
            orderItem.setOriginalPrice(price);

            // Tính FinalPrice cho đúng luật SQL: (Price * Quantity) - Discount
            // Vì hiện tại chưa có tính năng giảm giá nên trừ 0 (hoặc bỏ qua trừ)
            BigDecimal finalPrice = price.multiply(BigDecimal.valueOf(qty));
            orderItem.setFinalPrice(finalPrice);

            orderItemRepo.save(orderItem);
        }

        // ==========================================
        // 5. Xóa giỏ hàng sau khi đặt thành công
        // ==========================================

        // Bắn lệnh xuống thẳng DB để xóa sạch các item này
        cartItemRepository.deleteAll(cart.getCartItems());

        // Xóa tạm trong bộ nhớ (object) để đồng bộ dữ liệu
        cart.getCartItems().clear();
        cartRepo.save(cart);

        return order;
    }
}