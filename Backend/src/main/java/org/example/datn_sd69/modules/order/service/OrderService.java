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
        Map<Integer, CheckoutItemPrice> checkoutPriceMap = new LinkedHashMap<>();

        // ==========================================
        // 🛑 CHỐT CHẶN 1: KIỂM TRA VOUCHER (NẾU CÓ)
        // ==========================================
        BigDecimal discountAmount = BigDecimal.ZERO;
        Voucher appliedVoucher = null;
        LocalDateTime now = LocalDateTime.now();

        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            appliedVoucher = voucherRepo.findValidByCode(request.getVoucherCode().trim(), now)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Rất tiếc! Mã giảm giá không tồn tại, đã hết hạn, hoặc hết lượt sử dụng."
                    ));
        }

        // ==========================================
        // 🛑 CHỐT CHẶN 2: KIỂM TRA GIÁ TRỊ TỒN KHO & HẠN SỬ DỤNG TỪNG MÓN
        // ==========================================
        for (CartItem item : cartItems) {
            validateCartItem(item);
            ProductVariant variant = item.getProductVariant();

            if (!Integer.valueOf(1).equals(variant.getStatus())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sản phẩm '" + getSnapshotProductName(variant)
                                + "' (Loại: " + formatVariantName(variant)
                                + ") đang ngừng kinh doanh. Vui lòng quay lại giỏ hàng và xóa sản phẩm này!"
                );
            }

            int sellableQuantity = getSellableQuantity(variant);

            if (sellableQuantity < item.getQuantity()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sản phẩm '" + getSnapshotProductName(variant)
                                + "' (Loại: " + formatVariantName(variant)
                                + ") không đủ số lượng. Kho chỉ còn "
                                + sellableQuantity
                                + " sản phẩm có thể bán!"
                );
            }

            CheckoutItemPrice itemPrice = calculateCheckoutItemPrice(variant);
            checkoutPriceMap.put(item.getId(), itemPrice);

            BigDecimal lineTotal = itemPrice.finalUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
        }

        // ==========================================
        // 💥 ĐÃ FIX LỖI: TÍNH TOÁN TIỀN VOUCHER BẰNG ĐÚNG TÊN BIẾN
        // ==========================================
        if (appliedVoucher != null) {
            BigDecimal minOrderValue = appliedVoucher.getMinOrderValue() != null ? appliedVoucher.getMinOrderValue() : BigDecimal.ZERO;

            if (totalAmount.compareTo(minOrderValue) < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Đơn hàng chưa đạt giá trị tối thiểu " + minOrderValue + "đ để dùng mã giảm giá này."
                );
            }

            String discountType = appliedVoucher.getDiscountType() != null ? appliedVoucher.getDiscountType().toUpperCase() : "";
            BigDecimal discountValue = appliedVoucher.getDiscountValue() != null ? appliedVoucher.getDiscountValue() : BigDecimal.ZERO;

            if (discountType.contains("PERCENT") || discountType.contains("PERCENTAGE")) {
                discountAmount = totalAmount.multiply(discountValue).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                // Cắt phần vượt quá Max Discount
                BigDecimal maxDiscount = appliedVoucher.getMaxDiscount() != null ? appliedVoucher.getMaxDiscount() : BigDecimal.ZERO;
                if (maxDiscount.compareTo(BigDecimal.ZERO) > 0 && discountAmount.compareTo(maxDiscount) > 0) {
                    discountAmount = maxDiscount;
                }
            } else {
                discountAmount = discountValue;
            }

            // Đảm bảo không giảm âm tổng tiền
            if (discountAmount.compareTo(totalAmount) > 0) {
                discountAmount = totalAmount;
            }
        }

        discountAmount = normalizeMoney(discountAmount);

        // Phí vận chuyển cố định 30.000đ
        BigDecimal shippingFee = BigDecimal.valueOf(30000);

        // Công thức chuẩn: Tạm tính - Giảm giá + Phí vận chuyển
        BigDecimal finalAmount = totalAmount.subtract(discountAmount).add(shippingFee);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }
        finalAmount = normalizeMoney(finalAmount);

        /*
         * Chặn stale checkout:
         * FE có thể đang hiển thị dữ liệu cũ vì người dùng không reload trang.
         * Backend vẫn là source of truth và đã tự tính lại toàn bộ giá/voucher/phí ship.
         * Nếu FE gửi snapshot số tiền đang hiển thị và snapshot khác dữ liệu hiện tại,
         * tuyệt đối chưa tạo Order mà trả 409 để FE refetch và cho khách xác nhận lại.
         *
         * Các field expected* trong OrderRequest để nullable nhằm giữ tương thích
         * với caller cũ. Sau khi FE checkout đã gửi đầy đủ snapshot, có thể siết
         * @NotNull ở DTO nếu muốn bắt buộc mọi caller đều dùng stale-check.
         */
        validateCheckoutSnapshot(
                request,
                totalAmount,
                discountAmount,
                shippingFee,
                finalAmount
        );

        // 3. Tạo Order
        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderType("ONLINE");
        order.setCustomerName(normalizeText(request.getCustomerName(), "Tên người nhận"));
        order.setCustomerPhone(normalizeNoWhitespace(request.getCustomerPhone(), "Số điện thoại"));

        String finalShippingAddress = normalizeText(request.getShippingAddress(), "Địa chỉ giao hàng");
        if (Boolean.TRUE.equals(request.getIsVatRequired())) {
            String vatInfo = String.format(
                    " | [YÊU CẦU XUẤT VAT] MST: %s - Email: %s - Cty: %s - ĐC: %s",
                    request.getTaxCode().trim(),
                    request.getVatEmail().trim(),
                    request.getCompanyName().trim(),
                    request.getCompanyAddress().trim()
            );
            if ((finalShippingAddress + vatInfo).length() > 500) {
                finalShippingAddress = (finalShippingAddress + vatInfo).substring(0, 500);
            } else {
                finalShippingAddress += vatInfo;
            }
        }

        order.setShippingAddress(finalShippingAddress);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setShippingFee(shippingFee); // Lưu phí ship vào database
        order.setFinalAmount(finalAmount); // Tổng thanh toán đã bao gồm phí ship
        order.setPaymentMethod(paymentMethod);
        order.setStatus(ORDER_STATUS_PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setLoyaltyPointsApplied(false);
        order.setLoyaltyPointsEarned(0);
        order.setIsPaymentReported(false);

        if (appliedVoucher != null) {
            order.setVoucher(appliedVoucher);
            appliedVoucher.setUsedCount(appliedVoucher.getUsedCount() + 1);
            if (appliedVoucher.getUsedCount() >= appliedVoucher.getUsageLimit()) {
                appliedVoucher.setStatus(0);
            }
            voucherRepo.save(appliedVoucher);
        }

        Order savedOrder = orderRepo.save(order);

        /*
         * 4. Tạo OrderItem.
         */
        for (CartItem item : cartItems) {
            ProductVariant variant = item.getProductVariant();

            String itemImage = item.getThumbnailUrl();
            if ((itemImage == null || itemImage.trim().isEmpty()) && variant.getProduct() != null) {
                try {
                    itemImage = entityManager.createQuery(
                                    "SELECT img.imageUrl FROM ProductImage img WHERE img.product.id = :productId",
                                    String.class
                            )
                            .setParameter("productId", variant.getProduct().getId())
                            .setMaxResults(1)
                            .getResultStream()
                            .findFirst()
                            .orElse(null);
                } catch (Exception e) {
                    System.out.println("=== LỖI QUERY ẢNH ĐẶT HÀNG: " + e.getMessage());
                }
            }

            CheckoutItemPrice itemPrice = checkoutPriceMap.get(item.getId());
            if (itemPrice == null) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Không tìm thấy dữ liệu giá của sản phẩm trong giỏ hàng"
                );
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

        // 5. Dọn dẹp giỏ hàng
        cartItemRepository.deleteAll(cartItems);
        cart.getCartItems().clear();
        cartRepo.save(cart);

        orderMailService.sendOrderPlaced(savedOrder);

        // 6. Trả về kết quả
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

                jakarta.servlet.http.HttpServletRequest httpRequest =
                        ((org.springframework.web.context.request.ServletRequestAttributes)
                                org.springframework.web.context.request.RequestContextHolder
                                        .currentRequestAttributes()
                        ).getRequest();

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
                        hashData.append(fieldName)
                                .append('=')
                                .append(java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.US_ASCII));

                        query.append(java.net.URLEncoder.encode(fieldName, java.nio.charset.StandardCharsets.US_ASCII))
                                .append('=')
                                .append(java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.US_ASCII));

                        if (itr.hasNext()) {
                            query.append('&');
                            hashData.append('&');
                        }
                    }
                }

                String queryUrl = query.toString();
                String vnp_SecureHash = org.example.datn_sd69.common.config.VNPayConfig
                        .hmacSHA512(secretKey, hashData.toString());

                queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

                response.put("paymentUrl", vnp_PayUrl + "?" + queryUrl);
                response.put("message", "Chuyển hướng đến cổng thanh toán VNPay...");
            } catch (Exception e) {
                e.printStackTrace();
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Lỗi khi tạo link thanh toán VNPay"
                );
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
                    hashData.append(fieldName)
                            .append('=')
                            .append(java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.US_ASCII));

                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }

            String signValue = org.example.datn_sd69.common.config.VNPayConfig
                    .hmacSHA512(secretKey, hashData.toString());

            if (signValue.equals(vnp_SecureHash)) {
                String responseCode = params.get("vnp_ResponseCode");
                String txnRef = params.get("vnp_TxnRef");
                Integer orderId = Integer.parseInt(txnRef.split("_")[0]);
                Order order = orderRepo.findById(orderId).orElse(null);

                if (order != null) {
                    if ("00".equals(responseCode)) {
                        boolean wasPaymentReported = Boolean.TRUE.equals(order.getIsPaymentReported());
                        order.setStatus(ORDER_STATUS_PENDING);
                        order.setIsPaymentReported(true);
                        Order savedOrder = orderRepo.save(order);

                        if (!wasPaymentReported) {
                            orderMailService.sendPaymentSuccess(savedOrder);
                        }

                        response.put("success", true);
                        response.put("message", "Thanh toán VNPay thành công. Đơn hàng đang chờ xác nhận.");
                    } else if ("24".equals(responseCode)) {
                        boolean wasCancelled = ORDER_STATUS_CANCELLED == (order.getStatus() == null ? ORDER_STATUS_PENDING : order.getStatus());
                        order.setStatus(ORDER_STATUS_CANCELLED);
                        Order savedOrder = orderRepo.save(order);

                        if (!wasCancelled) {
                            orderMailService.sendOrderCancelled(savedOrder, "Khách hàng đã hủy giao dịch VNPay");
                        }

                        response.put("success", false);
                        response.put("message", "Khách hàng đã hủy giao dịch");
                    } else {
                        boolean wasCancelled = ORDER_STATUS_CANCELLED == (order.getStatus() == null ? ORDER_STATUS_PENDING : order.getStatus());
                        order.setStatus(ORDER_STATUS_CANCELLED);
                        Order savedOrder = orderRepo.save(order);

                        if (!wasCancelled) {
                            orderMailService.sendOrderCancelled(savedOrder, "Giao dịch VNPay không thành công (Mã lỗi: " + responseCode + ")");
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

    /**
     * So sánh snapshot tiền mà FE đang hiển thị với số tiền Backend vừa tính lại
     * từ dữ liệu hiện tại trong DB.
     *
     * Không tin các expected* để tính tiền; chúng CHỈ dùng để phát hiện stale UI.
     * Giá trị ghi vào Order/OrderItem vẫn luôn lấy từ Backend.
     */
    private void validateCheckoutSnapshot(
            OrderRequest request,
            BigDecimal currentTotalAmount,
            BigDecimal currentDiscountAmount,
            BigDecimal currentShippingFee,
            BigDecimal currentFinalAmount
    ) {
        if (request == null) {
            return;
        }

        List<String> changedFields = new ArrayList<>();

        if (isMoneyChanged(request.getExpectedTotalAmount(), currentTotalAmount)) {
            changedFields.add("tạm tính");
        }

        if (isMoneyChanged(request.getExpectedDiscountAmount(), currentDiscountAmount)) {
            changedFields.add("giảm giá");
        }

        if (isMoneyChanged(request.getExpectedShippingFee(), currentShippingFee)) {
            changedFields.add("phí vận chuyển");
        }

        if (isMoneyChanged(request.getExpectedFinalAmount(), currentFinalAmount)) {
            changedFields.add("tổng thanh toán");
        }

        if (!changedFields.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Thông tin thanh toán đã thay đổi ("
                            + String.join(", ", changedFields)
                            + "). Vui lòng kiểm tra lại giá, khuyến mãi và tổng tiền trước khi đặt hàng."
            );
        }
    }

    private boolean isMoneyChanged(BigDecimal expectedAmount, BigDecimal currentAmount) {
        if (expectedAmount == null) {
            return false;
        }

        return normalizeMoney(expectedAmount)
                .compareTo(normalizeMoney(currentAmount)) != 0;
    }

    private void validateCheckoutRequest(Integer customerId, OrderRequest request) {
        if (customerId == null || customerId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản khách hàng không hợp lệ");
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu đặt hàng không được để trống");
        }
        if (Boolean.TRUE.equals(request.getIsVatRequired())) {
            if (request.getTaxCode() == null || request.getTaxCode().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã số thuế không được để trống khi yêu cầu xuất VAT");
            }
            if (!request.getTaxCode().trim().matches("^[0-9-]{10,14}$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã số thuế không hợp lệ");
            }
            if (request.getVatEmail() == null || request.getVatEmail().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email nhận hóa đơn không được để trống");
            }
            if (!request.getVatEmail().trim().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Định dạng email nhận hóa đơn không đúng");
            }
            if (request.getCompanyName() == null || request.getCompanyName().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên công ty không được để trống");
            }
            if (request.getCompanyAddress() == null || request.getCompanyAddress().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Địa chỉ công ty không được để trống");
            }
        }
    }

    private int getSellableQuantity(ProductVariant variant) {
        if (variant == null || variant.getId() == null) {
            return 0;
        }

        Integer quantity =
                inventoryLotRepository.getSellableQuantityByVariantId(
                        variant.getId()
                );

        return quantity == null
                ? 0
                : Math.max(quantity, 0);
    }

    private void validateCartItem(CartItem item) {
        if (item == null || item.getProductVariant() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu giỏ hàng không hợp lệ");
        }
        if (item.getId() == null || item.getId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu sản phẩm trong giỏ hàng không hợp lệ");
        }
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng sản phẩm trong giỏ hàng không hợp lệ");
        }
    }

    private CheckoutItemPrice calculateCheckoutItemPrice(ProductVariant variant) {
        BigDecimal originalUnitPrice = normalizeMoney(variant.getPrice());
        if (originalUnitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá sản phẩm " + variant.getSku() + " không hợp lệ");
        }

        BigDecimal flashSalePercent = flashSalePriceService.findActiveFlashSalePercent(variant.getId());
        BigDecimal unitDiscountAmount = BigDecimal.ZERO;
        BigDecimal finalUnitPrice = originalUnitPrice;

        if (flashSalePercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal safePercent = flashSalePercent.min(BigDecimal.valueOf(100));
            unitDiscountAmount = originalUnitPrice
                    .multiply(safePercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            finalUnitPrice = originalUnitPrice.subtract(unitDiscountAmount);
            if (finalUnitPrice.compareTo(BigDecimal.ZERO) < 0) {
                finalUnitPrice = BigDecimal.ZERO;
            }
        }

        return new CheckoutItemPrice(
                normalizeMoney(originalUnitPrice),
                normalizeMoney(unitDiscountAmount),
                normalizeMoney(finalUnitPrice)
        );
    }

    private String normalizeText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " không được để trống");
        }
        return value.trim();
    }

    private String normalizeNoWhitespace(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " không được để trống");
        }
        return value.trim();
    }

    private String normalizePaymentMethod(String paymentMethod) {
        String value = normalizeNoWhitespace(paymentMethod, "Phương thức thanh toán").toUpperCase();
        if (!value.equals(PAYMENT_METHOD_COD)
                && !value.equals(PAYMENT_METHOD_VIETQR)
                && !value.equals(PAYMENT_METHOD_VNPAY)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phương thức thanh toán không hợp lệ");
        }
        return value;
    }

    private String normalizeOptionalNote(String note) {
        return note == null ? null : note.trim();
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String getSnapshotProductName(ProductVariant variant) {
        if (variant == null || variant.getProduct() == null) {
            return null;
        }
        return variant.getProduct().getName();
    }

    private String getSnapshotCapacityName(ProductVariant variant) {
        if (variant == null || variant.getCapacity() == null || variant.getCapacity().getValue() == null) {
            return null;
        }
        Double value = variant.getCapacity().getValue();
        if (value % 1 == 0) {
            return value.intValue() + "ml";
        }
        return value + "ml";
    }

    private String getSnapshotBottleTypeName(ProductVariant variant) {
        if (variant == null || variant.getBottleType() == null) {
            return null;
        }
        return variant.getBottleType().getName();
    }

    @Transactional
    public Map<String, Object> generateVnPayUrl(Integer orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng"
                ));

        if (order.getStatus() != ORDER_STATUS_PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đơn hàng không ở trạng thái chờ thanh toán"
            );
        }

        Map<String, Object> response = new LinkedHashMap<>();
        try {
            long amount = order.getFinalAmount().longValue() * 100;
            Map<String, String> vnp_Params = new java.util.HashMap<>();
            vnp_Params.put("vnp_Version", "2.1.0");
            vnp_Params.put("vnp_Command", "pay");
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.put("vnp_Amount", String.valueOf(amount));
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_TxnRef", order.getId() + "_" + System.currentTimeMillis());
            vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + order.getId());
            vnp_Params.put("vnp_OrderType", "other");
            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);

            jakarta.servlet.http.HttpServletRequest httpRequest =
                    ((org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder
                                    .currentRequestAttributes()
                    ).getRequest();

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
                    hashData.append(fieldName)
                            .append('=')
                            .append(java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.US_ASCII));
                    query.append(java.net.URLEncoder.encode(fieldName, java.nio.charset.StandardCharsets.US_ASCII))
                            .append('=')
                            .append(java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.US_ASCII));

                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }

            String queryUrl = query.toString();
            String vnp_SecureHash = org.example.datn_sd69.common.config.VNPayConfig
                    .hmacSHA512(secretKey, hashData.toString());

            queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
            response.put("paymentUrl", vnp_PayUrl + "?" + queryUrl);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi tạo lại link thanh toán VNPay");
        }
        return response;
    }

    @Transactional
    public void reportPayment(Integer orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng"
                ));
        order.setIsPaymentReported(true);
        orderRepo.save(order);
    }

    private record CheckoutItemPrice(
            BigDecimal originalUnitPrice,
            BigDecimal unitDiscountAmount,
            BigDecimal finalUnitPrice
    ) {
    }

    private String formatVariantName(ProductVariant v) {
        if (v == null) return "Loại";
        String capString = getSnapshotCapacityName(v);
        String bottleString = getSnapshotBottleTypeName(v);
        if (capString != null && bottleString != null) return capString + " - " + bottleString;
        if (capString != null) return capString;
        if (bottleString != null) return bottleString;
        return "Loại " + v.getId();
    }
}