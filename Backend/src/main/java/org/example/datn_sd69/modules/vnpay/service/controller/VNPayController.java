package org.example.datn_sd69.modules.vnpay.service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.datn_sd69.entity.Customer;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.entity.Voucher;
import org.example.datn_sd69.modules.pos.service.PosService;
import org.example.datn_sd69.modules.vnpay.service.VNPayService;
import org.example.datn_sd69.repository.CustomerRepository;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.example.datn_sd69.repository.OrderRepository;
import org.example.datn_sd69.repository.VoucherRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/vnpay")
@RequiredArgsConstructor
public class VNPayController {

    private static final int ORDER_STATUS_PENDING = 0;
    private static final int ORDER_STATUS_CONFIRMED = 1;
    private static final int ORDER_STATUS_COMPLETED = 3;
    private static final int ORDER_STATUS_CANCELLED = 4;

    private static final String PAYMENT_VNPAY = "VNPAY";
    private static final String PAYMENT_MIXED = "MIXED";
    private static final String PAYMENT_MIXED_VNPAY = "MIXED_VNPAY";
    private static final String PAYMENT_MIXED_VIETQR = "MIXED_VIETQR";

    private static final String TRANSFER_PROVIDER_VNPAY = "VNPAY";
    private static final String TRANSFER_PROVIDER_VIETQR = "VIETQR";

    private static final String ORDER_TYPE_ONLINE = "ONLINE";


    private static final BigDecimal POINT_RATE_AMOUNT = BigDecimal.valueOf(10_000);

    private final VNPayService vnPayService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PosService posService;
    private final CustomerRepository customerRepository;
    private final VoucherRepository voucherRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * VNPay IPN server-to-server.
     * <p>
     * Dùng để VNPay gọi về BE xác nhận giao dịch.
     * Trả về RspCode theo chuẩn VNPay.
     */
    @GetMapping("/ipn")
    @Transactional
    public ResponseEntity<Map<String, String>> handleIpn(@RequestParam Map<String, String> params) {
        log.info("[VNPay IPN] Nhận callback params: {}", params);

        boolean isValidSignature = vnPayService.verifyCallback(params);

        if (!isValidSignature) {
            log.warn("[VNPay IPN] Chữ ký không hợp lệ!");
            return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid Checksum"));
        }

        String responseCode = params.get("vnp_ResponseCode");
        String vnpAmount = params.get("vnp_Amount");

        Integer orderId = extractOrderId(params);

        if (orderId == null) {
            return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order Not Found"));
        }

        lockOrderRow(orderId);

        Order order = orderRepository.findById(orderId).orElse(null);

        if (order == null) {
            return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order Not Found"));
        }

        if (order.getStatus() == null || order.getStatus() != ORDER_STATUS_PENDING) {
            log.info("[VNPay IPN] Đơn #{} đã được xử lý trước đó. Status={}", orderId, order.getStatus());
            return ResponseEntity.ok(Map.of("RspCode", "02", "Message", "Order Already Confirmed"));
        }

        if (!isVnpayOrder(order)) {
            log.warn("[VNPay IPN] Đơn #{} không phải đơn VNPay/MIXED. Method={}", orderId, order.getPaymentMethod());
            return ResponseEntity.ok(Map.of("RspCode", "04", "Message", "Invalid Payment Method"));
        }

        if (!isValidVnpayAmount(order, vnpAmount)) {
            log.warn("[VNPay IPN] Sai số tiền. OrderId={}, Method={}, FinalAmount={}, VNPayAmount={}",
                    orderId, order.getPaymentMethod(), order.getFinalAmount(), vnpAmount);
            return ResponseEntity.ok(Map.of("RspCode", "04", "Message", "Invalid Amount"));
        }

        if ("00".equals(responseCode)) {
            processPaymentSuccess(order);
            log.info("[VNPay IPN] Đơn #{} thanh toán thành công.", orderId);
        } else {
            processPaymentFailure(order, responseCode);
            log.info("[VNPay IPN] Đơn #{} thanh toán thất bại. ResponseCode={}", orderId, responseCode);
        }

        return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
    }

