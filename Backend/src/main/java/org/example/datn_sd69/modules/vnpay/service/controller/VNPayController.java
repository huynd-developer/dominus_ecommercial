package org.example.datn_sd69.modules.vnpay.service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.datn_sd69.entity.Customer;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.entity.Voucher;
import org.example.datn_sd69.modules.vnpay.service.VNPayService;
import org.example.datn_sd69.repository.CustomerRepository;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.example.datn_sd69.repository.OrderRepository;
import org.example.datn_sd69.repository.ProductVariantRepository;
import org.example.datn_sd69.repository.VoucherRepository;
import org.springframework.http.ResponseEntity;
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
    private static final int ORDER_STATUS_COMPLETED = 3;
    private static final int ORDER_STATUS_CANCELLED = 4;

    private static final String PAYMENT_VNPAY = "VNPAY";
    private static final String PAYMENT_MIXED = "MIXED";

    private static final BigDecimal POINT_RATE_AMOUNT = BigDecimal.valueOf(10_000);

    private final VNPayService vnPayService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository variantRepository;
    private final CustomerRepository customerRepository;
    private final VoucherRepository voucherRepository;

    @GetMapping("/ipn")
    @Transactional
    public ResponseEntity<Map<String, String>> handleIpn(@RequestParam Map<String, String> params) {
        log.info("[VNPay IPN] Nhận callback params: {}", params);

        Map<String, String> paramsCopy = new HashMap<>(params);
        boolean isValidSignature = vnPayService.verifyCallback(paramsCopy);

        if (!isValidSignature) {
            log.warn("[VNPay IPN] Chữ ký không hợp lệ!");
            return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid Checksum"));
        }

        String responseCode = params.get("vnp_ResponseCode");
        String orderInfo = params.get("vnp_OrderInfo");
        String vnpAmount = params.get("vnp_Amount");

        Integer orderId = parseOrderId(orderInfo);

        if (orderId == null) {
            return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order Not Found"));
        }

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

    @GetMapping("/return")
    @Transactional
    public ResponseEntity<Map<String, Object>> handleReturn(@RequestParam Map<String, String> params) {
        log.info("[VNPay Return] Trình duyệt chuyển hướng về params: {}", params);

        Map<String, String> paramsCopy = new HashMap<>(params);
        boolean isValidSignature = vnPayService.verifyCallback(paramsCopy);

        String responseCode = params.get("vnp_ResponseCode");
        String orderInfo = params.get("vnp_OrderInfo");
        String vnpAmount = params.get("vnp_Amount");

        Integer orderId = parseOrderId(orderInfo);

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("responseCode", responseCode);
        result.put("orderInfo", orderInfo);
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

        result.put("success", freshOrder.getStatus() != null && freshOrder.getStatus() == ORDER_STATUS_COMPLETED);
        result.put("message", result.get("success").equals(true) ? "Thanh toán thành công" : "Thanh toán chưa hoàn tất");

        appendOrderDetailToResult(result, freshOrder, vnpAmount);

        return ResponseEntity.ok(result);
    }

    private Integer parseOrderId(String orderInfo) {
        try {
            if (orderInfo == null || orderInfo.trim().isBlank()) {
                return null;
            }

            return Integer.parseInt(orderInfo.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            log.error("Không thể ép kiểu orderId từ chuỗi OrderInfo: {}", orderInfo);
            return null;
        }
    }

    private boolean isVnpayOrder(Order order) {
        if (order == null || order.getPaymentMethod() == null) {
            return false;
        }

        String method = order.getPaymentMethod().trim().toUpperCase();

        return PAYMENT_VNPAY.equals(method) || PAYMENT_MIXED.equals(method);
    }

    private boolean isMixedOrder(Order order) {
        return order != null
                && order.getPaymentMethod() != null
                && PAYMENT_MIXED.equalsIgnoreCase(order.getPaymentMethod().trim());
    }

    private boolean isFullVnpayOrder(Order order) {
        return order != null
                && order.getPaymentMethod() != null
                && PAYMENT_VNPAY.equalsIgnoreCase(order.getPaymentMethod().trim());
    }

    /**
     * VNPay trả amount theo đơn vị *100.
     *
     * VNPAY:
     * - VNPay phải bằng đúng finalAmount.
     *
     * MIXED:
     * - VNPay chỉ là phần còn thiếu.
     * - Vì không thêm DB nên không có chỗ lưu cashGiven.
     * - Chỉ kiểm tra amount VNPay > 0 và < finalAmount.
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
            long finalAmountInVnpUnit = toVnpAmountUnit(finalAmount);

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

    private long toVnpAmountUnit(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }

        return amount
                .setScale(0, RoundingMode.DOWN)
                .multiply(BigDecimal.valueOf(100))
                .longValue();
    }

    private BigDecimal parseVnpAmountToVnd(String rawVnpAmount) {
        try {
            if (rawVnpAmount == null || rawVnpAmount.trim().isBlank()) {
                return BigDecimal.ZERO;
            }

            return new BigDecimal(rawVnpAmount)
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
        } catch (Exception e) {
            return BigDecimal.ZERO;
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

        BigDecimal vnpayPaidAmount = parseVnpAmountToVnd(rawVnpAmount);

        BigDecimal cashGiven = BigDecimal.ZERO;
        BigDecimal transferAmount = BigDecimal.ZERO;
        BigDecimal paidAmount = BigDecimal.ZERO;
        BigDecimal remainingAmount = finalAmount;

        if (isFullVnpayOrder(order)) {
            transferAmount = finalAmount;

            if (order.getStatus() != null && order.getStatus() == ORDER_STATUS_COMPLETED) {
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

            if (order.getStatus() != null && order.getStatus() == ORDER_STATUS_COMPLETED) {
                paidAmount = finalAmount;
                remainingAmount = BigDecimal.ZERO;
            } else {
                paidAmount = cashGiven;
                remainingAmount = transferAmount;
            }
        }

        result.put("orderId", order.getId());
        result.put("status", order.getStatus());
        result.put("paymentMethod", order.getPaymentMethod());

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
                    capacity = variant.getCapacity().getValue() + " ml";
                }

                if (variant.getBottleType() != null) {
                    bottle = variant.getBottleType().getName();
                }
            }

            BigDecimal unitPrice = oi.getOriginalPrice() != null
                    ? oi.getOriginalPrice()
                    : BigDecimal.ZERO;

            Integer quantity = oi.getQuantity() != null
                    ? oi.getQuantity()
                    : 0;

            BigDecimal lineTotal = oi.getFinalPrice() != null
                    ? oi.getFinalPrice()
                    : unitPrice.multiply(BigDecimal.valueOf(quantity));

            itemMap.put("productName", productName);
            itemMap.put("sku", sku);
            itemMap.put("variantName", buildVariantName(capacity, bottle));
            itemMap.put("capacityLabel", capacity);
            itemMap.put("bottleTypeName", bottle);
            itemMap.put("quantity", quantity);
            itemMap.put("price", unitPrice);
            itemMap.put("unitPrice", unitPrice);
            itemMap.put("lineTotal", lineTotal);

            itemsList.add(itemMap);
        }

        result.put("items", itemsList);
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

        order.setStatus(ORDER_STATUS_COMPLETED);
        order.setCompletedAt(LocalDateTime.now());

        if (order.getCustomer() != null && !Boolean.TRUE.equals(order.getLoyaltyPointsApplied())) {
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

        if (order.getVoucher() != null) {
            Voucher voucher = order.getVoucher();

            int usedCount = voucher.getUsedCount() != null
                    ? voucher.getUsedCount()
                    : 0;

            voucher.setUsedCount(usedCount + 1);
            voucherRepository.save(voucher);
        }

        orderRepository.save(order);
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

        order.setStatus(ORDER_STATUS_CANCELLED);
        orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

        for (OrderItem oi : items) {
            ProductVariant variant = oi.getProductVariant();

            if (variant != null) {
                int currentStock = variant.getStockQuantity() != null
                        ? variant.getStockQuantity()
                        : 0;

                int quantity = oi.getQuantity() != null
                        ? oi.getQuantity()
                        : 0;

                variant.setStockQuantity(currentStock + quantity);
                variantRepository.save(variant);
            }
        }

        /*
         * Không trừ usedCount voucher ở đây.
         * Với VNPAY/MIXED, PosServiceImpl chưa tăng usedCount khi tạo đơn pending.
         * usedCount chỉ tăng trong processPaymentSuccess().
         */
        log.info("[VNPay] Đơn #{} đã hủy do thanh toán thất bại. ResponseCode={}",
                order.getId(), responseCode);
    }
}