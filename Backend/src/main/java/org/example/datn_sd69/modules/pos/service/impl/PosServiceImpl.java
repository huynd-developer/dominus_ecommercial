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
    private final UserRepository userRepository;
    private final RoleRepository roleRepository; // BỔ SUNG: Dùng để gán quyền cho User mới
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EmployeeRepository employeeRepository;
    private final VoucherRepository voucherRepository;
    private final VNPayService vnPayService;

    @Override
    public ProductVariantPosResponse findVariantBySku(String sku) {
        ProductVariant variant = variantRepository
                .findBySkuIgnoreCaseAndIsDeletedFalse(sku)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với SKU: " + sku));

        if (variant.getStatus() != 1) {
            throw new RuntimeException("Sản phẩm này hiện không được bán");
        }
        if (variant.getStockQuantity() <= 0) {
            throw new RuntimeException("Sản phẩm đã hết hàng");
        }

        Product product = variant.getProduct();
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
                .orElse(null);
    }

    @Override
    @Transactional
    public PosOrderResponse checkout(PosCheckoutRequest request, String cashierEmail, String clientIp) {

        // --- 3.1 Validate ---
        String pm = request.getPaymentMethod().toUpperCase();
        if (!pm.equals("CASH") && !pm.equals("VNPAY")) {
            throw new RuntimeException("Phương thức thanh toán không hợp lệ.");
        }

        // --- 3.2 Lấy thu ngân ---
        Employee cashier = null;
        if (cashierEmail != null && !cashierEmail.isBlank()) {
            cashier = employeeRepository.findByUserEmail(cashierEmail).orElse(null);
        }
        final Employee finalCashier = cashier;

        // --- 3.3 TẠO MỚI KHÁCH HÀNG (FULL LOGIC FIX) ---
        Customer customer = null;
        if (request.getCustomerPhone() != null && !request.getCustomerPhone().isBlank()) {
            String cleanPhone = request.getCustomerPhone().trim();
            customer = customerRepository.findByPhone(cleanPhone).orElseGet(() -> {
                User newUser = new User();
                newUser.setPhone(cleanPhone);
                newUser.setName((request.getCustomerName() != null && !request.getCustomerName().isBlank())
                        ? request.getCustomerName().trim() : "Khách hàng mới");

                // 1. Fix lỗi Email bị NULL
                newUser.setEmail(cleanPhone + "@pos-store.com");

                // 2. Fix lỗi PasswordHash bị NULL (Tạo pass mặc định)
                newUser.setPasswordHash("$2a$10$OixMv6FshZExa7R.D0v6p.VlWkWZ6HwOuxKz5QZf6v3kXv3Xv3Xv.");

                // 3. Fix lỗi RoleId bị NULL (Gán quyền Mặc định)
                // GIẢ ĐỊNH: Tên role khách hàng trong DB của bạn là "CUSTOMER" hoặc "USER".
                // Hãy đổi tên "CUSTOMER" bên dưới cho khớp với data trong bảng Role của bạn.
                Role customerRole = roleRepository.findByName("USER")
                        .orElseThrow(() -> new RuntimeException("Hệ thống chưa có Role CUSTOMER, vui lòng cấu hình DB."));
                newUser.setRole(customerRole);

                // Các trường Status, CreatedAt, IsDeleted đã được auto-set ở Entity rồi nên bỏ qua

                User savedUser = userRepository.save(newUser);

                Customer newCustomer = new Customer();
                newCustomer.setUser(savedUser);
                newCustomer.setLoyaltyPoints(0);
                newCustomer.setCustomerRank("BRONZE");
                return customerRepository.save(newCustomer);
            });
        }
        final Customer finalCustomer = customer;

        // --- 3.4 Tính tiền & Trừ kho ---
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<LineItem> lineItems = new ArrayList<>();

        for (PosItemRequest item : request.getItems()) {
            ProductVariant variant = variantRepository
                    .findBySkuIgnoreCaseAndIsDeletedFalse(item.getSku())
                    .orElseThrow(() -> new RuntimeException("SKU không hợp lệ: " + item.getSku()));

            if (variant.getStatus() != 1) {
                throw new RuntimeException("Sản phẩm [" + item.getSku() + "] hiện không được bán");
            }
            if (variant.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Sản phẩm [" + variant.getProduct().getName() + "] chỉ còn " + variant.getStockQuantity());
            }

            BigDecimal lineTotal = variant.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
            lineItems.add(new LineItem(variant, item.getQuantity(), variant.getPrice(), lineTotal));
        }

        // --- 3.5 Voucher ---
        BigDecimal discountAmount = BigDecimal.ZERO;
        Voucher appliedVoucher = null;
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            Voucher voucher = voucherRepository
                    .findValidByCode(request.getVoucherCode(), LocalDateTime.now())
                    .orElseThrow(() -> new RuntimeException("Voucher không hợp lệ"));

            if (totalAmount.compareTo(voucher.getMinOrderValue()) < 0) {
                throw new RuntimeException("Đơn hàng chưa đạt tối thiểu để dùng voucher");
            }

            if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {
                discountAmount = totalAmount.multiply(voucher.getDiscountValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
                if (voucher.getMaxDiscount() != null && discountAmount.compareTo(voucher.getMaxDiscount()) > 0) {
                    discountAmount = voucher.getMaxDiscount();
                }
            } else {
                discountAmount = voucher.getDiscountValue();
                if (discountAmount.compareTo(totalAmount) > 0) discountAmount = totalAmount;
            }
            appliedVoucher = voucher;
        }

        BigDecimal finalAmount = totalAmount.subtract(discountAmount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;

        // --- 3.6 Lưu Order ---
        Order order = new Order();
        order.setOrderType("IN_STORE");
        order.setVoucher(appliedVoucher);
        order.setPaymentMethod(pm);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setFinalAmount(finalAmount);
        order.setStatus(pm.equals("CASH") ? 3 : 0);
        order.setCreatedAt(LocalDateTime.now());
        order.setCashier(finalCashier);

        if (finalCustomer != null) {
            order.setCustomer(finalCustomer);
            order.setCustomerName(finalCustomer.getUser().getName());
            order.setCustomerPhone(finalCustomer.getUser().getPhone());
        } else {
            order.setCustomer(null);
            order.setCustomerPhone(null);
            order.setCustomerName((request.getCustomerName() != null && !request.getCustomerName().isBlank())
                    ? request.getCustomerName().trim() : "Khách vãng lai");
        }

        Order savedOrder = orderRepository.save(order);
        List<PosOrderResponse.InvoiceItem> invoiceItems = new ArrayList<>();

        // --- 3.7 Order Items ---
        for (LineItem line : lineItems) {
            OrderItem oi = new OrderItem();
            oi.setOrder(savedOrder);
            oi.setProductVariant(line.variant());
            oi.setQuantity(line.quantity());
            oi.setOriginalPrice(line.unitPrice());
            oi.setDiscountAmount(BigDecimal.ZERO);
            oi.setFinalPrice(line.lineTotal());
            orderItemRepository.save(oi);

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

        // --- 3.8 Tích điểm ---
        if (pm.equals("CASH") && finalCustomer != null) {
            int pointsEarned = finalAmount.divide(BigDecimal.valueOf(10_000), 0, RoundingMode.DOWN).intValue();
            if (pointsEarned > 0) {
                finalCustomer.setLoyaltyPoints(finalCustomer.getLoyaltyPoints() + pointsEarned);
                customerRepository.save(finalCustomer);
            }
        }

        if (appliedVoucher != null) {
            appliedVoucher.setUsedCount(appliedVoucher.getUsedCount() + 1);
            voucherRepository.save(appliedVoucher);
        }

        String vnpayUrl = null;
        if (pm.equals("VNPAY")) {
            vnpayUrl = vnPayService.createPaymentUrl(savedOrder.getId(), finalAmount, clientIp);
        }

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
                .customerName(savedOrder.getCustomerName())
                .customerPhone(savedOrder.getCustomerPhone())
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