    /**
     * VNPay Return browser redirect.
     * <p>
     * FE hoặc trình duyệt có thể gọi URL này sau khi thanh toán.
     * Vẫn verify chữ ký để tránh giả callback.
     */
    @GetMapping("/return")
    @Transactional
    public ResponseEntity<Map<String, Object>> handleReturn(@RequestParam Map<String, String> params) {
        log.info("[VNPay Return] Trình duyệt chuyển hướng về params: {}", params);

        boolean isValidSignature = vnPayService.verifyCallback(params);

        String responseCode = params.get("vnp_ResponseCode");
        String vnpAmount = params.get("vnp_Amount");

        Integer orderId = extractOrderId(params);

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("responseCode", responseCode);
        result.put("orderInfo", params.get("vnp_OrderInfo"));
        result.put("txnRef", params.get("vnp_TxnRef"));
        result.put("amount", vnpAmount);
        result.put("transactionNo", params.get("vnp_TransactionNo"));
        result.put("bankCode", params.get("vnp_BankCode"));
        result.put("payDate", params.get("vnp_PayDate"));

        if (!isValidSignature) {
            result.put("message", "Chữ ký VNPay không hợp lệ");
            return ResponseEntity.ok(result);
        }

        if (orderId == null) {
            result.put("message", "Không tìm thấy mã đơn hàng trong VNPay callback");
            return ResponseEntity.ok(result);
        }

        lockOrderRow(orderId);

        Order order = orderRepository.findById(orderId).orElse(null);

        if (order == null) {
            result.put("message", "Không tìm thấy đơn hàng");
            return ResponseEntity.ok(result);
        }

        boolean validAmount = isValidVnpayAmount(order, vnpAmount);
        boolean successFromVnpay = "00".equals(responseCode);
        boolean canMarkSuccess = successFromVnpay && validAmount && isVnpayOrder(order);

        if (order.getStatus() != null && order.getStatus() == ORDER_STATUS_PENDING) {
            if (canMarkSuccess) {
                processPaymentSuccess(order);
                log.info("[VNPay Return] Đơn #{} cập nhật thành công.", orderId);
            } else if (!successFromVnpay) {
                processPaymentFailure(order, responseCode);
                log.info("[VNPay Return] Đơn #{} thanh toán thất bại. ResponseCode={}", orderId, responseCode);
            } else {
                log.warn("[VNPay Return] Không cập nhật đơn #{} vì dữ liệu callback không hợp lệ.", orderId);
            }
        }

        Order freshOrder = orderRepository.findById(orderId).orElse(order);

        boolean success = isPaymentSuccessStatus(freshOrder);

        result.put("success", success);

        if (success
                && isOnlineOrder(freshOrder)
                && freshOrder.getStatus() != null
                && freshOrder.getStatus() == ORDER_STATUS_PENDING
                && Boolean.TRUE.equals(freshOrder.getIsPaymentReported())) {
            result.put("message", "Thanh toán thành công. Đơn hàng đang chờ xác nhận.");
        } else {
            result.put("message", success ? "Thanh toán thành công" : "Thanh toán chưa hoàn tất");
        }

        appendOrderDetailToResult(result, freshOrder, vnpAmount);

        return ResponseEntity.ok(result);
    }

    private Integer extractOrderId(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return null;
        }

        Integer orderIdFromTxnRef = vnPayService.extractOrderIdFromTxnRef(params.get("vnp_TxnRef"));

        if (orderIdFromTxnRef != null) {
            return orderIdFromTxnRef;
        }

