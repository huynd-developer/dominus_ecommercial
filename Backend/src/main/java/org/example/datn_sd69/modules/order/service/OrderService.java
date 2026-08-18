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
public class OrderService { //[cite: 7]

    private static final int ORDER_STATUS_PENDING = 0; //[cite: 7]
    private static final int ORDER_STATUS_CONFIRMED = 1; //[cite: 7]
    private static final int ORDER_STATUS_CANCELLED = 4; //[cite: 7]

    private static final String PAYMENT_METHOD_COD = "COD"; //[cite: 7]
    private static final String PAYMENT_METHOD_VIETQR = "VIETQR"; //[cite: 7]
    private static final String PAYMENT_METHOD_VNPAY = "VNPAY"; //[cite: 7]

    private final CartRepository cartRepo; //[cite: 7]
    private final OrderRepository orderRepo; //[cite: 7]
    private final OrderItemRepository orderItemRepo; //[cite: 7]
    private final ProductVariantRepository variantRepo; //[cite: 7]
    private final CartItemRepository cartItemRepository; //[cite: 7]
    private final CustomerRepository customerRepo; //[cite: 7]
    private final InventoryLotRepository inventoryLotRepository;
    private final VoucherRepository voucherRepo; //[cite: 7]
    private final FlashSalePriceService flashSalePriceService; //[cite: 7]
    private final OrderMailService orderMailService;
    private final jakarta.persistence.EntityManager entityManager; //[cite: 7]

    @org.springframework.beans.factory.annotation.Value("${vnpay.tmnCode}")
    private String vnp_TmnCode; //[cite: 7]

    @org.springframework.beans.factory.annotation.Value("${vnpay.hashSecret}")
    private String secretKey; //[cite: 7]

    @org.springframework.beans.factory.annotation.Value("${vnpay.url}")
    private String vnp_PayUrl; //[cite: 7]

    @org.springframework.beans.factory.annotation.Value("${vnpay.onlineReturnUrl}")
    private String vnp_ReturnUrl; //[cite: 7]

