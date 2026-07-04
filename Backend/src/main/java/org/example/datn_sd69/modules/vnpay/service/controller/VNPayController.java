package org.example.datn_sd69.modules.vnpay.service.controller;

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
        String orderInfo    = params.get("vnp_OrderInfo");
        String vnpAmount    = params.get("vnp_Amount");

        Integer orderId = parseOrderId(orderInfo);
        if (orderId == null) {
            return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order Not Found"));
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order Not Found"));
        }

        // Kiểm tra trùng lặp (Idempotency)
        if (order.getStatus() != 0) {
            log.info("[VNPay IPN] Đơn #{} đã được xử lý trước đó. Status={}", orderId, order.getStatus());
            return ResponseEntity.ok(Map.of("RspCode", "02", "Message", "Order Already Confirmed"));
        }

        long expectedAmount = order.getFinalAmount().multiply(BigDecimal.valueOf(100)).longValue();
        long receivedAmount = Long.parseLong(vnpAmount);

        if (expectedAmount != receivedAmount) {
            return ResponseEntity.ok(Map.of("RspCode", "04", "Message", "Invalid Amount"));
        }

        if ("00".equals(responseCode)) {
            processPaymentSuccess(order);
            log.info("[VNPay IPN] Đơn #{} thanh toán thành công qua IPN.", orderId);
        } else {
            processPaymentFailure(order, responseCode);
            log.info("[VNPay IPN] Đơn #{} thất bại qua IPN.", orderId);
        }

        return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
    }

    @GetMapping("/return")
    @Transactional // Đảm bảo tính nhất quán dữ liệu khi đọc ghi đồng thời
    public ResponseEntity<Map<String, Object>> handleReturn(@RequestParam Map<String, String> params) {
        log.info("[VNPay Return] Trình duyệt chuyển hướng về params: {}", params);

        Map<String, String> paramsCopy = new HashMap<>(params);
        boolean isValid = vnPayService.verifyCallback(paramsCopy);
        String responseCode = params.get("vnp_ResponseCode");
        String orderInfo = params.get("vnp_OrderInfo");

        boolean isSuccess = isValid && "00".equals(responseCode);
        Integer orderId = parseOrderId(orderInfo);

        // Khởi tạo Map kết quả trả về cho Frontend
        Map<String, Object> result = new HashMap<>();
        result.put("success",       isSuccess);
        result.put("responseCode",  responseCode);
        result.put("orderInfo",     orderInfo);
        result.put("amount",        params.get("vnp_Amount"));
        result.put("transactionNo", params.get("vnp_TransactionNo"));
        result.put("bankCode",      params.get("vnp_BankCode"));
        result.put("payDate",       params.get("vnp_PayDate"));

        if (orderId != null) {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order != null) {
                // 1. Chỉ xử lý cập nhật trạng thái & cộng điểm nếu đơn hàng vẫn đang ở trạng thái CHỜ THANH TOÁN (Status = 0)
                // Trường hợp IPN đã chạy trước và đổi Status thành 3 rồi thì bỏ qua bước này để tránh trùng lặp
                if (order.getStatus() == 0) {
                    if (isSuccess) {
                        processPaymentSuccess(order);
                        log.info("[VNPay Return] Đơn #{} cập nhật THÀNH CÔNG và CỘNG ĐIỂM tại màn hình Return.", orderId);
                    } else {
                        processPaymentFailure(order, responseCode);
                        log.info("[VNPay Return] Đơn #{} thanh toán THẤT BẠI, đã tiến hành rollback dữ liệu.", orderId);
                    }
                }

                // 2. BỔ SUNG QUAN TRỌNG: Đổ dữ liệu chi tiết đơn hàng vào kết quả trả về cho Frontend in hóa đơn
                result.put("customerName", order.getCustomerName()); // Sẽ lấy được tên khách đã lưu lúc checkout
                result.put("createdAt", order.getCreatedAt() != null ? order.getCreatedAt().toString() : "");

                // Lấy danh sách sản phẩm thực tế của đơn hàng (hàm findByOrderId đã có sẵn trong Project của bạn)
                List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
                List<Map<String, Object>> itemsList = new java.util.ArrayList<>();

                for (OrderItem oi : orderItems) {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("productName", oi.getProductVariant().getProduct().getName());

                    // Khớp chuẩn với thuộc tính `item.variantName` mà Frontend Vue đang gọi để hiển thị dung tích/loại chai
                    String capacity = oi.getProductVariant().getCapacity() != null ? oi.getProductVariant().getCapacity().getValue() + " ml" : "";
                    String bottle = oi.getProductVariant().getBottleType() != null ? oi.getProductVariant().getBottleType().getName() : "";
                    itemMap.put("variantName", capacity + " - " + bottle);

                    itemMap.put("quantity", oi.getQuantity());
                    itemMap.put("price", oi.getOriginalPrice()); // Frontend sẽ tự nhân: item.price * item.quantity

                    itemsList.add(itemMap);
                }
                result.put("items", itemsList); // Trả mảng sản phẩm về cho v-for bên Vue lặp ra
            }
        }

        return ResponseEntity.ok(result);
    }

    // --- CÁC HÀM TRỢ GIÚP DÙNG CHUNG CHO CẢ IPN VÀ RETURN ---

    private Integer parseOrderId(String orderInfo) {
        try {
            if (orderInfo == null) return null;
            return Integer.parseInt(orderInfo.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            log.error("Không thể ép kiểu orderId từ chuỗi OrderInfo: {}", orderInfo);
            return null;
        }
    }

    private void processPaymentSuccess(Order order) {
        order.setStatus(3); // 3 = COMPLETED
        orderRepository.save(order);

        // Cộng điểm tích lũy cho khách thành viên
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
    }

    private void processPaymentFailure(Order order, String responseCode) {
        order.setStatus(4); // 4 = CANCELLED
        orderRepository.save(order);

        // 1. Hoàn trả lại số lượng tồn kho vật lý
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        for (OrderItem oi : items) {
            ProductVariant variant = oi.getProductVariant();
            if (variant != null) {
                variant.setStockQuantity(variant.getStockQuantity() + oi.getQuantity());
                variantRepository.save(variant);
            }
        }

        // 2. Hoàn lại lượt sử dụng voucher
        if (order.getVoucher() != null) {
            Voucher voucher = order.getVoucher();
            voucher.setUsedCount(Math.max(0, voucher.getUsedCount() - 1));
            voucherRepository.save(voucher);
        }
    }
}