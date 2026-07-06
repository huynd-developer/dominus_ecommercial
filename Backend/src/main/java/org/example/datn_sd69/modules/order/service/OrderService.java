package org.example.datn_sd69.modules.order.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Cart;
import org.example.datn_sd69.entity.CartItem;
import org.example.datn_sd69.entity.Customer;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.modules.order.dto.request.OrderRequest;
import org.example.datn_sd69.repository.CartItemRepository;
import org.example.datn_sd69.repository.CartRepository;
import org.example.datn_sd69.repository.CustomerRepository;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.example.datn_sd69.repository.OrderRepository;
import org.example.datn_sd69.repository.ProductVariantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int ORDER_STATUS_PENDING = 0;

    private static final int PRODUCT_VARIANT_ACTIVE = 1;

    private final CartRepository cartRepo;
    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final ProductVariantRepository variantRepo;
    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepo;

    @Value("${vnpay.tmnCode}")
    private String vnpTmnCode;

    @Value("${vnpay.hashSecret}")
    private String vnpHashSecret;

    @Value("${vnpay.url}")
    private String vnpUrl;

    @Value("${vnpay.returnUrl}")
    private String vnpReturnUrl;

    @Value("${vnpay.version}")
    private String vnpVersion;

    @Transactional
    public Map<String, Object> placeOrder(Integer customerId, OrderRequest request) {
        validateCheckoutRequest(customerId, request);

        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy khách hàng"
                ));

        Cart cart = cartRepo.findByCustomerUserId(customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Giỏ hàng trống"
                ));

        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Giỏ hàng không có sản phẩm nào"
            );
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        List<CartItem> cartItems = new ArrayList<>(cart.getCartItems());

        for (CartItem item : cartItems) {
            validateCartItem(item);

            ProductVariant variant = item.getProductVariant();

            if (variant.getStockQuantity() < item.getQuantity()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sản phẩm " + variant.getSku() + " chỉ còn " + variant.getStockQuantity() + " trong kho"
                );
            }

            BigDecimal lineTotal = variant.getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            totalAmount = totalAmount.add(lineTotal);
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal finalAmount = totalAmount.subtract(discountAmount);

        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderType("ONLINE");
        order.setCustomerName(normalizeText(request.getCustomerName(), "Tên người nhận"));
        order.setCustomerPhone(normalizeNoWhitespace(request.getCustomerPhone(), "Số điện thoại"));
        order.setShippingAddress(normalizeText(request.getShippingAddress(), "Địa chỉ giao hàng"));
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setFinalAmount(finalAmount);
        order.setPaymentMethod(normalizePaymentMethod(request.getPaymentMethod()));
        order.setStatus(ORDER_STATUS_PENDING);
        order.setCreatedAt(LocalDateTime.now());

        order.setLoyaltyPointsApplied(false);
        order.setLoyaltyPointsEarned(0);
        order.setCompletedAt(null);

        Order savedOrder = orderRepo.save(order);

        for (CartItem item : cartItems) {
            ProductVariant variant = item.getProductVariant();

            variant.setStockQuantity(variant.getStockQuantity() - item.getQuantity());
            variantRepo.save(variant);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProductVariant(variant);
            orderItem.setQuantity(item.getQuantity());
            orderItem.setOriginalPrice(variant.getPrice());
            orderItem.setDiscountAmount(BigDecimal.ZERO);

            BigDecimal finalPrice = variant.getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));

            orderItem.setFinalPrice(finalPrice);
            orderItem.setNote(normalizeOptionalNote(request.getNote()));
            orderItem.setImage(item.getThumbnailUrl());

            orderItemRepo.save(orderItem);
        }

        cartItemRepository.deleteAll(cartItems);
        cart.getCartItems().clear();
        cartRepo.save(cart);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("orderId", savedOrder.getId());
        response.put("status", savedOrder.getStatus());
        response.put("paymentMethod", savedOrder.getPaymentMethod());
        response.put("totalAmount", savedOrder.getTotalAmount());
        response.put("discountAmount", savedOrder.getDiscountAmount());
        response.put("finalAmount", savedOrder.getFinalAmount());
        response.put("message", "Đặt hàng thành công. Đơn hàng đang chờ xác nhận.");

        if ("VNPAY".equalsIgnoreCase(savedOrder.getPaymentMethod())) {
            response.put("paymentUrl", buildVnPayPaymentUrl(savedOrder));
        }

        return response;
    }

    private void validateCheckoutRequest(Integer customerId, OrderRequest request) {
        if (customerId == null || customerId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Tài khoản khách hàng không hợp lệ"
            );
        }

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Dữ liệu đặt hàng không được để trống"
            );
        }

        normalizeText(request.getCustomerName(), "Tên người nhận");
        normalizeNoWhitespace(request.getCustomerPhone(), "Số điện thoại");
        normalizeText(request.getShippingAddress(), "Địa chỉ giao hàng");
        normalizePaymentMethod(request.getPaymentMethod());
        normalizeOptionalNote(request.getNote());
    }

    private void validateCartItem(CartItem item) {
        if (item == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Dữ liệu giỏ hàng không hợp lệ"
            );
        }

        ProductVariant variant = item.getProductVariant();

        if (variant == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sản phẩm trong giỏ hàng không tồn tại"
            );
        }

        if (variant.getStatus() == null || variant.getStatus() != PRODUCT_VARIANT_ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sản phẩm " + variant.getSku() + " hiện không còn kinh doanh"
            );
        }

        if (variant.getPrice() == null || variant.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Giá sản phẩm không hợp lệ"
            );
        }

        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số lượng sản phẩm trong giỏ hàng không hợp lệ"
            );
        }

        if (variant.getStockQuantity() == null || variant.getStockQuantity() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tồn kho sản phẩm không hợp lệ"
            );
        }
    }

    private String normalizeText(String value, String fieldName) {
        if (value == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " không được để trống"
            );
        }

        String trimmed = value.trim();

        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " không được để trống"
            );
        }

        if (!trimmed.equals(value)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " không được có khoảng trắng ở đầu hoặc cuối"
            );
        }

        if (trimmed.contains("  ")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " không được chứa nhiều khoảng trắng liên tiếp"
            );
        }

        if (trimmed.length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " quá dài"
            );
        }

        return trimmed;
    }

    private String normalizeNoWhitespace(String value, String fieldName) {
        if (value == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " không được để trống"
            );
        }

        String trimmed = value.trim();

        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " không được để trống"
            );
        }

        if (!trimmed.equals(value)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " không được có khoảng trắng ở đầu hoặc cuối"
            );
        }

        if (trimmed.chars().anyMatch(Character::isWhitespace)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " không được chứa khoảng trắng"
            );
        }

        if ("Số điện thoại".equals(fieldName) && !trimmed.matches("^0\\d{9}$")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số điện thoại phải gồm 10 số và bắt đầu bằng 0"
            );
        }

        return trimmed;
    }

    private String normalizePaymentMethod(String paymentMethod) {
        String value = normalizeNoWhitespace(paymentMethod, "Phương thức thanh toán")
                .toUpperCase();

        if (!value.equals("COD") && !value.equals("VNPAY")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phương thức thanh toán chỉ hỗ trợ COD hoặc VNPAY"
            );
        }

        return value;
    }

    private String normalizeOptionalNote(String note) {
        if (note == null) {
            return null;
        }

        String trimmed = note.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        if (!trimmed.equals(note)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ghi chú không được có khoảng trắng ở đầu hoặc cuối"
            );
        }

        if (trimmed.length() > 255) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ghi chú tối đa 255 ký tự"
            );
        }

        return trimmed;
    }

    private String buildVnPayPaymentUrl(Order order) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            String vnpCreateDate = LocalDateTime.now().format(formatter);

            Map<String, String> vnpParams = new HashMap<>();
            vnpParams.put("vnp_Version", vnpVersion);
            vnpParams.put("vnp_Command", "pay");
            vnpParams.put("vnp_TmnCode", vnpTmnCode);
            vnpParams.put("vnp_Amount", String.valueOf(order.getFinalAmount().longValue() * 100));
            vnpParams.put("vnp_CurrCode", "VND");
            vnpParams.put("vnp_TxnRef", order.getId() + "_" + vnpCreateDate);
            vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + order.getId());
            vnpParams.put("vnp_OrderType", "other");
            vnpParams.put("vnp_Locale", "vn");
            vnpParams.put("vnp_ReturnUrl", vnpReturnUrl);
            vnpParams.put("vnp_IpAddr", "127.0.0.1");
            vnpParams.put("vnp_CreateDate", vnpCreateDate);

            List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
            Collections.sort(fieldNames);

            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            for (int i = 0; i < fieldNames.size(); i++) {
                String fieldName = fieldNames.get(i);
                String fieldValue = vnpParams.get(fieldName);

                if (fieldValue == null || fieldValue.isEmpty()) {
                    continue;
                }

                hashData.append(fieldName)
                        .append("=")
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()))
                        .append("=")
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                if (i < fieldNames.size() - 1) {
                    hashData.append("&");
                    query.append("&");
                }
            }

            String secureHash = hmacSHA512(vnpHashSecret, hashData.toString());

            return vnpUrl + "?" + query + "&vnp_SecureHash=" + secureHash;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không tạo được URL thanh toán VNPay"
            );
        }
    }

    private String hmacSHA512(String key, String data) throws Exception {
        Mac hmac512 = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKey = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8),
                "HmacSHA512"
        );

        hmac512.init(secretKey);

        byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder(result.length * 2);

        for (byte b : result) {
            sb.append(String.format("%02x", b & 0xff));
        }

        return sb.toString();
    }
}