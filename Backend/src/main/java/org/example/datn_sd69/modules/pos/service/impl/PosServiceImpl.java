package org.example.datn_sd69.modules.pos.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.*;
import org.example.datn_sd69.modules.pos.dto.*;
import org.example.datn_sd69.modules.pos.service.PosService;
import org.example.datn_sd69.modules.vnpay.service.VNPayService;
import org.example.datn_sd69.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PosServiceImpl implements PosService {

    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository productImageRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EmployeeRepository employeeRepository;
    private final VoucherRepository voucherRepository;
    private final VNPayService vnPayService;

    // ================================================================
    // 1. TÌM BIẾN THỂ THEO SKU — barcode scan hoặc tìm thủ công
    // ================================================================
    @Override
    public ProductVariantPosResponse findVariantBySku(String sku) {
        ProductVariant variant = variantRepository.findBySkuWithDetails(sku)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với SKU: " + sku));

        if (variant.getStatus() != 1) {
            throw new RuntimeException("Sản phẩm này hiện không được bán");
        }
        if (variant.getStockQuantity() <= 0) {
            throw new RuntimeException("Sản phẩm đã hết hàng");
        }

        Product product = variant.getProduct();

        // Lấy ảnh đại diện: ưu tiên IsPrimary=true, fallback ảnh đầu tiên
        String imageUrl = productImageRepository
                .findPrimaryByProductId(product.getId())
                .or(() -> productImageRepository.findFirstByProductId(product.getId()))
                .map(ProductImage::getImageUrl)
                .orElse(null);

        return ProductVariantPosResponse.builder()
                .variantId(variant.getId())
                .sku(variant.getSku())
                .productName(product.getName())
                .brandName(product.getBrand().getName())
                .capacityLabel(variant.getCapacity().getValue() + " ml")
                .bottleTypeName(variant.getBottleType().getName())
                .price(variant.getPrice())
                .stockQuantity(variant.getStockQuantity())
                .imageUrl(imageUrl)
                .build();
    }

    // ================================================================
    // 2. TÌM KHÁCH THEO SĐT
    // ================================================================
    @Override
    public CustomerPosResponse findCustomerByPhone(String phone) {
        return customerRepository.findByPhone(phone)
                .map(c -> CustomerPosResponse.builder()
                        .customerId(c.getUserId())
                        .name(c.getUser().getName())
                        .phone(c.getUser().getPhone())
                        .email(c.getUser().getEmail())
                        .customerRank(c.getCustomerRank())
                        .loyaltyPoints(c.getLoyaltyPoints())
                        .build())
                .orElse(null); // null = khách vãng lai
    }

    // ================================================================
    // 3. CHECKOUT — toàn bộ nghiệp vụ thanh toán POS
    // ================================================================
    @Override
    @Transactional
    public PosOrderResponse checkout(PosCheckoutRequest request, String cashierEmail, String clientIp) {

        // --- 3.1 Validate phương thức thanh toán ---
        String pm = request.getPaymentMethod().toUpperCase();
        if (!pm.equals("CASH") && !pm.equals("VNPAY")) {
            throw new RuntimeException("Phương thức thanh toán không hợp lệ. Chỉ chấp nhận: CASH hoặc VNPAY");
        }

        // --- 3.2 Lấy thu ngân từ JWT ---
        Employee cashier = null;
        if (cashierEmail != null && !cashierEmail.isBlank()) {
            cashier = employeeRepository.findByUserEmail(cashierEmail).orElse(null);
        }
        final Employee finalCashier = cashier;

        // --- 3.3 Tìm khách hàng theo SĐT (nullable) ---
        Customer customer = null;
        if (request.getCustomerPhone() != null && !request.getCustomerPhone().isBlank()) {
            customer = customerRepository.findByPhone(request.getCustomerPhone()).orElse(null);
        }
        final Customer finalCustomer = customer;

        // --- 3.4 Validate tồn kho & tính tổng tiền ---
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<LineItem> lineItems = new ArrayList<>();

        for (PosItemRequest item : request.getItems()) {
            ProductVariant variant = variantRepository.findBySkuWithDetails(item.getSku())
                    .orElseThrow(() -> new RuntimeException("SKU không hợp lệ: " + item.getSku()));

            if (variant.getStatus() != 1) {
                throw new RuntimeException("Sản phẩm [" + item.getSku() + "] hiện không được bán");
            }
            if (variant.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException(
                        "Sản phẩm [" + variant.getProduct().getName() + "] chỉ còn "
                                + variant.getStockQuantity() + " sản phẩm trong kho"
                );
            }

            BigDecimal lineTotal = variant.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
            lineItems.add(new LineItem(variant, item.getQuantity(), variant.getPrice(), lineTotal));
        }

        // --- 3.5 Áp dụng Voucher (nếu có) ---
        BigDecimal discountAmount = BigDecimal.ZERO;
        Voucher appliedVoucher = null;

        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            Voucher voucher = voucherRepository
                    .findValidByCode(request.getVoucherCode(), LocalDateTime.now())
                    .orElseThrow(() -> new RuntimeException(
                            "Voucher [" + request.getVoucherCode() + "] không hợp lệ hoặc đã hết hạn"
                    ));

            if (totalAmount.compareTo(voucher.getMinOrderValue()) < 0) {
                throw new RuntimeException(
                        "Đơn hàng tối thiểu " + voucher.getMinOrderValue() + " VND mới được dùng voucher này"
                );
            }

            if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {
                discountAmount = totalAmount
                        .multiply(voucher.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
                if (voucher.getMaxDiscount() != null && discountAmount.compareTo(voucher.getMaxDiscount()) > 0) {
                    discountAmount = voucher.getMaxDiscount();
                }
            } else {
                discountAmount = voucher.getDiscountValue();
                if (discountAmount.compareTo(totalAmount) > 0) {
                    discountAmount = totalAmount;
                }
            }
            appliedVoucher = voucher;
        }

        BigDecimal finalAmount = totalAmount.subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;

        // --- 3.6 Tạo Order ---
        Order order = new Order();
        order.setOrderType("IN_STORE");
        order.setCustomer(finalCustomer);
        order.setCashier(finalCashier);
        order.setVoucher(appliedVoucher);
        order.setPaymentMethod(pm);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setFinalAmount(finalAmount);
        // CASH = 3 (Hoàn thành), VNPAY = 0 (Chờ thanh toán). Tuy nhiên HÀNG ĐÃ ĐƯỢC GIỮ.
        order.setStatus(pm.equals("CASH") ? 3 : 0);
        order.setCreatedAt(LocalDateTime.now());

        if (finalCustomer != null) {
            order.setCustomerName(finalCustomer.getUser().getName());
            order.setCustomerPhone(finalCustomer.getUser().getPhone());
        } else if (request.getCustomerPhone() != null) {
            order.setCustomerPhone(request.getCustomerPhone());
        }

        Order savedOrder = orderRepository.save(order);
        List<PosOrderResponse.InvoiceItem> invoiceItems = new ArrayList<>();

        // --- 3.7 Lưu OrderItems & TRỪ KHO NGAY LẬP TỨC CHO MỌI PHƯƠNG THỨC ---
        for (LineItem line : lineItems) {
            OrderItem oi = new OrderItem();
            oi.setOrder(savedOrder);
            oi.setProductVariant(line.variant());
            oi.setQuantity(line.quantity());
            oi.setOriginalPrice(line.unitPrice());
            oi.setDiscountAmount(BigDecimal.ZERO);
            oi.setFinalPrice(line.lineTotal());
            orderItemRepository.save(oi);

            // Giữ kho vật lý để tránh việc bị khách online mua mất khi đang quét QR
            ProductVariant v = line.variant();
            v.setStockQuantity(v.getStockQuantity() - line.quantity());
            variantRepository.save(v);

            invoiceItems.add(PosOrderResponse.InvoiceItem.builder()
                    .productName(line.variant().getProduct().getName())
                    .sku(line.variant().getSku())
                    .capacityLabel(line.variant().getCapacity().getValue() + " ml")
                    .quantity(line.quantity())
                    .unitPrice(line.unitPrice())
                    .lineTotal(line.lineTotal())
                    .build());
        }

        // --- 3.8 Tích điểm loyalty (Chỉ cộng ngay cho CASH, VNPAY đợi IPN) ---
        if (pm.equals("CASH") && finalCustomer != null) {
            int pointsEarned = finalAmount
                    .divide(BigDecimal.valueOf(10_000), 0, RoundingMode.DOWN)
                    .intValue();
            if (pointsEarned > 0) {
                finalCustomer.setLoyaltyPoints(finalCustomer.getLoyaltyPoints() + pointsEarned);
                customerRepository.save(finalCustomer);
            }
        }

        // --- 3.9 GIỮ VOUCHER NGAY LẬP TỨC CHO MỌI PHƯƠNG THỨC ---
        if (appliedVoucher != null) {
            appliedVoucher.setUsedCount(appliedVoucher.getUsedCount() + 1);
            voucherRepository.save(appliedVoucher);
        }

        // --- 3.10 Sinh URL thanh toán VNPay (QR động) ---
        String vnpayUrl = null;
        if (pm.equals("VNPAY")) {
            vnpayUrl = vnPayService.createPaymentUrl(savedOrder.getId(), finalAmount, clientIp);
        }

        // --- 3.11 Build và trả response ---
        return PosOrderResponse.builder()
                .orderId(savedOrder.getId())
                .status(pm.equals("CASH") ? "COMPLETED" : "PENDING_PAYMENT")
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .paymentMethod(pm)
                .vnpayPaymentUrl(vnpayUrl)
                .createdAt(savedOrder.getCreatedAt())
                .cashierName(finalCashier != null ? finalCashier.getUser().getName() : null)
                .customerName(finalCustomer != null ? finalCustomer.getUser().getName() : null)
                .customerPhone(finalCustomer != null ? finalCustomer.getUser().getPhone() : request.getCustomerPhone())
                .items(invoiceItems)
                .build();
    }

    private record LineItem(
            ProductVariant variant,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {}
}