    @Transactional
    public Map<String, Object> placeOrder(Integer customerId, OrderRequest request) { //[cite: 7]
        validateCheckoutRequest(customerId, request); //[cite: 7]

        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy khách hàng"
                )); //[cite: 7]

        Cart cart = cartRepo.findByCustomerUserId(customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Giỏ hàng trống"
                )); //[cite: 7]

        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Giỏ hàng không có sản phẩm nào"
            ); //[cite: 7]
        }

        String paymentMethod = normalizePaymentMethod(request.getPaymentMethod()); //[cite: 7]
        BigDecimal totalAmount = BigDecimal.ZERO; //[cite: 7]

        List<CartItem> cartItems = new ArrayList<>(cart.getCartItems()); //[cite: 7]
        Map<Integer, CheckoutItemPrice> checkoutPriceMap = new LinkedHashMap<>(); //[cite: 7]

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

            /*
             * 2.1. ProductVariant chỉ quyết định SKU còn được phép bán hay không.
             * NSX/HSD không còn nằm ở ProductVariant vì một SKU có thể có nhiều lot.
             */
            if (!Integer.valueOf(1).equals(variant.getStatus())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sản phẩm '" + getSnapshotProductName(variant)
                                + "' (Loại: " + formatVariantName(variant)
                                + ") đang ngừng kinh doanh. Vui lòng quay lại giỏ hàng và xóa sản phẩm này!"
                );
            }

            /*
             * 2.2. Kiểm tra tồn CÓ THỂ BÁN thật từ InventoryLot.
             *
             * sellableQuantity = tổng QuantityOnHand của các lot:
             * - còn số lượng
             * - chưa hết hạn (ExpirationDate >= hôm nay)
             *
             * Không trừ kho ở đây. Đơn ONLINE mới vẫn ở PENDING;
             * AdminOrderServiceImpl.confirmOrder() mới FEFO + SALE_OUT.
             */
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

            // 2.3. Lấy giá Realtime từ DB (Tự động áp dụng hoặc gỡ bỏ Flash Sale theo giờ thực tế)
            CheckoutItemPrice itemPrice = calculateCheckoutItemPrice(variant);
            checkoutPriceMap.put(item.getId(), itemPrice);

            BigDecimal lineTotal = itemPrice.finalUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
        }

        discountAmount = normalizeMoney(discountAmount);

        discountAmount = normalizeMoney(discountAmount); //[cite: 7]

        // Phí vận chuyển cố định 30.000đ
        BigDecimal shippingFee = BigDecimal.valueOf(30000);

        // Công thức chuẩn: Tạm tính - Giảm giá + Phí vận chuyển
        BigDecimal finalAmount = totalAmount.subtract(discountAmount).add(shippingFee); //[cite: 7]
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO; //[cite: 7]
        }
        finalAmount = normalizeMoney(finalAmount); //[cite: 7]

        // 3. Tạo Order
        Order order = new Order(); //[cite: 7]
        order.setCustomer(customer); //[cite: 7]
        order.setOrderType("ONLINE"); //[cite: 7]
        order.setCustomerName(normalizeText(request.getCustomerName(), "Tên người nhận")); //[cite: 7]
        order.setCustomerPhone(normalizeNoWhitespace(request.getCustomerPhone(), "Số điện thoại")); //[cite: 7]

        String finalShippingAddress = normalizeText(request.getShippingAddress(), "Địa chỉ giao hàng"); //[cite: 7]
        if (Boolean.TRUE.equals(request.getIsVatRequired())) {
            String vatInfo = String.format(
                    " | [YÊU CẦU XUẤT VAT] MST: %s - Email: %s - Cty: %s - ĐC: %s",
                    request.getTaxCode().trim(),
                    request.getVatEmail().trim(),
                    request.getCompanyName().trim(),
                    request.getCompanyAddress().trim()
            ); //[cite: 7]
            if ((finalShippingAddress + vatInfo).length() > 500) {
                finalShippingAddress = (finalShippingAddress + vatInfo).substring(0, 500); //[cite: 7]
            } else {
                finalShippingAddress += vatInfo; //[cite: 7]
            }
        }

        order.setShippingAddress(finalShippingAddress); //[cite: 7]
        order.setTotalAmount(totalAmount); //[cite: 7]
        order.setDiscountAmount(discountAmount); //[cite: 7]
        order.setShippingFee(shippingFee); // Lưu phí ship vào database
        order.setFinalAmount(finalAmount); // Tổng thanh toán đã bao gồm phí ship
        order.setPaymentMethod(paymentMethod); //[cite: 7]
        order.setStatus(ORDER_STATUS_PENDING); //[cite: 7]
        order.setCreatedAt(LocalDateTime.now()); //[cite: 7]
        order.setLoyaltyPointsApplied(false); //[cite: 7]
        order.setLoyaltyPointsEarned(0); //[cite: 7]
        order.setIsPaymentReported(false); //[cite: 7]

        if (appliedVoucher != null) {
            order.setVoucher(appliedVoucher); //[cite: 7]
            appliedVoucher.setUsedCount(appliedVoucher.getUsedCount() + 1); //[cite: 7]
            if (appliedVoucher.getUsedCount() >= appliedVoucher.getUsageLimit()) {
                appliedVoucher.setStatus(0); //[cite: 7]
            }
            voucherRepo.save(appliedVoucher); //[cite: 7]
        }

        Order savedOrder = orderRepo.save(order); //[cite: 7]

        /*
         * 4. Tạo OrderItem.
         * Không trừ kho khi đơn mới ở trạng thái Chờ xác nhận.
         * Kho chỉ được trừ khi admin xác nhận đơn ở AdminOrderServiceImpl.confirmOrder().
         */
        for (CartItem item : cartItems) { //[cite: 7]
            ProductVariant variant = item.getProductVariant(); //[cite: 7]

            String itemImage = item.getThumbnailUrl(); //[cite: 7]
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
                            .orElse(null); //[cite: 7]
                } catch (Exception e) {
                    System.out.println("=== LỖI QUERY ẢNH ĐẶT HÀNG: " + e.getMessage()); //[cite: 7]
                }
            }

            CheckoutItemPrice itemPrice = checkoutPriceMap.get(item.getId()); //[cite: 7]
            if (itemPrice == null) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Không tìm thấy dữ liệu giá của sản phẩm trong giỏ hàng"
                ); //[cite: 7]
            }

            OrderItem orderItem = new OrderItem(); //[cite: 7]
            orderItem.setOrder(savedOrder); //[cite: 7]
            orderItem.setProductVariant(variant); //[cite: 7]
            orderItem.setQuantity(item.getQuantity()); //[cite: 7]

            // GÁN GIÁ THEO ĐƠN VỊ 1 SẢN PHẨM (Đúng Constraint DB)
            orderItem.setOriginalPrice(itemPrice.originalUnitPrice()); //[cite: 7]
            orderItem.setDiscountAmount(itemPrice.unitDiscountAmount()); //[cite: 7]
            orderItem.setFinalPrice(itemPrice.finalUnitPrice()); //[cite: 7]

            orderItem.setNote(normalizeOptionalNote(request.getNote())); //[cite: 7]
            orderItem.setImage(itemImage); //[cite: 7]
            orderItem.setProductName(getSnapshotProductName(variant)); //[cite: 7]
            orderItem.setSku(variant.getSku()); //[cite: 7]
            orderItem.setCapacityName(getSnapshotCapacityName(variant)); //[cite: 7]
            orderItem.setBottleTypeName(getSnapshotBottleTypeName(variant)); //[cite: 7]

            orderItemRepo.save(orderItem); //[cite: 7]
        }

        // 5. Dọn dẹp giỏ hàng
        cartItemRepository.deleteAll(cartItems); //[cite: 7]
        cart.getCartItems().clear(); //[cite: 7]
        cartRepo.save(cart); //[cite: 7]

        orderMailService.sendOrderPlaced(savedOrder);

        // 6. Trả về kết quả
        Map<String, Object> response = new LinkedHashMap<>(); //[cite: 7]
        response.put("orderId", savedOrder.getId()); //[cite: 7]
        response.put("status", savedOrder.getStatus()); //[cite: 7]
        response.put("paymentMethod", savedOrder.getPaymentMethod()); //[cite: 7]
        response.put("totalAmount", savedOrder.getTotalAmount()); //[cite: 7]
        response.put("discountAmount", savedOrder.getDiscountAmount()); //[cite: 7]
        response.put("shippingFee", savedOrder.getShippingFee()); // Trả về phí vận chuyển
        response.put("finalAmount", savedOrder.getFinalAmount()); //[cite: 7]

        if (PAYMENT_METHOD_VNPAY.equals(savedOrder.getPaymentMethod())) {
            try {
                long amount = savedOrder.getFinalAmount().longValue() * 100; //[cite: 7]
                Map<String, String> vnp_Params = new java.util.HashMap<>(); //[cite: 7]
                vnp_Params.put("vnp_Version", "2.1.0"); //[cite: 7]
                vnp_Params.put("vnp_Command", "pay"); //[cite: 7]
                vnp_Params.put("vnp_TmnCode", vnp_TmnCode); //[cite: 7]
                vnp_Params.put("vnp_Amount", String.valueOf(amount)); //[cite: 7]
                vnp_Params.put("vnp_CurrCode", "VND"); //[cite: 7]
                vnp_Params.put("vnp_TxnRef", savedOrder.getId() + "_" + System.currentTimeMillis()); //[cite: 7]
                vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + savedOrder.getId()); //[cite: 7]
                vnp_Params.put("vnp_OrderType", "other"); //[cite: 7]
                vnp_Params.put("vnp_Locale", "vn"); //[cite: 7]
                vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl); //[cite: 7]

                jakarta.servlet.http.HttpServletRequest httpRequest =
                        ((org.springframework.web.context.request.ServletRequestAttributes)
                                org.springframework.web.context.request.RequestContextHolder
                                        .currentRequestAttributes()
                        ).getRequest(); //[cite: 7]

                String ipAddr = httpRequest.getHeader("X-FORWARDED-FOR"); //[cite: 7]
                if (ipAddr == null || ipAddr.isEmpty()) {
                    ipAddr = httpRequest.getRemoteAddr(); //[cite: 7]
                }
                if (ipAddr != null && ipAddr.equals("0:0:0:0:0:0:0:1")) {
                    ipAddr = "127.0.0.1"; //[cite: 7]
                }

                vnp_Params.put("vnp_IpAddr", ipAddr); //[cite: 7]

                java.util.Calendar cld = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Etc/GMT+7")); //[cite: 7]
                java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyyMMddHHmmss"); //[cite: 7]

                vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime())); //[cite: 7]
                cld.add(java.util.Calendar.MINUTE, 15); //[cite: 7]
                vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime())); //[cite: 7]

                java.util.List<String> fieldNames = new java.util.ArrayList<>(vnp_Params.keySet()); //[cite: 7]
                java.util.Collections.sort(fieldNames); //[cite: 7]

                StringBuilder hashData = new StringBuilder(); //[cite: 7]
                StringBuilder query = new StringBuilder(); //[cite: 7]

                java.util.Iterator<String> itr = fieldNames.iterator();
                while (itr.hasNext()) {
                    String fieldName = itr.next();
                    String fieldValue = vnp_Params.get(fieldName);

                    if ((fieldValue != null) && (fieldValue.length() > 0)) {
                        hashData.append(fieldName)
                                .append('=')
                                .append(java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.US_ASCII)); //[cite: 7]

                        query.append(java.net.URLEncoder.encode(fieldName, java.nio.charset.StandardCharsets.US_ASCII))
                                .append('=')
                                .append(java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.US_ASCII)); //[cite: 7]

                        if (itr.hasNext()) {
                            query.append('&'); //[cite: 7]
                            hashData.append('&'); //[cite: 7]
                        }
                    }
                }

                String queryUrl = query.toString(); //[cite: 7]
                String vnp_SecureHash = org.example.datn_sd69.common.config.VNPayConfig
                        .hmacSHA512(secretKey, hashData.toString()); //[cite: 7]

                queryUrl += "&vnp_SecureHash=" + vnp_SecureHash; //[cite: 7]

                response.put("paymentUrl", vnp_PayUrl + "?" + queryUrl); //[cite: 7]
                response.put("message", "Chuyển hướng đến cổng thanh toán VNPay..."); //[cite: 7]
            } catch (Exception e) {
                e.printStackTrace();
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Lỗi khi tạo link thanh toán VNPay"
                ); //[cite: 7]
            }
        } else if (PAYMENT_METHOD_VIETQR.equals(savedOrder.getPaymentMethod())) {
            response.put("message", "Đơn hàng đã được tạo. Vui lòng quét mã QR để hoàn tất thanh toán."); //[cite: 7]
        } else {
            response.put("message", "Đặt hàng thành công. Đơn hàng đang chờ xác nhận."); //[cite: 7]
        }

        return response; //[cite: 7]
    }

    @Transactional
    public Map<String, Object> verifyVnPayReturn(Map<String, String> params) { //[cite: 7]
        Map<String, Object> response = new LinkedHashMap<>(); //[cite: 7]
        try {
            String vnp_SecureHash = params.get("vnp_SecureHash"); //[cite: 7]
            params.remove("vnp_SecureHash"); //[cite: 7]
            params.remove("vnp_SecureHashType"); //[cite: 7]

            java.util.List<String> fieldNames = new java.util.ArrayList<>(params.keySet()); //[cite: 7]
            java.util.Collections.sort(fieldNames); //[cite: 7]

            StringBuilder hashData = new StringBuilder(); //[cite: 7]
            java.util.Iterator<String> itr = fieldNames.iterator();

            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = params.get(fieldName);

                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName)
                            .append('=')
                            .append(java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.US_ASCII)); //[cite: 7]

                    if (itr.hasNext()) {
                        hashData.append('&'); //[cite: 7]
                    }
                }
            }

            String signValue = org.example.datn_sd69.common.config.VNPayConfig
                    .hmacSHA512(secretKey, hashData.toString()); //[cite: 7]

            if (signValue.equals(vnp_SecureHash)) {
                String responseCode = params.get("vnp_ResponseCode"); //[cite: 7]
                String txnRef = params.get("vnp_TxnRef"); //[cite: 7]
                Integer orderId = Integer.parseInt(txnRef.split("_")[0]); //[cite: 7]
                Order order = orderRepo.findById(orderId).orElse(null); //[cite: 7]

                if (order != null) {
                    if ("00".equals(responseCode)) {
                        /*
                         * VNPay trả về thành công chỉ có nghĩa là khách đã thanh toán.
                         * Không tự chuyển đơn sang Đã xác nhận vì xác nhận đơn là thao tác
                         * của shop/admin. Đơn vẫn phải ở trạng thái Chờ xác nhận.
                         */
                        boolean wasPaymentReported = Boolean.TRUE.equals(order.getIsPaymentReported());
                        order.setStatus(ORDER_STATUS_PENDING); //[cite: 7]
                        order.setIsPaymentReported(true); //[cite: 7]
                        Order savedOrder = orderRepo.save(order); //[cite: 7]

                        if (!wasPaymentReported) {
                            orderMailService.sendPaymentSuccess(savedOrder);
                        }

                        response.put("success", true); //[cite: 7]
                        response.put("message", "Thanh toán VNPay thành công. Đơn hàng đang chờ xác nhận."); //[cite: 7]
                    } else if ("24".equals(responseCode)) {
                        boolean wasCancelled = ORDER_STATUS_CANCELLED == (order.getStatus() == null ? ORDER_STATUS_PENDING : order.getStatus());
                        order.setStatus(ORDER_STATUS_CANCELLED); //[cite: 7]
                        Order savedOrder = orderRepo.save(order); //[cite: 7]

                        if (!wasCancelled) {
                            orderMailService.sendOrderCancelled(savedOrder, "Khách hàng đã hủy giao dịch VNPay");
                        }

                        response.put("success", false); //[cite: 7]
                        response.put("message", "Khách hàng đã hủy giao dịch"); //[cite: 7]
                    } else {
                        boolean wasCancelled = ORDER_STATUS_CANCELLED == (order.getStatus() == null ? ORDER_STATUS_PENDING : order.getStatus());
                        order.setStatus(ORDER_STATUS_CANCELLED); //[cite: 7]
                        Order savedOrder = orderRepo.save(order); //[cite: 7]

                        if (!wasCancelled) {
                            orderMailService.sendOrderCancelled(savedOrder, "Giao dịch VNPay không thành công (Mã lỗi: " + responseCode + ")");
                        }

                        response.put("success", false); //[cite: 7]
                        response.put("message", "Giao dịch không thành công (Mã lỗi: " + responseCode + ")"); //[cite: 7]
                    }
                } else {
                    response.put("success", false); //[cite: 7]
                    response.put("message", "Không tìm thấy đơn hàng"); //[cite: 7]
                }
            } else {
                response.put("success", false); //[cite: 7]
                response.put("message", "Chữ ký không hợp lệ"); //[cite: 7]
            }
        } catch (Exception e) {
            e.printStackTrace(); //[cite: 7]
            response.put("success", false); //[cite: 7]
            response.put("message", "Lỗi xác thực VNPay"); //[cite: 7]
        }
        return response; //[cite: 7]
    }

    private void validateCheckoutRequest(Integer customerId, OrderRequest request) { //[cite: 7]
        if (customerId == null || customerId <= 0) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tài khoản khách hàng không hợp lệ"); //[cite: 7]
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu đặt hàng không được để trống"); //[cite: 7]
        }
        if (Boolean.TRUE.equals(request.getIsVatRequired())) {
            if (request.getTaxCode() == null || request.getTaxCode().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã số thuế không được để trống khi yêu cầu xuất VAT"); //[cite: 7]
            }
            if (!request.getTaxCode().trim().matches("^[0-9-]{10,14}$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã số thuế không hợp lệ"); //[cite: 7]
            }
            if (request.getVatEmail() == null || request.getVatEmail().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email nhận hóa đơn không được để trống"); //[cite: 7]
            }
            if (!request.getVatEmail().trim().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Định dạng email nhận hóa đơn không đúng"); //[cite: 7]
            }
            if (request.getCompanyName() == null || request.getCompanyName().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên công ty không được để trống"); //[cite: 7]
            }
            if (request.getCompanyAddress() == null || request.getCompanyAddress().trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Địa chỉ công ty không được để trống"); //[cite: 7]
            }
        }
    }

    /**
     * Tồn bán được thật của một SKU từ InventoryLot.
     *
     * Cùng quy tắc với POS/Admin confirm:
     * QuantityOnHand > 0 và ExpirationDate >= hôm nay.
     */
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

    private void validateCartItem(CartItem item) { //[cite: 7]
        if (item == null || item.getProductVariant() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu giỏ hàng không hợp lệ"); //[cite: 7]
        }
        if (item.getId() == null || item.getId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu sản phẩm trong giỏ hàng không hợp lệ"); //[cite: 7]
        }
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng sản phẩm trong giỏ hàng không hợp lệ"); //[cite: 7]
        }
    }

    private CheckoutItemPrice calculateCheckoutItemPrice(ProductVariant variant) { //[cite: 7]
        BigDecimal originalUnitPrice = normalizeMoney(variant.getPrice()); //[cite: 7]
        if (originalUnitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá sản phẩm " + variant.getSku() + " không hợp lệ"); //[cite: 7]
        }

        BigDecimal flashSalePercent = flashSalePriceService.findActiveFlashSalePercent(variant.getId()); //[cite: 7]
        BigDecimal unitDiscountAmount = BigDecimal.ZERO; //[cite: 7]
        BigDecimal finalUnitPrice = originalUnitPrice; //[cite: 7]

        if (flashSalePercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal safePercent = flashSalePercent.min(BigDecimal.valueOf(100)); //[cite: 7]
            unitDiscountAmount = originalUnitPrice
                    .multiply(safePercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP); //[cite: 7]
            finalUnitPrice = originalUnitPrice.subtract(unitDiscountAmount); //[cite: 7]
            if (finalUnitPrice.compareTo(BigDecimal.ZERO) < 0) {
                finalUnitPrice = BigDecimal.ZERO; //[cite: 7]
            }
        }

        return new CheckoutItemPrice(
                normalizeMoney(originalUnitPrice),
                normalizeMoney(unitDiscountAmount),
                normalizeMoney(finalUnitPrice)
        ); //[cite: 7]
    }

    private String normalizeText(String value, String fieldName) { //[cite: 7]
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " không được để trống"); //[cite: 7]
        }
        return value.trim(); //[cite: 7]
    }

    private String normalizeNoWhitespace(String value, String fieldName) { //[cite: 7]
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " không được để trống"); //[cite: 7]
        }
        return value.trim(); //[cite: 7]
    }

    private String normalizePaymentMethod(String paymentMethod) { //[cite: 7]
        String value = normalizeNoWhitespace(paymentMethod, "Phương thức thanh toán").toUpperCase(); //[cite: 7]
        if (!value.equals(PAYMENT_METHOD_COD)
                && !value.equals(PAYMENT_METHOD_VIETQR)
                && !value.equals(PAYMENT_METHOD_VNPAY)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phương thức thanh toán không hợp lệ"); //[cite: 7]
        }
        return value; //[cite: 7]
    }

    private String normalizeOptionalNote(String note) { //[cite: 7]
        return note == null ? null : note.trim(); //[cite: 7]
    }

    private BigDecimal normalizeMoney(BigDecimal value) { //[cite: 7]
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP); //[cite: 7]
        }
        return value.setScale(2, RoundingMode.HALF_UP); //[cite: 7]
    }

    private String getSnapshotProductName(ProductVariant variant) { //[cite: 7]
        if (variant == null || variant.getProduct() == null) {
            return null; //[cite: 7]
        }
        return variant.getProduct().getName(); //[cite: 7]
    }

    private String getSnapshotCapacityName(ProductVariant variant) { //[cite: 7]
        if (variant == null || variant.getCapacity() == null || variant.getCapacity().getValue() == null) {
            return null; //[cite: 7]
        }
        Double value = variant.getCapacity().getValue(); //[cite: 7]
        if (value % 1 == 0) {
            return value.intValue() + "ml"; //[cite: 7]
        }
        return value + "ml"; //[cite: 7]
    }

    private String getSnapshotBottleTypeName(ProductVariant variant) { //[cite: 7]
        if (variant == null || variant.getBottleType() == null) {
            return null; //[cite: 7]
        }
        return variant.getBottleType().getName(); //[cite: 7]
    }

    @Transactional
    public Map<String, Object> generateVnPayUrl(Integer orderId) { //[cite: 7]
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng"
                )); //[cite: 7]

        if (order.getStatus() != ORDER_STATUS_PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đơn hàng không ở trạng thái chờ thanh toán"
            ); //[cite: 7]
        }

        Map<String, Object> response = new LinkedHashMap<>(); //[cite: 7]
        try {
            long amount = order.getFinalAmount().longValue() * 100; //[cite: 7]
            Map<String, String> vnp_Params = new java.util.HashMap<>(); //[cite: 7]
            vnp_Params.put("vnp_Version", "2.1.0"); //[cite: 7]
            vnp_Params.put("vnp_Command", "pay"); //[cite: 7]
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode); //[cite: 7]
            vnp_Params.put("vnp_Amount", String.valueOf(amount)); //[cite: 7]
            vnp_Params.put("vnp_CurrCode", "VND"); //[cite: 7]
            vnp_Params.put("vnp_TxnRef", order.getId() + "_" + System.currentTimeMillis()); //[cite: 7]
            vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + order.getId()); //[cite: 7]
            vnp_Params.put("vnp_OrderType", "other"); //[cite: 7]
            vnp_Params.put("vnp_Locale", "vn"); //[cite: 7]
            vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl); //[cite: 7]

            jakarta.servlet.http.HttpServletRequest httpRequest =
                    ((org.springframework.web.context.request.ServletRequestAttributes)
                            org.springframework.web.context.request.RequestContextHolder
                                    .currentRequestAttributes()
                    ).getRequest(); //[cite: 7]

            String ipAddr = httpRequest.getHeader("X-FORWARDED-FOR"); //[cite: 7]
            if (ipAddr == null || ipAddr.isEmpty()) {
                ipAddr = httpRequest.getRemoteAddr(); //[cite: 7]
            }
            if (ipAddr != null && ipAddr.equals("0:0:0:0:0:0:0:1")) {
                ipAddr = "127.0.0.1"; //[cite: 7]
            }
            vnp_Params.put("vnp_IpAddr", ipAddr); //[cite: 7]

            java.util.Calendar cld = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Etc/GMT+7")); //[cite: 7]
            java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyyMMddHHmmss"); //[cite: 7]
            vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime())); //[cite: 7]
            cld.add(java.util.Calendar.MINUTE, 15); //[cite: 7]
            vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime())); //[cite: 7]

            java.util.List<String> fieldNames = new java.util.ArrayList<>(vnp_Params.keySet()); //[cite: 7]
            java.util.Collections.sort(fieldNames); //[cite: 7]

            StringBuilder hashData = new StringBuilder(); //[cite: 7]
            StringBuilder query = new StringBuilder(); //[cite: 7]
            java.util.Iterator<String> itr = fieldNames.iterator();

            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);

                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName)
                            .append('=')
                            .append(java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.US_ASCII)); //[cite: 7]
                    query.append(java.net.URLEncoder.encode(fieldName, java.nio.charset.StandardCharsets.US_ASCII))
                            .append('=')
                            .append(java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.US_ASCII)); //[cite: 7]

                    if (itr.hasNext()) {
                        query.append('&'); //[cite: 7]
                        hashData.append('&'); //[cite: 7]
                    }
                }
            }

            String queryUrl = query.toString(); //[cite: 7]
            String vnp_SecureHash = org.example.datn_sd69.common.config.VNPayConfig
                    .hmacSHA512(secretKey, hashData.toString()); //[cite: 7]

            queryUrl += "&vnp_SecureHash=" + vnp_SecureHash; //[cite: 7]
            response.put("paymentUrl", vnp_PayUrl + "?" + queryUrl); //[cite: 7]
        } catch (Exception e) {
            e.printStackTrace(); //[cite: 7]
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi tạo lại link thanh toán VNPay"); //[cite: 7]
        }
        return response; //[cite: 7]
    }

    @Transactional
    public void reportPayment(Integer orderId) { //[cite: 7]
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng"
                )); //[cite: 7]
        order.setIsPaymentReported(true); //[cite: 7]
        orderRepo.save(order); //[cite: 7]
    }

    private record CheckoutItemPrice(
            BigDecimal originalUnitPrice,
            BigDecimal unitDiscountAmount,
            BigDecimal finalUnitPrice
    ) {
    } //[cite: 7]

    // Ném hàm này xuống cuối file OrderService.java
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