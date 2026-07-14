package org.example.datn_sd69.modules.pos.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Customer;
import org.example.datn_sd69.entity.Employee;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.entity.Product;
import org.example.datn_sd69.entity.ProductImage;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.entity.Role;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.entity.Voucher;
import org.example.datn_sd69.modules.pos.dto.response.PosTransferTargetResponse;
import org.example.datn_sd69.modules.pos.dto.request.PosHeldOrderCheckoutRequest;
import org.example.datn_sd69.modules.pos.dto.request.PosHoldRequest;
import org.example.datn_sd69.modules.pos.dto.response.CustomerPosResponse;
import org.example.datn_sd69.modules.pos.dto.request.PosCheckoutRequest;
import org.example.datn_sd69.modules.pos.dto.request.PosItemRequest;
import org.example.datn_sd69.modules.pos.dto.response.PosHeldOrderResponse;
import org.example.datn_sd69.modules.pos.dto.response.PosOrderResponse;
import org.example.datn_sd69.modules.pos.dto.response.ProductVariantPosResponse;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.datn_sd69.modules.pos.dto.request.PosTransferHeldOrderRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PosServiceImpl implements PosService {

    private static final int STATUS_ACTIVE = 1;

    private static final int ORDER_STATUS_PENDING = 0;
    private static final int ORDER_STATUS_COMPLETED = 3;
    private static final int ORDER_STATUS_CANCELLED = 4;

    private static final String PAYMENT_CASH = "CASH";
    private static final String PAYMENT_VNPAY = "VNPAY";
    private static final String PAYMENT_MIXED = "MIXED";
    private static final String PAYMENT_HOLD = "HOLD";
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
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public ProductVariantPosResponse findVariantBySku(String sku) {
        String cleanSku = normalizeSku(sku);

        ProductVariant variant = variantRepository
                .findBySkuIgnoreCaseAndIsDeletedFalse(cleanSku)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với SKU: " + cleanSku));

        validateVariantCanSell(variant, 1);

        Product product = variant.getProduct();

        String imageUrl = null;

        if (product != null && product.getId() != null) {
            imageUrl = productImageRepository
                    .findFirstByProduct_IdAndIsPrimaryTrue(product.getId())
                    .or(() -> productImageRepository.findFirstByProduct_Id(product.getId()))
                    .map(ProductImage::getImageUrl)
                    .orElse(null);
        }

        String unavailableReason = getVariantUnavailableReason(variant, 1);

        return ProductVariantPosResponse.builder()
                .variantId(variant.getId())
                .sku(variant.getSku())
                .productName(product != null ? product.getName() : null)
                .brandName(product != null && product.getBrand() != null ? product.getBrand().getName() : null)
                .capacityLabel(buildCapacityLabel(variant))
                .bottleTypeName(variant.getBottleType() != null ? variant.getBottleType().getName() : null)
                .price(variant.getPrice())
                .stockQuantity(variant.getStockQuantity())
                .manufacturingDate(variant.getManufacturingDate())
                .expirationDate(variant.getExpirationDate())
                .status(variant.getStatus())
                .expired(isVariantExpired(variant))
                .sellable(unavailableReason == null)
                .unavailableReason(unavailableReason)
                .imageUrl(imageUrl)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerPosResponse findCustomerByPhone(String phone) {
        String cleanPhone = normalizeRequiredPhone(phone);

        return customerRepository.findByUserPhone(cleanPhone)
                .map(this::toCustomerPosResponse)
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

        PaymentSummary paymentSummary = validateAndBuildPaymentSummary(request, finalAmount);

        User customerUser = customer.getUser();
        boolean completedImmediately = paymentSummary.completedImmediately();

        Order order = new Order();
        order.setOrderType("IN_STORE");
        order.setCustomer(customer);
        order.setCashier(cashier);
        order.setVoucher(appliedVoucher);
        order.setPaymentMethod(paymentSummary.paymentMethod());
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setFinalAmount(finalAmount);
        order.setStatus(completedImmediately ? ORDER_STATUS_COMPLETED : ORDER_STATUS_PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setCustomerName(customerUser != null ? customerUser.getName() : request.getCustomerName());
        order.setCustomerPhone(customerUser != null ? customerUser.getPhone() : request.getCustomerPhone());
        order.setLoyaltyPointsApplied(false);
        order.setLoyaltyPointsEarned(0);
        order.setCompletedAt(null);

        Order savedOrder = orderRepository.save(order);

        List<PosOrderResponse.InvoiceItem> invoiceItems = new ArrayList<>();

        for (LineItem line : lineItems) {
            ProductVariant variant = line.variant();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProductVariant(variant);
            orderItem.setQuantity(line.quantity());
            orderItem.setOriginalPrice(line.unitPrice());
            orderItem.setDiscountAmount(BigDecimal.ZERO);
            orderItem.setFinalPrice(line.lineTotal());

            orderItemRepository.save(orderItem);

            /*
             * Với VNPAY/MIXED, đây được hiểu là giữ hàng.
             * Nếu thanh toán VNPay thất bại/hết hạn thì callback/hủy đơn phải hoàn kho.
             */
            variant.setStockQuantity(variant.getStockQuantity() - line.quantity());
            variantRepository.save(variant);

            invoiceItems.add(PosOrderResponse.InvoiceItem.builder()
                    .productName(variant.getProduct() != null ? variant.getProduct().getName() : null)
                    .sku(variant.getSku())
                    .capacityLabel(buildCapacityLabel(variant))
                    .bottleTypeName(variant.getBottleType() != null ? variant.getBottleType().getName() : null)
                    .quantity(line.quantity())
                    .unitPrice(line.unitPrice())
                    .lineTotal(line.lineTotal())
                    .build());
        }

        int loyaltyPointsEarned = 0;
        Integer customerLoyaltyPointsAfter = customer.getLoyaltyPoints();

        if (completedImmediately) {
            loyaltyPointsEarned = applyLoyaltyPointsIfNeeded(savedOrder, customer);
            customerLoyaltyPointsAfter = customer.getLoyaltyPoints();

            if (appliedVoucher != null) {
                increaseVoucherUsedCount(appliedVoucher);
            }
        }

        String vnpayUrl = null;

        if (paymentSummary.needVnpayPayment()) {
            vnpayUrl = vnPayService.createPaymentUrl(
                    savedOrder.getId(),
                    paymentSummary.transferAmount(),
                    clientIp
            );
        }

        return PosOrderResponse.builder()
                .orderId(savedOrder.getId())
                .status(completedImmediately ? "COMPLETED" : "PENDING_PAYMENT")
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .paymentMethod(paymentSummary.paymentMethod())
                .paidAmount(paymentSummary.paidAmount())
                .remainingAmount(paymentSummary.remainingAmount())
                .cashGiven(paymentSummary.cashGiven())
                .transferAmount(paymentSummary.transferAmount())
                .changeAmount(paymentSummary.changeAmount())
                .vnpayPaymentUrl(vnpayUrl)
                .createdAt(savedOrder.getCreatedAt())
                .cashierName(cashier.getUser() != null ? cashier.getUser().getName() : null)
                .customerName(savedOrder.getCustomerName())
                .customerPhone(savedOrder.getCustomerPhone())
                .customerEmail(customer.getUser() != null ? customer.getUser().getEmail() : request.getCustomerEmail())
                .loyaltyPointsEarned(loyaltyPointsEarned)
                .customerLoyaltyPointsAfter(customerLoyaltyPointsAfter)
                .items(invoiceItems)
                .build();
    }

    private CustomerPosResponse toCustomerPosResponse(Customer customer) {
        User user = customer.getUser();

        return CustomerPosResponse.builder()
                .customerId(customer.getUserId())
                .name(user != null ? user.getName() : null)
                .phone(user != null ? user.getPhone() : null)
                .email(user != null ? user.getEmail() : null)
                .customerRank(customer.getCustomerRank())
                .loyaltyPoints(customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0)
                .build();
    }

    /**
     * DB hiện tại Customer.UserId là PK/FK tới Users.Id.
     * <p>
     * Logic POS đúng:
     * 1. Ưu tiên tìm Customer theo SĐT.
     * 2. Nếu không thấy theo SĐT nhưng email đã thuộc Customer cũ:
     * dùng lại Customer đó và cập nhật SĐT mới nếu SĐT chưa thuộc người khác.
     * 3. Nếu Users tồn tại nhưng chưa có Customer:
     * chỉ tạo Customer nếu user đó là role USER.
     * 4. Không tạo email giả.
     */
    private Customer resolveOrCreateCustomer(PosCheckoutRequest request) {
        String phone = normalizeRequiredPhone(request.getCustomerPhone());
        String name = normalizeRequiredName(request.getCustomerName());
        String email = normalizeRequiredEmail(request.getCustomerEmail());

        Customer customerByPhone = customerRepository.findByUserPhone(phone)
                .orElse(null);

        if (customerByPhone != null) {
            User user = requireCustomerUser(customerByPhone);

            User userByEmail = userRepository.findByEmailIgnoreCase(email)
                    .orElse(null);

            if (userByEmail != null && !userByEmail.getId().equals(user.getId())) {
                throw new RuntimeException("Email đã thuộc khách hàng khác.");
            }

            updateUserBasicInfo(user, name, phone, email);
            normalizeCustomerDefaultInfo(customerByPhone);

            return customerRepository.save(customerByPhone);
        }

        Customer customerByEmail = customerRepository.findByUserEmailIgnoreCase(email)
                .orElse(null);

        if (customerByEmail != null) {
            User user = requireCustomerUser(customerByEmail);

            User userByPhone = userRepository.findByPhone(phone)
                    .orElse(null);

            if (userByPhone != null && !userByPhone.getId().equals(user.getId())) {
                throw new RuntimeException("Số điện thoại đã thuộc khách hàng khác.");
            }

            updateUserBasicInfo(user, name, phone, email);
            normalizeCustomerDefaultInfo(customerByEmail);

            return customerRepository.save(customerByEmail);
        }

        User userByPhone = userRepository.findByPhone(phone)
                .orElse(null);

        if (userByPhone != null) {
            if (!isUserRole(userByPhone)) {
                throw new RuntimeException(
                        "Số điện thoại này đã thuộc tài khoản nhân viên/quản trị. Vui lòng nhập số điện thoại khác cho khách hàng."
                );
            }

            User userByEmail = userRepository.findByEmailIgnoreCase(email)
                    .orElse(null);

            if (userByEmail != null && !userByEmail.getId().equals(userByPhone.getId())) {
                throw new RuntimeException("Email đã thuộc tài khoản khác.");
            }

            if (userByPhone.getStatus() == null || userByPhone.getStatus() != STATUS_ACTIVE) {
                throw new RuntimeException("Tài khoản khách hàng đã bị khóa.");
            }

            updateUserBasicInfo(userByPhone, name, phone, email);

            Customer newCustomer = new Customer();
            newCustomer.setUser(userByPhone);
            newCustomer.setCustomerRank("BRONZE");
            newCustomer.setLoyaltyPoints(0);
            newCustomer.setDateOfBirth(null);
            newCustomer.setGender(null);

            return customerRepository.save(newCustomer);
        }

        User userByEmail = userRepository.findByEmailIgnoreCase(email)
                .orElse(null);

        if (userByEmail != null) {
            if (!isUserRole(userByEmail)) {
                throw new RuntimeException(
                        "Email này đã thuộc tài khoản nhân viên/quản trị. Vui lòng nhập email khác cho khách hàng."
                );
            }

            if (userByEmail.getStatus() == null || userByEmail.getStatus() != STATUS_ACTIVE) {
                throw new RuntimeException("Tài khoản khách hàng đã bị khóa.");
            }

            updateUserBasicInfo(userByEmail, name, phone, email);

            Customer newCustomer = new Customer();
            newCustomer.setUser(userByEmail);
            newCustomer.setCustomerRank("BRONZE");
            newCustomer.setLoyaltyPoints(0);
            newCustomer.setDateOfBirth(null);
            newCustomer.setGender(null);

            return customerRepository.save(newCustomer);
        }

        Role userRole = roleRepository.findByNameIgnoreCase("USER")
                .orElseThrow(() -> new RuntimeException("Hệ thống chưa có Role USER. Vui lòng kiểm tra bảng Roles."));

        User newUser = new User();
        newUser.setRole(userRole);
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setAddress(null);
        newUser.setAvatarUrl(null);
        newUser.setStatus(STATUS_ACTIVE);

        String randomPassword = UUID.randomUUID().toString() + "@Pos1";
        newUser.setPasswordHash(passwordEncoder.encode(randomPassword));

        User savedUser = userRepository.save(newUser);

        Customer newCustomer = new Customer();
        newCustomer.setUser(savedUser);
        newCustomer.setCustomerRank("BRONZE");
        newCustomer.setLoyaltyPoints(0);
        newCustomer.setDateOfBirth(null);
        newCustomer.setGender(null);

        return customerRepository.save(newCustomer);
    }

    private User requireCustomerUser(Customer customer) {
        if (customer == null || customer.getUser() == null) {
            throw new RuntimeException("Dữ liệu khách hàng bị lỗi: Customer không liên kết Users.");
        }

        User user = customer.getUser();

        if (user.getStatus() == null || user.getStatus() != STATUS_ACTIVE) {
            throw new RuntimeException("Tài khoản khách hàng đã bị khóa.");
        }

        return user;
    }

    private boolean isUserRole(User user) {
        if (user == null || user.getRole() == null || user.getRole().getName() == null) {
            return false;
        }

        String roleName = user.getRole().getName().trim().toUpperCase(Locale.ROOT);

        return "USER".equals(roleName) || "ROLE_USER".equals(roleName);
    }

    private void updateUserBasicInfo(
            User user,
            String name,
            String phone,
            String email
    ) {
        user.setName(name);
        user.setPhone(phone);
        user.setEmail(email);
        userRepository.save(user);
    }

    private void normalizeCustomerDefaultInfo(Customer customer) {
        if (customer.getCustomerRank() == null || customer.getCustomerRank().isBlank()) {
            customer.setCustomerRank("BRONZE");
        }

        if (customer.getLoyaltyPoints() == null) {
            customer.setLoyaltyPoints(0);
        }
    }

    private void validateCheckoutRequest(PosCheckoutRequest request) {
        if (request == null) {
            throw new RuntimeException("Dữ liệu hóa đơn không được để trống");
        }

        normalizePaymentMethod(request.getPaymentMethod());
        normalizeRequiredPhone(request.getCustomerPhone());
        normalizeRequiredName(request.getCustomerName());
        normalizeRequiredEmail(request.getCustomerEmail());

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Hóa đơn phải có ít nhất 1 sản phẩm");
        }
    }

    private PaymentSummary validateAndBuildPaymentSummary(
            PosCheckoutRequest request,
            BigDecimal finalAmount
    ) {
        String method = normalizePaymentMethod(request.getPaymentMethod());

        BigDecimal cashGiven = request.getCashGiven() != null
                ? request.getCashGiven()
                : BigDecimal.ZERO;

        BigDecimal transferAmount = request.getTransferAmount() != null
                ? request.getTransferAmount()
                : BigDecimal.ZERO;

        if (cashGiven.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Tiền mặt không được âm");
        }

        if (transferAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Tiền chuyển khoản không được âm");
        }

        if (finalAmount == null || finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Tổng tiền thanh toán không hợp lệ");
        }

        if (finalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return new PaymentSummary(
                    PAYMENT_CASH,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    false,
                    true
            );
        }

        if (PAYMENT_CASH.equals(method)) {
            if (cashGiven.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Vui lòng nhập số tiền khách đưa");
            }

            if (transferAmount.compareTo(BigDecimal.ZERO) > 0) {
                throw new RuntimeException("Thanh toán CASH không được gửi tiền chuyển khoản");
            }

            if (cashGiven.compareTo(finalAmount) < 0) {
                BigDecimal missingAmount = finalAmount.subtract(cashGiven);

                throw new RuntimeException(
                        "Tiền khách đưa còn thiếu "
                                + missingAmount.toPlainString()
                                + " đồng. Vui lòng chọn thêm phương thức thanh toán."
                );
            }

            BigDecimal changeAmount = cashGiven.subtract(finalAmount);

            return new PaymentSummary(
                    PAYMENT_CASH,
                    cashGiven,
                    BigDecimal.ZERO,
                    finalAmount,
                    BigDecimal.ZERO,
                    changeAmount,
                    false,
                    true
            );
        }

        if (PAYMENT_VNPAY.equals(method)) {
            if (cashGiven.compareTo(BigDecimal.ZERO) > 0) {
                throw new RuntimeException("Thanh toán VNPAY không được gửi tiền mặt");
            }

            if (transferAmount.compareTo(BigDecimal.ZERO) == 0) {
                transferAmount = finalAmount;
            }

            if (transferAmount.compareTo(finalAmount) != 0) {
                throw new RuntimeException("Số tiền VNPay phải bằng đúng số tiền cần thanh toán");
            }

            return new PaymentSummary(
                    PAYMENT_VNPAY,
                    BigDecimal.ZERO,
                    transferAmount,
                    BigDecimal.ZERO,
                    transferAmount,
                    BigDecimal.ZERO,
                    true,
                    false
            );
        }

        if (PAYMENT_MIXED.equals(method)) {
            if (cashGiven.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Thanh toán MIXED phải có số tiền mặt đã nhận");
            }

            if (cashGiven.compareTo(finalAmount) >= 0) {
                throw new RuntimeException("Tiền mặt đã đủ thanh toán, vui lòng chọn phương thức CASH");
            }

            BigDecimal expectedTransferAmount = finalAmount.subtract(cashGiven);

            if (transferAmount.compareTo(BigDecimal.ZERO) == 0) {
                transferAmount = expectedTransferAmount;
            }

            BigDecimal totalPaid = cashGiven.add(transferAmount);

            if (totalPaid.compareTo(finalAmount) < 0) {
                BigDecimal missingAmount = finalAmount.subtract(totalPaid);

                throw new RuntimeException(
                        "Khách còn thiếu "
                                + missingAmount.toPlainString()
                                + " đồng. Vui lòng chọn thêm phương thức thanh toán."
                );
            }

            if (totalPaid.compareTo(finalAmount) > 0) {
                throw new RuntimeException(
                        "Tổng tiền mặt và chuyển khoản đang vượt quá số tiền cần thanh toán. "
                                + "Nếu khách đưa dư tiền mặt, hãy trả lại tiền thừa ngay và chỉ nhập số tiền thực dùng để thanh toán."
                );
            }

            return new PaymentSummary(
                    PAYMENT_MIXED,
                    cashGiven,
                    transferAmount,
                    cashGiven,
                    transferAmount,
                    BigDecimal.ZERO,
                    true,
                    false
            );
        }

        throw new RuntimeException("Phương thức thanh toán không hợp lệ");
    }

    private String normalizePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.trim().isBlank()) {
            throw new RuntimeException("Phương thức thanh toán không được để trống");
        }

        String value = paymentMethod.trim().toUpperCase(Locale.ROOT);

        if (!PAYMENT_CASH.equals(value)
                && !PAYMENT_VNPAY.equals(value)
                && !PAYMENT_MIXED.equals(value)) {
            throw new RuntimeException("Phương thức thanh toán chỉ được là CASH, VNPAY hoặc MIXED");
        }

        return value;
    }

    private String normalizeSku(String sku) {
        if (sku == null || sku.trim().isBlank()) {
            throw new RuntimeException("SKU không được để trống");
        }

        return sku.trim();
    }

    private String normalizeRequiredPhone(String phone) {
        if (phone == null || phone.trim().isBlank()) {
            throw new RuntimeException("Số điện thoại khách hàng không được để trống");
        }

        String value = phone.replaceAll("[\\s.-]", "").trim();

        if (!value.matches("^(03|05|07|08|09)\\d{8}$")) {
            throw new RuntimeException("Số điện thoại không hợp lệ. SĐT phải gồm 10 số và bắt đầu bằng 03, 05, 07, 08 hoặc 09.");
        }

        return value;
    }

    private String normalizeRequiredName(String name) {
        if (name == null || name.trim().isBlank()) {
            throw new RuntimeException("Họ tên khách hàng không được để trống");
        }

        String value = name.trim().replaceAll("\\s+", " ");

        if (value.length() < 2 || value.length() > 100) {
            throw new RuntimeException("Họ tên khách hàng phải từ 2 đến 100 ký tự");
        }

        return value;
    }

    private String normalizeRequiredEmail(String email) {
        if (email == null || email.trim().isBlank()) {
            throw new RuntimeException("Email khách hàng không được để trống");
        }

        String value = email.trim().toLowerCase(Locale.ROOT);

        if (value.length() > 255) {
            throw new RuntimeException("Email khách hàng không được vượt quá 255 ký tự");
        }

        if (!value.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new RuntimeException("Email khách hàng không đúng định dạng");
        }

        return value;
    }

    private Employee resolveCashier(String cashierEmail) {
        if (cashierEmail == null || cashierEmail.trim().isBlank()) {
            throw new RuntimeException("Không xác định được nhân viên đang thanh toán.");
        }

        Employee employee = employeeRepository.findByUserEmail(cashierEmail.trim())
                .orElseThrow(() -> new RuntimeException("Tài khoản hiện tại không thuộc nhân viên."));

        if (employee.getUser() == null) {
            throw new RuntimeException("Nhân viên chưa được liên kết với tài khoản.");
        }

        if (employee.getUser().getStatus() == null || employee.getUser().getStatus() != STATUS_ACTIVE) {
            throw new RuntimeException("Tài khoản nhân viên đã bị khóa.");
        }

        return employee;
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
        String reason = getVariantUnavailableReason(variant, quantity);

        if (reason != null) {
            throw new RuntimeException(reason);
        }
    }

    private String getVariantUnavailableReason(ProductVariant variant, Integer quantity) {
        if (variant == null) {
            return "Biến thể sản phẩm không tồn tại";
        }

        String sku = variant.getSku() != null ? variant.getSku() : "N/A";

        if (Boolean.TRUE.equals(variant.getIsDeleted())) {
            return "Sản phẩm [" + sku + "] đã bị xóa, không thể bán";
        }

        if (variant.getStatus() == null || variant.getStatus() != STATUS_ACTIVE) {
            return "Sản phẩm [" + sku + "] hiện không được bán";
        }

        if (variant.getProduct() == null) {
            return "Sản phẩm của SKU [" + sku + "] không tồn tại";
        }

        if (variant.getProduct().getStatus() == null
                || variant.getProduct().getStatus() != STATUS_ACTIVE) {
            return "Sản phẩm [" + variant.getProduct().getName() + "] hiện không được bán";
        }

        if (variant.getPrice() == null || variant.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return "Sản phẩm [" + sku + "] chưa có giá bán hợp lệ";
        }

        if (variant.getStockQuantity() == null || variant.getStockQuantity() < 0) {
            return "Tồn kho của sản phẩm [" + sku + "] không hợp lệ";
        }

        if (quantity == null || quantity <= 0) {
            return "Số lượng sản phẩm [" + sku + "] không hợp lệ";
        }

        if (variant.getStockQuantity() < quantity) {
            return "Sản phẩm [" + variant.getProduct().getName() + "] chỉ còn " + variant.getStockQuantity();
        }

        LocalDate today = LocalDate.now();

        if (variant.getManufacturingDate() == null) {
            return "Sản phẩm [" + sku + "] chưa có ngày sản xuất";
        }

        if (variant.getExpirationDate() == null) {
            return "Sản phẩm [" + sku + "] chưa có hạn sử dụng";
        }

        if (variant.getManufacturingDate().isAfter(today)) {
            return "Sản phẩm [" + sku + "] chưa tới ngày được bán";
        }

        if (variant.getExpirationDate().isBefore(today)) {
            return "Sản phẩm [" + sku + "] đã hết hạn sử dụng";
        }

        if (!variant.getExpirationDate().isAfter(variant.getManufacturingDate())) {
            return "Sản phẩm [" + sku + "] có hạn sử dụng không hợp lệ";
        }

        return null;
    }

    private boolean isVariantExpired(ProductVariant variant) {
        return variant != null
                && variant.getExpirationDate() != null
                && variant.getExpirationDate().isBefore(LocalDate.now());
    }

    private Voucher resolveVoucher(String voucherCode, BigDecimal totalAmount) {
        if (voucherCode == null || voucherCode.trim().isBlank()) {
            return null;
        }

        Voucher voucher = voucherRepository
                .findValidByCode(voucherCode.trim(), LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Voucher không hợp lệ hoặc đã hết hạn"));

        if (voucher.getMinOrderValue() != null && totalAmount.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new RuntimeException("Đơn hàng chưa đạt tối thiểu để dùng voucher");
        }

        int usedCount = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;

        if (voucher.getUsageLimit() != null && usedCount >= voucher.getUsageLimit()) {
            throw new RuntimeException("Voucher đã hết lượt sử dụng");
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

            if (voucher.getMaxDiscount() != null && discountAmount.compareTo(voucher.getMaxDiscount()) > 0) {
                discountAmount = voucher.getMaxDiscount();
            }
        } else if ("FIXED".equalsIgnoreCase(voucher.getDiscountType())) {
            discountAmount = voucher.getDiscountValue();
        } else {
            throw new RuntimeException("Loại giảm giá của voucher không hợp lệ");
        }

        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        if (discountAmount.compareTo(totalAmount) > 0) {
            return totalAmount;
        }

        return discountAmount;
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

        int currentPoints = customer.getLoyaltyPoints() != null
                ? customer.getLoyaltyPoints()
                : 0;

        customer.setLoyaltyPoints(currentPoints + Math.max(pointsEarned, 0));
        updateCustomerRank(customer);
        customerRepository.save(customer);

        order.setLoyaltyPointsApplied(true);
        order.setLoyaltyPointsEarned(Math.max(pointsEarned, 0));
        order.setCompletedAt(LocalDateTime.now());
        orderRepository.save(order);

        return Math.max(pointsEarned, 0);
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

        if (voucher.getUsageLimit() != null && usedCount >= voucher.getUsageLimit()) {
            throw new RuntimeException("Voucher đã hết lượt sử dụng");
        }

        voucher.setUsedCount(usedCount + 1);
        voucherRepository.save(voucher);
    }

    private String buildCapacityLabel(ProductVariant variant) {
        if (variant.getCapacity() == null || variant.getCapacity().getValue() == null) {
            return null;
        }

        return variant.getCapacity().getValue() + " ml";
    }

    private record PaymentSummary(
            String paymentMethod,
            BigDecimal cashGiven,
            BigDecimal transferAmount,
            BigDecimal paidAmount,
            BigDecimal remainingAmount,
            BigDecimal changeAmount,
            boolean needVnpayPayment,
            boolean completedImmediately
    ) {
    }

    private record LineItem(
            ProductVariant variant,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }

    @Override
    @Transactional
    public PosOrderResponse holdOrder(
            PosHoldRequest request,
            String cashierEmail
    ) {
        validateHoldRequest(request);

        Employee cashier = resolveCashier(cashierEmail);
        Customer customer = resolveOrCreateCustomerFromHold(request);

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

        User customerUser = customer.getUser();

        Order order = new Order();
        order.setOrderType("IN_STORE");
        order.setCustomer(customer);
        order.setCashier(cashier);
        order.setVoucher(appliedVoucher);
        order.setPaymentMethod(PAYMENT_HOLD);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setFinalAmount(finalAmount);
        order.setStatus(ORDER_STATUS_PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setCompletedAt(null);
        order.setCustomerName(customerUser != null ? customerUser.getName() : request.getCustomerName());
        order.setCustomerPhone(customerUser != null ? customerUser.getPhone() : request.getCustomerPhone());
        order.setLoyaltyPointsApplied(false);
        order.setLoyaltyPointsEarned(0);

        Order savedOrder = orderRepository.save(order);

        List<PosOrderResponse.InvoiceItem> invoiceItems = new ArrayList<>();

        for (LineItem line : lineItems) {
            ProductVariant variant = line.variant();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProductVariant(variant);
            orderItem.setQuantity(line.quantity());
            orderItem.setOriginalPrice(line.unitPrice());
            orderItem.setDiscountAmount(BigDecimal.ZERO);
            orderItem.setFinalPrice(line.lineTotal());

            orderItemRepository.save(orderItem);

            /*
             * Phiếu treo không trừ kho ngay.
             * Khi thanh toán phiếu treo mới re-check tồn kho và trừ kho.
             */
            invoiceItems.add(PosOrderResponse.InvoiceItem.builder()
                    .productName(variant.getProduct() != null ? variant.getProduct().getName() : null)
                    .sku(variant.getSku())
                    .capacityLabel(buildCapacityLabel(variant))
                    .bottleTypeName(variant.getBottleType() != null ? variant.getBottleType().getName() : null)
                    .quantity(line.quantity())
                    .unitPrice(line.unitPrice())
                    .lineTotal(line.lineTotal())
                    .build());
        }

        return PosOrderResponse.builder()
                .orderId(savedOrder.getId())
                .status("HELD")
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .paymentMethod(PAYMENT_HOLD)
                .paidAmount(BigDecimal.ZERO)
                .remainingAmount(finalAmount)
                .cashGiven(BigDecimal.ZERO)
                .transferAmount(BigDecimal.ZERO)
                .changeAmount(BigDecimal.ZERO)
                .vnpayPaymentUrl(null)
                .createdAt(savedOrder.getCreatedAt())
                .cashierName(cashier.getUser() != null ? cashier.getUser().getName() : null)
                .customerName(savedOrder.getCustomerName())
                .customerPhone(savedOrder.getCustomerPhone())
                .customerEmail(customer.getUser() != null ? customer.getUser().getEmail() : request.getCustomerEmail())
                .loyaltyPointsEarned(0)
                .customerLoyaltyPointsAfter(customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0)
                .items(invoiceItems)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosHeldOrderResponse> getHeldOrders(String cashierEmail) {
        Employee currentEmployee = resolveCashier(cashierEmail);

        Integer cashierIdFilter = isManagerOrOwner(currentEmployee)
                ? null
                : currentEmployee.getUserId();

        return orderRepository.findHeldOrders(cashierIdFilter)
                .stream()
                .map(this::toHeldOrderResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PosOrderResponse getHeldOrderDetail(Integer orderId, String cashierEmail) {
        Employee currentEmployee = resolveCashier(cashierEmail);

        Order order = getHeldOrderOrThrow(orderId);

        validateCanViewHeldOrder(order, currentEmployee);

        List<OrderItem> items = orderItemRepository.findByOrderIdWithVariant(order.getId());

        return buildResponseFromOrder(
                order,
                items,
                "HELD",
                BigDecimal.ZERO,
                order.getFinalAmount() != null ? order.getFinalAmount() : BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                0
        );
    }

    @Override
    @Transactional
    public PosOrderResponse checkoutHeldOrder(
            Integer orderId,
            PosHeldOrderCheckoutRequest request,
            String cashierEmail,
            String clientIp
    ) {
        if (request == null) {
            throw new RuntimeException("Dữ liệu thanh toán phiếu treo không được để trống.");
        }

        Employee currentEmployee = resolveCashier(cashierEmail);

        Order order = getHeldOrderOrThrow(orderId);

        /*
         * Yêu cầu nghiệp vụ:
         * Một nhân viên chỉ được thanh toán phiếu/hóa đơn của chính mình.
         * Nếu nhân viên khác muốn thanh toán thì phải chuyển phiếu trước.
         */
        validateCashierCanCheckoutHeldOrder(order, currentEmployee);

        List<OrderItem> orderItems = orderItemRepository.findByOrderIdWithVariant(order.getId());

        if (orderItems.isEmpty()) {
            throw new RuntimeException("Phiếu treo không có sản phẩm.");
        }

        BigDecimal finalAmount = order.getFinalAmount() != null
                ? order.getFinalAmount()
                : BigDecimal.ZERO;

        PosCheckoutRequest paymentRequest = new PosCheckoutRequest();
        paymentRequest.setPaymentMethod(request.getPaymentMethod());
        paymentRequest.setCashGiven(request.getCashGiven());
        paymentRequest.setTransferAmount(request.getTransferAmount());
        paymentRequest.setCustomerPhone(order.getCustomerPhone());
        paymentRequest.setCustomerName(order.getCustomerName());
        paymentRequest.setCustomerEmail(
                order.getCustomer() != null && order.getCustomer().getUser() != null
                        ? order.getCustomer().getUser().getEmail()
                        : null
        );
        paymentRequest.setItems(List.of(new PosItemRequest()));

        PaymentSummary paymentSummary = validateAndBuildPaymentSummary(paymentRequest, finalAmount);

        /*
         * Phiếu treo chưa trừ kho.
         * Khi thanh toán mới re-check tồn kho và trừ kho.
         */
        for (OrderItem item : orderItems) {
            ProductVariant variant = item.getProductVariant();

            validateVariantCanSell(variant, item.getQuantity());
        }

        for (OrderItem item : orderItems) {
            ProductVariant variant = item.getProductVariant();

            variant.setStockQuantity(variant.getStockQuantity() - item.getQuantity());
            variantRepository.save(variant);
        }

        boolean completedImmediately = paymentSummary.completedImmediately();

        order.setPaymentMethod(paymentSummary.paymentMethod());
        order.setStatus(completedImmediately ? ORDER_STATUS_COMPLETED : ORDER_STATUS_PENDING);

        if (completedImmediately) {
            order.setCompletedAt(LocalDateTime.now());
        }

        Order savedOrder = orderRepository.save(order);

        int loyaltyPointsEarned = 0;

        if (completedImmediately && savedOrder.getCustomer() != null) {
            loyaltyPointsEarned = applyLoyaltyPointsIfNeeded(savedOrder, savedOrder.getCustomer());

            if (savedOrder.getVoucher() != null) {
                increaseVoucherUsedCount(savedOrder.getVoucher());
            }
        }

        String vnpayUrl = null;

        if (paymentSummary.needVnpayPayment()) {
            vnpayUrl = vnPayService.createPaymentUrl(
                    savedOrder.getId(),
                    paymentSummary.transferAmount(),
                    clientIp
            );
        }

        Integer customerPointAfter = savedOrder.getCustomer() != null
                ? savedOrder.getCustomer().getLoyaltyPoints()
                : 0;

        return buildResponseFromOrder(
                savedOrder,
                orderItems,
                completedImmediately ? "COMPLETED" : "PENDING_PAYMENT",
                paymentSummary.paidAmount(),
                paymentSummary.remainingAmount(),
                paymentSummary.cashGiven(),
                paymentSummary.transferAmount(),
                paymentSummary.changeAmount(),
                vnpayUrl,
                loyaltyPointsEarned,
                customerPointAfter
        );
    }
    @Override
    @Transactional
    public PosHeldOrderResponse transferHeldOrder(
            Integer orderId,
            PosTransferHeldOrderRequest request,
            String cashierEmail
    ) {
        if (request == null || request.getTargetEmployeeId() == null) {
            throw new RuntimeException("Nhân viên nhận phiếu không được để trống.");
        }

        Employee currentEmployee = resolveCashier(cashierEmail);

        Order order = getHeldOrderOrThrow(orderId);

        /*
         * Chỉ người đang giữ phiếu hoặc MANAGER/OWNER mới được chuyển phiếu.
         * Cashier khác không được tự lấy phiếu.
         */
        validateCanTransferHeldOrder(order, currentEmployee);

        Employee targetEmployee = employeeRepository.findById(request.getTargetEmployeeId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên nhận phiếu."));

        validateTargetEmployeeCanReceiveHeldOrder(targetEmployee);

        if (order.getCashier() != null
                && order.getCashier().getUserId() != null
                && order.getCashier().getUserId().equals(targetEmployee.getUserId())) {
            throw new RuntimeException("Phiếu này đã thuộc nhân viên được chọn.");
        }

        order.setCashier(targetEmployee);

        Order savedOrder = orderRepository.save(order);

        return toHeldOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public Map<String, Object> cancelHeldOrder(
            Integer orderId,
            String cashierEmail
    ) {
        Employee currentEmployee = resolveCashier(cashierEmail);

        Order order = getHeldOrderOrThrow(orderId);

        /*
         * Quyền hủy phiếu:
         * - CASHIER chỉ được hủy phiếu của mình
         * - MANAGER/OWNER được hủy phiếu của nhân viên khác
         */
        validateCanCancelHeldOrder(order, currentEmployee);

        /*
         * Phiếu treo hiện tại chưa trừ kho khi tạo.
         * Vì vậy hủy phiếu treo không hoàn kho.
         *
         * Voucher cũng chưa tăng usedCount khi treo phiếu,
         * nên hủy phiếu không cần giảm usedCount voucher.
         */
        order.setStatus(ORDER_STATUS_CANCELLED);
        order.setCompletedAt(null);

        Order savedOrder = orderRepository.save(order);

        return Map.of(
                "success", true,
                "orderId", savedOrder.getId(),
                "status", "CANCELLED",
                "message", "Đã hủy phiếu treo #" + savedOrder.getId()
        );
    }

    private void validateHoldRequest(PosHoldRequest request) {
        if (request == null) {
            throw new RuntimeException("Dữ liệu phiếu treo không được để trống.");
        }

        normalizeRequiredPhone(request.getCustomerPhone());
        normalizeRequiredName(request.getCustomerName());
        normalizeRequiredEmail(request.getCustomerEmail());

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Phiếu treo phải có ít nhất 1 sản phẩm.");
        }
    }

    private Customer resolveOrCreateCustomerFromHold(PosHoldRequest request) {
        PosCheckoutRequest checkoutRequest = new PosCheckoutRequest();
        checkoutRequest.setCustomerPhone(request.getCustomerPhone());
        checkoutRequest.setCustomerName(request.getCustomerName());
        checkoutRequest.setCustomerEmail(request.getCustomerEmail());
        checkoutRequest.setPaymentMethod(PAYMENT_CASH);
        checkoutRequest.setItems(request.getItems());

        return resolveOrCreateCustomer(checkoutRequest);
    }

    private Order getHeldOrderOrThrow(Integer orderId) {
        if (orderId == null) {
            throw new RuntimeException("Mã phiếu treo không được để trống.");
        }

        Order order = orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu treo."));

        if (order.getStatus() == null || order.getStatus() != ORDER_STATUS_PENDING) {
            throw new RuntimeException("Chỉ phiếu đang treo mới được xử lý.");
        }

        if (order.getPaymentMethod() == null
                || !PAYMENT_HOLD.equalsIgnoreCase(order.getPaymentMethod())) {
            throw new RuntimeException("Đây không phải phiếu treo tại quầy.");
        }

        return order;
    }

    private void validateCanViewHeldOrder(Order order, Employee currentEmployee) {
        if (isManagerOrOwner(currentEmployee)) {
            return;
        }

        if (isSameCashier(order, currentEmployee)) {
            return;
        }

        throw new RuntimeException("Bạn không có quyền xem phiếu treo của nhân viên khác.");
    }

    private void validateCanTransferHeldOrder(Order order, Employee currentEmployee) {
        if (isManagerOrOwner(currentEmployee)) {
            return;
        }

        if (isSameCashier(order, currentEmployee)) {
            return;
        }

        throw new RuntimeException("Bạn không có quyền chuyển phiếu treo của nhân viên khác.");
    }
    private void validateCanCancelHeldOrder(Order order, Employee currentEmployee) {
        if (isManagerOrOwner(currentEmployee)) {
            return;
        }

        if (isSameCashier(order, currentEmployee)) {
            return;
        }

        throw new RuntimeException("Bạn không có quyền hủy phiếu treo của nhân viên khác.");
    }
    private void validateCashierCanCheckoutHeldOrder(Order order, Employee currentEmployee) {
        if (!isSameCashier(order, currentEmployee)) {
            throw new RuntimeException("Phiếu này đang thuộc nhân viên khác. Vui lòng chuyển phiếu trước khi thanh toán.");
        }
    }

    private boolean isSameCashier(Order order, Employee employee) {
        if (order == null || order.getCashier() == null || employee == null) {
            return false;
        }

        Integer orderCashierId = order.getCashier().getUserId();
        Integer currentEmployeeId = employee.getUserId();

        return orderCashierId != null && orderCashierId.equals(currentEmployeeId);
    }

    private boolean isManagerOrOwner(Employee employee) {
        if (employee == null
                || employee.getUser() == null
                || employee.getUser().getRole() == null
                || employee.getUser().getRole().getName() == null) {
            return false;
        }

        String roleName = employee.getUser().getRole().getName()
                .trim()
                .toUpperCase(Locale.ROOT);

        return "MANAGER".equals(roleName)
                || "ROLE_MANAGER".equals(roleName)
                || "OWNER".equals(roleName)
                || "ROLE_OWNER".equals(roleName);
    }

    private void validateTargetEmployeeCanReceiveHeldOrder(Employee targetEmployee) {
        if (targetEmployee == null || targetEmployee.getUser() == null) {
            throw new RuntimeException("Nhân viên nhận phiếu không hợp lệ.");
        }

        if (targetEmployee.getUser().getStatus() == null
                || targetEmployee.getUser().getStatus() != STATUS_ACTIVE) {
            throw new RuntimeException("Nhân viên nhận phiếu đang bị khóa.");
        }
    }

    private PosHeldOrderResponse toHeldOrderResponse(Order order) {
        Customer customer = order.getCustomer();
        User customerUser = customer != null ? customer.getUser() : null;

        Employee cashier = order.getCashier();
        User cashierUser = cashier != null ? cashier.getUser() : null;

        return PosHeldOrderResponse.builder()
                .orderId(order.getId())
                .status("HELD")
                .paymentMethod(order.getPaymentMethod())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .createdAt(order.getCreatedAt())
                .cashierId(cashier != null ? cashier.getUserId() : null)
                .cashierName(cashierUser != null ? cashierUser.getName() : null)
                .customerId(customer != null ? customer.getUserId() : null)
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .customerEmail(customerUser != null ? customerUser.getEmail() : null)
                .build();
    }

    private PosOrderResponse buildResponseFromOrder(
            Order order,
            List<OrderItem> orderItems,
            String status,
            BigDecimal paidAmount,
            BigDecimal remainingAmount,
            BigDecimal cashGiven,
            BigDecimal transferAmount,
            BigDecimal changeAmount,
            String vnpayUrl,
            int loyaltyPointsEarned
    ) {
        Integer customerPointAfter = order.getCustomer() != null
                ? order.getCustomer().getLoyaltyPoints()
                : 0;

        return buildResponseFromOrder(
                order,
                orderItems,
                status,
                paidAmount,
                remainingAmount,
                cashGiven,
                transferAmount,
                changeAmount,
                vnpayUrl,
                loyaltyPointsEarned,
                customerPointAfter
        );
    }

    private PosOrderResponse buildResponseFromOrder(
            Order order,
            List<OrderItem> orderItems,
            String status,
            BigDecimal paidAmount,
            BigDecimal remainingAmount,
            BigDecimal cashGiven,
            BigDecimal transferAmount,
            BigDecimal changeAmount,
            String vnpayUrl,
            int loyaltyPointsEarned,
            Integer customerPointAfter
    ) {
        Customer customer = order.getCustomer();
        User customerUser = customer != null ? customer.getUser() : null;

        Employee cashier = order.getCashier();
        User cashierUser = cashier != null ? cashier.getUser() : null;

        List<PosOrderResponse.InvoiceItem> invoiceItems = orderItems.stream()
                .map(item -> {
                    ProductVariant variant = item.getProductVariant();

                    BigDecimal unitPrice = item.getOriginalPrice() != null
                            ? item.getOriginalPrice()
                            : BigDecimal.ZERO;

                    BigDecimal lineTotal = item.getFinalPrice() != null
                            ? item.getFinalPrice()
                            : unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

                    return PosOrderResponse.InvoiceItem.builder()
                            .productName(variant != null && variant.getProduct() != null ? variant.getProduct().getName() : null)
                            .sku(variant != null ? variant.getSku() : null)
                            .capacityLabel(variant != null ? buildCapacityLabel(variant) : null)
                            .bottleTypeName(variant != null && variant.getBottleType() != null ? variant.getBottleType().getName() : null)
                            .quantity(item.getQuantity())
                            .unitPrice(unitPrice)
                            .lineTotal(lineTotal)
                            .build();
                })
                .toList();

        return PosOrderResponse.builder()
                .orderId(order.getId())
                .status(status)
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .paymentMethod(order.getPaymentMethod())
                .paidAmount(paidAmount != null ? paidAmount : BigDecimal.ZERO)
                .remainingAmount(remainingAmount != null ? remainingAmount : BigDecimal.ZERO)
                .cashGiven(cashGiven != null ? cashGiven : BigDecimal.ZERO)
                .transferAmount(transferAmount != null ? transferAmount : BigDecimal.ZERO)
                .changeAmount(changeAmount != null ? changeAmount : BigDecimal.ZERO)
                .vnpayPaymentUrl(vnpayUrl)
                .createdAt(order.getCreatedAt())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .customerEmail(customerUser != null ? customerUser.getEmail() : null)
                .cashierName(cashierUser != null ? cashierUser.getName() : null)
                .loyaltyPointsEarned(loyaltyPointsEarned)
                .customerLoyaltyPointsAfter(customerPointAfter != null ? customerPointAfter : 0)
                .items(invoiceItems)
                .build();
    }
    @Override
    @Transactional(readOnly = true)
    public List<PosTransferTargetResponse> getTransferTargets(String cashierEmail) {
        Employee currentEmployee = resolveCashier(cashierEmail);

        return employeeRepository.findAll()
                .stream()
                .filter(this::canReceivePosTransfer)
                .filter(employee -> !employee.getUserId().equals(currentEmployee.getUserId()))
                .map(employee -> {
                    User user = employee.getUser();

                    return PosTransferTargetResponse.builder()
                            .employeeId(employee.getUserId())
                            .employeeCode(employee.getEmployeeCode())
                            .name(user != null ? user.getName() : null)
                            .email(user != null ? user.getEmail() : null)
                            .phone(user != null ? user.getPhone() : null)
                            .roleName(
                                    user != null && user.getRole() != null
                                            ? user.getRole().getName()
                                            : null
                            )
                            .jobTitle(employee.getJobTitle())
                            .build();
                })
                .toList();
    }

    private boolean canReceivePosTransfer(Employee employee) {
        if (employee == null || employee.getUser() == null) {
            return false;
        }

        User user = employee.getUser();

        if (user.getStatus() == null || user.getStatus() != STATUS_ACTIVE) {
            return false;
        }

        if (user.getRole() == null || user.getRole().getName() == null) {
            return false;
        }

        String roleName = user.getRole().getName().trim().toUpperCase(Locale.ROOT);

        return "CASHIER".equals(roleName)
                || "ROLE_CASHIER".equals(roleName)
                || "MANAGER".equals(roleName)
                || "ROLE_MANAGER".equals(roleName)
                || "OWNER".equals(roleName)
                || "ROLE_OWNER".equals(roleName);
    }
    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantPosResponse> getProductsForPos() {
        return variantRepository.findAll()
                .stream()
                .filter(variant -> !Boolean.TRUE.equals(variant.getIsDeleted()))
                .map(this::toProductVariantPosResponseForPosList)
                .toList();
    }
    private ProductVariantPosResponse toProductVariantPosResponseForPosList(ProductVariant variant) {
        Product product = variant.getProduct();

        String imageUrl = null;

        if (product != null && product.getId() != null) {
            imageUrl = productImageRepository
                    .findFirstByProduct_IdAndIsPrimaryTrue(product.getId())
                    .or(() -> productImageRepository.findFirstByProduct_Id(product.getId()))
                    .map(ProductImage::getImageUrl)
                    .orElse(null);
        }

        String unavailableReason = getVariantUnavailableReason(variant, 1);

        return ProductVariantPosResponse.builder()
                .variantId(variant.getId())
                .sku(variant.getSku())
                .productName(product != null ? product.getName() : null)
                .brandName(product != null && product.getBrand() != null ? product.getBrand().getName() : null)
                .capacityLabel(buildCapacityLabel(variant))
                .bottleTypeName(variant.getBottleType() != null ? variant.getBottleType().getName() : null)
                .price(variant.getPrice())
                .stockQuantity(variant.getStockQuantity())
                .manufacturingDate(variant.getManufacturingDate())
                .expirationDate(variant.getExpirationDate())
                .status(variant.getStatus())
                .expired(isVariantExpired(variant))
                .sellable(unavailableReason == null)
                .unavailableReason(unavailableReason)
                .imageUrl(imageUrl)
                .build();
    }
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> applyVoucher(
            String voucherCode,
            BigDecimal totalAmount
    ) {
        if (voucherCode == null || voucherCode.trim().isBlank()) {
            throw new RuntimeException("Vui lòng nhập mã giảm giá.");
        }

        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Giỏ hàng chưa có sản phẩm để áp mã giảm giá.");
        }

        String cleanCode = voucherCode.trim();

        Voucher voucher = resolveVoucher(cleanCode, totalAmount);

        BigDecimal discountAmount = calculateDiscountAmount(voucher, totalAmount);
        BigDecimal finalAmount = totalAmount.subtract(discountAmount);

        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("voucherCode", cleanCode);
        response.put("totalAmount", totalAmount);
        response.put("discountAmount", discountAmount);
        response.put("finalAmount", finalAmount);
        response.put("message", "Áp dụng mã giảm giá thành công.");

        return response;
    }
}