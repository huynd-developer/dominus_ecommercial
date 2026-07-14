package org.example.datn_sd69.modules.order.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Cart;
import org.example.datn_sd69.entity.CartItem;
import org.example.datn_sd69.entity.Customer;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.entity.Voucher;
import org.example.datn_sd69.modules.order.dto.request.OrderRequest;
import org.example.datn_sd69.modules.voucher.dto.request.VoucherRequest;
import org.example.datn_sd69.repository.CartItemRepository;
import org.example.datn_sd69.repository.CartRepository;
import org.example.datn_sd69.repository.CustomerRepository;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.example.datn_sd69.repository.OrderRepository;
import org.example.datn_sd69.repository.ProductVariantRepository;
import org.example.datn_sd69.repository.VoucherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int ORDER_STATUS_PENDING = 0;
    private static final int ORDER_STATUS_CANCELLED = 4;
    private static final int PRODUCT_VARIANT_ACTIVE = 1;
    private static final int ORDER_STATUS_COMPLETED = 3;
    private static final BigDecimal POINT_RATE_AMOUNT = BigDecimal.valueOf(10_000);
    private static final String PAYMENT_METHOD_COD = "COD";
    private static final String PAYMENT_METHOD_VIETQR = "VIETQR"; // ĐỔI VNPAY THÀNH VIETQR

    private final CartRepository cartRepo;
    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final ProductVariantRepository variantRepo;
    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepo;
    private final VoucherRepository voucherRepo;

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

        String paymentMethod = normalizePaymentMethod(request.getPaymentMethod());

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
            BigDecimal lineTotal = variant.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        Voucher appliedVoucher = null;

        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            appliedVoucher = voucherRepo.findByCode(request.getVoucherCode().trim())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá không tồn tại!"));

            if (appliedVoucher.getStatus() != 1
                    || appliedVoucher.getUsedCount() >= appliedVoucher.getUsageLimit()
                    || totalAmount.compareTo(appliedVoucher.getMinOrderValue()) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá không đủ điều kiện áp dụng!");
            }

            if ("PERCENT".equalsIgnoreCase(appliedVoucher.getDiscountType())) {
                BigDecimal percent = appliedVoucher.getDiscountValue().divide(BigDecimal.valueOf(100));
                discountAmount = totalAmount.multiply(percent);
            } else {
                discountAmount = appliedVoucher.getDiscountValue();
            }

            if (appliedVoucher.getMaxDiscount() != null && appliedVoucher.getMaxDiscount().compareTo(BigDecimal.ZERO) > 0) {
                if (discountAmount.compareTo(appliedVoucher.getMaxDiscount()) > 0) {
                    discountAmount = appliedVoucher.getMaxDiscount();
                }
            }
        }

        BigDecimal finalAmount = totalAmount.subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderType("ONLINE");
        order.setCustomerName(normalizeText(request.getCustomerName(), "Tên người nhận"));
        order.setCustomerPhone(normalizeNoWhitespace(request.getCustomerPhone(), "Số điện thoại"));
        order.setShippingAddress(normalizeText(request.getShippingAddress(), "Địa chỉ giao hàng"));
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setFinalAmount(finalAmount);
        order.setPaymentMethod(paymentMethod);
        order.setStatus(ORDER_STATUS_PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setLoyaltyPointsApplied(false);
        order.setLoyaltyPointsEarned(0);
        order.setCompletedAt(null);

        if (appliedVoucher != null) {
            order.setVoucher(appliedVoucher);
            appliedVoucher.setUsedCount(appliedVoucher.getUsedCount() + 1);
            if (appliedVoucher.getUsedCount() >= appliedVoucher.getUsageLimit()) {
                appliedVoucher.setStatus(0);
            }
            voucherRepo.save(appliedVoucher);
        }

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

            BigDecimal finalPrice = variant.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

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

        // CHỖ NÀY TRẢ VỀ LINK VIETQR HOẶC CHỜ XÁC NHẬN
        if (PAYMENT_METHOD_VIETQR.equalsIgnoreCase(savedOrder.getPaymentMethod())) {
            response.put("message", "Đơn hàng đã tạo. Vui lòng quét mã QR để thanh toán.");
            // Sinh link VietQR. (Lưu ý: Thay MB và 0123456789 thành Ngân hàng & STK của cửa hàng m)
            String qrUrl = String.format("https://img.vietqr.io/image/MB-0123456789-compact2.png?amount=%d&addInfo=ThanhToanDonHang%d",
                    savedOrder.getFinalAmount().longValue(), savedOrder.getId());
            response.put("qrUrl", qrUrl);
        } else {
            response.put("message", "Đặt hàng thành công. Đơn hàng đang chờ xác nhận.");
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

        if (variant.getPrice() == null || variant.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
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

        if (!value.equals(PAYMENT_METHOD_COD) && !value.equals(PAYMENT_METHOD_VIETQR)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phương thức thanh toán chỉ hỗ trợ COD hoặc VIETQR"
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

    @Transactional
    public Map<String, Object> completeOrderAndApplyLoyalty(Integer orderId) {
        if (orderId == null || orderId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã đơn hàng không hợp lệ"
            );
        }

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng"
                ));

        if (order.getStatus() != null && order.getStatus() == ORDER_STATUS_CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đơn hàng đã hủy, không thể hoàn thành"
            );
        }

        if (order.getStatus() != null && order.getStatus() == ORDER_STATUS_COMPLETED) {
            return buildCompleteOrderResponse(order, "Đơn hàng đã hoàn thành trước đó");
        }

        order.setStatus(ORDER_STATUS_COMPLETED);

        int pointsEarned = applyLoyaltyPointsIfNeeded(order);

        Order savedOrder = orderRepo.save(order);

        Map<String, Object> response = buildCompleteOrderResponse(
                savedOrder,
                "Đơn hàng đã hoàn thành"
        );

        response.put("loyaltyPointsEarned", pointsEarned);

        if (savedOrder.getCustomer() != null) {
            response.put("customerLoyaltyPointsAfter", savedOrder.getCustomer().getLoyaltyPoints());
            response.put("customerRank", savedOrder.getCustomer().getCustomerRank());
        }

        return response;
    }

    private int applyLoyaltyPointsIfNeeded(Order order) {
        if (order == null || order.getCustomer() == null) {
            if (order != null) {
                order.setLoyaltyPointsApplied(true);
                order.setLoyaltyPointsEarned(0);
                order.setCompletedAt(LocalDateTime.now());
            }
            return 0;
        }

        if (order.getStatus() == null || order.getStatus() != ORDER_STATUS_COMPLETED) {
            return 0;
        }

        if (Boolean.TRUE.equals(order.getLoyaltyPointsApplied())) {
            return order.getLoyaltyPointsEarned() != null ? order.getLoyaltyPointsEarned() : 0;
        }

        BigDecimal finalAmount = order.getFinalAmount() != null
                ? order.getFinalAmount()
                : BigDecimal.ZERO;

        int pointsEarned = finalAmount
                .divide(POINT_RATE_AMOUNT, 0, java.math.RoundingMode.DOWN)
                .intValue();

        Customer customer = order.getCustomer();

        if (pointsEarned > 0) {
            int currentPoints = customer.getLoyaltyPoints() != null
                    ? customer.getLoyaltyPoints()
                    : 0;

            customer.setLoyaltyPoints(currentPoints + pointsEarned);
            updateCustomerRank(customer);

            customerRepo.save(customer);
        }

        order.setLoyaltyPointsApplied(true);
        order.setLoyaltyPointsEarned(pointsEarned);
        order.setCompletedAt(LocalDateTime.now());

        return pointsEarned;
    }

    private void updateCustomerRank(Customer customer) {
        int points = customer.getLoyaltyPoints() != null
                ? customer.getLoyaltyPoints()
                : 0;

        if (points >= 5000) {
            customer.setCustomerRank("DIAMOND");
        } else if (points >= 2000) {
            customer.setCustomerRank("GOLD");
        } else if (points >= 500) {
            customer.setCustomerRank("SILVER");
        } else {
            customer.setCustomerRank("BRONZE");
        }
    }

    private Map<String, Object> buildCompleteOrderResponse(Order order, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("orderId", order.getId());
        response.put("status", order.getStatus());
        response.put("paymentMethod", order.getPaymentMethod());
        response.put("totalAmount", order.getTotalAmount());
        response.put("discountAmount", order.getDiscountAmount());
        response.put("finalAmount", order.getFinalAmount());
        response.put("loyaltyPointsApplied", order.getLoyaltyPointsApplied());
        response.put("loyaltyPointsEarned", order.getLoyaltyPointsEarned());
        response.put("completedAt", order.getCompletedAt());
        response.put("message", message);
        return response;
    }

    @Override
    public void createVoucher(VoucherRequest request) {
        // Tự sinh mã nếu để trống
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            request.setCode("SALE" + java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        }
        // ... (phần validate và save giữ nguyên)
    }
}