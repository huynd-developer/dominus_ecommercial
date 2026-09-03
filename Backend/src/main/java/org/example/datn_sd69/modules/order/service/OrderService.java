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
import org.example.datn_sd69.modules.promotion.service.FlashSalePriceService;
import org.example.datn_sd69.repository.CartItemRepository;
import org.example.datn_sd69.repository.CartRepository;
import org.example.datn_sd69.repository.CustomerRepository;
import org.example.datn_sd69.repository.InventoryLotRepository;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.example.datn_sd69.repository.OrderRepository;
import org.example.datn_sd69.repository.ProductVariantRepository;
import org.example.datn_sd69.repository.VoucherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int ORDER_STATUS_PENDING = 0;
    private static final int ORDER_STATUS_CONFIRMED = 1;
    private static final int ORDER_STATUS_CANCELLED = 4;

    private static final String PAYMENT_METHOD_COD = "COD";
    private static final String PAYMENT_METHOD_VIETQR = "VIETQR";
    private static final String PAYMENT_METHOD_VNPAY = "VNPAY";

    private final CartRepository cartRepo;
    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final ProductVariantRepository variantRepo;
    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepo;
    private final InventoryLotRepository inventoryLotRepository;
    private final VoucherRepository voucherRepo;
    private final FlashSalePriceService flashSalePriceService;
    private final OrderMailService orderMailService;
    private final jakarta.persistence.EntityManager entityManager;

    @org.springframework.beans.factory.annotation.Value("${vnpay.tmnCode}")
    private String vnp_TmnCode;

    @org.springframework.beans.factory.annotation.Value("${vnpay.hashSecret}")
    private String secretKey;

    @org.springframework.beans.factory.annotation.Value("${vnpay.url}")
    private String vnp_PayUrl;

    @org.springframework.beans.factory.annotation.Value("${vnpay.onlineReturnUrl}")
    private String vnp_ReturnUrl;

    @Transactional
    public Map<String, Object> placeOrder(Integer customerId, OrderRequest request) {
        validateCheckoutRequest(customerId, request);

        Customer customer = customerRepo.findById(customerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));

        Cart cart = cartRepo.findByCustomerUserId(customerId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giỏ hàng trống"));

        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giỏ hàng không có sản phẩm nào");
        }

        String paymentMethod = normalizePaymentMethod(request.getPaymentMethod());
        BigDecimal totalAmount = BigDecimal.ZERO;

        List<CartItem> cartItems = new ArrayList<>(cart.getCartItems());
        Map<Integer, CheckoutItemPrice> checkoutPriceMap = new LinkedHashMap<>();

        BigDecimal discountAmount = BigDecimal.ZERO;
        Voucher appliedVoucher = null;
        LocalDateTime now = LocalDateTime.now();

        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            appliedVoucher = lockValidVoucherForCheckout(request.getVoucherCode(), now);
        }

        for (CartItem item : cartItems) {
            validateCartItem(item);
            ProductVariant variant = item.getProductVariant();

            if (!Integer.valueOf(1).equals(variant.getStatus())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm '" + getSnapshotProductName(variant) + "' (Loại: " + formatVariantName(variant) + ") đang ngừng kinh doanh. Vui lòng quay lại giỏ hàng và xóa sản phẩm này!");
            }

            int sellableQuantity = getSellableQuantity(variant);

            if (sellableQuantity < item.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm '" + getSnapshotProductName(variant) + "' (Loại: " + formatVariantName(variant) + ") không đủ số lượng. Kho chỉ còn " + sellableQuantity + " sản phẩm có thể bán!");
            }

            CheckoutItemPrice itemPrice = calculateCheckoutItemPrice(variant);
            checkoutPriceMap.put(item.getId(), itemPrice);

            BigDecimal lineTotal = itemPrice.finalUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
        }

        if (appliedVoucher != null) {
            BigDecimal minOrderValue = appliedVoucher.getMinOrderValue() != null ? appliedVoucher.getMinOrderValue() : BigDecimal.ZERO;

            if (totalAmount.compareTo(minOrderValue) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng chưa đạt giá trị tối thiểu " + minOrderValue + "đ để dùng mã giảm giá này.");
            }

            String discountType = appliedVoucher.getDiscountType() != null ? appliedVoucher.getDiscountType().toUpperCase() : "";
            BigDecimal discountValue = appliedVoucher.getDiscountValue() != null ? appliedVoucher.getDiscountValue() : BigDecimal.ZERO;

            if (discountType.contains("PERCENT") || discountType.contains("PERCENTAGE")) {
                discountAmount = totalAmount.multiply(discountValue).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                BigDecimal maxDiscount = appliedVoucher.getMaxDiscount() != null ? appliedVoucher.getMaxDiscount() : BigDecimal.ZERO;
                if (maxDiscount.compareTo(BigDecimal.ZERO) > 0 && discountAmount.compareTo(maxDiscount) > 0) {
                    discountAmount = maxDiscount;
                }
            } else {
                discountAmount = discountValue;
            }

            if (discountAmount.compareTo(totalAmount) > 0) {
                discountAmount = totalAmount;
            }
        }

        discountAmount = normalizeMoney(discountAmount);

        BigDecimal shippingFee = BigDecimal.valueOf(30000);

        BigDecimal finalAmount = totalAmount.subtract(discountAmount).add(shippingFee);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }
        finalAmount = normalizeMoney(finalAmount);

        validateCheckoutSnapshot(request, totalAmount, discountAmount, shippingFee, finalAmount);

        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderType("ONLINE");
        order.setCustomerName(normalizeText(request.getCustomerName(), "Tên người nhận"));
        order.setCustomerPhone(normalizeNoWhitespace(request.getCustomerPhone(), "Số điện thoại"));

        String finalShippingAddress = normalizeText(request.getShippingAddress(), "Địa chỉ giao hàng");
        if (Boolean.TRUE.equals(request.getIsVatRequired())) {
            String vatInfo = String.format(" | [YÊU CẦU XUẤT VAT] MST: %s - Email: %s - Cty: %s - ĐC: %s", request.getTaxCode().trim(), request.getVatEmail().trim(), request.getCompanyName().trim(), request.getCompanyAddress().trim());
            if ((finalShippingAddress + vatInfo).length() > 500) {
                finalShippingAddress = (finalShippingAddress + vatInfo).substring(0, 500);
            } else {
                finalShippingAddress += vatInfo;
            }
        }

        order.setShippingAddress(finalShippingAddress);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setShippingFee(shippingFee);
        order.setFinalAmount(finalAmount);
        order.setPaymentMethod(paymentMethod);
        order.setStatus(ORDER_STATUS_PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setLoyaltyPointsApplied(false);
        order.setLoyaltyPointsEarned(0);
        order.setIsPaymentReported(false);

        if (appliedVoucher != null) {
            order.setVoucher(appliedVoucher);
            reserveVoucherUsage(appliedVoucher);
        }

        Order savedOrder = orderRepo.save(order);

        for (CartItem item : cartItems) {
            ProductVariant variant = item.getProductVariant();

            String itemImage = item.getThumbnailUrl();
            if ((itemImage == null || itemImage.trim().isEmpty()) && variant.getProduct() != null) {
                try {
                    itemImage = entityManager.createQuery("SELECT img.imageUrl FROM ProductImage img WHERE img.product.id = :productId", String.class).setParameter("productId", variant.getProduct().getId()).setMaxResults(1).getResultStream().findFirst().orElse(null);
                } catch (Exception e) {
                    System.out.println("=== LỖI QUERY ẢNH ĐẶT HÀNG: " + e.getMessage());
                }
            }

            CheckoutItemPrice itemPrice = checkoutPriceMap.get(item.getId());
            if (itemPrice == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tìm thấy dữ liệu giá của sản phẩm trong giỏ hàng");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProductVariant(variant);
            orderItem.setQuantity(item.getQuantity());

            orderItem.setOriginalPrice(itemPrice.originalUnitPrice());
            orderItem.setDiscountAmount(itemPrice.unitDiscountAmount());
            orderItem.setFinalPrice(itemPrice.finalUnitPrice());

            orderItem.setNote(normalizeOptionalNote(request.getNote()));
            orderItem.setImage(itemImage);
            orderItem.setProductName(getSnapshotProductName(variant));
            orderItem.setSku(variant.getSku());
            orderItem.setCapacityName(getSnapshotCapacityName(variant));
            orderItem.setBottleTypeName(getSnapshotBottleTypeName(variant));

            orderItemRepo.save(orderItem);
        }

        cartItemRepository.deleteAll(cartItems);
        cart.getCartItems().clear();
        cartRepo.save(cart);

// Chỉ COD gửi mail ngay khi đặt hàng.
// VNPay và VietQR sẽ gửi mail sau khi thanh toán/báo thanh toán thành công.
        if (PAYMENT_METHOD_COD.equals(savedOrder.getPaymentMethod())) {
            orderMailService.sendOrderPlacedAsync(savedOrder);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("orderId", savedOrder.getId());
        response.put("status", savedOrder.getStatus());
        response.put("paymentMethod", savedOrder.getPaymentMethod());
        response.put("totalAmount", savedOrder.getTotalAmount());
        response.put("discountAmount", savedOrder.getDiscountAmount());
        response.put("shippingFee", savedOrder.getShippingFee());
        response.put("finalAmount", savedOrder.getFinalAmount());

        if (PAYMENT_METHOD_VNPAY.equals(savedOrder.getPaymentMethod())) {
            try {
                long amount = savedOrder.getFinalAmount().longValue() * 100;
                Map<String, String> vnp_Params = new java.util.HashMap<>();
                vnp_Params.put("vnp_Version", "2.1.0");
                vnp_Params.put("vnp_Command", "pay");
                vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
                vnp_Params.put("vnp_Amount", String.valueOf(amount));
                vnp_Params.put("vnp_CurrCode", "VND");
                vnp_Params.put("vnp_TxnRef", savedOrder.getId() + "_" + System.currentTimeMillis());
                vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + savedOrder.getId());
                vnp_Params.put("vnp_OrderType", "other");
                vnp_Params.put("vnp_Locale", "vn");
                vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);

                jakarta.servlet.http.HttpServletRequest httpRequest = ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()).getRequest();

                String ipAddr = httpRequest.getHeader("X-FORWARDED-FOR");
                if (ipAddr == null || ipAddr.isEmpty()) {
                    ipAddr = httpRequest.getRemoteAddr();
                }
                if (ipAddr != null && ipAddr.equals("0:0:0:0:0:0:0:1")) {
                    ipAddr = "127.0.0.1";
                }

                vnp_Params.put("vnp_IpAddr", ipAddr);

                java.util.Calendar cld = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Etc/GMT+7"));
                java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyyMMddHHmmss");

                vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));
                cld.add(java.util.Calendar.MINUTE, 15);
                vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

                java.util.List<String> fieldNames = new java.util.ArrayList<>(vnp_Params.keySet());
                java.util.Collections.sort(fieldNames);

                StringBuilder hashData = new StringBuilder();
                StringBuilder query = new StringBuilder();

                java.util.Iterator<String> itr = fieldNames.iterator();
                while (itr.hasNext()) {
                    String fieldName = itr.next();
                    String fieldValue = vnp_Params.get(fieldName);

                    if ((fieldValue != null) && (fieldValue.length() > 0)) {
                        hashData.append(fieldName).append('=').append(java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.US_ASCII));

                        query.append(java.net.URLEncoder.encode(fieldName, java.nio.charset.StandardCharsets.US_ASCII)).append('=').append(java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.US_ASCII));

                        if (itr.hasNext()) {
                            query.append('&');
                            hashData.append('&');
                        }
                    }
                }

                String queryUrl = query.toString();
                String vnp_SecureHash = org.example.datn_sd69.common.config.VNPayConfig.hmacSHA512(secretKey, hashData.toString());

                queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

                response.put("paymentUrl", vnp_PayUrl + "?" + queryUrl);
                response.put("message", "Chuyển hướng đến cổng thanh toán VNPay...");
            } catch (Exception e) {
                e.printStackTrace();
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi tạo link thanh toán VNPay");
            }
        } else if (PAYMENT_METHOD_VIETQR.equals(savedOrder.getPaymentMethod())) {
            response.put("message", "Đơn hàng đã được tạo. Vui lòng quét mã QR để hoàn tất thanh toán.");
        } else {
            response.put("message", "Đặt hàng thành công. Đơn hàng đang chờ xác nhận.");
        }

        return response;
    }

    @Transactional
    public Map<String, Object> verifyVnPayReturn(Map<String, String> params) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            String vnp_SecureHash = params.get("vnp_SecureHash");
            params.remove("vnp_SecureHash");
            params.remove("vnp_SecureHashType");

            java.util.List<String> fieldNames = new java.util.ArrayList<>(params.keySet());
            java.util.Collections.sort(fieldNames);

            StringBuilder hashData = new StringBuilder();
            java.util.Iterator<String> itr = fieldNames.iterator();

            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = params.get(fieldName);

                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName).append('=').append(java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.US_ASCII));

                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }

            String signValue = org.example.datn_sd69.common.config.VNPayConfig.hmacSHA512(secretKey, hashData.toString());

            if (signValue.equals(vnp_SecureHash)) {
                String responseCode = params.get("vnp_ResponseCode");
                String txnRef = params.get("vnp_TxnRef");
                Integer orderId = Integer.parseInt(txnRef.split("_")[0]);

                Order order = orderRepo.findDetailByIdForUpdate(orderId).orElse(null);

                if (order != null) {
                    if ("00".equals(responseCode)) {
                        boolean canReportPayment = Integer.valueOf(ORDER_STATUS_PENDING).equals(order.getStatus());
                        boolean wasPaymentReported = Boolean.TRUE.equals(order.getIsPaymentReported());

                        if (canReportPayment) {
                            order.setStatus(ORDER_STATUS_PENDING);
                            order.setIsPaymentReported(true);
                            Order savedOrder = orderRepo.save(order);

                            if (!wasPaymentReported) {
                                orderMailService.sendPaymentSuccessAsync(savedOrder);
                            }

                            response.put("success", true);
                            response.put("message", "Thanh toán VNPay thành công. Đơn hàng đang chờ xác nhận.");
                        } else {
                            response.put("success", false);
                            response.put("message", "Đơn hàng đã được xử lý trước đó.");
                        }
                    } else if ("24".equals(responseCode)) {
                        boolean canCancel = Integer.valueOf(ORDER_STATUS_PENDING).equals(order.getStatus()) && !Boolean.TRUE.equals(order.getIsPaymentReported());

                        if (canCancel) {
                            order.setStatus(ORDER_STATUS_CANCELLED);
                            restoreVoucherUsage(order);
                            orderRepo.save(order);
                        }

                        response.put("success", false);
                        response.put("message", "Khách hàng đã hủy giao dịch");
                    } else {
                        boolean canCancel = Integer.valueOf(ORDER_STATUS_PENDING).equals(order.getStatus()) && !Boolean.TRUE.equals(order.getIsPaymentReported());

                        if (canCancel) {
                            order.setStatus(ORDER_STATUS_CANCELLED);
                            restoreVoucherUsage(order);
                            orderRepo.save(order);
                        }

                        response.put("success", false);
                        response.put("message", "Giao dịch không thành công (Mã lỗi: " + responseCode + ")");
                    }
                } else {
                    response.put("success", false);
                    response.put("message", "Không tìm thấy đơn hàng");
                }
            } else {
                response.put("success", false);
                response.put("message", "Chữ ký không hợp lệ");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi xác thực VNPay");
        }
        return response;
    }

    private Voucher lockValidVoucherForCheckout(String rawCode, LocalDateTime now) {
        String cleanCode = rawCode == null ? "" : rawCode.trim();

        Voucher candidate = voucherRepo.findByCodeIgnoreCase(cleanCode).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rất tiếc! Mã giảm giá không tồn tại, đã hết hạn, hoặc hết lượt sử dụng."));

        Voucher lockedVoucher = voucherRepo.findByIdForUpdate(candidate.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rất tiếc! Mã giảm giá không tồn tại, đã hết hạn, hoặc hết lượt sử dụng."));

        int usedCount = lockedVoucher.getUsedCount() != null ? lockedVoucher.getUsedCount() : 0;

        Integer usageLimit = lockedVoucher.getUsageLimit();

        boolean invalid = lockedVoucher.getCode() == null || !lockedVoucher.getCode().equalsIgnoreCase(cleanCode) || Boolean.TRUE.equals(lockedVoucher.getIsDeleted()) || !Integer.valueOf(1).equals(lockedVoucher.getStatus()) || (usageLimit != null && usedCount >= usageLimit);

        if (invalid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rất tiếc! Mã giảm giá không tồn tại, đã hết hạn, hoặc hết lượt sử dụng.");
        }

        return lockedVoucher;
    }

    private void reserveVoucherUsage(Voucher voucher) {
        if (voucher == null || voucher.getId() == null) {
            return;
        }

        int usedCount = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;
        voucher.setUsedCount(usedCount + 1);
        voucherRepo.save(voucher);
    }

    private void restoreVoucherUsage(Order order) {
        if (order == null || order.getVoucher() == null || order.getVoucher().getId() == null) {
            return;
        }

        Voucher lockedVoucher = voucherRepo.findByIdForUpdate(order.getVoucher().getId()).orElse(null);

        if (lockedVoucher == null) {
            return;
        }

        int usedCount = lockedVoucher.getUsedCount() != null ? lockedVoucher.getUsedCount() : 0;

        if (usedCount > 0) {
            lockedVoucher.setUsedCount(usedCount - 1);
            voucherRepo.save(lockedVoucher);
        }
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private void validateCheckoutRequest(Integer customerId, OrderRequest request) {
        if (customerId == null || request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thông tin yêu cầu đặt hàng không hợp lệ");
        }
    }

    private String normalizePaymentMethod(String method) {
        if (method == null || method.trim().isEmpty()) {
            return PAYMENT_METHOD_COD;
        }
        return method.trim().toUpperCase();
    }

    private void validateCartItem(CartItem item) {
        if (item == null || item.getProductVariant() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sản phẩm trong giỏ hàng không hợp lệ");
        }
    }

    private String getSnapshotProductName(ProductVariant variant) {
        if (variant != null && variant.getProduct() != null && variant.getProduct().getName() != null) {
            return variant.getProduct().getName();
        }
        return "Sản phẩm";
    }

    private String formatVariantName(ProductVariant variant) {
        if (variant == null) return "";
        String capacity = getSnapshotCapacityName(variant);
        String bottleType = getSnapshotBottleTypeName(variant);
        if (!capacity.isEmpty() && !bottleType.isEmpty()) {
            return capacity + " - " + bottleType;
        }
        return !capacity.isEmpty() ? capacity : bottleType;
    }

    // Tính tồn kho trực tiếp qua EntityManager
    private int getSellableQuantity(ProductVariant variant) {
        if (variant == null || variant.getId() == null) return 0;
        try {
            Long stock = entityManager.createQuery("SELECT COALESCE(SUM(i.quantityOnHand), 0) FROM InventoryLot i WHERE i.productVariant.id = :variantId", Long.class).setParameter("variantId", variant.getId()).getSingleResult();
            return stock != null ? stock.intValue() : 0;
        } catch (Exception e) {
            // Đã sửa: Sửa getQuantity() thành getStockQuantity()
            return variant.getStockQuantity() != null ? variant.getStockQuantity() : 0;
        }
    }

    private record CheckoutItemPrice(BigDecimal originalUnitPrice, BigDecimal unitDiscountAmount,
                                     BigDecimal finalUnitPrice) {
    }

    private CheckoutItemPrice calculateCheckoutItemPrice(ProductVariant variant) {
        BigDecimal originalPrice = variant.getPrice() != null ? variant.getPrice() : BigDecimal.ZERO;
        BigDecimal flashSalePrice = flashSalePriceService.getEffectiveFlashSalePrice(variant.getId());
        BigDecimal finalPrice = flashSalePrice != null ? flashSalePrice : originalPrice;
        BigDecimal discountAmount = originalPrice.subtract(finalPrice);
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            discountAmount = BigDecimal.ZERO;
            finalPrice = originalPrice;
        }
        return new CheckoutItemPrice(originalPrice, discountAmount, finalPrice);
    }

    private BigDecimal normalizeMoney(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private void validateCheckoutSnapshot(OrderRequest request, BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal shippingFee, BigDecimal finalAmount) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu checkout không hợp lệ");
        }
    }

    private String normalizeText(String text, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " không được để trống");
        }
        return text.trim();
    }

    private String normalizeNoWhitespace(String text, String fieldName) {
        String normalized = normalizeText(text, fieldName);
        if (normalized.contains(" ")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " không được chứa khoảng trắng");
        }
        return normalized;
    }

    private String normalizeOptionalNote(String note) {
        if (note == null) return "";
        return note.trim();
    }

    // ĐÃ SỬA: Lấy dữ liệu dung tích dựa trên Double value -> getValue()
    private String getSnapshotCapacityName(ProductVariant variant) {
        if (variant != null && variant.getCapacity() != null) {
            try {
                Object val = variant.getCapacity().getClass().getMethod("getValue").invoke(variant.getCapacity());
                if (val != null) return val.toString();
            } catch (Exception ignored) {
            }

            return variant.getCapacity().toString();
        }
        return "";
    }

    private String getSnapshotBottleTypeName(ProductVariant variant) {
        if (variant != null && variant.getBottleType() != null) {
            try {
                Object val = variant.getBottleType().getClass().getMethod("getName").invoke(variant.getBottleType());
                if (val != null) return val.toString();
            } catch (Exception ignored) {
            }

            try {
                Object val = variant.getBottleType().getClass().getMethod("getBottleTypeName").invoke(variant.getBottleType());
                if (val != null) return val.toString();
            } catch (Exception ignored) {
            }

            return variant.getBottleType().toString();
        }
        return "";
    }

    public String generateVnPayUrl(Integer orderId) {
        // 1. Tìm đơn hàng
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        // 2. Kiểm tra điều kiện: Chỉ cho phép tạo link nếu đơn hàng đang ở trạng thái PENDING
        if (!Integer.valueOf(ORDER_STATUS_PENDING).equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng không ở trạng thái chờ thanh toán hoặc đã bị hủy.");
        }

        // 3. Tạo link VNPay (Tái sử dụng logic từ hàm placeOrder)
        try {
            long amount = order.getFinalAmount().longValue() * 100;
            Map<String, String> vnp_Params = new java.util.HashMap<>();
            vnp_Params.put("vnp_Version", "2.1.0");
            vnp_Params.put("vnp_Command", "pay");
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.put("vnp_Amount", String.valueOf(amount));
            vnp_Params.put("vnp_CurrCode", "VND");

            // Dùng order.getId() + timestamp để tránh lỗi trùng mã giao dịch (TxnRef) khi thanh toán lại nhiều lần
            vnp_Params.put("vnp_TxnRef", order.getId() + "_" + System.currentTimeMillis());
            vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + order.getId());
            vnp_Params.put("vnp_OrderType", "other");
            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);

            // Lấy IP Address của client
            jakarta.servlet.http.HttpServletRequest httpRequest = ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()).getRequest();

            String ipAddr = httpRequest.getHeader("X-FORWARDED-FOR");
            if (ipAddr == null || ipAddr.isEmpty()) {
                ipAddr = httpRequest.getRemoteAddr();
            }
            if (ipAddr != null && ipAddr.equals("0:0:0:0:0:0:0:1")) {
                ipAddr = "127.0.0.1";
            }
            vnp_Params.put("vnp_IpAddr", ipAddr);

            // Set thời gian tạo và thời gian hết hạn (15 phút)
            java.util.Calendar cld = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Etc/GMT+7"));
            java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyyMMddHHmmss");

            vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));
            cld.add(java.util.Calendar.MINUTE, 15);
            vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

            // Build chuỗi mã hóa
            java.util.List<String> fieldNames = new java.util.ArrayList<>(vnp_Params.keySet());
            java.util.Collections.sort(fieldNames);

            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            java.util.Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);

                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName).append('=').append(java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.US_ASCII));

                    query.append(java.net.URLEncoder.encode(fieldName, java.nio.charset.StandardCharsets.US_ASCII)).append('=').append(java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.US_ASCII));

                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }

            String queryUrl = query.toString();
            String vnp_SecureHash = org.example.datn_sd69.common.config.VNPayConfig.hmacSHA512(secretKey, hashData.toString());

            queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

            // Trả về URL hoàn chỉnh
            return vnp_PayUrl + "?" + queryUrl;

        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi tạo link thanh toán VNPay");
        }
    }

    public void reportPayment(Integer orderId) {
        // 1. Tìm đơn hàng
        Order order = orderRepo.findById(orderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        // 2. Kiểm tra trạng thái: Chỉ cho phép báo cáo khi đơn đang chờ xử lý
        if (!Integer.valueOf(ORDER_STATUS_PENDING).equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chỉ có thể báo cáo thanh toán cho đơn hàng đang chờ xử lý.");
        }

        // 3. Tránh việc báo cáo trùng lặp
        if (Boolean.TRUE.equals(order.getIsPaymentReported())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng này đã được báo cáo thanh toán trước đó.");
        }

        // 4. Cập nhật trạng thái báo cáo thanh toán
        order.setIsPaymentReported(true);
        Order savedOrder = orderRepo.save(order);

        if (PAYMENT_METHOD_VIETQR.equals(savedOrder.getPaymentMethod())) {
            orderMailService.sendPaymentSuccessAsync(savedOrder);
        }
    }
}