        /*
         * Fallback giữ logic cũ:
         * vnp_OrderInfo = "Thanh toan don hang 132"
         */
        return parseOrderIdFromOrderInfo(params.get("vnp_OrderInfo"));
    }

    private Integer parseOrderIdFromOrderInfo(String orderInfo) {
        try {
            if (orderInfo == null || orderInfo.trim().isBlank()) {
                return null;
            }

            String cleaned = orderInfo.trim();

            /*
             * Giữ tương thích với OrderInfo cũ.
             * Lấy cụm số đầu tiên trong chuỗi.
             */
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("(\\d+)")
                    .matcher(cleaned);

            if (!matcher.find()) {
                return null;
            }

            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            log.error("Không thể ép kiểu orderId từ chuỗi OrderInfo: {}", orderInfo);
            return null;
        }
    }

    private boolean isPaymentSuccessStatus(Order order) {
        if (order == null || order.getStatus() == null) {
            return false;
        }

        /*
         * ONLINE:
         * - Thanh toán VNPay thành công KHÔNG đồng nghĩa với shop đã xác nhận đơn.
         * - Đơn vẫn PENDING = 0 và đánh dấu isPaymentReported = true.
         * - Sau đó admin xác nhận đơn mới FEFO + SALE_OUT và chuyển CONFIRMED = 1.
         *
         * Nếu admin đã xác nhận/hoàn thành sau đó thì vẫn là một giao dịch
         * thanh toán thành công, miễn isPaymentReported đã được ghi nhận.
         */
        if (isOnlineOrder(order)) {
            return Boolean.TRUE.equals(order.getIsPaymentReported())
                    && (
                    order.getStatus() == ORDER_STATUS_PENDING
                            || order.getStatus() == ORDER_STATUS_CONFIRMED
                            || order.getStatus() == ORDER_STATUS_COMPLETED
            );
        }

        /*
         * POS/MIXED giữ nguyên nghiệp vụ cũ:
         * thanh toán thành công có thể chuyển trực tiếp COMPLETED.
         */
        return order.getStatus() == ORDER_STATUS_CONFIRMED
                || order.getStatus() == ORDER_STATUS_COMPLETED;
    }

    private boolean isVnpayOrder(Order order) {
        if (order == null || order.getPaymentMethod() == null) {
            return false;
        }

        String method = order.getPaymentMethod().trim().toUpperCase();

        return PAYMENT_VNPAY.equals(method)
                || PAYMENT_MIXED.equals(method)
                || PAYMENT_MIXED_VNPAY.equals(method);
    }

    private boolean isMixedOrder(Order order) {
        if (order == null || order.getPaymentMethod() == null) {
            return false;
        }

        String method = order.getPaymentMethod().trim().toUpperCase();

        return PAYMENT_MIXED.equals(method)
                || PAYMENT_MIXED_VNPAY.equals(method);
    }

    private boolean isFullVnpayOrder(Order order) {
        return order != null
                && order.getPaymentMethod() != null
                && PAYMENT_VNPAY.equalsIgnoreCase(order.getPaymentMethod().trim());
    }

    private String toResponsePaymentMethod(String paymentMethod) {
        String method = paymentMethod == null
                ? ""
                : paymentMethod.trim().toUpperCase();

        if (PAYMENT_MIXED.equals(method)
                || PAYMENT_MIXED_VNPAY.equals(method)
                || PAYMENT_MIXED_VIETQR.equals(method)) {
            return PAYMENT_MIXED;
        }

        return method;
    }

    private String resolveTransferProviderFromPaymentMethod(String paymentMethod) {
        String method = paymentMethod == null
                ? ""
                : paymentMethod.trim().toUpperCase();

        if (PAYMENT_VNPAY.equals(method)
                || PAYMENT_MIXED.equals(method)
                || PAYMENT_MIXED_VNPAY.equals(method)) {
            return TRANSFER_PROVIDER_VNPAY;
        }

        if (PAYMENT_MIXED_VIETQR.equals(method)) {
            return TRANSFER_PROVIDER_VIETQR;
        }

        return null;
    }

    private boolean isOnlineOrder(Order order) {
        return order != null
                && order.getOrderType() != null
                && ORDER_TYPE_ONLINE.equalsIgnoreCase(order.getOrderType().trim());
    }

    /**
     * VNPay trả amount theo đơn vị *100.
     * <p>
     * VNPAY:
     * - VNPay phải bằng đúng finalAmount.
     * <p>
     * MIXED:
     * - VNPay chỉ là phần còn thiếu.
     * - Không thêm DB nên không có chỗ lưu cashGiven.
     * - Giữ logic cũ: chỉ kiểm tra amount VNPay > 0 và < finalAmount.
     */
    private boolean isValidVnpayAmount(Order order, String rawVnpAmount) {
        try {
            if (order == null || rawVnpAmount == null || rawVnpAmount.trim().isBlank()) {
                return false;
            }

            BigDecimal finalAmount = order.getFinalAmount() != null
                    ? order.getFinalAmount()
                    : BigDecimal.ZERO;

            if (finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return false;
            }

            long receivedAmount = Long.parseLong(rawVnpAmount);
            long finalAmountInVnpUnit = vnPayService.toVnpayAmountUnit(finalAmount);

            if (isFullVnpayOrder(order)) {
                return receivedAmount == finalAmountInVnpUnit;
            }

            if (isMixedOrder(order)) {
                return receivedAmount > 0 && receivedAmount < finalAmountInVnpUnit;
            }

            return false;
        } catch (Exception e) {
            log.error("Không kiểm tra được số tiền VNPay. rawVnpAmount={}", rawVnpAmount, e);
            return false;
        }
    }

    private void appendOrderDetailToResult(
            Map<String, Object> result,
            Order order,
            String rawVnpAmount
    ) {
        BigDecimal finalAmount = order.getFinalAmount() != null
                ? order.getFinalAmount()
                : BigDecimal.ZERO;

        BigDecimal totalAmount = order.getTotalAmount() != null
                ? order.getTotalAmount()
                : finalAmount;

        BigDecimal discountAmount = order.getDiscountAmount() != null
                ? order.getDiscountAmount()
                : BigDecimal.ZERO;

        BigDecimal vnpayPaidAmount = vnPayService.parseVnpayAmountToVnd(rawVnpAmount);

        BigDecimal cashGiven = BigDecimal.ZERO;
        BigDecimal transferAmount = BigDecimal.ZERO;
        BigDecimal paidAmount = BigDecimal.ZERO;
        BigDecimal remainingAmount = finalAmount;

        if (isFullVnpayOrder(order)) {
            transferAmount = finalAmount;

            if (isPaymentSuccessStatus(order)) {
                paidAmount = finalAmount;
                remainingAmount = BigDecimal.ZERO;
            }
        }

        if (isMixedOrder(order)) {
            transferAmount = vnpayPaidAmount;
            cashGiven = finalAmount.subtract(transferAmount);

            if (cashGiven.compareTo(BigDecimal.ZERO) < 0) {
                cashGiven = BigDecimal.ZERO;
            }

            if (isPaymentSuccessStatus(order)) {
                paidAmount = finalAmount;
                remainingAmount = BigDecimal.ZERO;
            } else {
                paidAmount = cashGiven;
                remainingAmount = transferAmount;
            }
        }

        result.put("orderId", order.getId());
        result.put("status", order.getStatus());
        result.put("statusText", getStatusText(order.getStatus()));
        result.put("orderType", order.getOrderType());
        result.put("paymentMethod", toResponsePaymentMethod(order.getPaymentMethod()));
        result.put("transferProvider", resolveTransferProviderFromPaymentMethod(order.getPaymentMethod()));

        result.put("totalAmount", totalAmount);
        result.put("discountAmount", discountAmount);
        result.put("finalAmount", finalAmount);

        result.put("paidAmount", paidAmount);
        result.put("remainingAmount", remainingAmount);
        result.put("cashGiven", cashGiven);
        result.put("transferAmount", transferAmount);
        result.put("changeAmount", BigDecimal.ZERO);

        result.put("customerName", order.getCustomerName());
        result.put("customerPhone", order.getCustomerPhone());
        result.put("createdAt", order.getCreatedAt() != null ? order.getCreatedAt().toString() : "");

        String customerEmail = "";

        if (order.getCustomer() != null && order.getCustomer().getUser() != null) {
            customerEmail = order.getCustomer().getUser().getEmail();
        }

        result.put("customerEmail", customerEmail);

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
        List<Map<String, Object>> itemsList = new ArrayList<>();

        for (OrderItem oi : orderItems) {
            Map<String, Object> itemMap = new HashMap<>();

            ProductVariant variant = oi.getProductVariant();

            String productName = "Sản phẩm";
            String sku = "";
            String capacity = "";
            String bottle = "";

            if (variant != null) {
                sku = variant.getSku();

                if (variant.getProduct() != null) {
                    productName = variant.getProduct().getName();
                }

                if (variant.getCapacity() != null && variant.getCapacity().getValue() != null) {
                    capacity = formatCapacityValue(variant.getCapacity().getValue());
                }

                if (variant.getBottleType() != null) {
                    bottle = variant.getBottleType().getName();
                }
            }

            Integer quantity = oi.getQuantity() != null
                    ? oi.getQuantity()
                    : 0;

            BigDecimal originalUnitPrice = oi.getOriginalPrice() != null
                    ? oi.getOriginalPrice()
                    : BigDecimal.ZERO;

            BigDecimal discountPerUnit = oi.getDiscountAmount() != null
                    ? oi.getDiscountAmount()
                    : BigDecimal.ZERO;

            BigDecimal finalUnitPrice = resolveFinalUnitPrice(oi, originalUnitPrice);
            BigDecimal lineTotal = resolveLineTotal(oi, quantity, originalUnitPrice, finalUnitPrice);

            itemMap.put("productName", productName);
            itemMap.put("sku", sku);
            itemMap.put("variantName", buildVariantName(capacity, bottle));
            itemMap.put("capacityLabel", capacity);
            itemMap.put("bottleTypeName", bottle);

            itemMap.put("quantity", quantity);
            itemMap.put("originalPrice", originalUnitPrice);
            itemMap.put("discountAmount", discountPerUnit);
            itemMap.put("finalPrice", finalUnitPrice);

            itemMap.put("price", finalUnitPrice);
            itemMap.put("unitPrice", finalUnitPrice);
            itemMap.put("lineTotal", lineTotal);

            itemsList.add(itemMap);
        }

        result.put("items", itemsList);
    }

    private BigDecimal resolveFinalUnitPrice(OrderItem item, BigDecimal originalUnitPrice) {
        if (item == null || item.getFinalPrice() == null) {
            return originalUnitPrice != null ? originalUnitPrice : BigDecimal.ZERO;
        }

        BigDecimal finalPrice = item.getFinalPrice();

        /*
         * Flow mới:
         * OrderItem.finalPrice là giá cuối / 1 sản phẩm.
         *
         * Flow cũ có thể lưu finalPrice là line total.
         * Hàm lineTotal bên dưới sẽ xử lý fallback.
         */
        return finalPrice;
    }

    private BigDecimal resolveLineTotal(
            OrderItem item,
            Integer quantity,
            BigDecimal originalUnitPrice,
            BigDecimal finalUnitPrice
    ) {
        int safeQuantity = quantity != null ? quantity : 0;

        if (safeQuantity <= 0) {
            return BigDecimal.ZERO;
        }

        if (item == null || item.getFinalPrice() == null) {
            return originalUnitPrice.multiply(BigDecimal.valueOf(safeQuantity));
        }

        BigDecimal rawFinalPrice = item.getFinalPrice();

        /*
         * Nếu quantity > 1 và finalPrice <= originalUnitPrice,
         * gần như chắc chắn finalPrice là giá / 1 sản phẩm theo flow mới.
         */
        if (safeQuantity > 1
                && originalUnitPrice != null
                && originalUnitPrice.compareTo(BigDecimal.ZERO) > 0
                && rawFinalPrice.compareTo(originalUnitPrice) <= 0) {
            return rawFinalPrice.multiply(BigDecimal.valueOf(safeQuantity));
        }

        /*
         * Với quantity = 1 thì unit price = line total.
         * Với flow cũ nếu finalPrice đã là line total thì giữ nguyên.
         */
        if (safeQuantity == 1) {
            return rawFinalPrice;
        }

        return rawFinalPrice;
    }

    private String formatCapacityValue(Object value) {
        if (value == null) {
            return "";
        }

        String rawText = String.valueOf(value).trim();

        if (rawText.isEmpty()) {
            return "";
        }

        String lower = rawText.toLowerCase().replace("ml", "").trim();

        try {
            BigDecimal number = new BigDecimal(lower);

            number = number.stripTrailingZeros();

            return number.toPlainString() + "ml";
        } catch (Exception ignored) {
            if (rawText.toLowerCase().contains("ml")) {
                return rawText.replace(" ", "");
            }

            return rawText + "ml";
        }
    }

    private String buildVariantName(String capacity, String bottle) {
        if ((capacity == null || capacity.isBlank()) && (bottle == null || bottle.isBlank())) {
            return "";
        }

        if (capacity == null || capacity.isBlank()) {
            return bottle;
        }

        if (bottle == null || bottle.isBlank()) {
            return capacity;
        }

        return capacity + " - " + bottle;
    }

    private void processPaymentSuccess(Order order) {
        if (order == null || order.getStatus() == null || order.getStatus() != ORDER_STATUS_PENDING) {
            return;
        }

        /*
         * ONLINE:
         * VNPay success chỉ có nghĩa là khách ĐÃ THANH TOÁN.
         * Không được tự xác nhận đơn và không được xuất kho tại callback payment.
         *
         * Flow chuẩn:
         * PENDING + isPaymentReported=false
         *   -> VNPay success
         * PENDING + isPaymentReported=true
         *   -> admin xác nhận
         * FEFO + SALE_OUT + CONFIRMED.
         */
        if (isOnlineOrder(order)) {
            if (Boolean.TRUE.equals(order.getIsPaymentReported())) {
                log.info(
                        "[VNPay] Đơn ONLINE #{} đã được ghi nhận thanh toán trước đó, giữ trạng thái Chờ xác nhận.",
                        order.getId()
                );
                return;
            }

            order.setStatus(ORDER_STATUS_PENDING);
            order.setIsPaymentReported(true);
            orderRepository.save(order);

            log.info(
                    "[VNPay] Đơn ONLINE #{} thanh toán thành công, giữ trạng thái Chờ xác nhận.",
                    order.getId()
            );
            return;
        }

        /*
         * POS / MIXED:
         * Thanh toán xong thì hoàn thành đơn như nghiệp vụ hiện tại.
         * Voucher đã được reserve từ lúc POS bắt đầu checkout/payment pending,
         * callback thành công tuyệt đối không tăng usedCount lần 2.
         */
        order.setStatus(ORDER_STATUS_COMPLETED);
        order.setCompletedAt(LocalDateTime.now());

        applyLoyaltyPointsIfNeeded(order);

        orderRepository.save(order);
    }

    private void applyLoyaltyPointsIfNeeded(Order order) {
        if (order == null || order.getCustomer() == null) {
            return;
        }

        if (Boolean.TRUE.equals(order.getLoyaltyPointsApplied())) {
            return;
        }

        Customer customer = order.getCustomer();

        int currentPoints = customer.getLoyaltyPoints() != null
                ? customer.getLoyaltyPoints()
                : 0;

        BigDecimal finalAmount = order.getFinalAmount() != null
                ? order.getFinalAmount()
                : BigDecimal.ZERO;

        int pointsEarned = finalAmount
                .divide(POINT_RATE_AMOUNT, 0, RoundingMode.DOWN)
                .intValue();

        pointsEarned = Math.max(pointsEarned, 0);

        customer.setLoyaltyPoints(currentPoints + pointsEarned);
        updateCustomerRank(customer);
        customerRepository.save(customer);

        order.setLoyaltyPointsApplied(true);
        order.setLoyaltyPointsEarned(pointsEarned);
    }

    private void updateCustomerRank(Customer customer) {
        if (customer == null) {
            return;
        }

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

    private void processPaymentFailure(Order order, String responseCode) {
        if (order == null || order.getStatus() == null || order.getStatus() != ORDER_STATUS_PENDING) {
            return;
        }

        /*
         * ONLINE đã được VNPay ghi nhận thanh toán thành công vẫn giữ PENDING
         * để chờ admin xác nhận. Nếu Return/IPN lặp hoặc đến khác thứ tự với
         * response lỗi thì tuyệt đối không được hủy một đơn đã trả tiền.
         */
        if (isOnlineOrder(order) && Boolean.TRUE.equals(order.getIsPaymentReported())) {
            log.warn(
                    "[VNPay] Bỏ qua callback thất bại cho đơn ONLINE #{} vì giao dịch đã được ghi nhận thanh toán.",
                    order.getId()
            );
            return;
        }

        /*
         * ONLINE khi còn PENDING chưa SALE_OUT InventoryLot, vì vậy payment fail
         * tuyệt đối không được cộng kho.
         *
         * POS/IN_STORE thì ngược lại: checkout đã SALE_OUT theo FEFO để giữ hàng,
         * nên phải RETURN_IN đúng các lot đã xuất thông qua PosService.
         */
        if (!isOnlineOrder(order)) {
            posService.restoreStockAfterVnpayFailure(order.getId());
        }

        order.setStatus(ORDER_STATUS_CANCELLED);
        orderRepository.save(order);

        /*
         * Cả ONLINE và POS/MIXED đều reserve Voucher trước khi chờ VNPay.
         * Payment fail/cancel phải hoàn đúng 1 lượt.
         * Order row đã được khóa nên callback lặp không thể hoàn hai lần.
         */
        restoreVoucherUsage(order);

        log.info("[VNPay] Đơn #{} đã hủy do thanh toán thất bại. ResponseCode={}",
                order.getId(), responseCode);
    }

    /**
     * Khóa row Orders trong transaction callback hiện tại.
     *
     * VNPay có thể gọi IPN và Return gần như đồng thời. Khóa này đảm bảo chỉ
     * một callback xử lý trạng thái/kho của cùng một order tại một thời điểm,
     * tránh SALE_OUT hoặc RETURN_IN hai lần.
     */
    private void lockOrderRow(Integer orderId) {
        if (orderId == null) {
            return;
        }

        jdbcTemplate.queryForList(
                "SELECT Id FROM dbo.[Orders] WITH (UPDLOCK, ROWLOCK, HOLDLOCK) WHERE Id = ?",
                Integer.class,
                orderId
        );
    }

    private void restoreVoucherUsage(Order order) {
        if (order == null
                || order.getVoucher() == null
                || order.getVoucher().getId() == null) {
            return;
        }

        Voucher lockedVoucher = voucherRepository
                .findByIdForUpdate(order.getVoucher().getId())
                .orElse(null);

        if (lockedVoucher == null) {
            return;
        }

        int usedCount = lockedVoucher.getUsedCount() != null
                ? lockedVoucher.getUsedCount()
                : 0;

        if (usedCount > 0) {
            lockedVoucher.setUsedCount(usedCount - 1);
            voucherRepository.save(lockedVoucher);
        }
    }

    private String getStatusText(Integer status) {
        if (status == null) {
            return "Không xác định";
        }

        return switch (status) {
            case ORDER_STATUS_PENDING -> "Chờ xác nhận";
            case ORDER_STATUS_CONFIRMED -> "Đã xác nhận";
            case ORDER_STATUS_COMPLETED -> "Hoàn thành";
            case ORDER_STATUS_CANCELLED -> "Đã hủy";
            default -> "Không xác định";
        };
    }
}