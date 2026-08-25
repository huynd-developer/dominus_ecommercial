package org.example.datn_sd69.modules.pos.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Customer;
import org.example.datn_sd69.entity.InventoryLot;
import org.example.datn_sd69.entity.StockMovement;
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
import org.example.datn_sd69.modules.pos.dto.request.PosSaveCustomerRequest;
import org.example.datn_sd69.modules.pos.service.PosService;
import org.example.datn_sd69.modules.vnpay.service.VNPayService;
import org.example.datn_sd69.repository.CustomerRepository;
import org.example.datn_sd69.repository.EmployeeRepository;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.example.datn_sd69.repository.OrderRepository;
import org.example.datn_sd69.repository.ProductImageRepository;
import org.example.datn_sd69.repository.ProductVariantRepository;
import org.example.datn_sd69.repository.InventoryLotRepository;
import org.example.datn_sd69.repository.StockMovementRepository;
import org.example.datn_sd69.repository.RoleRepository;
import org.example.datn_sd69.repository.UserRepository;
import org.example.datn_sd69.repository.VoucherRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.example.datn_sd69.modules.pos.dto.request.PosTransferHeldOrderRequest;
import org.example.datn_sd69.modules.vietqr.dto.VietQrResponse;
import org.example.datn_sd69.modules.vietqr.service.VietQrService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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
    private static final String PAYMENT_VIETQR = "VIETQR";
    private static final String PAYMENT_MIXED = "MIXED";

    /*
     * Lưu DB để phân biệt rõ:
     * - MIXED_VNPAY  = Tiền mặt + VNPay
     * - MIXED_VIETQR = Tiền mặt + VietQR
     */
    private static final String PAYMENT_MIXED_VNPAY = "MIXED_VNPAY";
    private static final String PAYMENT_MIXED_VIETQR = "MIXED_VIETQR";

    private static final String TRANSFER_PROVIDER_VNPAY = "VNPAY";
    private static final String TRANSFER_PROVIDER_VIETQR = "VIETQR";

    private static final String PAYMENT_HOLD = "HOLD";
    private static final BigDecimal POINT_RATE_AMOUNT = BigDecimal.valueOf(10_000);

    /*
     * InventoryLot / StockMovement
     *
     * 3 = SALE_OUT
     * 4 = RETURN_IN
     */
    private static final byte MOVEMENT_SALE_OUT = 3;
    private static final byte MOVEMENT_RETURN_IN = 4;

    /*
     * ReferenceType riêng cho POS để có thể truy vết và hoàn đúng
     * các lot đã xuất khi payment pending bị hủy.
     */
    private static final String STOCK_REFERENCE_POS_ORDER = "POS_ORDER";
    private static final String STOCK_REFERENCE_POS_ROLLBACK = "POS_ORDER_ROLLBACK";

    private final ProductVariantRepository variantRepository;
    private final InventoryLotRepository inventoryLotRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductImageRepository productImageRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final EmployeeRepository employeeRepository;
    private final VoucherRepository voucherRepository;
    private final VNPayService vnPayService;
    private final VietQrService vietQrService;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(readOnly = true)
    public ProductVariantPosResponse findVariantBySku(String sku) {
        String cleanSku = normalizeSku(sku);

        ProductVariant variant = variantRepository.findPosVisibleBySku(cleanSku).orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với SKU: " + cleanSku));

        validateVariantCanSell(variant, 1);

        Product product = variant.getProduct();

        String imageUrl = null;

        if (product != null && product.getId() != null) {
            imageUrl = productImageRepository.findFirstByProduct_IdAndIsPrimaryTrue(product.getId()).or(() -> productImageRepository.findFirstByProduct_Id(product.getId())).map(ProductImage::getImageUrl).orElse(null);
        }

        String unavailableReason = getVariantUnavailableReason(variant, 1);

        int sellableQuantity = getSellableQuantity(variant);
        InventoryLot nextFefoLot = getNextSellableLot(variant);

        return ProductVariantPosResponse.builder()
                .variantId(variant.getId())
                .sku(variant.getSku())
                .productName(product != null ? product.getName() : null)
                .brandName(product != null && product.getBrand() != null ? product.getBrand().getName() : null)
                .capacityLabel(buildCapacityLabel(variant))
                .bottleTypeName(variant.getBottleType() != null ? variant.getBottleType().getName() : null)
                .price(variant.getPrice())
                /*
                 * stockQuantity giữ để FE POS hiện tại chưa bị vỡ contract,
                 * nhưng giá trị là tồn CÓ THỂ BÁN thật từ InventoryLot.
                 */
                .stockQuantity(sellableQuantity)
                .sellableQuantity(sellableQuantity)
                /*
                 * Hai field ngày giữ compatibility.
                 * Nếu có hàng bán được, đây là ngày của lot FEFO tiếp theo.
                 */
                .manufacturingDate(nextFefoLot != null ? nextFefoLot.getManufacturedDate() : null)
                .expirationDate(nextFefoLot != null ? nextFefoLot.getExpirationDate() : null)
                .status(variant.getStatus())
                .expired(isSkuUnavailableBecauseExpired(variant))
                .sellable(unavailableReason == null)
                .unavailableReason(unavailableReason)
                .imageUrl(imageUrl)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerPosResponse findCustomerByPhone(String phone) {
        String cleanPhone = normalizeRequiredPhone(phone);

        return customerRepository.findByUserPhone(cleanPhone).map(this::toCustomerPosResponse).orElse(null);
    }

    @Override
    @Transactional
    public CustomerPosResponse saveCustomerForPos(PosSaveCustomerRequest request) {
        if (request == null) {
            throw new RuntimeException("Thông tin khách hàng không được để trống.");
        }

        Customer customer = resolveOrCreateCustomer(request.getPhone(), request.getName(), request.getEmail());

        return toCustomerPosResponse(customer);
    }

    @Override
    @Transactional
    public PosOrderResponse checkout(PosCheckoutRequest request, String cashierEmail, String clientIp) {
        validateCheckoutRequest(request);

        Employee cashier = resolveCashier(cashierEmail);
        Customer customer = resolveOrCreateCustomer(request);

        Map<String, Integer> skuQuantityMap = mergeItemsBySku(request.getItems());
        Map<String, BigDecimal> expectedUnitPriceBySku =
                buildExpectedUnitPriceBySku(request.getItems());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<LineItem> lineItems = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : skuQuantityMap.entrySet()) {
            String sku = entry.getKey();
            Integer quantity = entry.getValue();

            ProductVariant variant = variantRepository.findPosVisibleBySku(sku)
                    .orElseThrow(() -> new RuntimeException("SKU không hợp lệ: " + sku));

            validateVariantCanSell(variant, quantity);

            BigDecimal unitPrice = variant.getPrice() != null
                    ? variant.getPrice()
                    : BigDecimal.ZERO;

            validateExpectedUnitPrice(
                    sku,
                    expectedUnitPriceBySku.get(sku),
                    unitPrice
            );

            BigDecimal lineTotal =
                    unitPrice.multiply(BigDecimal.valueOf(quantity));

            totalAmount = totalAmount.add(lineTotal);
            lineItems.add(new LineItem(variant, quantity, unitPrice, lineTotal));
        }

        Voucher appliedVoucher = resolveVoucherForMutation(request.getVoucherCode(), totalAmount);

        BigDecimal discountAmount = calculateDiscountAmount(appliedVoucher, totalAmount);
        BigDecimal finalAmount = totalAmount.subtract(discountAmount);

        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        /*
         * Chặn POS checkout dùng dữ liệu FE đã stale.
         *
         * expected* chỉ là snapshot FE đang hiển thị.
         * Backend vẫn tự đọc giá ProductVariant, voucher và InventoryLot hiện tại.
         * Nếu snapshot khác dữ liệu Backend vừa tính, trả 409 trước khi tạo Order
         * hoặc xuất kho để thu ngân xem lại số tiền mới.
         */
        validatePosSnapshot(
                request.getExpectedTotalAmount(),
                request.getExpectedDiscountAmount(),
                request.getExpectedFinalAmount(),
                totalAmount,
                discountAmount,
                finalAmount
        );

        PaymentSummary paymentSummary =
                validateAndBuildPaymentSummary(request, finalAmount);

        /*
         * POS giữ 1 lượt Voucher ngay khi bắt đầu checkout, kể cả payment online
         * còn PENDING. Nhờ vậy Voucher không thể bị đơn khác lấy mất lượt trong
         * lúc VNPay/VietQR đang chờ callback. Nếu payment bị hủy/thất bại thì
         * các flow rollback bên dưới sẽ hoàn đúng 1 lượt.
         */
        reserveVoucherUsage(appliedVoucher);

        User customerUser = customer.getUser();
        boolean completedImmediately = paymentSummary.completedImmediately();

        Order order = new Order();
        order.setOrderType("IN_STORE");
        order.setCustomer(customer);
        order.setCashier(cashier);
        order.setVoucher(appliedVoucher);
        order.setPaymentMethod(paymentSummary.orderPaymentMethod());
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
            /*
             * POS bán theo giá tại quầy: dùng ProductVariant.Price.
             * Không tự áp dụng Flash Sale online.
             *
             * Quy ước chung của OrderItem sau khi sửa DB:
             * - OriginalPrice = đơn giá gốc tại thời điểm bán
             * - DiscountAmount = giảm giá trên 1 sản phẩm
             * - FinalPrice = đơn giá sau giảm trên 1 sản phẩm
             * - Tổng dòng = FinalPrice * Quantity
             */
            orderItem.setOriginalPrice(line.unitPrice());
            orderItem.setDiscountAmount(BigDecimal.ZERO);
            orderItem.setFinalPrice(line.unitPrice());

            orderItemRepository.save(orderItem);

            /*
             * Với CASH: xuất kho ngay.
             * Với VNPAY/VIETQR/MIXED pending: giữ hàng theo nghiệp vụ cũ
             * bằng SALE_OUT ngay; nếu payment bị hủy sẽ RETURN_IN đúng lot.
             *
             * Không trừ ProductVariant.StockQuantity.
             * InventoryLot được phân bổ theo FEFO.
             */
            postSaleOutByFefo(
                    variant,
                    line.quantity(),
                    savedOrder.getId(),
                    cashier.getUserId()
            );

            invoiceItems.add(PosOrderResponse.InvoiceItem.builder().productName(variant.getProduct() != null ? variant.getProduct().getName() : null).sku(variant.getSku()).capacityLabel(buildCapacityLabel(variant)).bottleTypeName(variant.getBottleType() != null ? variant.getBottleType().getName() : null).quantity(line.quantity()).unitPrice(line.unitPrice()).lineTotal(line.lineTotal()).build());
        }

        int loyaltyPointsEarned = 0;
        Integer customerLoyaltyPointsAfter = customer.getLoyaltyPoints();

        if (completedImmediately) {
            loyaltyPointsEarned = applyLoyaltyPointsIfNeeded(savedOrder, customer);
            customerLoyaltyPointsAfter = customer.getLoyaltyPoints();
        }

        String vnpayUrl = null;
        String vietQrImageUrl = null;
        String vietQrContent = null;

        if (paymentSummary.needVnpayPayment()) {
            vnpayUrl = vnPayService.createPaymentUrl(savedOrder.getId(), paymentSummary.transferAmount(), clientIp);
        }

        if (paymentSummary.needVietQrPayment()) {
            VietQrResponse vietQr = vietQrService.createPaymentQr(savedOrder.getId(), paymentSummary.transferAmount());

            vietQrImageUrl = vietQr.getQrImageUrl();
            vietQrContent = vietQr.getTransferContent();
        }

        return PosOrderResponse.builder().orderId(savedOrder.getId()).status(completedImmediately ? "COMPLETED" : "PENDING_PAYMENT").totalAmount(totalAmount).discountAmount(discountAmount).finalAmount(finalAmount).voucherCode(appliedVoucher != null ? appliedVoucher.getCode() : null).paymentMethod(paymentSummary.responsePaymentMethod()).transferProvider(paymentSummary.transferProvider()).paidAmount(paymentSummary.paidAmount()).remainingAmount(paymentSummary.remainingAmount()).cashGiven(paymentSummary.cashGiven()).transferAmount(paymentSummary.transferAmount()).changeAmount(paymentSummary.changeAmount()).vnpayPaymentUrl(vnpayUrl).vietQrImageUrl(vietQrImageUrl).vietQrContent(vietQrContent).createdAt(savedOrder.getCreatedAt()).cashierName(cashier.getUser() != null ? cashier.getUser().getName() : null).customerName(savedOrder.getCustomerName()).customerPhone(savedOrder.getCustomerPhone()).customerEmail(customer.getUser() != null ? customer.getUser().getEmail() : request.getCustomerEmail()).loyaltyPointsEarned(loyaltyPointsEarned).customerLoyaltyPointsAfter(customerLoyaltyPointsAfter).items(invoiceItems).build();
    }

    private CustomerPosResponse toCustomerPosResponse(Customer customer) {
        User user = customer.getUser();

        return CustomerPosResponse.builder().customerId(customer.getUserId()).name(user != null ? user.getName() : null).phone(user != null ? user.getPhone() : null).email(user != null ? user.getEmail() : null).customerRank(customer.getCustomerRank()).loyaltyPoints(customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0).build();
    }

    /**
     * DB hiện tại Customer.UserId là PK/FK tới Users.Id.
     * <p>
     * Nghiệp vụ POS:
     * 1. POS chỉ tìm/dùng lại khách hàng hoặc tạo khách hàng mới.
     * 2. SĐT + email của khách đã tồn tại phải khớp cùng một User/Customer.
     * 3. POS không tự cập nhật name/phone/email của User đã tồn tại.
     * 4. Nếu phone/email xung đột thì chặn để tránh ghi đè dữ liệu khách hàng.
     * 5. User role USER đã tồn tại nhưng chưa có Customer chỉ được tạo Customer
     * khi cả phone và email đều cùng thuộc chính User đó.
     */
    private Customer resolveOrCreateCustomer(PosCheckoutRequest request) {
        if (request == null) {
            throw new RuntimeException("Thông tin khách hàng không được để trống.");
        }

        return resolveOrCreateCustomer(request.getCustomerPhone(), request.getCustomerName(), request.getCustomerEmail());
    }

    private Customer resolveOrCreateCustomer(String rawPhone, String rawName, String rawEmail) {
        String phone = normalizeRequiredPhone(rawPhone);
        String name = normalizeRequiredName(rawName);
        String email = normalizeRequiredEmail(rawEmail);

        /*
         * 1. Customer đã tồn tại theo SĐT.
         * Chỉ dùng lại khi email request đúng với email hiện tại của Customer đó.
         * Tuyệt đối không dùng POS để đổi email/phone/name của khách cũ.
         */
        Customer customerByPhone = customerRepository.findByUserPhone(phone).orElse(null);

        if (customerByPhone != null) {
            User user = requireCustomerUser(customerByPhone);

            if (!sameEmail(user.getEmail(), email)) {
                throw new RuntimeException(
                        "Số điện thoại đã thuộc khách hàng có email khác. Vui lòng kiểm tra lại thông tin khách hàng."
                );
            }

            normalizeCustomerDefaultInfo(customerByPhone);
            return customerRepository.save(customerByPhone);
        }

        /*
         * 2. Không có Customer theo SĐT nhưng email đã thuộc Customer khác.
         * Đây là case gây lỗi cũ: phone mới + email cũ làm đổi SĐT của User.
         * Logic đúng là chặn, không update User.
         */
        Customer customerByEmail = customerRepository.findByUserEmailIgnoreCase(email).orElse(null);

        if (customerByEmail != null) {
            User user = requireCustomerUser(customerByEmail);

            if (!samePhone(user.getPhone(), phone)) {
                throw new RuntimeException(
                        "Email đã thuộc khách hàng có số điện thoại khác. Vui lòng sử dụng đúng thông tin khách hàng."
                );
            }

            normalizeCustomerDefaultInfo(customerByEmail);
            return customerRepository.save(customerByEmail);
        }

        /*
         * 3. Chưa có Customer tương ứng, kiểm tra trực tiếp bảng Users.
         * Trường hợp này giữ tương thích với dữ liệu User role USER đã tồn tại
         * nhưng chưa có bản ghi Customer.
         */
        User userByPhone = userRepository.findByPhone(phone).orElse(null);
        User userByEmail = userRepository.findByEmailIgnoreCase(email).orElse(null);

        if (userByPhone != null
                && userByEmail != null
                && !userByPhone.getId().equals(userByEmail.getId())) {
            throw new RuntimeException(
                    "Số điện thoại và email đang thuộc hai tài khoản khác nhau. Vui lòng kiểm tra lại thông tin khách hàng."
            );
        }

        if (userByPhone != null) {
            if (!isUserRole(userByPhone)) {
                throw new RuntimeException(
                        "Số điện thoại này đã thuộc tài khoản nhân viên/quản trị. Vui lòng nhập số điện thoại khác cho khách hàng."
                );
            }

            if (userByPhone.getStatus() == null || userByPhone.getStatus() != STATUS_ACTIVE) {
                throw new RuntimeException("Tài khoản khách hàng đã bị khóa.");
            }

            if (!sameEmail(userByPhone.getEmail(), email)) {
                throw new RuntimeException(
                        "Số điện thoại đã thuộc tài khoản có email khác. Vui lòng kiểm tra lại thông tin khách hàng."
                );
            }

            if (userByEmail != null && !userByEmail.getId().equals(userByPhone.getId())) {
                throw new RuntimeException("Email đã thuộc tài khoản khác.");
            }

            Customer newCustomer = new Customer();
            newCustomer.setUser(userByPhone);
            newCustomer.setCustomerRank("BRONZE");
            newCustomer.setLoyaltyPoints(0);
            newCustomer.setDateOfBirth(null);
            newCustomer.setGender(null);

            return customerRepository.save(newCustomer);
        }

        if (userByEmail != null) {
            if (!isUserRole(userByEmail)) {
                throw new RuntimeException(
                        "Email này đã thuộc tài khoản nhân viên/quản trị. Vui lòng nhập email khác cho khách hàng."
                );
            }

            if (userByEmail.getStatus() == null || userByEmail.getStatus() != STATUS_ACTIVE) {
                throw new RuntimeException("Tài khoản khách hàng đã bị khóa.");
            }

            /*
             * Có User theo email nhưng không có User theo phone => phone request
             * không phải phone hiện tại của User đó. Không được tự đổi phone.
             */
            throw new RuntimeException(
                    "Email đã thuộc tài khoản có số điện thoại khác. Vui lòng sử dụng đúng thông tin khách hàng."
            );
        }

        /*
         * 4. Cả phone và email đều chưa tồn tại => tạo User + Customer mới.
         */
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

    private Customer resolveOrCreateCustomerFromHold(PosHoldRequest request) {
        if (request == null) {
            throw new RuntimeException("Thông tin khách hàng không được để trống.");
        }

        return resolveOrCreateCustomer(request.getCustomerPhone(), request.getCustomerName(), request.getCustomerEmail());
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

    private boolean samePhone(String dbPhone, String normalizedPhone) {
        if (dbPhone == null || normalizedPhone == null) {
            return false;
        }

        String cleanDbPhone = dbPhone.replaceAll("[\\s.-]", "").trim();
        return cleanDbPhone.equals(normalizedPhone);
    }

    private boolean sameEmail(String dbEmail, String normalizedEmail) {
        if (dbEmail == null || normalizedEmail == null) {
            return false;
        }

        return dbEmail.trim().equalsIgnoreCase(normalizedEmail);
    }

    private void normalizeCustomerDefaultInfo(Customer customer) {
        if (customer.getCustomerRank() == null || customer.getCustomerRank().isBlank()) {
            customer.setCustomerRank("BRONZE");
        }

        if (customer.getLoyaltyPoints() == null) {
            customer.setLoyaltyPoints(0);
        }
    }

    /**
     * Snapshot từ FE chỉ dùng để phát hiện màn hình POS đang stale.
     *
     * Không dùng expected* để tính tiền hoặc ghi Order.
     */
    private void validatePosSnapshot(
            BigDecimal expectedTotalAmount,
            BigDecimal expectedDiscountAmount,
            BigDecimal expectedFinalAmount,
            BigDecimal currentTotalAmount,
            BigDecimal currentDiscountAmount,
            BigDecimal currentFinalAmount
    ) {
        List<String> changedFields = new ArrayList<>();

        if (isMoneyChanged(expectedTotalAmount, currentTotalAmount)) {
            changedFields.add("tạm tính");
        }

        if (isMoneyChanged(expectedDiscountAmount, currentDiscountAmount)) {
            changedFields.add("giảm giá");
        }

        if (isMoneyChanged(expectedFinalAmount, currentFinalAmount)) {
            changedFields.add("tổng thanh toán");
        }

        if (!changedFields.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Thông tin bán hàng đã thay đổi ("
                            + String.join(", ", changedFields)
                            + "). Vui lòng cập nhật lại dữ liệu POS và xác nhận lại."
            );
        }
    }

    private boolean isMoneyChanged(
            BigDecimal expectedAmount,
            BigDecimal currentAmount
    ) {
        if (expectedAmount == null) {
            return false;
        }

        BigDecimal safeCurrent =
                currentAmount != null
                        ? currentAmount
                        : BigDecimal.ZERO;

        return expectedAmount.compareTo(safeCurrent) != 0;
    }

    private Map<String, BigDecimal> buildExpectedUnitPriceBySku(
            List<PosItemRequest> items
    ) {
        Map<String, BigDecimal> expectedUnitPriceBySku =
                new LinkedHashMap<>();

        if (items == null || items.isEmpty()) {
            return expectedUnitPriceBySku;
        }

        for (PosItemRequest item : items) {
            if (item == null || item.getExpectedUnitPrice() == null) {
                continue;
            }

            String sku = normalizeSku(item.getSku());
            BigDecimal expectedUnitPrice = item.getExpectedUnitPrice();

            BigDecimal previous =
                    expectedUnitPriceBySku.putIfAbsent(
                            sku,
                            expectedUnitPrice
                    );

            if (previous != null
                    && previous.compareTo(expectedUnitPrice) != 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Giá hiển thị của SKU "
                                + sku
                                + " không nhất quán. Vui lòng cập nhật lại POS."
                );
            }
        }

        return expectedUnitPriceBySku;
    }

    private void validateExpectedUnitPrice(
            String sku,
            BigDecimal expectedUnitPrice,
            BigDecimal currentUnitPrice
    ) {
        if (expectedUnitPrice == null) {
            return;
        }

        BigDecimal safeCurrent =
                currentUnitPrice != null
                        ? currentUnitPrice
                        : BigDecimal.ZERO;

        if (expectedUnitPrice.compareTo(safeCurrent) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Giá SKU "
                            + sku
                            + " đã thay đổi từ "
                            + expectedUnitPrice.toPlainString()
                            + " thành "
                            + safeCurrent.toPlainString()
                            + ". Vui lòng cập nhật lại POS và xác nhận lại."
            );
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

    private PaymentSummary validateAndBuildPaymentSummary(PosCheckoutRequest request, BigDecimal finalAmount) {
        String method = normalizePaymentMethod(request.getPaymentMethod());

        BigDecimal cashGiven = request.getCashGiven() != null ? request.getCashGiven() : BigDecimal.ZERO;

        BigDecimal transferAmount = request.getTransferAmount() != null ? request.getTransferAmount() : BigDecimal.ZERO;

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
            return new PaymentSummary(PAYMENT_CASH, PAYMENT_CASH, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, false, true);
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

                throw new RuntimeException("Tiền khách đưa còn thiếu " + missingAmount.toPlainString() + " đồng. Vui lòng chọn thêm phương thức thanh toán.");
            }

            BigDecimal changeAmount = cashGiven.subtract(finalAmount);

            return new PaymentSummary(PAYMENT_CASH, PAYMENT_CASH, null, cashGiven, BigDecimal.ZERO, finalAmount, BigDecimal.ZERO, changeAmount, false, false, true);
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

            return new PaymentSummary(PAYMENT_VNPAY, PAYMENT_VNPAY, TRANSFER_PROVIDER_VNPAY, BigDecimal.ZERO, transferAmount, BigDecimal.ZERO, transferAmount, BigDecimal.ZERO, true, false, false);
        }

        if (PAYMENT_VIETQR.equals(method)) {
            if (cashGiven.compareTo(BigDecimal.ZERO) > 0) {
                throw new RuntimeException("Thanh toán VIETQR không được gửi tiền mặt. Nếu khách đưa một phần tiền mặt, hãy dùng MIXED.");
            }

            if (transferAmount.compareTo(BigDecimal.ZERO) == 0) {
                transferAmount = finalAmount;
            }

            if (transferAmount.compareTo(finalAmount) != 0) {
                throw new RuntimeException("Số tiền VietQR phải bằng đúng số tiền cần thanh toán");
            }

            return new PaymentSummary(PAYMENT_VIETQR, PAYMENT_VIETQR, TRANSFER_PROVIDER_VIETQR, BigDecimal.ZERO, transferAmount, BigDecimal.ZERO, transferAmount, BigDecimal.ZERO, false, true, false);
        }

        if (PAYMENT_MIXED.equals(method) || PAYMENT_MIXED_VNPAY.equals(method) || PAYMENT_MIXED_VIETQR.equals(method)) {

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

                throw new RuntimeException("Khách còn thiếu " + missingAmount.toPlainString() + " đồng. Vui lòng chọn thêm phương thức thanh toán.");
            }

            if (totalPaid.compareTo(finalAmount) > 0) {
                throw new RuntimeException("Tổng tiền mặt và chuyển khoản đang vượt quá số tiền cần thanh toán. " + "Nếu khách đưa dư tiền mặt, hãy trả lại tiền thừa ngay và chỉ nhập số tiền thực dùng để thanh toán.");
            }

            String transferProvider = resolveMixedTransferProvider(method, request.getTransferProvider());

            String orderPaymentMethod = TRANSFER_PROVIDER_VNPAY.equals(transferProvider) ? PAYMENT_MIXED_VNPAY : PAYMENT_MIXED_VIETQR;

            return new PaymentSummary(PAYMENT_MIXED, orderPaymentMethod, transferProvider, cashGiven, transferAmount, cashGiven, transferAmount, BigDecimal.ZERO, TRANSFER_PROVIDER_VNPAY.equals(transferProvider), TRANSFER_PROVIDER_VIETQR.equals(transferProvider), false);
        }

        throw new RuntimeException("Phương thức thanh toán không hợp lệ.");
    }

    private record PaymentSummary(String responsePaymentMethod, String orderPaymentMethod, String transferProvider,
                                  BigDecimal cashGiven, BigDecimal transferAmount, BigDecimal paidAmount,
                                  BigDecimal remainingAmount, BigDecimal changeAmount, boolean needVnpayPayment,
                                  boolean needVietQrPayment, boolean completedImmediately) {
    }

    private Map<String, Integer> mergeItemsBySku(List<PosItemRequest> items) {
        Map<String, Integer> skuQuantityMap = new LinkedHashMap<>();

        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Danh sách sản phẩm không được để trống.");
        }

        for (PosItemRequest item : items) {
            if (item == null) {
                continue;
            }

            String sku = normalizeSku(item.getSku());
            Integer quantity = item.getQuantity();

            if (sku.isBlank()) {
                throw new RuntimeException("SKU sản phẩm không được để trống.");
            }

            if (quantity == null || quantity <= 0) {
                throw new RuntimeException("Số lượng sản phẩm phải lớn hơn 0.");
            }

            skuQuantityMap.merge(sku, quantity, Integer::sum);
        }

        if (skuQuantityMap.isEmpty()) {
            throw new RuntimeException("Danh sách sản phẩm không hợp lệ.");
        }

        return skuQuantityMap;
    }

    private List<LineItem> buildLineItemsFromRequestItems(List<PosItemRequest> items) {
        Map<String, Integer> skuQuantityMap = mergeItemsBySku(items);
        Map<String, BigDecimal> expectedUnitPriceBySku =
                buildExpectedUnitPriceBySku(items);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<LineItem> lineItems = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : skuQuantityMap.entrySet()) {
            String sku = entry.getKey();
            Integer quantity = entry.getValue();

            ProductVariant variant = variantRepository.findPosVisibleBySku(sku)
                    .orElseThrow(() -> new RuntimeException("SKU không hợp lệ: " + sku));

            validateVariantCanSell(variant, quantity);

            BigDecimal unitPrice = variant.getPrice() != null
                    ? variant.getPrice()
                    : BigDecimal.ZERO;

            validateExpectedUnitPrice(
                    sku,
                    expectedUnitPriceBySku.get(sku),
                    unitPrice
            );

            BigDecimal lineTotal =
                    unitPrice.multiply(BigDecimal.valueOf(quantity));

            totalAmount = totalAmount.add(lineTotal);
            lineItems.add(new LineItem(variant, quantity, unitPrice, lineTotal));
        }

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Tổng tiền phiếu treo không hợp lệ.");
        }

        return lineItems;
    }

    private BigDecimal calculateTotalAmountFromLineItems(List<LineItem> lineItems) {
        if (lineItems == null || lineItems.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return lineItems.stream().map(LineItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record LineItem(ProductVariant variant, Integer quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    private void validateNoActiveHeldOrderForPhone(String rawPhone) {
        String cleanPhone = normalizeRequiredPhone(rawPhone);

        List<Order> existingHeldOrders = orderRepository.findActiveHeldOrdersByCustomerPhone(cleanPhone);

        if (existingHeldOrders == null || existingHeldOrders.isEmpty()) {
            return;
        }

        Order existingOrder = existingHeldOrders.get(0);

        throw new RuntimeException("Khách hàng này đang có phiếu treo #" + existingOrder.getId() + " chưa thanh toán. Vui lòng mở phiếu treo đó để cập nhật sản phẩm.");
    }

    @Override
    @Transactional
    public PosOrderResponse holdOrder(PosHoldRequest request, String cashierEmail) {
        validateHoldRequest(request);

        /*
         * Không cho cùng một SĐT có nhiều phiếu treo chưa thanh toán.
         * Nếu khách muốn mua thêm thì mở phiếu treo cũ và cập nhật sản phẩm/voucher.
         */
        validateNoActiveHeldOrderForPhone(request.getCustomerPhone());

        Employee cashier = resolveCashier(cashierEmail);
        Customer customer = resolveOrCreateCustomerFromHold(request);

        List<LineItem> lineItems = buildLineItemsFromRequestItems(request.getItems());
        BigDecimal totalAmount = calculateTotalAmountFromLineItems(lineItems);

        Voucher appliedVoucher = resolveVoucher(request.getVoucherCode(), totalAmount);

        BigDecimal discountAmount = calculateDiscountAmount(appliedVoucher, totalAmount);
        BigDecimal finalAmount = totalAmount.subtract(discountAmount);

        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        validatePosSnapshot(
                request.getExpectedTotalAmount(),
                request.getExpectedDiscountAmount(),
                request.getExpectedFinalAmount(),
                totalAmount,
                discountAmount,
                finalAmount
        );

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

        List<OrderItem> savedItems = saveHeldOrderItems(savedOrder, lineItems);

        return buildResponseFromOrder(savedOrder, savedItems, "HELD", BigDecimal.ZERO, finalAmount, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, 0);
    }

    /**
     * Cập nhật phiếu treo đang mở.
     * <p>
     * Chỉ cập nhật:
     * - sản phẩm
     * - số lượng
     * - voucher
     * - tổng tiền
     * <p>
     * Không cập nhật khách hàng.
     */
    @Override
    @Transactional
    public PosOrderResponse updateHeldOrder(Integer orderId, PosHoldRequest request, String cashierEmail) {
        validateHoldRequest(request);

        Employee currentEmployee = resolveCashier(cashierEmail);
        Order order = getHeldOrderForUpdateOrThrow(orderId);

        validateCanTransferHeldOrder(order, currentEmployee);

        List<LineItem> lineItems = buildLineItemsFromRequestItems(request.getItems());
        BigDecimal totalAmount = calculateTotalAmountFromLineItems(lineItems);

        Voucher appliedVoucher = resolveVoucher(request.getVoucherCode(), totalAmount);

        BigDecimal discountAmount = calculateDiscountAmount(appliedVoucher, totalAmount);
        BigDecimal finalAmount = totalAmount.subtract(discountAmount);

        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        validatePosSnapshot(
                request.getExpectedTotalAmount(),
                request.getExpectedDiscountAmount(),
                request.getExpectedFinalAmount(),
                totalAmount,
                discountAmount,
                finalAmount
        );

        /*
         * Phiếu treo chưa trừ kho, nên cập nhật sản phẩm chỉ cần thay OrderItem.
         * Không hoàn kho, không trừ kho ở bước này.
         */
        List<OrderItem> oldItems = orderItemRepository.findByOrderIdWithVariant(order.getId());

        if (!oldItems.isEmpty()) {
            orderItemRepository.deleteAll(oldItems);
            orderItemRepository.flush();
        }

        order.setVoucher(appliedVoucher);
        order.setPaymentMethod(PAYMENT_HOLD);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setFinalAmount(finalAmount);
        order.setStatus(ORDER_STATUS_PENDING);
        order.setCompletedAt(null);

        /*
         * Không set lại customer/customerName/customerPhone.
         * Đây là yêu cầu nghiệp vụ: mở phiếu treo không được sửa khách hàng.
         */
        Order savedOrder = orderRepository.save(order);

        List<OrderItem> savedItems = saveHeldOrderItems(savedOrder, lineItems);

        return buildResponseFromOrder(savedOrder, savedItems, "HELD", BigDecimal.ZERO, finalAmount, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, 0);
    }

    private List<OrderItem> saveHeldOrderItems(Order order, List<LineItem> lineItems) {
        List<OrderItem> savedItems = new ArrayList<>();

        for (LineItem line : lineItems) {
            ProductVariant variant = line.variant();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductVariant(variant);
            orderItem.setQuantity(line.quantity());
            /*
             * Phiếu treo POS cũng lưu giá theo đơn vị 1 sản phẩm.
             * Không lưu FinalPrice = lineTotal vì sẽ sai với CHECK constraint mới.
             */
            orderItem.setOriginalPrice(line.unitPrice());
            orderItem.setDiscountAmount(BigDecimal.ZERO);
            orderItem.setFinalPrice(line.unitPrice());

            savedItems.add(orderItemRepository.save(orderItem));
        }

        return savedItems;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosHeldOrderResponse> getHeldOrders(String cashierEmail) {
        Employee currentEmployee = resolveCashier(cashierEmail);

        /*
         * CASHIER chỉ thấy phiếu của chính mình.
         * MANAGER/OWNER thấy tất cả để có thể chuyển/hủy khi cần.
         */
        Integer cashierIdFilter = isManagerOrOwner(currentEmployee)
                ? null
                : currentEmployee.getUserId();

        return orderRepository.findHeldOrders(cashierIdFilter)
                .stream()
                /*
                 * Chặn thêm ở service để tránh FE hiển thị nhầm đơn đã chuyển
                 * sang VNPAY/MIXED_VNPAY/VIETQR nhưng còn sót trong state/cache.
                 */
                .filter(this::isHeldOrderAtCounter)
                .map(order -> toHeldOrderResponse(order, currentEmployee))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PosOrderResponse getHeldOrderDetail(Integer orderId, String cashierEmail) {
        Employee currentEmployee = resolveCashier(cashierEmail);

        Order order = getHeldOrderOrThrow(orderId);

        validateCanViewHeldOrder(order, currentEmployee);

        List<OrderItem> items = orderItemRepository.findByOrderIdWithVariant(order.getId());

        PosOrderResponse response = buildResponseFromOrder(
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

        applyHeldOrderPermissions(response, order, currentEmployee);

        return response;
    }

    @Override
    @Transactional
    public PosOrderResponse checkoutHeldOrder(Integer orderId, PosHeldOrderCheckoutRequest request, String cashierEmail, String clientIp) {
        if (request == null) {
            throw new RuntimeException("Dữ liệu thanh toán phiếu treo không được để trống.");
        }

        Employee currentEmployee = resolveCashier(cashierEmail);

        Order order = getHeldOrderForUpdateOrThrow(orderId);

        /*
         * Một nhân viên chỉ được thanh toán phiếu/hóa đơn của chính mình.
         * Nếu nhân viên khác muốn thanh toán thì phải chuyển phiếu trước.
         */
        validateCashierCanCheckoutHeldOrder(order, currentEmployee);

        List<OrderItem> orderItems = orderItemRepository.findByOrderIdWithVariant(order.getId());

        if (orderItems.isEmpty()) {
            throw new RuntimeException("Phiếu treo không có sản phẩm.");
        }

        BigDecimal totalAmount =
                order.getTotalAmount() != null
                        ? order.getTotalAmount()
                        : BigDecimal.ZERO;

        BigDecimal discountAmount =
                order.getDiscountAmount() != null
                        ? order.getDiscountAmount()
                        : BigDecimal.ZERO;

        BigDecimal finalAmount =
                order.getFinalAmount() != null
                        ? order.getFinalAmount()
                        : BigDecimal.ZERO;

        /*
         * Phiếu treo là snapshot đã lưu trong Order/OrderItem.
         * Không tự re-price theo ProductVariant ở đây để không đổi nghiệp vụ cũ.
         * Chỉ chặn nếu FE đang mở bản phiếu treo cũ hơn dữ liệu Order hiện tại.
         */
        validatePosSnapshot(
                request.getExpectedTotalAmount(),
                request.getExpectedDiscountAmount(),
                request.getExpectedFinalAmount(),
                totalAmount,
                discountAmount,
                finalAmount
        );

        PosCheckoutRequest paymentRequest = new PosCheckoutRequest();
        paymentRequest.setPaymentMethod(request.getPaymentMethod());
        paymentRequest.setCashGiven(request.getCashGiven());
        paymentRequest.setTransferAmount(request.getTransferAmount());
        paymentRequest.setTransferProvider(request.getTransferProvider());
        paymentRequest.setCustomerPhone(order.getCustomerPhone());
        paymentRequest.setCustomerName(order.getCustomerName());
        paymentRequest.setCustomerEmail(order.getCustomer() != null && order.getCustomer().getUser() != null ? order.getCustomer().getUser().getEmail() : null);
        paymentRequest.setItems(List.of(new PosItemRequest()));

        PaymentSummary paymentSummary = validateAndBuildPaymentSummary(paymentRequest, finalAmount);

        /*
         * Phiếu treo không giữ lượt Voucher. Chỉ khi bắt đầu thanh toán mới
         * khóa + revalidate Voucher hiện tại rồi reserve đúng 1 lượt.
         * Tổng tiền của phiếu treo vẫn giữ snapshot cũ như nghiệp vụ hiện tại.
         */
        Voucher reservedVoucher = reserveVoucherForHeldCheckout(
                order.getVoucher(),
                totalAmount
        );
        order.setVoucher(reservedVoucher);

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

            /*
             * Phiếu treo chưa trừ kho.
             * Khi thanh toán mới xuất InventoryLot theo FEFO.
             */
            postSaleOutByFefo(
                    variant,
                    item.getQuantity(),
                    order.getId(),
                    currentEmployee.getUserId()
            );
        }

        boolean completedImmediately = paymentSummary.completedImmediately();

        order.setPaymentMethod(paymentSummary.orderPaymentMethod());
        order.setStatus(completedImmediately ? ORDER_STATUS_COMPLETED : ORDER_STATUS_PENDING);

        if (completedImmediately) {
            order.setCompletedAt(LocalDateTime.now());
        }

        Order savedOrder = orderRepository.save(order);

        int loyaltyPointsEarned = 0;

        if (completedImmediately && savedOrder.getCustomer() != null) {
            loyaltyPointsEarned = applyLoyaltyPointsIfNeeded(savedOrder, savedOrder.getCustomer());
        }

        String vnpayUrl = null;
        String vietQrImageUrl = null;
        String vietQrContent = null;

        if (paymentSummary.needVnpayPayment()) {
            vnpayUrl = vnPayService.createPaymentUrl(savedOrder.getId(), paymentSummary.transferAmount(), clientIp);
        }

        if (paymentSummary.needVietQrPayment()) {
            VietQrResponse vietQr = vietQrService.createPaymentQr(savedOrder.getId(), paymentSummary.transferAmount());

            vietQrImageUrl = vietQr.getQrImageUrl();
            vietQrContent = vietQr.getTransferContent();
        }

        Integer customerPointAfter = savedOrder.getCustomer() != null ? savedOrder.getCustomer().getLoyaltyPoints() : 0;

        return buildResponseFromOrder(savedOrder, orderItems, completedImmediately ? "COMPLETED" : "PENDING_PAYMENT", paymentSummary.paidAmount(), paymentSummary.remainingAmount(), paymentSummary.cashGiven(), paymentSummary.transferAmount(), paymentSummary.changeAmount(), vnpayUrl, vietQrImageUrl, vietQrContent, loyaltyPointsEarned, customerPointAfter);
    }

    @Override
    @Transactional
    public PosHeldOrderResponse transferHeldOrder(Integer orderId, PosTransferHeldOrderRequest request, String cashierEmail) {
        if (request == null || request.getTargetEmployeeId() == null) {
            throw new RuntimeException("Nhân viên nhận phiếu không được để trống.");
        }

        Employee currentEmployee = resolveCashier(cashierEmail);

        Order order = getHeldOrderForUpdateOrThrow(orderId);

        /*
         * Chỉ người đang giữ phiếu hoặc MANAGER/OWNER mới được chuyển phiếu.
         * Cashier khác không được tự lấy phiếu.
         */
        validateCanTransferHeldOrder(order, currentEmployee);

        Employee targetEmployee = employeeRepository.findById(request.getTargetEmployeeId()).orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên nhận phiếu."));

        validateTargetEmployeeCanReceiveHeldOrder(targetEmployee);

        if (order.getCashier() != null && order.getCashier().getUserId() != null && order.getCashier().getUserId().equals(targetEmployee.getUserId())) {
            throw new RuntimeException("Phiếu này đã thuộc nhân viên được chọn.");
        }

        order.setCashier(targetEmployee);

        Order savedOrder = orderRepository.save(order);

        return toHeldOrderResponse(savedOrder, currentEmployee);
    }

    @Override
    @Transactional
    public Map<String, Object> cancelHeldOrder(Integer orderId, String cashierEmail) {
        Employee currentEmployee = resolveCashier(cashierEmail);

        Order order = getHeldOrderForUpdateOrThrow(orderId);

        /*
         * Quyền hủy phiếu:
         * - CASHIER chỉ được hủy phiếu của mình
         * - MANAGER/OWNER được hủy phiếu của nhân viên khác
         */
        validateCanCancelHeldOrder(order, currentEmployee);

        /*
         * Phiếu treo chưa trừ kho khi tạo/cập nhật.
         * Vì vậy hủy phiếu treo không hoàn kho.
         *
         * Voucher cũng chưa tăng usedCount khi treo phiếu,
         * nên hủy phiếu không cần giảm usedCount voucher.
         */
        order.setStatus(ORDER_STATUS_CANCELLED);
        order.setCompletedAt(null);

        Order savedOrder = orderRepository.save(order);

        return Map.of("success", true, "orderId", savedOrder.getId(), "status", "CANCELLED", "message", "Đã hủy phiếu treo #" + savedOrder.getId());
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

    private String normalizePaymentMethod(String paymentMethod) {
        String method = paymentMethod == null ? "" : paymentMethod.trim().toUpperCase(Locale.ROOT);

        if (!PAYMENT_CASH.equals(method) && !PAYMENT_VNPAY.equals(method) && !PAYMENT_VIETQR.equals(method) && !PAYMENT_MIXED.equals(method) && !PAYMENT_MIXED_VNPAY.equals(method) && !PAYMENT_MIXED_VIETQR.equals(method)) {
            throw new RuntimeException("Phương thức thanh toán không hợp lệ.");
        }

        return method;
    }

    private String resolveMixedTransferProvider(String paymentMethod, String transferProvider) {
        String method = paymentMethod == null ? "" : paymentMethod.trim().toUpperCase(Locale.ROOT);

        if (PAYMENT_MIXED_VNPAY.equals(method)) {
            return TRANSFER_PROVIDER_VNPAY;
        }

        if (PAYMENT_MIXED_VIETQR.equals(method)) {
            return TRANSFER_PROVIDER_VIETQR;
        }

        String provider = transferProvider == null ? "" : transferProvider.trim().toUpperCase(Locale.ROOT);

        /*
         * Giữ tương thích code cũ:
         * FE cũ gửi MIXED nhưng không gửi transferProvider thì mặc định là VietQR.
         */
        if (provider.isBlank()) {
            return TRANSFER_PROVIDER_VIETQR;
        }

        if (!TRANSFER_PROVIDER_VNPAY.equals(provider) && !TRANSFER_PROVIDER_VIETQR.equals(provider)) {
            throw new RuntimeException("Nhà cung cấp chuyển khoản không hợp lệ. Chỉ hỗ trợ VNPAY hoặc VIETQR.");
        }

        return provider;
    }

    private String toResponsePaymentMethod(String paymentMethod) {
        String method = paymentMethod == null ? "" : paymentMethod.trim().toUpperCase(Locale.ROOT);

        if (PAYMENT_MIXED_VNPAY.equals(method) || PAYMENT_MIXED_VIETQR.equals(method)) {
            return PAYMENT_MIXED;
        }

        return method;
    }

    private String resolveTransferProviderFromPaymentMethod(String paymentMethod) {
        String method = paymentMethod == null ? "" : paymentMethod.trim().toUpperCase(Locale.ROOT);

        if (PAYMENT_VNPAY.equals(method) || PAYMENT_MIXED_VNPAY.equals(method)) {
            return TRANSFER_PROVIDER_VNPAY;
        }

        if (PAYMENT_VIETQR.equals(method) || PAYMENT_MIXED_VIETQR.equals(method)) {
            return TRANSFER_PROVIDER_VIETQR;
        }

        /*
         * Dữ liệu cũ nếu từng lưu paymentMethod = MIXED thì coi là CASH + VIETQR.
         */
        if (PAYMENT_MIXED.equals(method)) {
            return TRANSFER_PROVIDER_VIETQR;
        }

        return null;
    }

    private boolean isVietQrPaymentMethod(String paymentMethod) {
        String method = paymentMethod == null ? "" : paymentMethod.trim().toUpperCase(Locale.ROOT);

        return PAYMENT_VIETQR.equals(method) || PAYMENT_MIXED_VIETQR.equals(method) || PAYMENT_MIXED.equals(method);
    }

    private String normalizeSku(String sku) {
        if (sku == null || sku.trim().isBlank()) {
            throw new RuntimeException("SKU không được để trống.");
        }

        return sku.trim();
    }

    private String normalizeRequiredPhone(String phone) {
        String cleanPhone = phone == null ? "" : phone.replaceAll("[\\s.-]", "").trim();

        if (cleanPhone.isBlank()) {
            throw new RuntimeException("Số điện thoại khách hàng không được để trống.");
        }

        if (!cleanPhone.matches("^(03|05|07|08|09)\\d{8}$")) {
            throw new RuntimeException("Số điện thoại khách hàng không hợp lệ. Vui lòng nhập 10 số, bắt đầu bằng 03, 05, 07, 08 hoặc 09.");
        }

        return cleanPhone;
    }

    private String normalizeRequiredName(String name) {
        String cleanName = name == null ? "" : name.trim().replaceAll("\\s+", " ");

        if (cleanName.isBlank()) {
            throw new RuntimeException("Họ tên khách hàng không được để trống.");
        }

        if (cleanName.length() < 2 || cleanName.length() > 100) {
            throw new RuntimeException("Họ tên khách hàng phải từ 2 đến 100 ký tự.");
        }

        return cleanName;
    }

    private String normalizeRequiredEmail(String email) {
        String cleanEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);

        if (cleanEmail.isBlank()) {
            throw new RuntimeException("Email khách hàng không được để trống.");
        }

        if (!cleanEmail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new RuntimeException("Email khách hàng không đúng định dạng.");
        }

        return cleanEmail;
    }

    private void validateVariantCanSell(ProductVariant variant, int quantity) {
        String reason = getVariantUnavailableReason(variant, quantity);

        if (reason != null) {
            throw new RuntimeException(reason);
        }
    }

    /**
     * POS kiểm tra tồn từ InventoryLot.
     *
     * Không dùng:
     * - ProductVariant.StockQuantity
     * - ProductVariant.ManufacturingDate
     * - ProductVariant.ExpirationDate
     */
    private String getVariantUnavailableReason(ProductVariant variant, int quantity) {
        if (variant == null) {
            return "Sản phẩm không hợp lệ.";
        }

        if (Boolean.TRUE.equals(variant.getIsDeleted())) {
            return "Sản phẩm đã bị xóa.";
        }

        Product product = variant.getProduct();

        if (product == null) {
            return "Sản phẩm cha không hợp lệ.";
        }

        if (Boolean.TRUE.equals(product.getIsDeleted())) {
            return "Sản phẩm đã bị xóa.";
        }

        if (variant.getStatus() == null || variant.getStatus() != STATUS_ACTIVE) {
            return "Biến thể sản phẩm đang ngừng bán.";
        }

        if (quantity <= 0) {
            return "Số lượng sản phẩm phải lớn hơn 0.";
        }

        int sellableQuantity = getSellableQuantity(variant);

        if (sellableQuantity <= 0) {
            if (isSkuUnavailableBecauseExpired(variant)) {
                return "Sản phẩm " + variant.getSku() + " không còn lô còn hạn để bán.";
            }

            return "Sản phẩm " + variant.getSku() + " đã hết hàng trong kho.";
        }

        if (sellableQuantity < quantity) {
            return "Sản phẩm " + variant.getSku()
                    + " không đủ tồn kho. Còn "
                    + sellableQuantity
                    + " sản phẩm có thể bán.";
        }

        return null;
    }

    private int getSellableQuantity(ProductVariant variant) {
        if (variant == null || variant.getId() == null) {
            return 0;
        }

        Integer quantity = inventoryLotRepository
                .getSellableQuantityByVariantId(variant.getId());

        return quantity != null ? quantity : 0;
    }

    private InventoryLot getNextSellableLot(ProductVariant variant) {
        if (variant == null || variant.getId() == null) {
            return null;
        }

        return inventoryLotRepository
                .findNextSellableLot(variant.getId())
                .orElse(null);
    }

    /**
     * true khi không còn lot bán được nhưng vẫn còn tồn ở lot đã hết hạn.
     */
    private boolean isSkuUnavailableBecauseExpired(ProductVariant variant) {
        if (variant == null || variant.getId() == null) {
            return false;
        }

        if (getSellableQuantity(variant) > 0) {
            return false;
        }

        Integer expiredQuantity = inventoryLotRepository
                .getExpiredOnHandQuantityByVariantId(variant.getId());

        return expiredQuantity != null && expiredQuantity > 0;
    }

    /**
     * Xuất kho theo FEFO:
     * ExpirationDate ASC -> ReceivedDate ASC -> InventoryLot.Id ASC.
     *
     * Lot hết hạn và lot hết tồn không được chọn.
     */
    private void postSaleOutByFefo(
            ProductVariant variant,
            int quantity,
            Integer orderId,
            Integer createdBy
    ) {
        if (variant == null || variant.getId() == null) {
            throw new RuntimeException("Biến thể sản phẩm không hợp lệ.");
        }

        if (quantity <= 0) {
            throw new RuntimeException("Số lượng sản phẩm phải lớn hơn 0.");
        }

        if (orderId == null) {
            throw new RuntimeException("Không xác định được hóa đơn để xuất kho.");
        }

        if (createdBy == null) {
            throw new RuntimeException("Không xác định được nhân viên xuất kho.");
        }

        List<InventoryLot> lots = inventoryLotRepository
                .findSellableLotsForUpdateFefo(variant.getId());

        int totalSellable = lots.stream()
                .map(InventoryLot::getQuantityOnHand)
                .filter(q -> q != null && q > 0)
                .mapToInt(Integer::intValue)
                .sum();

        if (totalSellable < quantity) {
            throw new RuntimeException(
                    "Sản phẩm " + variant.getSku()
                            + " không đủ tồn kho. Còn "
                            + totalSellable
                            + " sản phẩm có thể bán."
            );
        }

        int remaining = quantity;

        for (InventoryLot lot : lots) {
            if (remaining <= 0) {
                break;
            }

            int onHand = lot.getQuantityOnHand() != null
                    ? lot.getQuantityOnHand()
                    : 0;

            if (onHand <= 0) {
                continue;
            }

            int take = Math.min(onHand, remaining);

            postStockMovement(
                    lot.getId(),
                    MOVEMENT_SALE_OUT,
                    -take,
                    createdBy,
                    STOCK_REFERENCE_POS_ORDER,
                    orderId.longValue(),
                    null,
                    "POS xuất bán theo FEFO - Order #" + orderId
            );

            remaining -= take;
        }

        if (remaining != 0) {
            throw new RuntimeException(
                    "Không thể phân bổ đủ tồn kho FEFO cho SKU "
                            + variant.getSku()
            );
        }
    }

    /**
     * Callback VNPay không có Authentication của thu ngân.
     *
     * Vì POS/IN_STORE đã SALE_OUT InventoryLot ngay khi bắt đầu thanh toán
     * VNPAY/MIXED_VNPAY, callback thất bại phải hoàn đúng lot đã xuất.
     *
     * Chỉ xử lý tồn kho. Trạng thái đơn, voucher và các nghiệp vụ thanh toán
     * vẫn do VNPayController xử lý như trước.
     */
    @Override
    @Transactional
    public void restoreStockAfterVnpayFailure(Integer orderId) {
        if (orderId == null) {
            throw new RuntimeException("Mã hóa đơn không hợp lệ.");
        }

        Order order = orderRepository.findDetailByIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy hóa đơn POS cần hoàn kho sau VNPay thất bại."
                ));

        if (!isCounterOrder(order)) {
            throw new RuntimeException(
                    "Chỉ đơn POS/IN_STORE mới được hoàn kho bằng luồng VNPay POS."
            );
        }

        /*
         * Callback lặp sau khi đơn đã rời PENDING không được hoàn kho lần nữa.
         * VNPayController cũng khóa row Orders trước khi xử lý callback.
         */
        if (order.getStatus() == null
                || order.getStatus() != ORDER_STATUS_PENDING) {
            return;
        }

        String paymentMethod = order.getPaymentMethod() == null
                ? ""
                : order.getPaymentMethod().trim().toUpperCase(Locale.ROOT);

        boolean isVnpayPendingOrder = PAYMENT_VNPAY.equals(paymentMethod)
                || PAYMENT_MIXED_VNPAY.equals(paymentMethod)
                || PAYMENT_MIXED.equals(paymentMethod);

        if (!isVnpayPendingOrder) {
            throw new RuntimeException(
                    "Hóa đơn này không phải hóa đơn POS đang chờ thanh toán VNPay."
            );
        }

        Employee cashier = order.getCashier();
        Integer createdBy = cashier != null
                ? cashier.getUserId()
                : null;

        if (createdBy == null) {
            throw new RuntimeException(
                    "Không xác định được nhân viên để ghi lịch sử hoàn kho POS."
            );
        }

        restorePosOrderStock(
                order.getId(),
                createdBy,
                "Hoàn kho POS do VNPay báo thanh toán thất bại - Order #"
                        + order.getId()
        );
    }

    /**
     * Hoàn đúng các InventoryLot đã SALE_OUT của POS order.
     *
     * Mỗi RETURN_IN mới phải trỏ:
     * ReferenceLineId = StockMovement.Id của SALE_OUT gốc.
     *
     * Nhờ vậy cùng một SALE_OUT không bị hoàn lặp khi order đi qua nhiều vòng:
     * SALE_OUT -> RETURN_IN -> SALE_OUT -> RETURN_IN.
     *
     * Không chạy FEFO lại khi hoàn.
     */
    private void restorePosOrderStock(
            Integer orderId,
            Integer createdBy,
            String reason
    ) {
        if (orderId == null) {
            throw new RuntimeException("Mã hóa đơn không hợp lệ.");
        }

        if (createdBy == null) {
            throw new RuntimeException("Không xác định được nhân viên hoàn kho.");
        }

        List<StockMovement> saleOutMovements =
                stockMovementRepository.findByReference(
                        STOCK_REFERENCE_POS_ORDER,
                        orderId.longValue(),
                        MOVEMENT_SALE_OUT
                );

        if (saleOutMovements.isEmpty()) {
            throw new RuntimeException(
                    "Không tìm thấy lịch sử xuất kho của hóa đơn #"
                            + orderId
                            + " để hoàn kho."
            );
        }

        Map<Long, Integer> returnedQuantityBySaleMovementId =
                loadPosReturnedQuantityBySaleMovementId(
                        orderId,
                        saleOutMovements
                );

        for (StockMovement saleMovement : saleOutMovements) {
            if (saleMovement == null
                    || saleMovement.getId() == null
                    || saleMovement.getInventoryLot() == null
                    || saleMovement.getInventoryLot().getId() == null) {
                throw new RuntimeException(
                        "Lịch sử xuất kho của hóa đơn #"
                                + orderId
                                + " không còn liên kết InventoryLot."
                );
            }

            long saleMovementId =
                    saleMovement.getId().longValue();

            int soldQuantity =
                    saleMovement.getQuantityChange() != null
                            ? Math.abs(saleMovement.getQuantityChange())
                            : 0;

            if (soldQuantity <= 0) {
                continue;
            }

            int alreadyReturned =
                    returnedQuantityBySaleMovementId
                            .getOrDefault(saleMovementId, 0);

            int quantityToReturn =
                    Math.max(soldQuantity - alreadyReturned, 0);

            if (quantityToReturn <= 0) {
                continue;
            }

            postStockMovement(
                    saleMovement.getInventoryLot().getId(),
                    MOVEMENT_RETURN_IN,
                    quantityToReturn,
                    createdBy,
                    STOCK_REFERENCE_POS_ROLLBACK,
                    orderId.longValue(),
                    saleMovementId,
                    reason
            );

            returnedQuantityBySaleMovementId.merge(
                    saleMovementId,
                    quantityToReturn,
                    Integer::sum
            );
        }
    }

    /**
     * Tổng số lượng POS đã RETURN_IN cho từng SALE_OUT gốc.
     *
     * Chỉ movement theo chuẩn mới:
     * POS_ORDER_ROLLBACK + RETURN_IN
     * với ReferenceLineId trỏ tới StockMovement.Id của SALE_OUT.
     *
     * Nếu gặp dữ liệu rollback legacy không thể truy ngược chính xác SALE_OUT,
     * dừng lại thay vì tự suy đoán và có nguy cơ cộng tồn hai lần.
     */
    private Map<Long, Integer> loadPosReturnedQuantityBySaleMovementId(
            Integer orderId,
            List<StockMovement> saleOutMovements
    ) {
        List<StockMovement> returnMovements =
                stockMovementRepository.findByReference(
                        STOCK_REFERENCE_POS_ROLLBACK,
                        orderId.longValue(),
                        MOVEMENT_RETURN_IN
                );

        Map<Long, Integer> returned = new LinkedHashMap<>();

        for (StockMovement movement : returnMovements) {
            if (movement == null
                    || movement.getQuantityChange() == null
                    || movement.getQuantityChange() <= 0) {
                continue;
            }

            Long referenceLineId = movement.getReferenceLineId();

            boolean referencesSaleMovement =
                    referenceLineId != null
                            && saleOutMovements.stream()
                            .anyMatch(saleMovement ->
                                    saleMovement != null
                                            && saleMovement.getId() != null
                                            && referenceLineId.equals(
                                            saleMovement.getId().longValue()
                                    )
                            );

            if (!referencesSaleMovement) {
                throw new RuntimeException(
                        "Hóa đơn #"
                                + orderId
                                + " có lịch sử hoàn kho POS legacy không xác định được SALE_OUT gốc. "
                                + "Cần đối soát StockMovement trước khi hoàn kho tiếp."
                );
            }

            returned.merge(
                    referenceLineId,
                    movement.getQuantityChange(),
                    Integer::sum
            );
        }

        return returned;
    }

    /**
     * Dùng procedure chuẩn của module kho để cập nhật lot + ghi movement
     * trong cùng transaction hiện tại.
     */
    private void postStockMovement(
            Integer inventoryLotId,
            byte movementType,
            int quantityChange,
            Integer createdBy,
            String referenceType,
            Long referenceId,
            Long referenceLineId,
            String reason
    ) {
        jdbcTemplate.update(
                """
                EXEC dbo.usp_PostStockMovement
                    @InventoryLotId = ?,
                    @MovementType = ?,
                    @QuantityChange = ?,
                    @CreatedBy = ?,
                    @ReferenceType = ?,
                    @ReferenceId = ?,
                    @ReferenceLineId = ?,
                    @Reason = ?
                """,
                inventoryLotId,
                movementType,
                quantityChange,
                createdBy,
                referenceType,
                referenceId,
                referenceLineId,
                reason
        );
    }

    private Voucher resolveVoucher(String voucherCode, BigDecimal totalAmount) {
        if (voucherCode == null || voucherCode.trim().isBlank()) {
            return null;
        }

        String cleanCode = voucherCode.trim();

        Voucher voucher = voucherRepository.findByCodeIgnoreCase(cleanCode).orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại."));

        validateVoucherCanApply(voucher, totalAmount);

        return voucher;
    }

    private void validateVoucherCanApply(Voucher voucher, BigDecimal totalAmount) {
        if (voucher == null) {
            throw new RuntimeException("Mã giảm giá không hợp lệ.");
        }

        LocalDateTime now = LocalDateTime.now();

        if (Boolean.TRUE.equals(voucher.getIsDeleted())) {
            throw new RuntimeException("Mã giảm giá không tồn tại.");
        }

        if (voucher.getStatus() == null || voucher.getStatus() != STATUS_ACTIVE) {
            throw new RuntimeException("Mã giảm giá đã ngừng hoạt động.");
        }

        if (voucher.getStartDate() != null && voucher.getStartDate().isAfter(now)) {
            throw new RuntimeException("Mã giảm giá chưa đến thời gian sử dụng.");
        }

        if (voucher.getEndDate() != null && !voucher.getEndDate().isAfter(now)) {
            throw new RuntimeException("Mã giảm giá đã hết hạn sử dụng.");
        }

        Integer usageLimit = voucher.getUsageLimit();
        Integer usedCount = voucher.getUsedCount();

        if (usageLimit != null && usageLimit > 0 && usedCount != null && usedCount >= usageLimit) {
            throw new RuntimeException("Mã giảm giá đã hết lượt sử dụng.");
        }

        BigDecimal minOrderValue = voucher.getMinOrderValue() != null ? voucher.getMinOrderValue() : BigDecimal.ZERO;

        if (totalAmount == null || totalAmount.compareTo(minOrderValue) < 0) {
            throw new RuntimeException("Đơn tối thiểu " + minOrderValue.toPlainString() + " đồng để dùng mã giảm giá này.");
        }
    }

    private BigDecimal calculateDiscountAmount(Voucher voucher, BigDecimal totalAmount) {
        if (voucher == null) {
            return BigDecimal.ZERO;
        }

        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discountValue = voucher.getDiscountValue() != null ? voucher.getDiscountValue() : BigDecimal.ZERO;

        BigDecimal discountAmount;

        if ("PERCENT".equalsIgnoreCase(voucher.getDiscountType()) || "PERCENTAGE".equalsIgnoreCase(voucher.getDiscountType()) || "%".equalsIgnoreCase(voucher.getDiscountType())) {
            discountAmount = totalAmount.multiply(discountValue).divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);

            if (voucher.getMaxDiscount() != null && voucher.getMaxDiscount().compareTo(BigDecimal.ZERO) > 0 && discountAmount.compareTo(voucher.getMaxDiscount()) > 0) {
                discountAmount = voucher.getMaxDiscount();
            }
        } else if ("FIXED".equalsIgnoreCase(voucher.getDiscountType()) || "AMOUNT".equalsIgnoreCase(voucher.getDiscountType()) || "MONEY".equalsIgnoreCase(voucher.getDiscountType())) {
            discountAmount = discountValue;
        } else {
            throw new RuntimeException("Loại giảm giá của voucher không hợp lệ.");
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

        BigDecimal finalAmount = order.getFinalAmount() != null ? order.getFinalAmount() : BigDecimal.ZERO;

        int pointsEarned = finalAmount.divide(POINT_RATE_AMOUNT, 0, RoundingMode.DOWN).intValue();

        int currentPoints = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;

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
        int points = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0;

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

    private Voucher resolveVoucherForMutation(String voucherCode, BigDecimal totalAmount) {
        if (voucherCode == null || voucherCode.trim().isBlank()) {
            return null;
        }

        String cleanCode = voucherCode.trim();

        Voucher candidate = voucherRepository.findByCodeIgnoreCase(cleanCode)
                .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại."));

        Voucher lockedVoucher = voucherRepository.findByIdForUpdate(candidate.getId())
                .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại."));

        if (lockedVoucher.getCode() == null
                || !lockedVoucher.getCode().equalsIgnoreCase(cleanCode)) {
            throw new RuntimeException("Mã giảm giá đã thay đổi. Vui lòng áp dụng lại mã.");
        }

        validateVoucherCanApply(lockedVoucher, totalAmount);

        return lockedVoucher;
    }

    private void reserveVoucherUsage(Voucher voucher) {
        if (voucher == null) {
            return;
        }

        int usedCount = voucher.getUsedCount() != null
                ? voucher.getUsedCount()
                : 0;

        Integer usageLimit = voucher.getUsageLimit();

        if (usageLimit != null && usageLimit > 0 && usedCount >= usageLimit) {
            throw new RuntimeException("Voucher đã hết lượt sử dụng.");
        }

        voucher.setUsedCount(usedCount + 1);
        voucherRepository.save(voucher);
    }

    private Voucher reserveVoucherForHeldCheckout(
            Voucher voucher,
            BigDecimal totalAmount
    ) {
        if (voucher == null || voucher.getId() == null) {
            return null;
        }

        Voucher lockedVoucher = voucherRepository.findByIdForUpdate(voucher.getId())
                .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại."));

        validateVoucherCanApply(lockedVoucher, totalAmount);
        reserveVoucherUsage(lockedVoucher);

        return lockedVoucher;
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

    private String buildCapacityLabel(ProductVariant variant) {
        if (variant == null || variant.getCapacity() == null) {
            return null;
        }

        if (variant.getCapacity().getValue() == null) {
            return null;
        }

        return variant.getCapacity().getValue() + " ml";
    }

    private Employee resolveCashier(String cashierEmail) {
        if (cashierEmail == null || cashierEmail.trim().isBlank()) {
            throw new RuntimeException("Không xác định được nhân viên đang thao tác.");
        }

        Employee employee = employeeRepository.findByUserEmail(cashierEmail.trim()).orElseThrow(() -> new RuntimeException("Tài khoản hiện tại không thuộc nhân viên."));

        if (employee.getUser() == null) {
            throw new RuntimeException("Nhân viên chưa được liên kết với tài khoản.");
        }

        if (employee.getUser().getStatus() == null || employee.getUser().getStatus() != STATUS_ACTIVE) {
            throw new RuntimeException("Tài khoản nhân viên đã bị khóa.");
        }

        return employee;
    }

    private Order getHeldOrderOrThrow(Integer orderId) {
        if (orderId == null) {
            throw new RuntimeException("Mã phiếu treo không được để trống.");
        }

        /*
         * Chỉ lấy đúng phiếu treo tại quầy.
         * Không dùng findDetailById() vì method đó lấy mọi loại đơn:
         * ONLINE, đơn đã chuyển sang VNPAY, MIXED_VNPAY, VIETQR...
         */
        Order order = orderRepository.findHeldOrderById(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy phiếu treo tại quầy hoặc phiếu không còn ở trạng thái đang treo."
                ));

        if (!isHeldOrderAtCounter(order)) {
            throw new RuntimeException(
                    "Không tìm thấy phiếu treo tại quầy hoặc phiếu không còn ở trạng thái đang treo."
            );
        }

        return order;
    }

    private Order getHeldOrderForUpdateOrThrow(Integer orderId) {
        if (orderId == null) {
            throw new RuntimeException("Mã phiếu treo không được để trống.");
        }

        Order order = orderRepository.findHeldOrderByIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy phiếu treo tại quầy hoặc phiếu không còn ở trạng thái đang treo."
                ));

        if (!isHeldOrderAtCounter(order)) {
            throw new RuntimeException(
                    "Không tìm thấy phiếu treo tại quầy hoặc phiếu không còn ở trạng thái đang treo."
            );
        }

        return order;
    }

    private boolean isCounterOrder(Order order) {
        if (order == null || order.getOrderType() == null) {
            return false;
        }

        String orderType = order.getOrderType().trim().toUpperCase(Locale.ROOT);

        return "IN_STORE".equals(orderType) || "POS".equals(orderType);
    }

    private boolean isHeldOrderAtCounter(Order order) {
        if (order == null) {
            return false;
        }

        boolean pending = order.getStatus() != null
                && order.getStatus() == ORDER_STATUS_PENDING;

        boolean holdPayment = order.getPaymentMethod() != null
                && PAYMENT_HOLD.equalsIgnoreCase(order.getPaymentMethod());

        return pending && holdPayment && isCounterOrder(order);
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

        throw new RuntimeException("Bạn không có quyền sửa/chuyển phiếu treo của nhân viên khác.");
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
        if (employee == null || employee.getUser() == null || employee.getUser().getRole() == null || employee.getUser().getRole().getName() == null) {
            return false;
        }

        String roleName = employee.getUser().getRole().getName().trim().toUpperCase(Locale.ROOT);

        return "MANAGER".equals(roleName) || "ROLE_MANAGER".equals(roleName) || "OWNER".equals(roleName) || "ROLE_OWNER".equals(roleName);
    }

    private void validateTargetEmployeeCanReceiveHeldOrder(Employee targetEmployee) {
        if (targetEmployee == null || targetEmployee.getUser() == null) {
            throw new RuntimeException("Nhân viên nhận phiếu không hợp lệ.");
        }

        if (targetEmployee.getUser().getStatus() == null || targetEmployee.getUser().getStatus() != STATUS_ACTIVE) {
            throw new RuntimeException("Nhân viên nhận phiếu đang bị khóa.");
        }

        if (!canReceivePosTransfer(targetEmployee)) {
            throw new RuntimeException("Nhân viên nhận phiếu không có quyền xử lý POS.");
        }
    }

    private PosHeldOrderResponse toHeldOrderResponse(Order order, Employee currentEmployee) {
        Customer customer = order.getCustomer();
        User customerUser = customer != null ? customer.getUser() : null;

        Employee cashier = order.getCashier();
        User cashierUser = cashier != null ? cashier.getUser() : null;

        boolean ownOrder = isSameCashier(order, currentEmployee);
        boolean managerOrOwner = isManagerOrOwner(currentEmployee);

        /*
         * Nghiệp vụ:
         * - Chỉ nhân viên đang giữ phiếu mới được mở để sửa/thanh toán.
         * - MANAGER/OWNER được nhìn thấy tất cả, được chuyển/hủy,
         *   nhưng không thanh toán thay nếu chưa chuyển phiếu.
         */
        boolean canOpen = ownOrder;
        boolean canCheckout = ownOrder;
        boolean canTransfer = ownOrder || managerOrOwner;
        boolean canCancel = ownOrder || managerOrOwner;

        HeldOrderQuantitySummary quantitySummary = getHeldOrderQuantitySummary(order);

        return PosHeldOrderResponse.builder()
                .orderId(order.getId())
                .status("HELD")
                .paymentMethod(order.getPaymentMethod())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .createdAt(order.getCreatedAt())
                .itemCount(quantitySummary.itemCount())
                .totalQuantity(quantitySummary.totalQuantity())
                .cashierId(cashier != null ? cashier.getUserId() : null)
                .cashierName(cashierUser != null ? cashierUser.getName() : null)
                .customerId(customer != null ? customer.getUserId() : null)
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .customerEmail(customerUser != null ? customerUser.getEmail() : null)
                .ownOrder(ownOrder)
                .canOpen(canOpen)
                .canCheckout(canCheckout)
                .canTransfer(canTransfer)
                .canCancel(canCancel)
                .build();
    }

    private HeldOrderQuantitySummary getHeldOrderQuantitySummary(Order order) {
        if (order == null || order.getId() == null) {
            return new HeldOrderQuantitySummary(0, 0);
        }

        List<OrderItem> items = orderItemRepository.findByOrderIdWithVariant(order.getId());

        if (items == null || items.isEmpty()) {
            return new HeldOrderQuantitySummary(0, 0);
        }

        int itemCount = 0;
        int totalQuantity = 0;

        for (OrderItem item : items) {
            if (item == null) {
                continue;
            }

            itemCount++;

            Integer quantity = item.getQuantity();
            if (quantity != null && quantity > 0) {
                totalQuantity += quantity;
            }
        }

        return new HeldOrderQuantitySummary(itemCount, totalQuantity);
    }

    private record HeldOrderQuantitySummary(Integer itemCount, Integer totalQuantity) {
    }

    private void applyHeldOrderPermissions(
            PosOrderResponse response,
            Order order,
            Employee currentEmployee
    ) {
        if (response == null) {
            return;
        }

        boolean ownOrder = isSameCashier(order, currentEmployee);
        boolean managerOrOwner = isManagerOrOwner(currentEmployee);

        response.setOwnOrder(ownOrder);
        response.setCanOpen(ownOrder);
        response.setCanCheckout(ownOrder);
        response.setCanTransfer(ownOrder || managerOrOwner);
        response.setCanCancel(ownOrder || managerOrOwner);
    }

    private PosOrderResponse buildResponseFromOrder(Order order, List<OrderItem> orderItems, String status, BigDecimal paidAmount, BigDecimal remainingAmount, BigDecimal cashGiven, BigDecimal transferAmount, BigDecimal changeAmount, String vnpayUrl, int loyaltyPointsEarned) {
        Integer customerPointAfter = order.getCustomer() != null ? order.getCustomer().getLoyaltyPoints() : 0;

        return buildResponseFromOrder(order, orderItems, status, paidAmount, remainingAmount, cashGiven, transferAmount, changeAmount, vnpayUrl, null, null, loyaltyPointsEarned, customerPointAfter);
    }

    private PosOrderResponse buildResponseFromOrder(Order order, List<OrderItem> orderItems, String status, BigDecimal paidAmount, BigDecimal remainingAmount, BigDecimal cashGiven, BigDecimal transferAmount, BigDecimal changeAmount, String vnpayUrl, int loyaltyPointsEarned, Integer customerPointAfter) {
        return buildResponseFromOrder(order, orderItems, status, paidAmount, remainingAmount, cashGiven, transferAmount, changeAmount, vnpayUrl, null, null, loyaltyPointsEarned, customerPointAfter);
    }

    private PosOrderResponse buildResponseFromOrder(Order order, List<OrderItem> orderItems, String status, BigDecimal paidAmount, BigDecimal remainingAmount, BigDecimal cashGiven, BigDecimal transferAmount, BigDecimal changeAmount, String vnpayUrl, String vietQrImageUrl, String vietQrContent, int loyaltyPointsEarned, Integer customerPointAfter) {
        Customer customer = order.getCustomer();
        User customerUser = customer != null ? customer.getUser() : null;

        Employee cashier = order.getCashier();
        User cashierUser = cashier != null ? cashier.getUser() : null;

        List<PosOrderResponse.InvoiceItem> invoiceItems = orderItems == null ? List.of() : orderItems.stream().map(item -> {
            ProductVariant variant = item.getProductVariant();

            BigDecimal unitPrice = item.getFinalPrice() != null
                    ? item.getFinalPrice()
                    : (item.getOriginalPrice() != null ? item.getOriginalPrice() : BigDecimal.ZERO);

            Integer quantity = item.getQuantity() != null ? item.getQuantity() : 0;

            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

            return PosOrderResponse.InvoiceItem.builder().productName(variant != null && variant.getProduct() != null ? variant.getProduct().getName() : null).sku(variant != null ? variant.getSku() : null).capacityLabel(variant != null ? buildCapacityLabel(variant) : null).bottleTypeName(variant != null && variant.getBottleType() != null ? variant.getBottleType().getName() : null).quantity(quantity).unitPrice(unitPrice).lineTotal(lineTotal).build();
        }).toList();

        return PosOrderResponse.builder().orderId(order.getId()).status(status).totalAmount(order.getTotalAmount()).discountAmount(order.getDiscountAmount()).finalAmount(order.getFinalAmount()).voucherCode(order.getVoucher() != null ? order.getVoucher().getCode() : null).paymentMethod(toResponsePaymentMethod(order.getPaymentMethod())).transferProvider(resolveTransferProviderFromPaymentMethod(order.getPaymentMethod())).paidAmount(paidAmount != null ? paidAmount : BigDecimal.ZERO).remainingAmount(remainingAmount != null ? remainingAmount : BigDecimal.ZERO).cashGiven(cashGiven != null ? cashGiven : BigDecimal.ZERO).transferAmount(transferAmount != null ? transferAmount : BigDecimal.ZERO).changeAmount(changeAmount != null ? changeAmount : BigDecimal.ZERO).vnpayPaymentUrl(vnpayUrl).vietQrImageUrl(vietQrImageUrl).vietQrContent(vietQrContent).createdAt(order.getCreatedAt()).customerName(order.getCustomerName()).customerPhone(order.getCustomerPhone()).customerEmail(customerUser != null ? customerUser.getEmail() : null).cashierName(cashierUser != null ? cashierUser.getName() : null).loyaltyPointsEarned(loyaltyPointsEarned).customerLoyaltyPointsAfter(customerPointAfter != null ? customerPointAfter : 0).items(invoiceItems).build();
    }

    @Override
    @Transactional
    public PosOrderResponse confirmVietQrPayment(Integer orderId, String cashierEmail) {
        Employee currentEmployee = resolveCashier(cashierEmail);

        if (orderId == null) {
            throw new RuntimeException("Mã hóa đơn không hợp lệ.");
        }

        Order order = orderRepository.findDetailByIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn VietQR."));

        if (order.getStatus() == null || order.getStatus() != ORDER_STATUS_PENDING) {
            throw new RuntimeException("Chỉ hóa đơn đang chờ thanh toán mới được xác nhận VietQR.");
        }

        String paymentMethod = order.getPaymentMethod() == null ? "" : order.getPaymentMethod().trim().toUpperCase(Locale.ROOT);

        if (!isVietQrPaymentMethod(paymentMethod)) {
            throw new RuntimeException("Hóa đơn này không phải thanh toán VietQR.");
        }

        if (!isSameCashier(order, currentEmployee) && !isManagerOrOwner(currentEmployee)) {
            throw new RuntimeException("Bạn không có quyền xác nhận thanh toán hóa đơn của nhân viên khác.");
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderIdWithVariant(order.getId());

        if (orderItems.isEmpty()) {
            throw new RuntimeException("Hóa đơn không có sản phẩm.");
        }

        order.setStatus(ORDER_STATUS_COMPLETED);
        order.setCompletedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        int loyaltyPointsEarned = 0;

        if (savedOrder.getCustomer() != null) {
            loyaltyPointsEarned = applyLoyaltyPointsIfNeeded(savedOrder, savedOrder.getCustomer());
        }

        /*
         * Voucher đã được reserve từ lúc checkout bắt đầu payment.
         * Xác nhận VietQR chỉ hoàn tất Order, tuyệt đối không tăng usedCount lần 2.
         */
        Integer customerPointAfter = savedOrder.getCustomer() != null ? savedOrder.getCustomer().getLoyaltyPoints() : 0;

        BigDecimal finalAmount = savedOrder.getFinalAmount() != null ? savedOrder.getFinalAmount() : BigDecimal.ZERO;

        return buildResponseFromOrder(savedOrder, orderItems, "COMPLETED", finalAmount, BigDecimal.ZERO, BigDecimal.ZERO, finalAmount, BigDecimal.ZERO, null, null, null, loyaltyPointsEarned, customerPointAfter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosTransferTargetResponse> getTransferTargets(String cashierEmail) {
        Employee currentEmployee = resolveCashier(cashierEmail);

        return employeeRepository.findAll().stream().filter(this::canReceivePosTransfer).filter(employee -> !employee.getUserId().equals(currentEmployee.getUserId())).map(employee -> {
            User user = employee.getUser();

            return PosTransferTargetResponse.builder().employeeId(employee.getUserId()).employeeCode(employee.getEmployeeCode()).name(user != null ? user.getName() : null).email(user != null ? user.getEmail() : null).phone(user != null ? user.getPhone() : null).roleName(user != null && user.getRole() != null ? user.getRole().getName() : null).jobTitle(employee.getJobTitle()).build();
        }).toList();
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

        return "CASHIER".equals(roleName) || "ROLE_CASHIER".equals(roleName) || "MANAGER".equals(roleName) || "ROLE_MANAGER".equals(roleName) || "OWNER".equals(roleName) || "ROLE_OWNER".equals(roleName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantPosResponse> getProductsForPos() {
        return variantRepository.findVisibleVariantsForPos()
                .stream()
                .filter(this::isVariantVisibleInPosList)
                .map(this::toProductVariantPosResponseForPosList)
                .toList();
    }

    private boolean isVariantVisibleInPosList(ProductVariant variant) {
        if (variant == null) {
            return false;
        }

        if (Boolean.TRUE.equals(variant.getIsDeleted())) {
            return false;
        }

        Product product = variant.getProduct();

        if (product == null) {
            return false;
        }

        return !Boolean.TRUE.equals(product.getIsDeleted());
    }

    private ProductVariantPosResponse toProductVariantPosResponseForPosList(ProductVariant variant) {
        Product product = variant.getProduct();

        String imageUrl = null;

        if (product != null && product.getId() != null) {
            imageUrl = productImageRepository.findFirstByProduct_IdAndIsPrimaryTrue(product.getId()).or(() -> productImageRepository.findFirstByProduct_Id(product.getId())).map(ProductImage::getImageUrl).orElse(null);
        }

        String unavailableReason = getVariantUnavailableReason(variant, 1);

        int sellableQuantity = getSellableQuantity(variant);
        InventoryLot nextFefoLot = getNextSellableLot(variant);

        return ProductVariantPosResponse.builder()
                .variantId(variant.getId())
                .sku(variant.getSku())
                .productName(product != null ? product.getName() : null)
                .brandName(product != null && product.getBrand() != null ? product.getBrand().getName() : null)
                .capacityLabel(buildCapacityLabel(variant))
                .bottleTypeName(variant.getBottleType() != null ? variant.getBottleType().getName() : null)
                .price(variant.getPrice())
                /*
                 * stockQuantity giữ để FE POS hiện tại chưa bị vỡ contract,
                 * nhưng giá trị là tồn CÓ THỂ BÁN thật từ InventoryLot.
                 */
                .stockQuantity(sellableQuantity)
                .sellableQuantity(sellableQuantity)
                /*
                 * Hai field ngày giữ compatibility.
                 * Nếu có hàng bán được, đây là ngày của lot FEFO tiếp theo.
                 */
                .manufacturingDate(nextFefoLot != null ? nextFefoLot.getManufacturedDate() : null)
                .expirationDate(nextFefoLot != null ? nextFefoLot.getExpirationDate() : null)
                .status(variant.getStatus())
                .expired(isSkuUnavailableBecauseExpired(variant))
                .sellable(unavailableReason == null)
                .unavailableReason(unavailableReason)
                .imageUrl(imageUrl)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> applyVoucher(String voucherCode, BigDecimal totalAmount) {
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


    /**
     * Hủy payment intent đang chờ và đưa hóa đơn online pending quay lại phiếu treo HOLD.
     *
     * Lý do cần endpoint này:
     * - Khi bấm VietQR/VNPay, hệ thống đã tạo hóa đơn PENDING_PAYMENT và đã giữ/trừ kho.
     * - Nếu khách chưa thanh toán rồi bấm X/Đổi phương thức để sửa sản phẩm hoặc voucher,
     *   không được chỉ mở khóa ở FE vì DB đã giữ hàng.
     * - Phải hoàn lại lượng tồn kho đã giữ, đổi paymentMethod về HOLD,
     *   để lần thanh toán sau trừ kho lại đúng một lần.
     */
    @Override
    @Transactional
    public PosOrderResponse cancelPendingPayment(Integer orderId, String cashierEmail) {
        if (orderId == null) {
            throw new RuntimeException("Mã hóa đơn không hợp lệ.");
        }

        Employee currentEmployee = resolveCashier(cashierEmail);

        Order order = orderRepository.findPendingPaymentOrderByIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy hóa đơn đang chờ thanh toán hoặc hóa đơn không còn ở trạng thái chờ thanh toán."
                ));

        if (!isSameCashier(order, currentEmployee) && !isManagerOrOwner(currentEmployee)) {
            throw new RuntimeException("Bạn không có quyền hủy thanh toán hóa đơn của nhân viên khác.");
        }

        String paymentMethod = order.getPaymentMethod() == null
                ? ""
                : order.getPaymentMethod().trim().toUpperCase(Locale.ROOT);

        /*
         * Không cho rollback đơn MIXED vì hệ thống hiện chưa có bảng PaymentTransaction
         * để lưu riêng khoản tiền mặt đã nhận. Nếu đã nhận tiền mặt một phần thì chỉ
         * được đổi cổng chuyển khoản bằng retry-payment, không được sửa sản phẩm/voucher.
         */
        if (PAYMENT_MIXED.equals(paymentMethod)
                || PAYMENT_MIXED_VNPAY.equals(paymentMethod)
                || PAYMENT_MIXED_VIETQR.equals(paymentMethod)) {
            throw new RuntimeException(
                    "Đơn đã nhận tiền mặt một phần, không được sửa sản phẩm/voucher. Vui lòng chỉ đổi phương thức chuyển khoản hoặc hủy hóa đơn theo quyền quản lý."
            );
        }

        if (!PAYMENT_VNPAY.equals(paymentMethod) && !PAYMENT_VIETQR.equals(paymentMethod)) {
            throw new RuntimeException("Hóa đơn này không phải hóa đơn đang chờ thanh toán online.");
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderIdWithVariant(order.getId());

        if (orderItems.isEmpty()) {
            throw new RuntimeException("Hóa đơn không có sản phẩm.");
        }

        /*
         * Quay lại HOLD phải RETURN_IN đúng các InventoryLot
         * đã SALE_OUT lúc bắt đầu payment online.
         */
        restorePosOrderStock(
                order.getId(),
                currentEmployee.getUserId(),
                "Hoàn kho POS do hủy thanh toán online - Order #" + order.getId()
        );

        /*
         * Payment pending đã reserve Voucher ở lúc checkout.
         * Khi quay lại HOLD phải hoàn đúng 1 lượt; HOLD không giữ lượt Voucher.
         */
        restoreVoucherUsage(order);

        order.setPaymentMethod(PAYMENT_HOLD);
        order.setStatus(ORDER_STATUS_PENDING);
        order.setCompletedAt(null);

        Order savedOrder = orderRepository.save(order);

        Integer customerPointAfter = savedOrder.getCustomer() != null
                ? savedOrder.getCustomer().getLoyaltyPoints()
                : 0;

        PosOrderResponse response = buildResponseFromOrder(
                savedOrder,
                orderItems,
                "HELD",
                BigDecimal.ZERO,
                savedOrder.getFinalAmount() != null ? savedOrder.getFinalAmount() : BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                null,
                0,
                customerPointAfter
        );

        applyHeldOrderPermissions(response, savedOrder, currentEmployee);

        return response;
    }

    /**
     * Hủy hóa đơn POS đã nhận tiền mặt một phần.
     *
     * Không dùng cancelPendingPayment() cho MIXED vì hàm đó đưa đơn về HOLD để sửa,
     * còn MIXED đã có tiền mặt phải xử lý theo nghiệp vụ hủy + hoàn tiền.
     */
    @Override
    @Transactional
    public Map<String, Object> cancelPartialPaidOrder(Integer orderId, String cashierEmail) {
        if (orderId == null) {
            throw new RuntimeException("Mã hóa đơn không hợp lệ.");
        }

        Employee currentEmployee = resolveCashier(cashierEmail);

        Order order = orderRepository.findDetailByIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn cần hủy."));

        if (!isCounterOrder(order)) {
            throw new RuntimeException("Chỉ được hủy hóa đơn tại quầy.");
        }

        if (order.getStatus() == null || order.getStatus() != ORDER_STATUS_PENDING) {
            throw new RuntimeException("Chỉ được hủy hóa đơn tại quầy đang chờ thanh toán.");
        }

        if (!isSameCashier(order, currentEmployee) && !isManagerOrOwner(currentEmployee)) {
            throw new RuntimeException("Bạn không có quyền hủy hóa đơn của nhân viên khác.");
        }

        String paymentMethod = order.getPaymentMethod() == null
                ? ""
                : order.getPaymentMethod().trim().toUpperCase(Locale.ROOT);

        boolean partialPaidPayment = PAYMENT_MIXED.equals(paymentMethod)
                || PAYMENT_MIXED_VNPAY.equals(paymentMethod)
                || PAYMENT_MIXED_VIETQR.equals(paymentMethod);

        if (!partialPaidPayment) {
            throw new RuntimeException(
                    "Hóa đơn này chưa ghi nhận tiền mặt một phần. Nếu chỉ là VNPay/VietQR chưa thanh toán, hãy dùng chức năng hủy thanh toán online."
            );
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderIdWithVariant(order.getId());

        if (orderItems.isEmpty()) {
            throw new RuntimeException("Hóa đơn không có sản phẩm.");
        }

        /*
         * Hóa đơn MIXED pending đã SALE_OUT InventoryLot theo FEFO.
         * Hủy hóa đơn phải RETURN_IN đúng các lot đã xuất.
         */
        restorePosOrderStock(
                order.getId(),
                currentEmployee.getUserId(),
                "Hoàn kho POS do hủy hóa đơn thanh toán một phần - Order #" + order.getId()
        );

        /*
         * Hóa đơn payment pending đã reserve Voucher từ lúc bắt đầu checkout.
         * Hủy hóa đơn phải hoàn đúng 1 lượt.
         */
        restoreVoucherUsage(order);

        order.setStatus(ORDER_STATUS_CANCELLED);
        order.setCompletedAt(null);

        Order savedOrder = orderRepository.save(order);

        return Map.of(
                "success", true,
                "orderId", savedOrder.getId(),
                "status", "CANCELLED",
                "message", "Đã hủy hóa đơn #" + savedOrder.getId() + ". Vui lòng hoàn lại tiền mặt đã nhận cho khách."
        );
    }

    @Override
    @Transactional
    public PosOrderResponse retryPendingPayment(
            Integer orderId,
            PosHeldOrderCheckoutRequest request,
            String cashierEmail,
            String clientIp
    ) {
        if (request == null) {
            throw new RuntimeException("Dữ liệu thanh toán không được để trống.");
        }

        Employee currentEmployee = resolveCashier(cashierEmail);

        Order order = orderRepository.findPendingPaymentOrderByIdForUpdate(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy hóa đơn đang chờ thanh toán hoặc hóa đơn không còn ở trạng thái chờ thanh toán."
                ));

        if (!isSameCashier(order, currentEmployee) && !isManagerOrOwner(currentEmployee)) {
            throw new RuntimeException("Bạn không có quyền thanh toán hóa đơn của nhân viên khác.");
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderIdWithVariant(order.getId());

        if (orderItems.isEmpty()) {
            throw new RuntimeException("Hóa đơn không có sản phẩm.");
        }

        BigDecimal totalAmount =
                order.getTotalAmount() != null
                        ? order.getTotalAmount()
                        : BigDecimal.ZERO;

        BigDecimal discountAmount =
                order.getDiscountAmount() != null
                        ? order.getDiscountAmount()
                        : BigDecimal.ZERO;

        BigDecimal finalAmount =
                order.getFinalAmount() != null
                        ? order.getFinalAmount()
                        : BigDecimal.ZERO;

        validatePosSnapshot(
                request.getExpectedTotalAmount(),
                request.getExpectedDiscountAmount(),
                request.getExpectedFinalAmount(),
                totalAmount,
                discountAmount,
                finalAmount
        );

        PosCheckoutRequest paymentRequest = new PosCheckoutRequest();
        paymentRequest.setPaymentMethod(request.getPaymentMethod());
        paymentRequest.setCashGiven(request.getCashGiven());
        paymentRequest.setTransferAmount(request.getTransferAmount());
        paymentRequest.setTransferProvider(request.getTransferProvider());

        paymentRequest.setCustomerPhone(order.getCustomerPhone());
        paymentRequest.setCustomerName(order.getCustomerName());
        paymentRequest.setCustomerEmail(
                order.getCustomer() != null && order.getCustomer().getUser() != null
                        ? order.getCustomer().getUser().getEmail()
                        : null
        );

        paymentRequest.setItems(List.of(new PosItemRequest()));

        PaymentSummary paymentSummary =
                validateAndBuildPaymentSummary(paymentRequest, finalAmount);

        /*
         * Quan trọng:
         * Đơn pending payment đã trừ/giữ kho ở lần checkout đầu.
         * Retry thanh toán KHÔNG được trừ kho lần nữa.
         */
        boolean completedImmediately = paymentSummary.completedImmediately();

        order.setPaymentMethod(paymentSummary.orderPaymentMethod());
        order.setStatus(completedImmediately ? ORDER_STATUS_COMPLETED : ORDER_STATUS_PENDING);

        if (completedImmediately) {
            order.setCompletedAt(LocalDateTime.now());
        }

        Order savedOrder = orderRepository.save(order);

        int loyaltyPointsEarned = 0;

        if (completedImmediately && savedOrder.getCustomer() != null) {
            loyaltyPointsEarned = applyLoyaltyPointsIfNeeded(savedOrder, savedOrder.getCustomer());
            // Voucher đã reserve từ lần checkout đầu nên retry không tăng usedCount lần 2.
        }

        String vnpayUrl = null;
        String vietQrImageUrl = null;
        String vietQrContent = null;

        if (paymentSummary.needVnpayPayment()) {
            vnpayUrl = vnPayService.createPaymentUrl(
                    savedOrder.getId(),
                    paymentSummary.transferAmount(),
                    clientIp
            );
        }

        if (paymentSummary.needVietQrPayment()) {
            VietQrResponse vietQr = vietQrService.createPaymentQr(
                    savedOrder.getId(),
                    paymentSummary.transferAmount()
            );

            vietQrImageUrl = vietQr.getQrImageUrl();
            vietQrContent = vietQr.getTransferContent();
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
                vietQrImageUrl,
                vietQrContent,
                loyaltyPointsEarned,
                customerPointAfter
        );
    }
}