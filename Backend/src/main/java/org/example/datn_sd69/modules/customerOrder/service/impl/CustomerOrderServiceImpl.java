package org.example.datn_sd69.modules.customerOrder.service.impl;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.order.dto.request.CancelOrderRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import org.example.datn_sd69.entity.Brand;
import org.example.datn_sd69.entity.Customer;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.entity.Product;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.entity.ReturnRequest;
import org.example.datn_sd69.entity.ReturnRequestItem;
import org.example.datn_sd69.entity.ReturnRequestMedia;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.modules.customerOrder.dto.CustomerOrderItemResponse;
import org.example.datn_sd69.modules.customerOrder.dto.CustomerOrderResponse;
import org.example.datn_sd69.modules.customerOrder.service.CustomerOrderService;
import org.example.datn_sd69.repository.CustomerRepository;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.example.datn_sd69.repository.OrderRepository;
import org.example.datn_sd69.repository.ProductVariantRepository;
import org.example.datn_sd69.repository.ReturnRequestItemRepository;
import org.example.datn_sd69.repository.ReturnRequestMediaRepository;
import org.example.datn_sd69.repository.ReturnRequestRepository;
import org.example.datn_sd69.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class CustomerOrderServiceImpl implements CustomerOrderService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_CONFIRMED = 1;
    private static final int STATUS_SHIPPING = 2;
    private static final int STATUS_COMPLETED = 3;
    private static final int STATUS_CANCELLED = 4;
    private static final int STATUS_DELIVERY_FAILED = 5;
    private static final int STATUS_RETURN_REQUESTED = 6;
    private static final int STATUS_RETURN_COMPLETED = 7;

    private static final int RETURN_ITEM_STATUS_PENDING = 0;
    private static final int RETURN_ITEM_STATUS_ACCEPTED = 1;
    private static final int RETURN_ITEM_STATUS_REJECTED = 2;
    private static final int RETURN_ITEM_STATUS_COMPLETED = 3;
    private static final int RETURN_ITEM_STATUS_CUSTOMER_CANCELLED = 4;

    private static final int RETURN_TYPE_RECEIVED_WITH_PROBLEM_VALUE = 1;
    private static final int RETURN_TYPE_NOT_RECEIVED_OR_MISSING_VALUE = 2;

    private static final int REFUND_METHOD_BANK_TRANSFER_VALUE = 1;
    private static final int REFUND_METHOD_STORE_VALUE = 2;

    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int MEDIA_TYPE_VIDEO = 2;

    private static final int MAX_RETURN_IMAGE_COUNT = 6;
    private static final int MAX_RETURN_VIDEO_COUNT = 1;

    private static final long MAX_RETURN_IMAGE_SIZE = 20L * 1024L * 1024L;
    private static final long MAX_RETURN_VIDEO_SIZE = 200L * 1024L * 1024L;

    private static final String RETURN_TYPE_RECEIVED_WITH_PROBLEM = "RECEIVED_WITH_PROBLEM";
    private static final String RETURN_TYPE_NOT_RECEIVED_OR_MISSING = "NOT_RECEIVED_OR_MISSING";

    private static final String REFUND_METHOD_BANK_TRANSFER = "BANK_TRANSFER";
    private static final String REFUND_METHOD_STORE = "STORE";

    private static final String RETURN_CLOUDINARY_FOLDER = "return-requests";

    private static final int MIN_BANK_NAME_LENGTH = 2;
    private static final int MAX_BANK_NAME_LENGTH = 100;
    private static final int MIN_BANK_ACCOUNT_HOLDER_LENGTH = 2;
    private static final int MAX_BANK_ACCOUNT_HOLDER_LENGTH = 100;

    private static final Set<String> RECEIVED_PROBLEM_REASONS = Set.of(
            "Thiếu hàng",
            "Người bán gửi sai hàng",
            "Hàng bể vỡ",
            "Hàng bể vỡ - Thùng hàng không nguyên vẹn",
            "Hàng bể vỡ - Hàng trầy/xước/nứt",
            "Hàng bể vỡ - Rò rỉ chất lỏng",
            "Hàng bể vỡ - Hàng bể/vỡ vụn",
            "Hàng bể vỡ - Khác",
            "Hàng lỗi, không hoạt động",
            "Hàng hết hạn sử dụng",
            "Khác với mô tả",
            "Hàng đã qua sử dụng",
            "Hàng giả, nhái"
    );

    private static final Set<String> NOT_RECEIVED_OR_MISSING_REASONS = Set.of(
            "Chưa nhận được hàng",
            "Thiếu hàng",
            "Thùng hàng rỗng"
    );

    private static final Set<String> SUPPORTED_BANK_NAMES = Set.of(
            "Vietcombank",
            "BIDV",
            "VietinBank",
            "Agribank",
            "MB Bank",
            "Techcombank",
            "ACB",
            "VPBank",
            "TPBank",
            "Sacombank",
            "VIB",
            "SHB",
            "HDBank",
            "MSB",
            "OCB",
            "Eximbank",
            "LPBank",
            "SeABank",
            "Nam A Bank",
            "Bac A Bank",
            "ABBank",
            "PVcomBank",
            "NCB",
            "KienlongBank",
            "VietBank",
            "SaigonBank"
    );

    private static final Set<String> EVIDENCE_REQUIRED_REASONS = Set.of(
            "Thiếu hàng",
            "Người bán gửi sai hàng",
            "Hàng bể vỡ",
            "Hàng bể vỡ - Thùng hàng không nguyên vẹn",
            "Hàng bể vỡ - Hàng trầy/xước/nứt",
            "Hàng bể vỡ - Rò rỉ chất lỏng",
            "Hàng bể vỡ - Hàng bể/vỡ vụn",
            "Hàng bể vỡ - Khác",
            "Hàng lỗi, không hoạt động",
            "Hàng hết hạn sử dụng",
            "Khác với mô tả",
            "Hàng đã qua sử dụng",
            "Hàng giả, nhái",
            "Thùng hàng rỗng"
    );

    private static final Set<String> CANCEL_REASONS = Set.of(
            "Muốn thay đổi địa chỉ nhận hàng",
            "Muốn thay đổi số điện thoại nhận hàng",
            "Muốn thay đổi sản phẩm hoặc phân loại",
            "Muốn thay đổi số lượng sản phẩm",
            "Muốn thay đổi phương thức thanh toán",
            "Quên áp dụng mã giảm giá",
            "Đặt nhầm sản phẩm",
            "Không còn nhu cầu mua nữa",
            "Tìm thấy sản phẩm phù hợp hơn",
            "Khác"
    );

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = Set.of(".mp4", ".mov", ".webm");

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnRequestItemRepository returnRequestItemRepository;
    private final ReturnRequestMediaRepository returnRequestMediaRepository;
    private final Cloudinary cloudinary;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerOrderResponse> getMyOrders() {
        Customer customer = getCurrentCustomer();

        return orderRepository.findByCustomer_UserIdOrderByCreatedAtDesc(customer.getUserId())
                .stream()
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                    return mapToOrderResponse(order, items);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerOrderResponse getOrderDetail(Integer orderId) {
        Customer customer = getCurrentCustomer();

        validateId(orderId, "orderId");

        Order order = orderRepository.findByIdAndCustomer_UserId(orderId, customer.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng hoặc đơn hàng không thuộc tài khoản của bạn"
                ));

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

        return mapToOrderResponse(order, items);
    }

    @Override
    @Transactional
    public void cancelOrder(Integer orderId, CancelOrderRequest request) {
        Customer customer = getCurrentCustomer();

        validateId(orderId, "orderId");

        String cancelReason = normalizeCancelReason(request);

        Order order = orderRepository.findByIdAndCustomer_UserId(orderId, customer.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng hoặc đơn hàng không thuộc tài khoản của bạn"
                ));

        if (!canCancelOrder(order)) {
            throw badRequest("Chỉ được hủy đơn hàng khi đơn đang ở trạng thái chờ xác nhận");
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

        if (items.isEmpty()) {
            throw badRequest("Đơn hàng không có sản phẩm, không thể hủy");
        }

        restoreStockWhenCancel(items);

        order.setStatus(STATUS_CANCELLED);
        order.setCancelReason(cancelReason);
        order.setCancelledAt(LocalDateTime.now());

        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void requestReturnOrder(
            Integer orderId,
            String returnType,
            String reason,
            String description,
            String email,
            String refundMethod,
            String bankName,
            String bankAccountNumber,
            String bankAccountHolder,
            String returnItemsJson,
            List<MultipartFile> mediaFiles
    ) {
        Customer customer = getCurrentCustomer();

        validateId(orderId, "orderId");

        Order order = orderRepository.findByIdAndCustomer_UserId(orderId, customer.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng hoặc đơn hàng không thuộc tài khoản của bạn."
                ));

        if (order.getStatus() == null || order.getStatus() != STATUS_COMPLETED) {
            throw badRequest("Chỉ có thể yêu cầu hoàn hàng đối với đơn hàng đã hoàn thành.");
        }

        if (returnRequestItemRepository.existsByReturnRequest_Order_IdAndStatus(order.getId(), RETURN_ITEM_STATUS_PENDING)) {
            throw badRequest("Đơn hàng đang có sản phẩm chờ xử lý hoàn hàng.");
        }

        Integer cleanReturnType = normalizeReturnType(returnType);
        String cleanReason = normalizeRequired(reason, "Lý do hoàn hàng");
        String cleanDescription = normalizeOptionalCollapsed(description);
        String cleanEmail = normalizeOptional(email);
        Integer cleanRefundMethod = normalizeRefundMethod(refundMethod);
        String cleanBankName = normalizeOptionalCollapsed(bankName);
        String cleanBankAccountNumber = normalizeOptional(bankAccountNumber);
        String cleanBankAccountHolder = normalizeOptionalCollapsed(bankAccountHolder);

        validateReturnReason(cleanReturnType, cleanReason);
        validateDescription(cleanReason, cleanDescription);
        validateEmailIfPresent(cleanEmail);
        validateBankInfoIfNeeded(
                cleanRefundMethod,
                cleanBankName,
                cleanBankAccountNumber,
                cleanBankAccountHolder
        );
        validateReturnMediaFiles(cleanReason, mediaFiles);

        List<ReturnItemPayload> returnItemPayloads = parseReturnItems(returnItemsJson);
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());

        if (orderItems.isEmpty()) {
            throw badRequest("Đơn hàng không có sản phẩm để hoàn.");
        }

        Map<Integer, OrderItem> orderItemMap = new HashMap<>();
        for (OrderItem item : orderItems) {
            orderItemMap.put(item.getId(), item);
        }

        List<ReturnRequestItem> returnRequestItems = new ArrayList<>();
        Set<Integer> usedOrderItemIds = new HashSet<>();
        BigDecimal totalRefundAmount = BigDecimal.ZERO;

        for (ReturnItemPayload payload : returnItemPayloads) {
            if (payload == null || payload.orderItemId() == null || payload.orderItemId() <= 0) {
                throw badRequest("Sản phẩm hoàn hàng không hợp lệ.");
            }

            if (!usedOrderItemIds.add(payload.orderItemId())) {
                throw badRequest("Sản phẩm hoàn hàng bị trùng trong yêu cầu.");
            }

            OrderItem orderItem = orderItemMap.get(payload.orderItemId());
            if (orderItem == null) {
                throw badRequest("Sản phẩm hoàn hàng không thuộc đơn hàng này.");
            }

            Integer returnQuantity = payload.quantity();
            if (returnQuantity == null || returnQuantity <= 0) {
                throw badRequest("Số lượng hoàn phải lớn hơn 0.");
            }

            Integer orderedQuantity = orderItem.getQuantity();
            if (orderedQuantity == null || orderedQuantity <= 0) {
                throw badRequest("Số lượng sản phẩm trong đơn hàng không hợp lệ.");
            }

            if (returnQuantity > orderedQuantity) {
                throw badRequest("Số lượng hoàn không được vượt quá số lượng đã mua.");
            }

            BigDecimal itemRefundAmount = calculateItemRefundAmount(
                    order,
                    orderItems,
                    orderItem,
                    returnQuantity
            );
            totalRefundAmount = totalRefundAmount.add(itemRefundAmount);

            ReturnRequestItem returnRequestItem = new ReturnRequestItem();
            returnRequestItem.setOrderItem(orderItem);
            returnRequestItem.setReturnQuantity(returnQuantity);
            returnRequestItem.setRefundAmount(itemRefundAmount);
            returnRequestItem.setStatus(RETURN_ITEM_STATUS_PENDING);
            returnRequestItems.add(returnRequestItem);
        }

        BigDecimal orderFinalAmount = moneyOrZero(order.getFinalAmount());
        if (totalRefundAmount.compareTo(orderFinalAmount) > 0) {
            throw badRequest("Số tiền hoàn không được vượt quá số tiền đơn hàng.");
        }

        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setOrder(order);
        returnRequest.setReturnType(cleanReturnType);
        returnRequest.setReason(cleanReason);
        returnRequest.setDescription(cleanDescription);
        returnRequest.setRefundMethod(cleanRefundMethod);
        returnRequest.setRefundAmount(totalRefundAmount);
        returnRequest.setCreatedAt(LocalDateTime.now());

        if (REFUND_METHOD_BANK_TRANSFER_VALUE == cleanRefundMethod) {
            returnRequest.setBankName(cleanBankName);
            returnRequest.setBankAccountNumber(cleanBankAccountNumber);
            returnRequest.setBankAccountHolder(cleanBankAccountHolder);
        } else {
            returnRequest.setBankName(null);
            returnRequest.setBankAccountNumber(null);
            returnRequest.setBankAccountHolder(null);
        }

        ReturnRequest savedReturnRequest = returnRequestRepository.save(returnRequest);

        for (ReturnRequestItem item : returnRequestItems) {
            item.setReturnRequest(savedReturnRequest);
        }
        returnRequestItemRepository.saveAll(returnRequestItems);

        saveReturnRequestMedia(savedReturnRequest, mediaFiles);

        order.setStatus(STATUS_RETURN_REQUESTED);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void cancelReturnRequest(Integer orderId) {
        Customer customer = getCurrentCustomer();

        validateId(orderId, "orderId");

        Order order = orderRepository.findByIdAndCustomer_UserId(orderId, customer.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng hoặc đơn hàng không thuộc tài khoản của bạn."
                ));

        if (order.getStatus() == null || order.getStatus() != STATUS_RETURN_REQUESTED) {
            throw badRequest("Đơn hàng không ở trạng thái yêu cầu hoàn hàng.");
        }

        ReturnRequest returnRequest = returnRequestRepository
                .findTopByOrder_IdOrderByCreatedAtDesc(order.getId())
                .orElseThrow(() -> badRequest("Không tìm thấy yêu cầu hoàn hàng."));

        List<ReturnRequestItem> returnItems =
                returnRequestItemRepository.findByReturnRequest_Id(returnRequest.getId());

        if (returnItems.isEmpty()) {
            throw badRequest("Yêu cầu hoàn hàng không có sản phẩm.");
        }

        boolean hasProcessedItem = returnItems.stream()
                .anyMatch(item -> item.getStatus() == null
                        || item.getStatus() != RETURN_ITEM_STATUS_PENDING);

        if (hasProcessedItem) {
            throw badRequest("Yêu cầu hoàn hàng đã được xử lý, không thể hủy.");
        }

        for (ReturnRequestItem item : returnItems) {
            item.setStatus(RETURN_ITEM_STATUS_CUSTOMER_CANCELLED);
        }

        returnRequestItemRepository.saveAll(returnItems);

        order.setStatus(STATUS_COMPLETED);
        orderRepository.save(order);
    }

    private String normalizeCancelReason(CancelOrderRequest request) {
        if (request == null || request.getCancelReason() == null || request.getCancelReason().isBlank()) {
            throw badRequest("Vui lòng chọn lý do hủy đơn");
        }

        String reason = request.getCancelReason()
                .trim()
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("\\s{2,}", " ");

        if (reason.length() > 255) {
            throw badRequest("Lý do hủy đơn không được vượt quá 255 ký tự");
        }

        if (!CANCEL_REASONS.contains(reason)) {
            throw badRequest("Lý do hủy đơn không hợp lệ");
        }

        return reason;
    }

    private void restoreStockWhenCancel(List<OrderItem> items) {
        for (OrderItem item : items) {
            ProductVariant variant = item.getProductVariant();

            if (variant == null) {
                throw badRequest("Dữ liệu sản phẩm trong đơn hàng không hợp lệ");
            }

            Integer quantity = item.getQuantity();

            if (quantity == null || quantity <= 0) {
                throw badRequest("Số lượng sản phẩm trong đơn hàng không hợp lệ");
            }

            Integer currentStock = variant.getStockQuantity() == null
                    ? 0
                    : variant.getStockQuantity();

            variant.setStockQuantity(currentStock + quantity);
            productVariantRepository.save(variant);
        }
    }

    private CustomerOrderResponse mapToOrderResponse(Order order, List<OrderItem> items) {
        List<CustomerOrderItemResponse> itemResponses = items.stream()
                .map(this::mapToOrderItemResponse)
                .toList();

        ReturnRequest returnRequest = findLatestReturnRequestForCustomerView(order);
        List<String> returnMediaUrls = getReturnMediaUrls(returnRequest);

        return new CustomerOrderResponse(
                order.getId(),
                order.getOrderType(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getShippingAddress(),
                moneyOrZero(order.getTotalAmount()),
                moneyOrZero(order.getDiscountAmount()),
                moneyOrZero(order.getFinalAmount()),
                order.getPaymentMethod(),
                order.getStatus(),
                getStatusText(order.getStatus()),
                canCancelOrder(order),
                order.getCreatedAt(),
                order.getCancelReason(),
                order.getCancelledAt(),
                returnRequest != null ? returnRequest.getReason() : null,
                returnRequest != null ? returnRequest.getDescription() : null,
                returnRequest != null ? returnRequest.getCreatedAt() : null,
                returnRequest != null ? moneyOrZero(returnRequest.getRefundAmount()) : null,
                returnMediaUrls,
                itemResponses
        );
    }

    private ReturnRequest findLatestReturnRequestForCustomerView(Order order) {
        if (order == null || order.getId() == null || order.getStatus() == null) {
            return null;
        }

        boolean shouldShowReturnInfo = Integer.valueOf(STATUS_RETURN_REQUESTED).equals(order.getStatus())
                || Integer.valueOf(STATUS_RETURN_COMPLETED).equals(order.getStatus());

        if (!shouldShowReturnInfo) {
            return null;
        }

        return returnRequestRepository
                .findTopByOrder_IdOrderByCreatedAtDesc(order.getId())
                .orElse(null);
    }

    private List<String> getReturnMediaUrls(ReturnRequest returnRequest) {
        if (returnRequest == null || returnRequest.getId() == null) {
            return List.of();
        }

        return returnRequestMediaRepository.findByReturnRequest_Id(returnRequest.getId())
                .stream()
                .map(ReturnRequestMedia::getMediaUrl)
                .filter(url -> url != null && !url.trim().isEmpty())
                .map(String::trim)
                .toList();
    }

    private CustomerOrderItemResponse mapToOrderItemResponse(OrderItem item) {
        ProductVariant variant = item.getProductVariant();
        Product product = variant != null ? variant.getProduct() : null;
        Brand brand = product != null ? product.getBrand() : null;

        BigDecimal originalPrice = moneyOrZero(item.getOriginalPrice());
        BigDecimal discountAmount = moneyOrZero(item.getDiscountAmount());
        BigDecimal finalPrice = moneyOrZero(item.getFinalPrice());

        Integer quantity = item.getQuantity() == null ? 0 : item.getQuantity();

        BigDecimal lineTotal = finalPrice.multiply(BigDecimal.valueOf(quantity));

        return new CustomerOrderItemResponse(
                item.getId(),

                variant != null ? variant.getId() : null,
                product != null ? product.getId() : null,

                product != null ? product.getName() : null,
                brand != null ? brand.getName() : null,
                variant != null ? variant.getSku() : null,

                getCapacityText(variant),
                getBottleTypeText(variant),

                variant != null ? variant.getManufacturingDate() : null,
                variant != null ? variant.getExpirationDate() : null,

                item.getQuantity(),

                originalPrice,
                discountAmount,
                finalPrice,
                lineTotal,

                item.getNote(),
                item.getImage()
        );
    }

    private boolean canCancelOrder(Order order) {
        if (order == null || order.getStatus() == null) {
            return false;
        }

        return Integer.valueOf(STATUS_PENDING).equals(order.getStatus());
    }

    private String getStatusText(Integer status) {
        if (status == null) {
            return "Không xác định";
        }

        return switch (status) {
            case STATUS_PENDING -> "Chờ xác nhận";
            case STATUS_CONFIRMED -> "Đã xác nhận";
            case STATUS_SHIPPING -> "Đang giao hàng";
            case STATUS_COMPLETED -> "Hoàn thành";
            case STATUS_CANCELLED -> "Đã hủy";
            case STATUS_DELIVERY_FAILED -> "Giao hàng thất bại";
            case STATUS_RETURN_REQUESTED -> "Yêu cầu hoàn hàng / đổi trả";
            case STATUS_RETURN_COMPLETED -> "Hoàn hàng / đổi trả hoàn tất";
            default -> "Không xác định";
        };
    }

    private String getCapacityText(ProductVariant variant) {
        if (variant == null || variant.getCapacity() == null || variant.getCapacity().getValue() == null) {
            return null;
        }

        Double value = variant.getCapacity().getValue();

        if (value % 1 == 0) {
            return value.intValue() + "ml";
        }

        return value + "ml";
    }

    private String getBottleTypeText(ProductVariant variant) {
        if (variant == null || variant.getBottleType() == null) {
            return null;
        }

        return variant.getBottleType().getName();
    }

    private Customer getCurrentCustomer() {
        User user = getCurrentUser();

        return customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Tài khoản hiện tại không phải khách hàng"
                ));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Bạn chưa đăng nhập"
            );
        }

        String email = authentication.getName();

        if (email == null || email.trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token không hợp lệ"
            );
        }

        return userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Không tìm thấy tài khoản đăng nhập"
                ));
    }

    private void validateId(Integer id, String fieldName) {
        if (id == null || id <= 0) {
            throw badRequest(fieldName + " phải là số nguyên dương");
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw badRequest(fieldName + " không được để trống");
        }

        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String cleanValue = value.trim();

        return cleanValue.isEmpty() ? null : cleanValue;
    }

    private Integer normalizeReturnType(String returnType) {
        String value = normalizeRequired(returnType, "Tình huống hoàn hàng").toUpperCase();

        if (RETURN_TYPE_RECEIVED_WITH_PROBLEM.equals(value)) {
            return RETURN_TYPE_RECEIVED_WITH_PROBLEM_VALUE;
        }

        if (RETURN_TYPE_NOT_RECEIVED_OR_MISSING.equals(value)) {
            return RETURN_TYPE_NOT_RECEIVED_OR_MISSING_VALUE;
        }

        throw badRequest("Tình huống hoàn hàng không hợp lệ");
    }

    private Integer normalizeRefundMethod(String refundMethod) {
        String value = normalizeRequired(refundMethod, "Phương án hoàn tiền").toUpperCase();

        if (REFUND_METHOD_BANK_TRANSFER.equals(value)) {
            return REFUND_METHOD_BANK_TRANSFER_VALUE;
        }

        if (REFUND_METHOD_STORE.equals(value)) {
            return REFUND_METHOD_STORE_VALUE;
        }

        throw badRequest("Phương án hoàn tiền không hợp lệ");
    }

    private String normalizeOptionalCollapsed(String value) {
        String cleanValue = normalizeOptional(value);

        if (cleanValue == null) {
            return null;
        }

        return cleanValue.replaceAll("\\s+", " ");
    }

    private void validateReturnReason(Integer returnType, String reason) {
        if (Integer.valueOf(RETURN_TYPE_RECEIVED_WITH_PROBLEM_VALUE).equals(returnType)) {
            if (!RECEIVED_PROBLEM_REASONS.contains(reason)) {
                throw badRequest("Lý do hoàn hàng không hợp lệ với tình huống đã chọn");
            }

            return;
        }

        if (Integer.valueOf(RETURN_TYPE_NOT_RECEIVED_OR_MISSING_VALUE).equals(returnType)) {
            if (!NOT_RECEIVED_OR_MISSING_REASONS.contains(reason)) {
                throw badRequest("Lý do hoàn hàng không hợp lệ với tình huống đã chọn");
            }

            return;
        }

        throw badRequest("Tình huống hoàn hàng không hợp lệ");
    }

    private void validateDescription(String reason, String description) {
        if (description != null && description.length() > 2000) {
            throw badRequest("Mô tả không được vượt quá 2000 ký tự");
        }

        if ("Hàng bể vỡ - Khác".equals(reason)
                && (description == null || description.length() < 10)) {
            throw badRequest("Vui lòng mô tả rõ hơn khi chọn lý do Khác, tối thiểu 10 ký tự");
        }
    }

    private void validateEmailIfPresent(String email) {
        if (email == null) {
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw badRequest("Email không hợp lệ");
        }
    }

    private void validateBankInfoIfNeeded(
            Integer refundMethod,
            String bankName,
            String bankAccountNumber,
            String bankAccountHolder
    ) {
        if (!Integer.valueOf(REFUND_METHOD_BANK_TRANSFER_VALUE).equals(refundMethod)) {
            return;
        }

        if (bankName == null) {
            throw badRequest("Vui lòng chọn ngân hàng");
        }

        if (!SUPPORTED_BANK_NAMES.contains(bankName)) {
            throw badRequest("Vui lòng chọn ngân hàng trong danh sách hỗ trợ");
        }

        if (bankName.length() < MIN_BANK_NAME_LENGTH || bankName.length() > MAX_BANK_NAME_LENGTH) {
            throw badRequest("Tên ngân hàng phải từ 2 đến 100 ký tự");
        }

        if (!bankName.matches(".*\\p{L}.*")) {
            throw badRequest("Tên ngân hàng phải có ít nhất một chữ cái");
        }

        if (bankName.matches("^[0-9]+$")) {
            throw badRequest("Tên ngân hàng không được chỉ gồm số");
        }

        if (!bankName.matches("^[\\p{L}0-9\\s.()\\-/&]+$")) {
            throw badRequest("Tên ngân hàng chứa ký tự không hợp lệ");
        }

        if (bankAccountNumber == null) {
            throw badRequest("Vui lòng nhập số tài khoản");
        }

        if (!bankAccountNumber.matches("^[0-9]{6,30}$")) {
            throw badRequest("Số tài khoản chỉ gồm số và từ 6 đến 30 ký tự");
        }

        if (bankAccountNumber.matches("^0+$")) {
            throw badRequest("Số tài khoản không hợp lệ");
        }

        if (bankAccountHolder == null) {
            throw badRequest("Vui lòng nhập tên chủ tài khoản");
        }

        if (bankAccountHolder.trim().split("\\s+").length < 2) {
            throw badRequest("Tên chủ tài khoản phải gồm ít nhất 2 từ");
        }

        if (bankAccountHolder.length() < MIN_BANK_ACCOUNT_HOLDER_LENGTH
                || bankAccountHolder.length() > MAX_BANK_ACCOUNT_HOLDER_LENGTH) {
            throw badRequest("Tên chủ tài khoản phải từ 2 đến 100 ký tự");
        }

        if (!bankAccountHolder.matches(".*\\p{L}.*")) {
            throw badRequest("Tên chủ tài khoản phải có ít nhất một chữ cái");
        }

        if (bankAccountHolder.matches(".*[0-9].*")) {
            throw badRequest("Tên chủ tài khoản không được chứa số");
        }

        if (!bankAccountHolder.matches("^[\\p{L}\\s'.-]+$")) {
            throw badRequest("Tên chủ tài khoản chứa ký tự không hợp lệ");
        }
    }

    private List<ReturnItemPayload> parseReturnItems(String returnItems) {
        String cleanJson = normalizeRequired(returnItems, "Danh sách sản phẩm cần hoàn");

        try {
            ReturnItemPayload[] parsedItems = objectMapper.readValue(
                    cleanJson,
                    ReturnItemPayload[].class
            );

            if (parsedItems == null || parsedItems.length == 0) {
                throw badRequest("Vui lòng chọn sản phẩm cần hoàn hàng.");
            }

            return Arrays.asList(parsedItems);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw badRequest("Danh sách sản phẩm cần hoàn không hợp lệ.");
        }
    }

    private BigDecimal calculateItemRefundAmount(
            Order order,
            List<OrderItem> allOrderItems,
            OrderItem orderItem,
            Integer returnQuantity
    ) {
        Integer orderedQuantity = orderItem.getQuantity();

        if (orderedQuantity == null || orderedQuantity <= 0) {
            throw badRequest("Số lượng sản phẩm trong đơn hàng không hợp lệ.");
        }

        BigDecimal unitFinalPrice = moneyOrZero(orderItem.getFinalPrice());

        if (unitFinalPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw badRequest("Giá hoàn của sản phẩm không hợp lệ.");
        }

        BigDecimal returnBaseAmount = unitFinalPrice
                .multiply(BigDecimal.valueOf(returnQuantity));

        BigDecimal orderDiscountAmount = moneyOrZero(order.getDiscountAmount());
        BigDecimal discountBaseAmount = calculateOrderItemTotalBeforeOrderDiscount(allOrderItems);

        if (orderDiscountAmount.compareTo(BigDecimal.ZERO) <= 0
                || discountBaseAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return returnBaseAmount.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal allocatedDiscountAmount = orderDiscountAmount
                .multiply(returnBaseAmount)
                .divide(discountBaseAmount, 6, RoundingMode.HALF_UP);

        BigDecimal refundAmount = returnBaseAmount.subtract(allocatedDiscountAmount);

        if (refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return refundAmount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateOrderItemTotalBeforeOrderDiscount(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItem item : orderItems) {
            if (item == null) {
                continue;
            }

            Integer quantity = item.getQuantity();

            if (quantity == null || quantity <= 0) {
                continue;
            }

            BigDecimal unitFinalPrice = moneyOrZero(item.getFinalPrice());

            if (unitFinalPrice.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            total = total.add(unitFinalPrice.multiply(BigDecimal.valueOf(quantity)));
        }

        return total;
    }

    private void validateReturnMediaFiles(String reason, List<MultipartFile> mediaFiles) {
        boolean hasMedia = mediaFiles != null && !mediaFiles.isEmpty();

        if (EVIDENCE_REQUIRED_REASONS.contains(reason) && !hasMedia) {
            throw badRequest("Vui lòng tải lên ảnh hoặc video bằng chứng cho lý do hoàn hàng này");
        }

        if (!hasMedia) {
            return;
        }

        int imageCount = 0;
        int videoCount = 0;

        for (MultipartFile file : mediaFiles) {
            if (file == null || file.isEmpty()) {
                throw badRequest("File bằng chứng không hợp lệ");
            }

            String contentType = file.getContentType() == null
                    ? ""
                    : file.getContentType().toLowerCase();

            String extension = getFileExtension(file.getOriginalFilename());

            boolean isImage = contentType.startsWith("image/");
            boolean isVideo = contentType.startsWith("video/");

            if (!isImage && !isVideo) {
                throw badRequest("Chỉ được tải lên ảnh hoặc video bằng chứng");
            }

            if (isImage) {
                imageCount++;

                if (imageCount > MAX_RETURN_IMAGE_COUNT) {
                    throw badRequest("Chỉ được tải tối đa 6 hình ảnh");
                }

                if (file.getSize() > MAX_RETURN_IMAGE_SIZE) {
                    throw badRequest("Mỗi hình ảnh không được vượt quá 20MB");
                }

                if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
                    throw badRequest("Ảnh bằng chứng chỉ hỗ trợ định dạng JPG, JPEG, PNG hoặc WEBP");
                }
            }

            if (isVideo) {
                videoCount++;

                if (videoCount > MAX_RETURN_VIDEO_COUNT) {
                    throw badRequest("Chỉ được tải tối đa 1 video");
                }

                if (file.getSize() > MAX_RETURN_VIDEO_SIZE) {
                    throw badRequest("Video không được vượt quá 200MB");
                }

                if (!ALLOWED_VIDEO_EXTENSIONS.contains(extension)) {
                    throw badRequest("Video bằng chứng chỉ hỗ trợ định dạng MP4, MOV hoặc WEBM");
                }
            }
        }
    }

    private void saveReturnRequestMedia(ReturnRequest returnRequest, List<MultipartFile> mediaFiles) {
        if (mediaFiles == null || mediaFiles.isEmpty()) {
            return;
        }

        if (returnRequest == null || returnRequest.getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể lưu file bằng chứng hoàn hàng"
            );
        }

        try {
            List<ReturnRequestMedia> mediaList = new ArrayList<>();

            for (MultipartFile file : mediaFiles) {
                String contentType = file.getContentType() == null
                        ? ""
                        : file.getContentType().toLowerCase();

                Integer mediaType = contentType.startsWith("video/")
                        ? MEDIA_TYPE_VIDEO
                        : MEDIA_TYPE_IMAGE;

                Map<?, ?> uploadResult = cloudinary.uploader().upload(
                        file.getBytes(),
                        ObjectUtils.asMap(
                                "folder", RETURN_CLOUDINARY_FOLDER,
                                "resource_type", "auto",
                                "public_id", buildReturnMediaPublicId(returnRequest.getId())
                        )
                );

                Object secureUrl = uploadResult.get("secure_url");
                String mediaUrl = secureUrl == null ? null : String.valueOf(secureUrl).trim();

                if (mediaUrl == null || mediaUrl.isEmpty()) {
                    throw new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Cloudinary không trả về đường dẫn file bằng chứng hoàn hàng"
                    );
                }

                ReturnRequestMedia media = new ReturnRequestMedia();
                media.setReturnRequest(returnRequest);
                media.setMediaType(mediaType);
                media.setMediaUrl(mediaUrl);
                media.setCreatedAt(LocalDateTime.now());

                mediaList.add(media);
            }

            returnRequestMediaRepository.saveAll(mediaList);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi upload file bằng chứng hoàn hàng lên Cloudinary"
            );
        }
    }

    private String buildReturnMediaPublicId(Integer returnRequestId) {
        return "return_" + returnRequestId + "_" + UUID.randomUUID();
    }

    private String getFileExtension(String originalFileName) {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            return "";
        }

        String cleanName = Paths.get(originalFileName).getFileName().toString();
        int dotIndex = cleanName.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == cleanName.length() - 1) {
            return "";
        }

        String extension = cleanName.substring(dotIndex).toLowerCase();

        if (extension.length() > 10) {
            return "";
        }

        return extension;
    }

    private BigDecimal moneyOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private record ReturnItemPayload(
            Integer orderItemId,
            Integer quantity
    ) {
    }
}