    package org.example.datn_sd69.modules.order.service;
    
    import lombok.RequiredArgsConstructor;
    import org.example.datn_sd69.entity.Cart;
    import org.example.datn_sd69.entity.CartItem;
    import org.example.datn_sd69.entity.Customer;
    import org.example.datn_sd69.entity.Order;
    import org.example.datn_sd69.entity.OrderItem;
    import org.example.datn_sd69.entity.ProductVariant;
    import org.example.datn_sd69.entity.PromotionVariant;
    import org.example.datn_sd69.entity.Voucher;
    import org.example.datn_sd69.modules.order.dto.request.OrderRequest;
    import org.example.datn_sd69.repository.CartItemRepository;
    import org.example.datn_sd69.repository.CartRepository;
    import org.example.datn_sd69.repository.CustomerRepository;
    import org.example.datn_sd69.repository.OrderItemRepository;
    import org.example.datn_sd69.repository.OrderRepository;
    import org.example.datn_sd69.repository.ProductVariantRepository;
    import org.example.datn_sd69.repository.PromotionVariantRepository;
    import org.example.datn_sd69.repository.VoucherRepository;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.http.HttpStatus;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;
    import org.springframework.web.server.ResponseStatusException;
    
    import javax.crypto.Mac;
    import javax.crypto.spec.SecretKeySpec;
    import java.math.BigDecimal;
    import java.math.RoundingMode;
    import java.net.URLEncoder;
    import java.nio.charset.StandardCharsets;
    import java.time.LocalDate;
    import java.time.LocalDateTime;
    import java.time.format.DateTimeFormatter;
    import java.util.*;
    
    @Service
    @RequiredArgsConstructor
    public class OrderService {
    
        private static final int ORDER_STATUS_PENDING = 0;
        private static final int ORDER_STATUS_CONFIRMED = 1;
        private static final int ORDER_STATUS_SHIPPING = 2;
        private static final int ORDER_STATUS_COMPLETED = 3;
        private static final int ORDER_STATUS_CANCELLED = 4;
        private static final int ORDER_STATUS_DELIVERY_FAILED = 5;
        private static final int ORDER_STATUS_RETURN_REQUESTED = 6;
        private static final int ORDER_STATUS_RETURN_COMPLETED = 7;
    
        private static final int PRODUCT_VARIANT_ACTIVE = 1;
    
        private static final BigDecimal POINT_RATE_AMOUNT = BigDecimal.valueOf(10_000);
    
        private static final String PAYMENT_METHOD_COD = "COD";
        private static final String PAYMENT_METHOD_VNPAY = "VNPAY";
    
        private final CartRepository cartRepo;
        private final OrderRepository orderRepo;
        private final OrderItemRepository orderItemRepo;
        private final ProductVariantRepository variantRepo;
        private final CartItemRepository cartItemRepository;
        private final CustomerRepository customerRepo;
        private final VoucherRepository voucherRepo;
        private final PromotionVariantRepository promotionVariantRepository;
    
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
    
            List<CartItem> cartItems = new ArrayList<>(cartItemRepository.findByCartId(cart.getId()));
    
            if (cartItems.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Giỏ hàng không có sản phẩm nào"
                );
            }
    
            String paymentMethod = normalizePaymentMethod(request.getPaymentMethod());
    
            BigDecimal totalAmount = BigDecimal.ZERO;
            Map<Integer, PriceSnapshot> priceSnapshotMap = new HashMap<>();
    
            for (CartItem item : cartItems) {
                validateCartItem(item);
    
                ProductVariant variant = item.getProductVariant();
    
                if (variant.getStockQuantity() < item.getQuantity()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Sản phẩm " + variant.getSku()
                                    + " chỉ còn " + variant.getStockQuantity()
                                    + " trong kho"
                    );
                }
    
                PriceSnapshot priceSnapshot = resolveCurrentPrice(variant);
                priceSnapshotMap.put(item.getId(), priceSnapshot);
    
                BigDecimal lineTotal = priceSnapshot.finalPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
    
                totalAmount = totalAmount.add(lineTotal);
            }
    
            VoucherApplyResult voucherApplyResult = resolveVoucherDiscount(
                    request.getVoucherCode(),
                    totalAmount
            );
    
            Voucher appliedVoucher = voucherApplyResult.voucher();
            BigDecimal voucherDiscountAmount = voucherApplyResult.discountAmount();
    
            BigDecimal finalAmount = totalAmount.subtract(voucherDiscountAmount);
    
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
            order.setDiscountAmount(voucherDiscountAmount);
            order.setFinalAmount(finalAmount);
            order.setPaymentMethod(paymentMethod);
            order.setStatus(ORDER_STATUS_PENDING);
            order.setCreatedAt(LocalDateTime.now());
            order.setLoyaltyPointsApplied(false);
            order.setLoyaltyPointsEarned(0);
            order.setCompletedAt(null);
    
            if (appliedVoucher != null) {
                order.setVoucher(appliedVoucher);
                reserveVoucherUsage(appliedVoucher);
            }
    
            Order savedOrder = orderRepo.save(order);
    
            for (CartItem item : cartItems) {
                ProductVariant variant = item.getProductVariant();
                PriceSnapshot priceSnapshot = priceSnapshotMap.get(item.getId());
    
                variant.setStockQuantity(variant.getStockQuantity() - item.getQuantity());
                variantRepo.save(variant);
    
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(savedOrder);
                orderItem.setProductVariant(variant);
                orderItem.setQuantity(item.getQuantity());
    
                orderItem.setOriginalPrice(priceSnapshot.originalPrice());
                orderItem.setDiscountAmount(priceSnapshot.discountAmount());
                orderItem.setFinalPrice(priceSnapshot.finalPrice());
    
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
            response.put("voucherCode", appliedVoucher != null ? appliedVoucher.getCode() : null);
    
            if (PAYMENT_METHOD_VNPAY.equalsIgnoreCase(savedOrder.getPaymentMethod())) {
                response.put("message", "Đơn hàng đã được tạo. Vui lòng hoàn tất thanh toán VNPay.");
                response.put("paymentUrl", buildVnPayPaymentUrl(savedOrder));
            } else {
                response.put("message", "Đặt hàng thành công. Đơn hàng đang chờ xác nhận.");
            }
    
            return response;
        }
    
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
    
            if (Integer.valueOf(ORDER_STATUS_CONFIRMED).equals(order.getStatus())
                    || Integer.valueOf(ORDER_STATUS_SHIPPING).equals(order.getStatus())
                    || Integer.valueOf(ORDER_STATUS_COMPLETED).equals(order.getStatus())) {
                return buildPaymentResponse(
                        true,
                        order,
                        "Đơn hàng đã được xác nhận thanh toán trước đó"
                );
            }
    
            if (Integer.valueOf(ORDER_STATUS_CANCELLED).equals(order.getStatus())) {
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
                /*
                 * Không có PaymentStatus riêng trong DB nên không được dùng Order.Status
                 * để biểu diễn "đã thanh toán".
                 *
                 * VNPay thành công:
                 * - Thanh toán: thành công, trả success = true cho FE
                 * - Đơn hàng: vẫn chờ cửa hàng xác nhận
                 *
                 * Admin sẽ xác nhận đơn thủ công:
                 * 0 -> 1 -> 2 -> 3
                 */
                order.setStatus(ORDER_STATUS_PENDING);
                orderRepo.save(order);

                return buildPaymentResponse(
                        true,
                        order,
                        "Thanh toán VNPay thành công. Đơn hàng đang chờ cửa hàng xác nhận."
                );
            }
    
            restoreStockForOrder(order);
            restoreVoucherUsage(order);
    
            order.setStatus(ORDER_STATUS_CANCELLED);
            orderRepo.save(order);
    
            return buildPaymentResponse(
                    false,
                    order,
                    "Thanh toán VNPay thất bại hoặc bị hủy. Đơn hàng đã được hủy và hoàn tồn kho."
            );
        }
    
        @Transactional
        public Map<String, Object> updateOrderStatus(Integer orderId, Integer newStatus) {
            validateId(orderId, "orderId");
            validateOrderStatus(newStatus);
    
            Order order = orderRepo.findById(orderId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Không tìm thấy đơn hàng"
                    ));
    
            Integer currentStatus = order.getStatus();
    
            if (Objects.equals(currentStatus, newStatus)) {
                return buildOrderStatusResponse(order, "Trạng thái đơn hàng không thay đổi");
            }
    
            validateStatusTransition(currentStatus, newStatus);
    
            if (newStatus == ORDER_STATUS_CANCELLED) {
                restoreStockForOrder(order);
                restoreVoucherUsage(order);
                order.setStatus(ORDER_STATUS_CANCELLED);
                orderRepo.save(order);
                return buildOrderStatusResponse(order, "Đã hủy đơn hàng và hoàn tồn kho");
            }
    
            if (newStatus == ORDER_STATUS_DELIVERY_FAILED) {
                restoreStockForOrder(order);
                restoreVoucherUsage(order);
                order.setStatus(ORDER_STATUS_DELIVERY_FAILED);
                orderRepo.save(order);
                return buildOrderStatusResponse(order, "Đã chuyển đơn sang giao hàng thất bại và hoàn tồn kho");
            }
    
            if (newStatus == ORDER_STATUS_COMPLETED) {
                order.setStatus(ORDER_STATUS_COMPLETED);
                int pointsEarned = applyLoyaltyPointsIfNeeded(order);
                orderRepo.save(order);
    
                Map<String, Object> response = buildOrderStatusResponse(order, "Đơn hàng đã hoàn thành");
                response.put("loyaltyPointsEarned", pointsEarned);
    
                if (order.getCustomer() != null) {
                    response.put("customerLoyaltyPointsAfter", order.getCustomer().getLoyaltyPoints());
                    response.put("customerRank", order.getCustomer().getCustomerRank());
                }
    
                return response;
            }
    
            order.setStatus(newStatus);
            orderRepo.save(order);
    
            return buildOrderStatusResponse(order, "Cập nhật trạng thái đơn hàng thành công");
        }
    
        @Transactional
        public Map<String, Object> completeOrderAndApplyLoyalty(Integer orderId) {
            return updateOrderStatus(orderId, ORDER_STATUS_COMPLETED);
        }
    
        private PriceSnapshot resolveCurrentPrice(ProductVariant variant) {
            BigDecimal originalPrice = moneyOrZero(variant.getPrice());
    
            PromotionVariant activePromotion = findActivePromotion(variant);
    
            if (activePromotion == null || activePromotion.getDiscountPercent() == null) {
                return new PriceSnapshot(
                        originalPrice,
                        BigDecimal.ZERO,
                        originalPrice
                );
            }
    
            BigDecimal discountPercent = BigDecimal.valueOf(
                    activePromotion.getDiscountPercent().doubleValue()
            );
    
            BigDecimal discountAmount = originalPrice
                    .multiply(discountPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    
            if (discountAmount.compareTo(originalPrice) > 0) {
                discountAmount = originalPrice;
            }
    
            BigDecimal finalPrice = originalPrice.subtract(discountAmount).max(BigDecimal.ZERO);
    
            return new PriceSnapshot(
                    originalPrice,
                    discountAmount,
                    finalPrice
            );
        }
    
        private PromotionVariant findActivePromotion(ProductVariant variant) {
            if (variant == null || variant.getId() == null) {
                return null;
            }
    
            List<PromotionVariant> activePromotions =
                    promotionVariantRepository.findActivePromotionByProductVariantId(
                            variant.getId(),
                            LocalDateTime.now(),
                            LocalDate.now()
                    );
    
            if (activePromotions == null || activePromotions.isEmpty()) {
                return null;
            }
    
            return activePromotions.get(0);
        }
    
        private VoucherApplyResult resolveVoucherDiscount(String rawVoucherCode, BigDecimal orderTotal) {
            String voucherCode = normalizeOptionalVoucherCode(rawVoucherCode);
    
            if (voucherCode == null) {
                return new VoucherApplyResult(null, BigDecimal.ZERO);
            }
    
            Voucher voucher = voucherRepo.findByCode(voucherCode)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Mã giảm giá không tồn tại"
                    ));
    
            validateVoucherCanApply(voucher, orderTotal);
    
            BigDecimal discountAmount;
    
            if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())
                    || "PERCENTAGE".equalsIgnoreCase(voucher.getDiscountType())) {
                BigDecimal percent = moneyOrZero(voucher.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
    
                discountAmount = orderTotal.multiply(percent);
            } else {
                discountAmount = moneyOrZero(voucher.getDiscountValue());
            }
    
            if (voucher.getMaxDiscount() != null
                    && voucher.getMaxDiscount().compareTo(BigDecimal.ZERO) > 0
                    && discountAmount.compareTo(voucher.getMaxDiscount()) > 0) {
                discountAmount = voucher.getMaxDiscount();
            }
    
            if (discountAmount.compareTo(orderTotal) > 0) {
                discountAmount = orderTotal;
            }
    
            if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
                discountAmount = BigDecimal.ZERO;
            }
    
            return new VoucherApplyResult(
                    voucher,
                    discountAmount.setScale(2, RoundingMode.HALF_UP)
            );
        }
    
        private void validateVoucherCanApply(Voucher voucher, BigDecimal orderTotal) {
            if (voucher == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Mã giảm giá không tồn tại"
                );
            }
    
            if (voucher.getStatus() == null || voucher.getStatus() != 1) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Mã giảm giá đã bị khóa hoặc ngừng hoạt động"
                );
            }
    
            Integer usedCount = voucher.getUsedCount() == null ? 0 : voucher.getUsedCount();
            Integer usageLimit = voucher.getUsageLimit();
    
            if (usageLimit != null && usageLimit > 0 && usedCount >= usageLimit) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Mã giảm giá đã hết lượt sử dụng"
                );
            }
    
            BigDecimal minOrderValue = moneyOrZero(voucher.getMinOrderValue());
    
            if (orderTotal.compareTo(minOrderValue) < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Đơn hàng cần tối thiểu "
                                + minOrderValue.stripTrailingZeros().toPlainString()
                                + " để áp dụng mã giảm giá này"
                );
            }
    
            if (voucher.getDiscountValue() == null
                    || voucher.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Giá trị mã giảm giá không hợp lệ"
                );
            }
        }
    
        private void reserveVoucherUsage(Voucher voucher) {
            if (voucher == null) {
                return;
            }
    
            Integer usedCount = voucher.getUsedCount() == null ? 0 : voucher.getUsedCount();
    
            voucher.setUsedCount(usedCount + 1);
            voucherRepo.save(voucher);
        }
    
        private void restoreVoucherUsage(Order order) {
            if (order == null || order.getVoucher() == null) {
                return;
            }
    
            Voucher voucher = order.getVoucher();
    
            Integer usedCount = voucher.getUsedCount() == null ? 0 : voucher.getUsedCount();
    
            if (usedCount > 0) {
                voucher.setUsedCount(usedCount - 1);
                voucherRepo.save(voucher);
            }
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
                return order.getLoyaltyPointsEarned() != null
                        ? order.getLoyaltyPointsEarned()
                        : 0;
            }
    
            BigDecimal finalAmount = order.getFinalAmount() != null
                    ? order.getFinalAmount()
                    : BigDecimal.ZERO;
    
            int pointsEarned = finalAmount
                    .divide(POINT_RATE_AMOUNT, 0, RoundingMode.DOWN)
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
            normalizeOptionalVoucherCode(request.getVoucherCode());
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
    
            String sku = variant.getSku() != null ? variant.getSku() : "N/A";
    
            if (Boolean.TRUE.equals(variant.getIsDeleted())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sản phẩm " + sku + " đã bị xóa, không thể mua"
                );
            }
    
            if (variant.getStatus() == null || variant.getStatus() != PRODUCT_VARIANT_ACTIVE) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sản phẩm " + sku + " hiện không còn kinh doanh"
                );
            }
    
            if (variant.getProduct() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sản phẩm " + sku + " không tồn tại"
                );
            }
    
            if (variant.getProduct().getStatus() == null
                    || variant.getProduct().getStatus() != PRODUCT_VARIANT_ACTIVE) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sản phẩm " + variant.getProduct().getName() + " hiện không còn kinh doanh"
                );
            }
    
            if (variant.getPrice() == null || variant.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Giá sản phẩm " + sku + " không hợp lệ"
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
                        "Tồn kho sản phẩm " + sku + " không hợp lệ"
                );
            }
    
            LocalDate today = LocalDate.now();
    
            if (variant.getManufacturingDate() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sản phẩm " + sku + " chưa có ngày sản xuất"
                );
            }
    
            if (variant.getExpirationDate() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sản phẩm " + sku + " chưa có hạn sử dụng"
                );
            }
    
            if (variant.getManufacturingDate().isAfter(today)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sản phẩm " + sku + " chưa tới ngày được bán"
                );
            }
    
            if (variant.getExpirationDate().isBefore(today)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sản phẩm " + sku + " đã hết hạn sử dụng"
                );
            }
    
            if (!variant.getExpirationDate().isAfter(variant.getManufacturingDate())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sản phẩm " + sku + " có hạn sử dụng không hợp lệ"
                );
            }
        }
    
        private void validateId(Integer id, String fieldName) {
            if (id == null || id <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        fieldName + " phải là số nguyên dương"
                );
            }
        }
    
        private void validateOrderStatus(Integer status) {
            if (status == null || status < 0 || status > 7) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Trạng thái đơn hàng không hợp lệ"
                );
            }
        }
    
        private void validateStatusTransition(Integer currentStatus, Integer newStatus) {
            if (currentStatus == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Trạng thái hiện tại của đơn hàng không hợp lệ"
                );
            }
    
            if (currentStatus == ORDER_STATUS_COMPLETED) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Đơn hàng đã hoàn thành, không thể đổi trạng thái"
                );
            }
    
            if (currentStatus == ORDER_STATUS_CANCELLED) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Đơn hàng đã hủy, không thể đổi trạng thái"
                );
            }
    
            if (currentStatus == ORDER_STATUS_DELIVERY_FAILED) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Đơn hàng giao thất bại, không thể đổi trạng thái"
                );
            }
    
            boolean valid =
                    (currentStatus == ORDER_STATUS_PENDING
                            && (newStatus == ORDER_STATUS_CONFIRMED
                            || newStatus == ORDER_STATUS_CANCELLED))
                            ||
                            (currentStatus == ORDER_STATUS_CONFIRMED
                                    && (newStatus == ORDER_STATUS_SHIPPING
                                    || newStatus == ORDER_STATUS_CANCELLED))
                            ||
                            (currentStatus == ORDER_STATUS_SHIPPING
                                    && (newStatus == ORDER_STATUS_COMPLETED
                                    || newStatus == ORDER_STATUS_DELIVERY_FAILED))
                            ||
                            (currentStatus == ORDER_STATUS_COMPLETED
                                    && newStatus == ORDER_STATUS_RETURN_REQUESTED)
                            ||
                            (currentStatus == ORDER_STATUS_RETURN_REQUESTED
                                    && newStatus == ORDER_STATUS_RETURN_COMPLETED);
    
            if (!valid) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Không thể chuyển trạng thái từ "
                                + getStatusText(currentStatus)
                                + " sang "
                                + getStatusText(newStatus)
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
    
        private String normalizeOptionalVoucherCode(String voucherCode) {
            if (voucherCode == null) {
                return null;
            }
    
            String trimmed = voucherCode.trim();
    
            if (trimmed.isEmpty()) {
                return null;
            }
    
            if (!trimmed.equals(voucherCode)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Mã giảm giá không được có khoảng trắng ở đầu hoặc cuối"
                );
            }
    
            if (trimmed.length() > 50) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Mã giảm giá tối đa 50 ký tự"
                );
            }
    
            if (!trimmed.matches("^[A-Za-z0-9_-]+$")) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Mã giảm giá chỉ được chứa chữ, số, dấu gạch ngang hoặc gạch dưới"
                );
            }
    
            return trimmed.toUpperCase();
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

        private Map<String, Object> buildPaymentResponse(
                boolean success,
                Order order,
                String message
        ) {
            Map<String, Object> response = new LinkedHashMap<>();

            response.put("success", success);
            response.put("orderId", order.getId());

            response.put("orderStatus", order.getStatus());
            response.put("orderStatusText", getStatusText(order.getStatus()));

            /*
             * Không có PaymentStatus trong DB nên paymentSuccess chỉ là dữ liệu trả về
             * cho màn hình payment-return, không lưu lâu dài.
             */
            response.put("paymentSuccess", success);
            response.put(
                    "paymentStatusText",
                    success
                            ? "Đã thanh toán"
                            : "Thanh toán thất bại hoặc chưa hoàn tất"
            );

            response.put("paymentMethod", order.getPaymentMethod());
            response.put("totalAmount", order.getTotalAmount());
            response.put("discountAmount", order.getDiscountAmount());
            response.put("finalAmount", order.getFinalAmount());
            response.put("message", message);

            return response;
        }
    
        private Map<String, Object> buildOrderStatusResponse(Order order, String message) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("orderId", order.getId());
            response.put("status", order.getStatus());
            response.put("statusText", getStatusText(order.getStatus()));
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
    
        private String getStatusText(Integer status) {
            if (status == null) {
                return "Không xác định";
            }
    
            return switch (status) {
                case ORDER_STATUS_PENDING -> "Chờ xác nhận";
                case ORDER_STATUS_CONFIRMED -> "Đã xác nhận";
                case ORDER_STATUS_SHIPPING -> "Đang giao hàng";
                case ORDER_STATUS_COMPLETED -> "Hoàn thành";
                case ORDER_STATUS_CANCELLED -> "Đã hủy";
                case ORDER_STATUS_DELIVERY_FAILED -> "Giao hàng thất bại";
                case ORDER_STATUS_RETURN_REQUESTED -> "Yêu cầu hoàn hàng / đổi trả";
                case ORDER_STATUS_RETURN_COMPLETED -> "Hoàn hàng / đổi trả hoàn tất";
                default -> "Không xác định";
            };
        }
    
        private BigDecimal moneyOrZero(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }
    
        private record PriceSnapshot(
                BigDecimal originalPrice,
                BigDecimal discountAmount,
                BigDecimal finalPrice
        ) {
        }
    
        private record VoucherApplyResult(
                Voucher voucher,
                BigDecimal discountAmount
        ) {
        }
    }