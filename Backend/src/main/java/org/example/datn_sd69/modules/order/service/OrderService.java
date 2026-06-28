package org.example.datn_sd69.modules.order.service;

import jakarta.transaction.Transactional;
import org.example.datn_sd69.entity.*;
import org.example.datn_sd69.modules.order.dto.request.OrderRequest;
import org.example.datn_sd69.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            String vnpCreateDate = java.time.LocalDateTime.now().format(formatter);

            // ⚠️ QUAN TRỌNG: M phải điền cái chuỗi HashSecret lấy từ mail VNPay Sandbox vào đây
            // Mã TmnCode của m đang là GX7E4QMO nên bắt buộc phải có SecretKey đi kèm
            String vnp_HashSecret = "4NBUT8VATC523HQV2EO329GO1BU1E00F";

            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", "2.1.0");
            vnp_Params.put("vnp_Command", "pay");
            vnp_Params.put("vnp_TmnCode", "FE4Z3PVL");
            vnp_Params.put("vnp_Amount", String.valueOf(finalAmount.longValue() * 100));
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_TxnRef", order.getId() + "_" + vnpCreateDate);
            vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + order.getId());
            vnp_Params.put("vnp_OrderType", "other");
            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_ReturnUrl", "http://localhost:5173/payment-return");
            vnp_Params.put("vnp_IpAddr", "127.0.0.1");
            vnp_Params.put("vnp_CreateDate", vnpCreateDate);

            // 1. Sắp xếp tham số theo bảng chữ cái (Bắt buộc của VNPay)
            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    try {
                        hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                        query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString())).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                        if (itr.hasNext()) {
                            query.append('&');
                            hashData.append('&');
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }

            // 2. Tạo chữ ký (Secure Hash) bằng HMAC SHA-512
            String queryUrl = query.toString();
            String vnp_SecureHash = "";
            try {
                Mac hmac512 = Mac.getInstance("HmacSHA512");
                SecretKeySpec secretKey = new SecretKeySpec(vnp_HashSecret.getBytes(), "HmacSHA512");
                hmac512.init(secretKey);
                byte[] result = hmac512.doFinal(hashData.toString().getBytes(StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder(2 * result.length);
                for (byte b : result) {
                    sb.append(String.format("%02x", b & 0xff));
                }
                vnp_SecureHash = sb.toString();
            } catch (Exception e) { e.printStackTrace(); }

            // 3. Gắn chữ ký vào cuối URL
            queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
            String paymentUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?" + queryUrl;

            response.put("paymentUrl", paymentUrl);
        }

        return response;
    }
}