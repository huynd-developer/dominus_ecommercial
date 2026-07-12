package org.example.datn_sd69.modules.order.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Cart;
import org.example.datn_sd69.entity.CartItem;
import org.example.datn_sd69.entity.Customer;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.entity.Voucher; // IMPORT THÊM VOUCHER
import org.example.datn_sd69.modules.order.dto.request.OrderRequest;
import org.example.datn_sd69.repository.CartItemRepository;
import org.example.datn_sd69.repository.CartRepository;
import org.example.datn_sd69.repository.CustomerRepository;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.example.datn_sd69.repository.OrderRepository;
import org.example.datn_sd69.repository.ProductVariantRepository;
import org.example.datn_sd69.repository.VoucherRepository; // IMPORT THÊM VOUCHER REPO
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
    private static final int ORDER_STATUS_CONFIRMED = 1;
    private static final int ORDER_STATUS_CANCELLED = 4;
    private static final int PRODUCT_VARIANT_ACTIVE = 1;
    private static final int ORDER_STATUS_COMPLETED = 3;
    private static final BigDecimal POINT_RATE_AMOUNT = BigDecimal.valueOf(10_000);
    private static final String PAYMENT_METHOD_COD = "COD";
    private static final String PAYMENT_METHOD_VNPAY = "VNPAY";

    private final CartRepository cartRepo;
    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final ProductVariantRepository variantRepo;
    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepo;
    private final VoucherRepository voucherRepo; // INJECT THÊM THẰNG NÀY

    @Value("${vnpay.tmnCode}")
    private String vnpTmnCode;

    @Value("${vnpay.hashSecret}")
    private String vnpHashSecret;

    @Value("${vnpay.url}")
    private String vnpUrl;

    @Value("${vnpay.onlineReturnUrl}")
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

        String paymentMethod = normalizePaymentMethod(request.getPaymentMethod());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<CartItem> cartItems = new ArrayList<>(cart.getCartItems());

        // 1. TÍNH TỔNG TIỀN GỐC TRƯỚC
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

        // 2. XỬ LÝ VOUCHER (NẾU CÓ)
        BigDecimal discountAmount = BigDecimal.ZERO;
        Voucher appliedVoucher = null;

        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            appliedVoucher = voucherRepo.findByCode(request.getVoucherCode().trim())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá không tồn tại!"));

            // Validate lại lần cuối xem mã còn hợp lệ không
            if (appliedVoucher.getStatus() != 1
                    || appliedVoucher.getUsedCount() >= appliedVoucher.getUsageLimit()
                    || totalAmount.compareTo(appliedVoucher.getMinOrderValue()) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã giảm giá không đủ điều kiện áp dụng!");
            }

            // Tính tiền giảm
            if ("PERCENT".equalsIgnoreCase(appliedVoucher.getDiscountType())) {
                BigDecimal percent = appliedVoucher.getDiscountValue().divide(BigDecimal.valueOf(100));
                discountAmount = totalAmount.multiply(percent);
            } else {
                discountAmount = appliedVoucher.getDiscountValue();
            }

            // Ép giới hạn giảm tối đa
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

        // 3. TẠO ĐƠN HÀNG
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

        // NẾU CÓ DÙNG VOUCHER THÌ LƯU VÀO ĐƠN HÀNG VÀ CẬP NHẬT LƯỢT DÙNG
        if (appliedVoucher != null) {
            order.setVoucher(appliedVoucher); // Cần chắc chắn Entity Order đã có field private Voucher voucher;

            // Cập nhật lượt dùng
            appliedVoucher.setUsedCount(appliedVoucher.getUsedCount() + 1);
            if (appliedVoucher.getUsedCount() >= appliedVoucher.getUsageLimit()) {
                appliedVoucher.setStatus(0); // Tự động khóa nếu hết lượt
            }
            voucherRepo.save(appliedVoucher);
        }

        Order savedOrder = orderRepo.save(order);

        // 4. LƯU CHI TIẾT ĐƠN VÀ TRỪ TỒN KHO
        for (CartItem item : cartItems) {
            ProductVariant variant = item.getProductVariant();

            variant.setStockQuantity(variant.getStockQuantity() - item.getQuantity());
            variantRepo.save(variant);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProductVariant(variant);
            orderItem.setQuantity(item.getQuantity());
            orderItem.setOriginalPrice(variant.getPrice());
            orderItem.setDiscountAmount(BigDecimal.ZERO); // Tạm set 0 ở mức Item

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

        if (PAYMENT_METHOD_VNPAY.equalsIgnoreCase(savedOrder.getPaymentMethod())) {
            response.put("message", "Đơn hàng đã được tạo. Vui lòng hoàn tất thanh toán VNPay.");
            response.put("paymentUrl", buildVnPayPaymentUrl(savedOrder));
        } else {
            response.put("message", "Đặt hàng thành công. Đơn hàng đang chờ xác nhận.");
        }

        return response;
    }

    // =========================================================================
    // CÁC HÀM BÊN DƯỚI GIỮ NGUYÊN HOÀN TOÀN NHƯ CŨ CỦA M
    // =========================================================================

    @Transactional
    public Map<String, Object> verifyVnPayReturn(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Dữ liệu VNPay trả về không hợp lệ"
            );
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        String vnpAmount = params.get("vnp_Amount");

        if (txnRef == null || txnRef.trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thiếu mã giao dịch VNPay"
            );
        }

        if (!isValidVnPaySignature(params)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chữ ký VNPay không hợp lệ"
            );
        }

        Integer orderId = extractOrderIdFromTxnRef(txnRef);

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng"
                ));

        if (!PAYMENT_METHOD_VNPAY.equalsIgnoreCase(order.getPaymentMethod())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đơn hàng này không sử dụng phương thức VNPay"
            );
        }

        if (!isValidVnPayAmount(order, vnpAmount)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số tiền VNPay trả về không khớp với đơn hàng"
            );
        }

        if (order.getStatus() != null && order.getStatus() == ORDER_STATUS_CONFIRMED) {
            return buildPaymentResponse(
                    true,
                    order,
                    "Đơn hàng đã được xác nhận thanh toán trước đó"
            );
        }

        if (order.getStatus() != null && order.getStatus() == ORDER_STATUS_CANCELLED) {
            return buildPaymentResponse(
                    false,
                    order,
                    "Đơn hàng đã bị hủy trước đó"
            );
        }

        boolean paymentSuccess =
                "00".equals(responseCode)
                        && ("00".equals(transactionStatus)
                        || transactionStatus == null
                        || transactionStatus.isBlank());

        if (paymentSuccess) {
            return buildPaymentResponse(
                    true,
                    order,
                    "Thanh toán VNPay thành công. Đơn hàng đang chờ cửa hàng xác nhận."
            );
        }

        restoreStockForOrder(order);

        order.setStatus(ORDER_STATUS_CANCELLED);
        orderRepo.save(order);

        return buildPaymentResponse(
                false,
                order,
                "Thanh toán VNPay thất bại hoặc bị hủy"
        );
    }

    private void restoreStockForOrder(Order order) {
        List<OrderItem> orderItems = orderItemRepo.findByOrderId(order.getId());

        for (OrderItem item : orderItems) {
            if (item == null || item.getProductVariant() == null || item.getQuantity() == null) {
                continue;
            }

            ProductVariant variant = item.getProductVariant();

            int currentStock = variant.getStockQuantity() != null
                    ? variant.getStockQuantity()
                    : 0;

            variant.setStockQuantity(currentStock + item.getQuantity());
            variantRepo.save(variant);
        }
    }

    private Map<String, Object> buildPaymentResponse(
            boolean success,
            Order order,
            String message
    ) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", success);
        response.put("orderId", order.getId());
        response.put("orderStatus", order.getStatus());
        response.put("paymentMethod", order.getPaymentMethod());
        response.put("totalAmount", order.getTotalAmount());
        response.put("discountAmount", order.getDiscountAmount());
        response.put("finalAmount", order.getFinalAmount());
        response.put("message", message);
        return response;
    }

    private Integer extractOrderIdFromTxnRef(String txnRef) {
        try {
            String rawOrderId = txnRef.contains("_")
                    ? txnRef.substring(0, txnRef.indexOf("_"))
                    : txnRef;

            return Integer.parseInt(rawOrderId);
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã đơn hàng trong giao dịch VNPay không hợp lệ"
            );
        }
    }

    private boolean isValidVnPayAmount(Order order, String vnpAmount) {
        try {
            if (order == null || order.getFinalAmount() == null) {
                return false;
            }

            if (vnpAmount == null || vnpAmount.trim().isEmpty()) {
                return false;
            }

            BigDecimal expectedAmount = order.getFinalAmount()
                    .multiply(BigDecimal.valueOf(100));

            BigDecimal actualAmount = new BigDecimal(vnpAmount);

            return expectedAmount.compareTo(actualAmount) == 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isValidVnPaySignature(Map<String, String> params) {
        try {
            String receivedHash = params.get("vnp_SecureHash");

            if (receivedHash == null || receivedHash.trim().isEmpty()) {
                return false;
            }

            Map<String, String> fields = new HashMap<>(params);
            fields.remove("vnp_SecureHash");
            fields.remove("vnp_SecureHashType");

            List<String> fieldNames = new ArrayList<>(fields.keySet());
            Collections.sort(fieldNames);

            StringBuilder hashData = new StringBuilder();

            for (String fieldName : fieldNames) {
                String fieldValue = fields.get(fieldName);

                if (fieldValue == null || fieldValue.isEmpty()) {
                    continue;
                }

                if (hashData.length() > 0) {
                    hashData.append("&");
                }

                hashData.append(fieldName)
                        .append("=")
                        .append(urlEncode(fieldValue));
            }

            String calculatedHash = hmacSHA512(vnpHashSecret, hashData.toString());

            return calculatedHash.equalsIgnoreCase(receivedHash);
        } catch (Exception ex) {
            return false;
        }
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

        if (!value.equals(PAYMENT_METHOD_COD) && !value.equals(PAYMENT_METHOD_VNPAY)) {
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

            for (String fieldName : fieldNames) {
                String fieldValue = vnpParams.get(fieldName);

                if (fieldValue == null || fieldValue.isEmpty()) {
                    continue;
                }

                if (hashData.length() > 0) {
                    hashData.append("&");
                }

                if (query.length() > 0) {
                    query.append("&");
                }

                hashData.append(fieldName)
                        .append("=")
                        .append(urlEncode(fieldValue));

                query.append(urlEncode(fieldName))
                        .append("=")
                        .append(urlEncode(fieldValue));
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

    private String urlEncode(String value) throws Exception {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII.toString());
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
}