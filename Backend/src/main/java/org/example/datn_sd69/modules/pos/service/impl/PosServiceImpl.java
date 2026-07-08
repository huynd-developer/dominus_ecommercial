package org.example.datn_sd69.modules.pos.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.*;
import org.example.datn_sd69.modules.pos.dto.CustomerPosResponse;
import org.example.datn_sd69.modules.pos.dto.PosCheckoutRequest;
import org.example.datn_sd69.modules.pos.dto.PosItemRequest;
import org.example.datn_sd69.modules.pos.dto.PosOrderResponse;
import org.example.datn_sd69.modules.pos.dto.ProductVariantPosResponse;
import org.example.datn_sd69.modules.pos.service.PosService;
import org.example.datn_sd69.modules.vnpay.service.VNPayService;
import org.example.datn_sd69.repository.CustomerRepository;
import org.example.datn_sd69.repository.EmployeeRepository;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.example.datn_sd69.repository.OrderRepository;
import org.example.datn_sd69.repository.ProductImageRepository;
import org.example.datn_sd69.repository.ProductVariantRepository;
import org.example.datn_sd69.repository.RoleRepository;
import org.example.datn_sd69.repository.UserRepository;
import org.example.datn_sd69.repository.VoucherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PosServiceImpl implements PosService {

    private static final int STATUS_ACTIVE = 1;

    private static final int ORDER_STATUS_PENDING = 0;
    private static final int ORDER_STATUS_COMPLETED = 3;

    private static final String PAYMENT_CASH = "CASH";
    private static final String PAYMENT_VNPAY = "VNPAY";

    private static final BigDecimal POINT_RATE_AMOUNT = BigDecimal.valueOf(10_000);

    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository productImageRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EmployeeRepository employeeRepository;
    private final VoucherRepository voucherRepository;
    private final VNPayService vnPayService;

    @Override
    public ProductVariantPosResponse findVariantBySku(String sku) {
        String cleanSku = normalizeSku(sku);

        ProductVariant variant = variantRepository
                .findBySkuIgnoreCaseAndIsDeletedFalse(cleanSku)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với SKU: " + cleanSku));

        validateVariantCanSell(variant, 1);

        Product product = variant.getProduct();

        String imageUrl = productImageRepository
                .findPrimaryByProductId(product.getId())
                .or(() -> productImageRepository.findFirstByProductId(product.getId()))
                .map(ProductImage::getImageUrl)
                .orElse(null);

        return ProductVariantPosResponse.builder()
                .variantId(variant.getId())
                .sku(variant.getSku())
                .productName(product != null ? product.getName() : null)
                .brandName(product != null && product.getBrand() != null ? product.getBrand().getName() : null)
                .capacityLabel(variant.getCapacity() != null && variant.getCapacity().getValue() != null
                        ? variant.getCapacity().getValue() + " ml"
                        : null)
                .bottleTypeName(variant.getBottleType() != null ? variant.getBottleType().getName() : null)
                .price(variant.getPrice())
                .stockQuantity(variant.getStockQuantity())
                .imageUrl(imageUrl)
                .build();
    }

    @Override
    public CustomerPosResponse findCustomerByPhone(String phone) {
        String cleanPhone = normalizeOptionalPhone(phone);

        if (cleanPhone == null) {
            return null;
        }

        return customerRepository.findByPhone(cleanPhone)
                .map(c -> CustomerPosResponse.builder()
                        .customerId(c.getUserId())
                        .name(c.getUser() != null ? c.getUser().getName() : null)
                        .phone(c.getUser() != null ? c.getUser().getPhone() : null)
                        .email(c.getUser() != null ? c.getUser().getEmail() : null)
                        .customerRank(c.getCustomerRank())
                        .loyaltyPoints(c.getLoyaltyPoints())
                        .build())
                .orElse(null);
    }

    @Override
    @Transactional
    public PosOrderResponse checkout(
            PosCheckoutRequest request,
            String cashierEmail,
            String clientIp
    ) {
        validateCheckoutRequest(request);

        String paymentMethod = normalizePaymentMethod(request.getPaymentMethod());

        Employee cashier = resolveCashier(cashierEmail);
        Customer customer = resolveOrCreateCustomer(request);

        Map<String, Integer> skuQuantityMap = mergeItemsBySku(request.getItems());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<LineItem> lineItems = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : skuQuantityMap.entrySet()) {
            String sku = entry.getKey();
            Integer quantity = entry.getValue();

            ProductVariant variant = variantRepository
                    .findBySkuIgnoreCaseAndIsDeletedFalse(sku)
                    .orElseThrow(() -> new RuntimeException("SKU không hợp lệ: " + sku));

            validateVariantCanSell(variant, quantity);

            BigDecimal unitPrice = variant.getPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

            totalAmount = totalAmount.add(lineTotal);
            lineItems.add(new LineItem(variant, quantity, unitPrice, lineTotal));
        }

        Voucher appliedVoucher = resolveVoucher(request.getVoucherCode(), totalAmount);
        BigDecimal discountAmount = calculateDiscountAmount(appliedVoucher, totalAmount);
        BigDecimal finalAmount = totalAmount.subtract(discountAmount);

        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        validateCashInfoIfProvided(request, finalAmount);

        boolean completedImmediately = PAYMENT_CASH.equals(paymentMethod);

        Order order = new Order();
        order.setOrderType("IN_STORE");
        order.setVoucher(appliedVoucher);
        order.setPaymentMethod(paymentMethod);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setFinalAmount(finalAmount);
        order.setStatus(completedImmediately ? ORDER_STATUS_COMPLETED : ORDER_STATUS_PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setCashier(cashier);
        order.setLoyaltyPointsApplied(false);
        order.setLoyaltyPointsEarned(0);
        order.setCompletedAt(null);

        if (customer != null) {
            order.setCustomer(customer);
            order.setCustomerName(customer.getUser() != null ? customer.getUser().getName() : null);
            order.setCustomerPhone(customer.getUser() != null ? customer.getUser().getPhone() : null);
        } else {
            order.setCustomer(null);
            order.setCustomerPhone(null);
            order.setCustomerName(
                    request.getCustomerName() != null && !request.getCustomerName().isBlank()
                            ? request.getCustomerName().trim()
                            : "Khách vãng lai"
            );
        }

        Order savedOrder = orderRepository.save(order);

        List<PosOrderResponse.InvoiceItem> invoiceItems = new ArrayList<>();

        for (LineItem line : lineItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProductVariant(line.variant());
            orderItem.setQuantity(line.quantity());
            orderItem.setOriginalPrice(line.unitPrice());
            orderItem.setDiscountAmount(BigDecimal.ZERO);
            orderItem.setFinalPrice(line.lineTotal());

            orderItemRepository.save(orderItem);

            ProductVariant variant = line.variant();
            variant.setStockQuantity(variant.getStockQuantity() - line.quantity());
            variantRepository.save(variant);

            invoiceItems.add(PosOrderResponse.InvoiceItem.builder()
                    .productName(variant.getProduct() != null ? variant.getProduct().getName() : null)
                    .sku(variant.getSku())
                    .capacityLabel(variant.getCapacity() != null && variant.getCapacity().getValue() != null
                            ? variant.getCapacity().getValue() + " ml"
                            : null)
                    .quantity(line.quantity())
                    .unitPrice(line.unitPrice())
                    .lineTotal(line.lineTotal())
                    .build());
        }

        int loyaltyPointsEarned = 0;
        Integer customerLoyaltyPointsAfter = null;

        if (completedImmediately) {
            loyaltyPointsEarned = applyLoyaltyPointsIfNeeded(savedOrder, customer);
            customerLoyaltyPointsAfter = customer != null ? customer.getLoyaltyPoints() : null;

            if (appliedVoucher != null) {
                increaseVoucherUsedCount(appliedVoucher);
            }
        }

        String vnpayUrl = null;

        if (PAYMENT_VNPAY.equals(paymentMethod)) {
            vnpayUrl = vnPayService.createPaymentUrl(savedOrder.getId(), finalAmount, clientIp);
        }

        return PosOrderResponse.builder()
                .orderId(savedOrder.getId())
                .status(completedImmediately ? "COMPLETED" : "PENDING_PAYMENT")
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .paymentMethod(paymentMethod)
                .vnpayPaymentUrl(vnpayUrl)
                .createdAt(savedOrder.getCreatedAt())
                .cashierName(cashier != null && cashier.getUser() != null ? cashier.getUser().getName() : null)
                .customerName(savedOrder.getCustomerName())
                .customerPhone(savedOrder.getCustomerPhone())
                .loyaltyPointsEarned(loyaltyPointsEarned)
                .customerLoyaltyPointsAfter(customerLoyaltyPointsAfter)
                .items(invoiceItems)
                .build();
    }

    private void validateCheckoutRequest(PosCheckoutRequest request) {
        if (request == null) {
            throw new RuntimeException("Dữ liệu hóa đơn không được để trống");
        }

        normalizePaymentMethod(request.getPaymentMethod());

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Hóa đơn phải có ít nhất 1 sản phẩm");
        }
    }

    private String normalizePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.trim().isBlank()) {
            throw new RuntimeException("Phương thức thanh toán không được để trống");
        }

        String value = paymentMethod.trim().toUpperCase();

        if (!PAYMENT_CASH.equals(value) && !PAYMENT_VNPAY.equals(value)) {
            throw new RuntimeException("Phương thức thanh toán chỉ được là CASH hoặc VNPAY");
        }

        return value;
    }

    private String normalizeSku(String sku) {
        if (sku == null || sku.trim().isBlank()) {
            throw new RuntimeException("SKU không được để trống");
        }

        return sku.trim();
    }

    private String normalizeOptionalPhone(String phone) {
        if (phone == null || phone.trim().isBlank()) {
            return null;
        }

        String value = phone.replaceAll("[\\s.-]", "").trim();

        if (!value.matches("^(03|05|07|08|09)\\d{8}$")) {
            throw new RuntimeException("Số điện thoại không hợp lệ. SĐT phải gồm 10 số và bắt đầu bằng 03, 05, 07, 08 hoặc 09.");
        }

        return value;
    }

    private Employee resolveCashier(String cashierEmail) {
        if (cashierEmail == null || cashierEmail.isBlank()) {
            return null;
        }

        return employeeRepository.findByUserEmail(cashierEmail)
                .orElse(null);
    }

    private Customer resolveOrCreateCustomer(PosCheckoutRequest request) {
        String cleanPhone = normalizeOptionalPhone(request.getCustomerPhone());

        if (cleanPhone == null) {
            return null;
        }

        return customerRepository.findByPhone(cleanPhone)
                .orElseGet(() -> createPosCustomer(cleanPhone, request.getCustomerName()));
    }

    private Customer createPosCustomer(String phone, String customerName) {
        String name = customerName != null && !customerName.trim().isBlank()
                ? customerName.trim()
                : "Khách hàng mới";

        User newUser = new User();
        newUser.setPhone(phone);
        newUser.setName(name);
        newUser.setEmail(phone + "@pos-store.com");

        /*
         * Tài khoản tạo từ POS chỉ để gắn điểm/thành viên.
         * Nếu khách muốn đăng nhập thật thì nên cho họ đổi email/mật khẩu sau.
         */
        newUser.setPasswordHash("$2a$10$OixMv6FshZExa7R.D0v6p.VlWkWZ6HwOuxKz5QZf6v3kXv3Xv3Xv.");

        Role customerRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Hệ thống chưa có Role USER, vui lòng cấu hình DB."));

        newUser.setRole(customerRole);

        User savedUser = userRepository.save(newUser);

        Customer newCustomer = new Customer();
        newCustomer.setUser(savedUser);
        newCustomer.setLoyaltyPoints(0);
        newCustomer.setCustomerRank("BRONZE");

        return customerRepository.save(newCustomer);
    }

    private Map<String, Integer> mergeItemsBySku(List<PosItemRequest> items) {
        Map<String, Integer> skuQuantityMap = new LinkedHashMap<>();

        for (PosItemRequest item : items) {
            if (item == null) {
                throw new RuntimeException("Dòng sản phẩm không hợp lệ");
            }

            String sku = normalizeSku(item.getSku());

            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new RuntimeException("Số lượng của SKU [" + sku + "] phải lớn hơn 0");
            }

            skuQuantityMap.merge(sku, item.getQuantity(), Integer::sum);
        }

        return skuQuantityMap;
    }

    private void validateVariantCanSell(ProductVariant variant, Integer quantity) {
        if (variant == null) {
            throw new RuntimeException("Biến thể sản phẩm không tồn tại");
        }

        if (variant.getStatus() == null || variant.getStatus() != STATUS_ACTIVE) {
            throw new RuntimeException("Sản phẩm [" + variant.getSku() + "] hiện không được bán");
        }

        if (variant.getProduct() == null) {
            throw new RuntimeException("Sản phẩm của SKU [" + variant.getSku() + "] không tồn tại");
        }

        if (variant.getProduct().getStatus() == null || variant.getProduct().getStatus() != STATUS_ACTIVE) {
            throw new RuntimeException("Sản phẩm [" + variant.getProduct().getName() + "] hiện không được bán");
        }

        if (variant.getPrice() == null || variant.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Sản phẩm [" + variant.getSku() + "] chưa có giá bán hợp lệ");
        }

        if (variant.getStockQuantity() == null || variant.getStockQuantity() < 0) {
            throw new RuntimeException("Tồn kho của sản phẩm [" + variant.getSku() + "] không hợp lệ");
        }

        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Số lượng sản phẩm [" + variant.getSku() + "] không hợp lệ");
        }

        if (variant.getStockQuantity() < quantity) {
            throw new RuntimeException(
                    "Sản phẩm [" + variant.getProduct().getName() + "] chỉ còn " + variant.getStockQuantity()
            );
        }
    }

    private Voucher resolveVoucher(String voucherCode, BigDecimal totalAmount) {
        if (voucherCode == null || voucherCode.trim().isBlank()) {
            return null;
        }

        Voucher voucher = voucherRepository
                .findValidByCode(voucherCode.trim(), LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Voucher không hợp lệ"));

        if (voucher.getMinOrderValue() != null
                && totalAmount.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new RuntimeException("Đơn hàng chưa đạt tối thiểu để dùng voucher");
        }

        return voucher;
    }

    private BigDecimal calculateDiscountAmount(Voucher voucher, BigDecimal totalAmount) {
        if (voucher == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal discountAmount;

        if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType())) {
            discountAmount = totalAmount
                    .multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);

            if (voucher.getMaxDiscount() != null
                    && discountAmount.compareTo(voucher.getMaxDiscount()) > 0) {
                discountAmount = voucher.getMaxDiscount();
            }
        } else {
            discountAmount = voucher.getDiscountValue();
        }

        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        if (discountAmount.compareTo(totalAmount) > 0) {
            return totalAmount;
        }

        return discountAmount;
    }

    private void validateCashInfoIfProvided(PosCheckoutRequest request, BigDecimal finalAmount) {
        if (!PAYMENT_CASH.equals(normalizePaymentMethod(request.getPaymentMethod()))) {
            return;
        }

        if (request.getCashGiven() == null) {
            return;
        }

        if (request.getCashGiven().compareTo(finalAmount) < 0) {
            throw new RuntimeException("Tiền khách đưa không đủ để thanh toán");
        }

        BigDecimal expectedChange = request.getCashGiven().subtract(finalAmount);

        if (request.getChangeAmount() != null
                && request.getChangeAmount().compareTo(expectedChange) != 0) {
            throw new RuntimeException("Tiền thừa không khớp với số tiền phải trả");
        }
    }

    private int applyLoyaltyPointsIfNeeded(Order order, Customer customer) {
        if (order == null || customer == null) {
            return 0;
        }

        if (order.getStatus() == null || order.getStatus() != ORDER_STATUS_COMPLETED) {
            return 0;
        }

        if (Boolean.TRUE.equals(order.getLoyaltyPointsApplied())) {
            return order.getLoyaltyPointsEarned() != null ? order.getLoyaltyPointsEarned() : 0;
        }

        BigDecimal finalAmount = order.getFinalAmount() != null
                ? order.getFinalAmount()
                : BigDecimal.ZERO;

        int pointsEarned = finalAmount
                .divide(POINT_RATE_AMOUNT, 0, RoundingMode.DOWN)
                .intValue();

        if (pointsEarned <= 0) {
            order.setLoyaltyPointsApplied(true);
            order.setLoyaltyPointsEarned(0);
            order.setCompletedAt(LocalDateTime.now());
            orderRepository.save(order);
            return 0;
        }

        int currentPoints = customer.getLoyaltyPoints() != null
                ? customer.getLoyaltyPoints()
                : 0;

        customer.setLoyaltyPoints(currentPoints + pointsEarned);
        updateCustomerRank(customer);

        customerRepository.save(customer);

        order.setLoyaltyPointsApplied(true);
        order.setLoyaltyPointsEarned(pointsEarned);
        order.setCompletedAt(LocalDateTime.now());
        orderRepository.save(order);

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

    private void increaseVoucherUsedCount(Voucher voucher) {
        int usedCount = voucher.getUsedCount() != null
                ? voucher.getUsedCount()
                : 0;

        voucher.setUsedCount(usedCount + 1);
        voucherRepository.save(voucher);
    }

    private record LineItem(
            ProductVariant variant,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }
}