package org.example.datn_sd69.modules.order.service;

import jakarta.transaction.Transactional;
import org.example.datn_sd69.entity.*;
import org.example.datn_sd69.modules.order.dto.request.OrderRequest;
import org.example.datn_sd69.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class OrderService {
    @Autowired private CartRepository cartRepo;
    @Autowired private OrderRepository orderRepo;
    @Autowired private OrderItemRepository orderItemRepo;
    @Autowired private ProductVariantRepository variantRepo;
    @Autowired private CartItemRepository cartItemRepository;

    @Transactional
    public Map<String, Object> placeOrder(Integer customerId, OrderRequest request) {
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

            // Kiểm tra tồn kho trước khi đặt (Chống Overselling)
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

        // ÁP DỤNG CÔNG THỨC TÍNH TIỀN CỦA TASK
        BigDecimal discountAmount = BigDecimal.ZERO; // Hiện tại discount = 0
        BigDecimal finalAmount = totalAmount.subtract(discountAmount); // FinalAmount = TotalAmount - DiscountAmount

        // 3. Tạo Order
        Order order = new Order();
        order.setOrderType("ONLINE"); // Yêu cầu task
        order.setCustomerName(request.getCustomerName());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setShippingAddress(request.getShippingAddress());
        order.setTotalAmount(totalAmount);

        // Cần đảm bảo Entity Order của bạn có trường discountAmount nhé
        // order.setDiscountAmount(discountAmount);

        order.setFinalAmount(finalAmount);
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

            BigDecimal finalPrice = price.multiply(BigDecimal.valueOf(qty));
            orderItem.setFinalPrice(finalPrice);

            orderItem.setNote(request.getNote());

            orderItemRepo.save(orderItem);
        }

        // 5. Xóa giỏ hàng
        cartItemRepository.deleteAll(cart.getCartItems());
        cart.getCartItems().clear();
        cartRepo.save(cart);

        // ==========================================
        // 6. XỬ LÝ TRẢ VỀ DỮ LIỆU ĐỂ FE REDIRECT (VNPay / COD)
        // ==========================================
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", order.getId());
        response.put("message", "Đặt hàng thành công!");

        if ("VNPAY".equalsIgnoreCase(request.getPaymentMethod())) {
            // YÊU CẦU TASK: Trỏ request thanh toán sang môi trường VNPay Sandbox với tmnCode=GX7E4QMO
            // Đây là URL giả lập cấu trúc VNPay. Trong thực tế nhóm bạn sẽ cần class VNPayConfig để hash mã vnp_SecureHash.
            String vnpayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html" +
                    "?vnp_Version=2.1.0" +
                    "&vnp_Command=pay" +
                    "&vnp_TmnCode=GX7E4QMO" +
                    "&vnp_Amount=" + (finalAmount.longValue() * 100) + // VNPay quy định nhân 100
                    "&vnp_CurrCode=VND" +
                    "&vnp_TxnRef=" + order.getId() +
                    "&vnp_OrderInfo=Thanh toan don hang " + order.getId() +
                    "&vnp_OrderType=other" +
                    "&vnp_Locale=vn" +
                    "&vnp_ReturnUrl=http://localhost:5173/payment-return" + // Trả về FE của bạn
                    "&vnp_IpAddr=127.0.0.1" +
                    "&vnp_CreateDate=20230101120000" +
                    "&vnp_SecureHash=MOCK_HASH_DE_TEST"; // Phải dùng HMAC-SHA512 để gen thật

            response.put("paymentUrl", vnpayUrl);
        }

        return response;
    }
}