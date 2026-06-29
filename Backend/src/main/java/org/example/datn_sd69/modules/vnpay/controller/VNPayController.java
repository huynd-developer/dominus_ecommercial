package org.example.datn_sd69.modules.vnpay.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.datn_sd69.entity.*;
import org.example.datn_sd69.modules.vnpay.service.VNPayService;
import org.example.datn_sd69.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/vnpay")
@RequiredArgsConstructor
public class VNPayController {

    private final VNPayService vnPayService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository variantRepository;
    private final CustomerRepository customerRepository;
    private final VoucherRepository voucherRepository; // Đã thêm để phục vụ việc Hoàn Voucher

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
        String orderInfo    = params.get("vnp_OrderInfo");
        String vnpAmount    = params.get("vnp_Amount");

        Integer orderId;
        try {
            orderId = Integer.parseInt(orderInfo.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            log.error("[VNPay IPN] Không parse được orderId từ chuỗi: {}", orderInfo);
            return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order Not Found"));
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.error("[VNPay IPN] Không tìm thấy đơn hàng ID: {}", orderId);
            return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order Not Found"));
        }

        // Idempotency: Đơn đã xử lý rồi thì không xử lý lại
        if (order.getStatus() != 0) {
            log.info("[VNPay IPN] Đơn #{} đã được xử lý trước đó. Status={}", orderId, order.getStatus());
            return ResponseEntity.ok(Map.of("RspCode", "02", "Message", "Order Already Confirmed"));
        }

        long expectedAmount = order.getFinalAmount().multiply(BigDecimal.valueOf(100)).longValue();
        long receivedAmount = Long.parseLong(vnpAmount);

        if (expectedAmount != receivedAmount) {
            log.warn("[VNPay IPN] Số tiền không khớp. Expected={}, Received={}", expectedAmount, receivedAmount);
            return ResponseEntity.ok(Map.of("RspCode", "04", "Message", "Invalid Amount"));
        }

        if ("00".equals(responseCode)) {
            // ==========================================
            // LUỒNG THÀNH CÔNG
            // ==========================================
            order.setStatus(3);
            orderRepository.save(order);

            // Chỉ cần cộng điểm Loyalty (Vì kho và voucher đã được giữ từ lúc tạo đơn)
            if (order.getCustomer() != null) {
                Customer customer = order.getCustomer();
                int points = order.getFinalAmount()
                        .divide(BigDecimal.valueOf(10_000), 0, RoundingMode.DOWN)
                        .intValue();
                if (points > 0) {
                    customer.setLoyaltyPoints(customer.getLoyaltyPoints() + points);
                    customerRepository.save(customer);
                }
            }

            log.info("[VNPay IPN] Đơn #{} thanh toán thành công. Đã ghi nhận hoàn tất.", orderId);

        } else {
            // ==========================================
            // LUỒNG THẤT BẠI HOẶC BỊ HỦY -> ROLLBACK LẠI DATA
            // ==========================================
            order.setStatus(4); // Status 4 = Đã huỷ
            orderRepository.save(order);

            // 1. Rollback Kho (Hoàn trả lại số lượng)
            List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
            for (OrderItem oi : items) {
                ProductVariant variant = oi.getProductVariant();
                if (variant != null) {
                    variant.setStockQuantity(variant.getStockQuantity() + oi.getQuantity());
                    variantRepository.save(variant);
                }
            }

            // 2. Rollback Voucher (Trả lại 1 lượt dùng)
            if (order.getVoucher() != null) {
                Voucher voucher = order.getVoucher();
                voucher.setUsedCount(Math.max(0, voucher.getUsedCount() - 1));
                voucherRepository.save(voucher);
            }

            log.info("[VNPay IPN] Đơn #{} thất bại/hủy. Đã hoàn trả kho và voucher (Rollback). ResponseCode={}", orderId, responseCode);
        }

        return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
    }

    @GetMapping("/return")
    public ResponseEntity<Map<String, Object>> handleReturn(@RequestParam Map<String, String> params) {
        Map<String, String> paramsCopy = new HashMap<>(params);
        boolean isValid = vnPayService.verifyCallback(paramsCopy);
        String responseCode = params.get("vnp_ResponseCode");

        Map<String, Object> result = new HashMap<>();
        result.put("success",       isValid && "00".equals(responseCode));
        result.put("responseCode",  responseCode);
        result.put("orderInfo",     params.get("vnp_OrderInfo"));
        result.put("amount",        params.get("vnp_Amount"));
        result.put("transactionNo", params.get("vnp_TransactionNo"));
        result.put("bankCode",      params.get("vnp_BankCode"));
        result.put("payDate",       params.get("vnp_PayDate"));

        return ResponseEntity.ok(result);
    }
}