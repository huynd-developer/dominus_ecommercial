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
import org.example.datn_sd69.entity.OrderDeliveryEvidence;
import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.entity.Product;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.entity.ReturnRequest;
import org.example.datn_sd69.entity.ReturnRequestItem;
import org.example.datn_sd69.entity.ReturnRequestMedia;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.modules.customerOrder.dto.response.CustomerOrderItemResponse;
import org.example.datn_sd69.modules.customerOrder.dto.response.CustomerOrderResponse;
import org.example.datn_sd69.modules.customerOrder.dto.response.CustomerReturnItemResponse;
import org.example.datn_sd69.modules.customerOrder.dto.request.SubmitDeliveryRefundBankRequest;
import org.example.datn_sd69.modules.customerOrder.service.CustomerOrderService;
import org.example.datn_sd69.modules.order.service.OrderMailService;
import org.example.datn_sd69.repository.CustomerRepository;
import org.example.datn_sd69.repository.OrderDeliveryEvidenceRepository;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.example.datn_sd69.repository.OrderRepository;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
    private static final int STATUS_AWAITING_REFUND = 8;

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

    private static final int DELIVERY_EVIDENCE_TYPE_SUCCESS = 1;
    private static final int DELIVERY_EVIDENCE_TYPE_FAILED = 2;

    private static final int MAX_RETURN_IMAGE_COUNT = 6;
    private static final int MAX_RETURN_VIDEO_COUNT = 1;

    private static final long RETURN_REQUEST_DEADLINE_DAYS = 3L;

    private static final long MAX_TOTAL_RETURN_IMAGE_SIZE = 10L * 1024L * 1024L;
    private static final long MAX_RETURN_VIDEO_SIZE = 10L * 1024L * 1024L;

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

    private static final String VIETQR_BANKS_API_URL = "https://api.vietqr.io/v2/banks";
    private static final long VIETQR_BANK_CACHE_MINUTES = 60L;

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
    private final OrderDeliveryEvidenceRepository orderDeliveryEvidenceRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnRequestItemRepository returnRequestItemRepository;
    private final ReturnRequestMediaRepository returnRequestMediaRepository;
    private final Cloudinary cloudinary;
    private final ObjectMapper objectMapper;
    private final OrderMailService orderMailService;

    private final RestTemplate vietQrRestTemplate = new RestTemplate();

    private volatile Set<String> cachedSupportedBankNames = Collections.emptySet();
    private volatile LocalDateTime cachedSupportedBankNamesAt;

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
    public CustomerOrderResponse submitDeliveryRefundBank(
            Integer orderId,
            SubmitDeliveryRefundBankRequest request
    ) {
        Customer customer = getCurrentCustomer();

        validateId(orderId, "orderId");

        Order order = orderRepository.findByIdAndCustomer_UserId(orderId, customer.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng hoặc đơn hàng không thuộc tài khoản của bạn"
                ));

        // MỞ CHECK: CHOP PHÉP ĐIỀN FORM TÀI KHOẢN KHI STATUS = 5 HOẶC STATUS = 8
        if (order.getStatus() == null || (order.getStatus() != STATUS_DELIVERY_FAILED && order.getStatus() != STATUS_AWAITING_REFUND)) {
            throw badRequest("Chỉ được nhập thông tin hoàn tiền cho đơn giao hàng thất bại hoặc đơn đã hủy chờ hoàn tiền");
        }

        if (!isDeliveryRefundRequired(order)) {
            throw badRequest("Đơn hàng này không phát sinh hoàn tiền");
        }

        if (order.getDeliveryRefundedAt() != null) {
            throw badRequest("Đơn hàng này đã được shop xác nhận hoàn tiền");
        }

        if (hasAnyDeliveryRefundBankInfo(order)) {
            throw badRequest("Thông tin tài khoản hoàn tiền đã được gửi, không thể chỉnh sửa. Vui lòng liên hệ shop nếu cần thay đổi.");
        }

        String bankName = normalizeOptionalCollapsed(request == null ? null : request.bankName());
        String bankAccountNumber = normalizeDeliveryRefundBankAccountNumber(
                request == null ? null : request.bankAccountNumber()
        );
        String bankAccountHolder = normalizeOptionalCollapsed(
                request == null ? null : request.bankAccountHolder()
        );

        validateDeliveryRefundBankInfo(bankName, bankAccountNumber, bankAccountHolder);

        order.setDeliveryRefundBankName(bankName);
        order.setDeliveryRefundBankAccountNumber(bankAccountNumber);
        order.setDeliveryRefundBankAccountHolder(bankAccountHolder);

        Order savedOrder = orderRepository.save(order);
        orderMailService.sendDeliveryRefundBankSubmitted(savedOrder);

        List<OrderItem> items = orderItemRepository.findByOrderId(savedOrder.getId());

        return mapToOrderResponse(savedOrder, items);
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

        /*
         * Đơn Chờ xác nhận chưa trừ kho, nên khách hủy ở trạng thái này không cộng lại kho.
         * Kho chỉ được trừ khi admin xác nhận đơn và chỉ được cộng lại ở các luồng hoàn/hoàn tiền phù hợp.
         */
        if (isPrepaidPaymentMethod(order.getPaymentMethod())) {
            order.setStatus(STATUS_AWAITING_REFUND);
            order.setDeliveryRefundAmount(moneyOrZero(order.getFinalAmount()));
        } else {
            order.setStatus(STATUS_CANCELLED);
            order.setDeliveryRefundAmount(null);
        }

        order.setCancelReason(cancelReason);
        order.setCancelledAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        orderMailService.sendOrderCancelled(savedOrder, cancelReason);
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

        validateReturnRequestDeadline(order, LocalDateTime.now());

        validatePreviousReturnRequestBeforeCreate(order);

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

        BigDecimal returnShippingFee = calculateReturnShippingFee(
                order,
                orderItems,
                returnItemPayloads,
                cleanReturnType,
                cleanReason
        );
        totalRefundAmount = totalRefundAmount.add(returnShippingFee).setScale(2, RoundingMode.HALF_UP);

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
        Order savedOrder = orderRepository.save(order);
        orderMailService.sendReturnRequested(savedOrder);
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
        Order savedOrder = orderRepository.save(order);
        orderMailService.sendReturnRequestCancelled(savedOrder);
    }

    private boolean isPrepaidPaymentMethod(String paymentMethod) {
        String method = normalizeOptional(paymentMethod);
        if (method == null) return false;
        String upperMethod = method.toUpperCase(Locale.ROOT);
        if (upperMethod.contains("COD")) return false;
        return upperMethod.contains("VNPAY")
                || upperMethod.contains("VIETQR")
                || upperMethod.contains("QR")
                || upperMethod.contains("BANK")
                || upperMethod.contains("TRANSFER")
                || upperMethod.contains("MOMO");
    }

    private void validatePreviousReturnRequestBeforeCreate(Order order) {
        if (order == null || order.getId() == null) {
            return;
        }

        ReturnRequest latestReturnRequest = returnRequestRepository
                .findTopByOrder_IdOrderByCreatedAtDesc(order.getId())
                .orElse(null);

        if (latestReturnRequest == null || latestReturnRequest.getId() == null) {
            return;
        }

        List<ReturnRequestItem> latestReturnItems = returnRequestItemRepository
                .findByReturnRequest_Id(latestReturnRequest.getId());

        if (latestReturnItems == null || latestReturnItems.isEmpty()) {
            return;
        }

        boolean allCustomerCancelled = latestReturnItems.stream()
                .allMatch(item -> item != null
                        && Integer.valueOf(RETURN_ITEM_STATUS_CUSTOMER_CANCELLED).equals(item.getStatus()));

        if (allCustomerCancelled) {
            return;
        }

        if (hasAnyReturnItemStatus(latestReturnItems, RETURN_ITEM_STATUS_PENDING)
                || latestReturnItems.stream().anyMatch(item -> item == null || item.getStatus() == null)) {
            throw badRequest("Đơn hàng đang có sản phẩm chờ xử lý hoàn hàng.");
        }

        if (hasAnyReturnItemStatus(latestReturnItems, RETURN_ITEM_STATUS_ACCEPTED)) {
            throw badRequest("Yêu cầu hoàn hàng đã được chấp nhận, không thể gửi lại.");
        }

        if (hasAnyReturnItemStatus(latestReturnItems, RETURN_ITEM_STATUS_REJECTED)) {
            throw badRequest("Yêu cầu hoàn hàng đã bị từ chối, không thể gửi lại. Vui lòng liên hệ hỗ trợ nếu cần khiếu nại.");
        }

        if (hasAnyReturnItemStatus(latestReturnItems, RETURN_ITEM_STATUS_COMPLETED)) {
            throw badRequest("Đơn hàng đã được xử lý hoàn tiền, không thể gửi lại yêu cầu hoàn hàng.");
        }

        throw badRequest("Yêu cầu hoàn hàng trước đó đã được xử lý, không thể gửi lại.");
    }

    private boolean hasAnyReturnItemStatus(List<ReturnRequestItem> returnItems, Integer status) {
        return returnItems != null
                && returnItems.stream()
                .anyMatch(item -> item != null && Integer.valueOf(status).equals(item.getStatus()));
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

    private CustomerOrderResponse mapToOrderResponse(Order order, List<OrderItem> items) {
        List<CustomerOrderItemResponse> itemResponses = items.stream()
                .map(this::mapToOrderItemResponse)
                .toList();

        ReturnRequest returnRequest = findLatestReturnRequestForCustomerView(order);
        List<String> returnMediaUrls = getReturnMediaUrls(returnRequest);
        List<ReturnRequestItem> rawReturnItems = getReturnRequestItemsForCustomerView(returnRequest);
        List<CustomerReturnItemResponse> returnItems = rawReturnItems.stream()
                .map(this::mapToCustomerReturnItemResponse)
                .toList();

        BigDecimal returnShippingFee = returnRequest == null
                ? BigDecimal.ZERO
                : calculateReturnShippingFeeForExistingRequest(order, rawReturnItems, returnRequest);

        String returnProcessStatus = returnRequest == null
                ? null
                : resolveReturnProcessStatus(order, rawReturnItems);

        List<String> deliverySuccessMediaUrls = getDeliveryEvidenceUrls(order, DELIVERY_EVIDENCE_TYPE_SUCCESS);
        List<String> deliveryFailedMediaUrls = getDeliveryEvidenceUrls(order, DELIVERY_EVIDENCE_TYPE_FAILED);

        return new CustomerOrderResponse(
                order.getId(),
                order.getOrderType(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getShippingAddress(),
                moneyOrZero(order.getTotalAmount()),
                moneyOrZero(order.getDiscountAmount()),
                moneyOrZero(order.getFinalAmount()),
                getOrderShippingFee(order),
                returnShippingFee,
                order.getPaymentMethod(),
                order.getStatus(),
                getStatusText(order.getStatus()),
                canCancelOrder(order),
                order.getCreatedAt(),
                order.getCancelReason(),
                order.getCancelledAt(),
                normalizeOptional(order.getDeliveryCompletedByName()),
                normalizeOptional(order.getDeliveryFailedReason()),
                normalizeOptional(order.getDeliveryFailedDescription()),
                order.getDeliveryFailedAt(),
                normalizeOptional(order.getDeliveryFailedByName()),
                order.getDeliveryRefundAmount(),
                normalizeOptional(order.getDeliveryRefundBankName()),
                normalizeOptional(order.getDeliveryRefundBankAccountNumber()),
                normalizeOptional(order.getDeliveryRefundBankAccountHolder()),
                order.getDeliveryRefundedAt(),
                normalizeOptional(order.getDeliveryRefundedByName()),
                isDeliveryRefundRequired(order),
                hasDeliveryRefundBankInfo(order),
                order.getDeliveryRefundedAt() != null,
                canSubmitDeliveryRefundBank(order),
                deliverySuccessMediaUrls,
                deliveryFailedMediaUrls,
                returnRequest != null ? returnRequest.getReason() : null,
                returnRequest != null ? returnRequest.getDescription() : null,
                returnRequest != null ? returnRequest.getCreatedAt() : null,
                returnRequest != null ? resolveReturnRefundAmount(returnRequest, rawReturnItems, returnShippingFee) : null,
                returnMediaUrls,
                returnItems,
                returnProcessStatus,
                getReturnProcessStatusText(returnProcessStatus),
                resolveReturnRejectReason(rawReturnItems),
                returnRequest != null ? formatRefundMethod(returnRequest.getRefundMethod()) : null,
                returnRequest != null ? returnRequest.getBankName() : null,
                returnRequest != null ? returnRequest.getBankAccountNumber() : null,
                returnRequest != null ? returnRequest.getBankAccountHolder() : null,
                null,
                null,
                null,
                null,
                itemResponses
        );
    }

    private List<String> getDeliveryEvidenceUrls(Order order, Integer evidenceType) {
        if (order == null || order.getId() == null) {
            return List.of();
        }

        return orderDeliveryEvidenceRepository
                .findByOrder_IdAndEvidenceTypeOrderByCreatedAtAsc(order.getId(), evidenceType)
                .stream()
                .map(OrderDeliveryEvidence::getImageUrl)
                .filter(url -> url != null && !url.trim().isEmpty())
                .toList();
    }

    private ReturnRequest findLatestReturnRequestForCustomerView(Order order) {
        if (order == null || order.getId() == null) {
            return null;
        }

        ReturnRequest returnRequest = returnRequestRepository
                .findTopByOrder_IdOrderByCreatedAtDesc(order.getId())
                .orElse(null);

        if (returnRequest == null) {
            return null;
        }

        Integer orderStatus = order.getStatus();

        if (Integer.valueOf(STATUS_RETURN_REQUESTED).equals(orderStatus)
                || Integer.valueOf(STATUS_RETURN_COMPLETED).equals(orderStatus)) {
            return returnRequest;
        }

        List<ReturnRequestItem> returnItems = getReturnRequestItemsForCustomerView(returnRequest);

        boolean hasRejectedItem = returnItems.stream()
                .anyMatch(item -> item != null
                        && Integer.valueOf(RETURN_ITEM_STATUS_REJECTED).equals(item.getStatus()));

        return hasRejectedItem ? returnRequest : null;
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

    private List<ReturnRequestItem> getReturnRequestItemsForCustomerView(ReturnRequest returnRequest) {
        if (returnRequest == null || returnRequest.getId() == null) {
            return List.of();
        }

        return returnRequestItemRepository.findByReturnRequest_Id(returnRequest.getId())
                .stream()
                .filter(returnItem -> returnItem != null
                        && !Integer.valueOf(RETURN_ITEM_STATUS_CUSTOMER_CANCELLED).equals(returnItem.getStatus()))
                .toList();
    }

    private List<CustomerReturnItemResponse> getReturnItemsForCustomerView(ReturnRequest returnRequest) {
        return getReturnRequestItemsForCustomerView(returnRequest)
                .stream()
                .map(this::mapToCustomerReturnItemResponse)
                .toList();
    }

    private CustomerReturnItemResponse mapToCustomerReturnItemResponse(ReturnRequestItem returnItem) {
        OrderItem orderItem = returnItem.getOrderItem();
        ProductVariant variant = orderItem != null ? orderItem.getProductVariant() : null;
        Product product = variant != null ? variant.getProduct() : null;
        Brand brand = product != null ? product.getBrand() : null;

        Integer orderedQuantity = orderItem != null && orderItem.getQuantity() != null
                ? orderItem.getQuantity()
                : 0;

        Integer returnQuantity = returnItem.getReturnQuantity() == null
                ? 0
                : returnItem.getReturnQuantity();

        BigDecimal unitFinalPrice = orderItem == null
                ? BigDecimal.ZERO
                : moneyOrZero(orderItem.getFinalPrice());

        BigDecimal itemAmount = unitFinalPrice
                .multiply(BigDecimal.valueOf(returnQuantity))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal refundAmount = moneyOrZero(returnItem.getRefundAmount())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal voucherAllocatedAmount = itemAmount.subtract(refundAmount);
        if (voucherAllocatedAmount.compareTo(BigDecimal.ZERO) < 0) {
            voucherAllocatedAmount = BigDecimal.ZERO;
        }
        voucherAllocatedAmount = voucherAllocatedAmount.setScale(2, RoundingMode.HALF_UP);

        Integer returnItemStatus = returnItem.getStatus() == null
                ? RETURN_ITEM_STATUS_PENDING
                : returnItem.getStatus();

        return new CustomerReturnItemResponse(
                orderItem != null ? orderItem.getId() : null,
                product != null ? product.getId() : null,
                variant != null ? variant.getId() : null,
                product != null ? product.getName() : null,
                brand != null ? brand.getName() : null,
                variant != null ? variant.getSku() : null,
                orderItem != null ? orderItem.getImage() : null,
                getCapacityText(variant),
                getBottleTypeText(variant),
                orderedQuantity,
                returnQuantity,
                unitFinalPrice.setScale(2, RoundingMode.HALF_UP),
                itemAmount,
                voucherAllocatedAmount,
                refundAmount,
                returnItemStatus,
                getReturnItemStatusText(returnItemStatus),
                normalizeOptionalCollapsed(returnItem.getRejectReason())
        );
    }

    private String resolveReturnProcessStatus(Order order, List<ReturnRequestItem> returnItems) {
        if (returnItems != null && !returnItems.isEmpty()) {
            if (areAllReturnItemsStatus(returnItems, RETURN_ITEM_STATUS_PENDING)) {
                return "PENDING";
            }

            if (areAllReturnItemsStatus(returnItems, RETURN_ITEM_STATUS_ACCEPTED)) {
                return "ACCEPTED";
            }

            if (areAllReturnItemsStatus(returnItems, RETURN_ITEM_STATUS_REJECTED)) {
                return "REJECTED";
            }

            if (areAllReturnItemsStatus(returnItems, RETURN_ITEM_STATUS_COMPLETED)) {
                return "REFUNDED";
            }

            return "PARTIAL";
        }

        if (order != null && Integer.valueOf(STATUS_RETURN_COMPLETED).equals(order.getStatus())) {
            return "REFUNDED";
        }

        if (order != null && Integer.valueOf(STATUS_RETURN_REQUESTED).equals(order.getStatus())) {
            return "PENDING";
        }

        return null;
    }

    private boolean areAllReturnItemsStatus(List<ReturnRequestItem> returnItems, Integer status) {
        return returnItems != null
                && !returnItems.isEmpty()
                && returnItems.stream().allMatch(item -> item != null && Integer.valueOf(status).equals(item.getStatus()));
    }

    private String getReturnProcessStatusText(String processStatus) {
        if (processStatus == null) {
            return null;
        }

        return switch (processStatus) {
            case "PENDING" -> "Chờ shop xử lý";
            case "ACCEPTED" -> "Đã chấp nhận / Chờ hoàn tiền";
            case "REJECTED" -> "Đã từ chối hoàn hàng";
            case "REFUNDED" -> "Đã xử lý hoàn tiền";
            case "PARTIAL" -> "Đang xử lý một phần";
            default -> null;
        };
    }

    private String resolveReturnRejectReason(List<ReturnRequestItem> returnItems) {
        if (returnItems == null || returnItems.isEmpty()) {
            return null;
        }

        return returnItems.stream()
                .map(ReturnRequestItem::getRejectReason)
                .map(this::normalizeOptionalCollapsed)
                .filter(reason -> reason != null && !reason.isEmpty())
                .findFirst()
                .orElse(null);
    }

    private String getReturnItemStatusText(Integer status) {
        if (status == null) {
            return "Chờ xử lý";
        }

        return switch (status) {
            case RETURN_ITEM_STATUS_PENDING -> "Chờ xử lý";
            case RETURN_ITEM_STATUS_ACCEPTED -> "Đã chấp nhận";
            case RETURN_ITEM_STATUS_REJECTED -> "Từ chối hoàn hàng";
            case RETURN_ITEM_STATUS_COMPLETED -> "Đã hoàn tiền";
            case RETURN_ITEM_STATUS_CUSTOMER_CANCELLED -> "Khách đã hủy yêu cầu";
            default -> "Không xác định";
        };
    }

    private String formatRefundMethod(Integer refundMethod) {
        if (refundMethod == null) {
            return null;
        }

        return switch (refundMethod) {
            case REFUND_METHOD_BANK_TRANSFER_VALUE -> REFUND_METHOD_BANK_TRANSFER;
            case REFUND_METHOD_STORE_VALUE -> REFUND_METHOD_STORE;
            default -> "UNKNOWN";
        };
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

    private void validateReturnRequestDeadline(Order order, LocalDateTime now) {
        LocalDateTime completedAt = getOrderCompletedAt(order);

        if (completedAt == null) {
            throw badRequest("Đơn hàng chưa có thời gian hoàn thành, không thể yêu cầu hoàn hàng.");
        }

        LocalDateTime deadline = completedAt.plusDays(RETURN_REQUEST_DEADLINE_DAYS);

        if (now.isAfter(deadline)) {
            throw badRequest("Đã quá hạn 3 ngày kể từ lúc đơn hàng hoàn thành, không thể yêu cầu trả hàng / hoàn tiền.");
        }
    }

    private LocalDateTime getOrderCompletedAt(Order order) {
        if (order == null) {
            return null;
        }

        if (order.getCompletedAt() != null) {
            return order.getCompletedAt();
        }

        return order.getCreatedAt();
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
            case STATUS_AWAITING_REFUND -> "Đã hủy / Chờ hoàn tiền";
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

    private boolean isDeliveryRefundRequired(Order order) {
        return order != null
                && moneyOrZero(order.getDeliveryRefundAmount()).compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean hasDeliveryRefundBankInfo(Order order) {
        if (order == null) {
            return false;
        }

        return normalizeOptional(order.getDeliveryRefundBankName()) != null
                && normalizeOptional(order.getDeliveryRefundBankAccountNumber()) != null
                && normalizeOptional(order.getDeliveryRefundBankAccountHolder()) != null;
    }

    private boolean hasAnyDeliveryRefundBankInfo(Order order) {
        if (order == null) {
            return false;
        }

        return normalizeOptional(order.getDeliveryRefundBankName()) != null
                || normalizeOptional(order.getDeliveryRefundBankAccountNumber()) != null
                || normalizeOptional(order.getDeliveryRefundBankAccountHolder()) != null;
    }

    // MỞ RỘNG CHECK CHO PHÉP STATUS = 8 CŨNG TRẢ VỀ TRUE
    private boolean canSubmitDeliveryRefundBank(Order order) {
        return order != null
                && (Integer.valueOf(STATUS_DELIVERY_FAILED).equals(order.getStatus()) || Integer.valueOf(STATUS_AWAITING_REFUND).equals(order.getStatus()))
                && isDeliveryRefundRequired(order)
                && order.getDeliveryRefundedAt() == null
                && !hasAnyDeliveryRefundBankInfo(order);
    }

    private String normalizeDeliveryRefundBankAccountNumber(String value) {
        String cleanValue = normalizeOptional(value);

        if (cleanValue == null) {
            return null;
        }

        return cleanValue.replaceAll("\\s+", "");
    }

    private boolean isSupportedBankName(String bankName) {
        String cleanBankName = normalizeOptionalCollapsed(bankName);

        if (cleanBankName == null) {
            return false;
        }

        return getSupportedBankNamesFromVietQr()
                .stream()
                .anyMatch(supportedBankName -> supportedBankName.equalsIgnoreCase(cleanBankName));
    }

    private Set<String> getSupportedBankNamesFromVietQr() {
        LocalDateTime now = LocalDateTime.now();
        Set<String> cachedBankNames = cachedSupportedBankNames;

        if (cachedSupportedBankNamesAt != null
                && cachedBankNames != null
                && !cachedBankNames.isEmpty()
                && cachedSupportedBankNamesAt.plusMinutes(VIETQR_BANK_CACHE_MINUTES).isAfter(now)) {
            return cachedBankNames;
        }

        try {
            Set<String> fetchedBankNames = fetchSupportedBankNamesFromVietQr();

            if (!fetchedBankNames.isEmpty()) {
                cachedSupportedBankNames = fetchedBankNames;
                cachedSupportedBankNamesAt = now;
                return fetchedBankNames;
            }
        } catch (RestClientException exception) {
            if (cachedBankNames != null && !cachedBankNames.isEmpty()) {
                return cachedBankNames;
            }

            throw badRequest("Không tải được danh sách ngân hàng từ VietQR. Vui lòng thử lại sau.");
        }

        if (cachedBankNames != null && !cachedBankNames.isEmpty()) {
            return cachedBankNames;
        }

        throw badRequest("Danh sách ngân hàng VietQR đang trống. Vui lòng thử lại sau.");
    }

    private Set<String> fetchSupportedBankNamesFromVietQr() {
        Map<?, ?> response = vietQrRestTemplate.getForObject(VIETQR_BANKS_API_URL, Map.class);

        if (response == null) {
            return Collections.emptySet();
        }

        Object data = response.get("data");

        if (!(data instanceof List<?> banks)) {
            return Collections.emptySet();
        }

        Set<String> bankNames = new LinkedHashSet<>();

        for (Object bank : banks) {
            if (!(bank instanceof Map<?, ?> bankMap)) {
                continue;
            }

            addSupportedBankName(bankNames, bankMap.get("name"));
            addSupportedBankName(bankNames, bankMap.get("shortName"));
            addSupportedBankName(bankNames, bankMap.get("code"));
        }

        return bankNames;
    }

    private void addSupportedBankName(Set<String> bankNames, Object value) {
        String bankName = normalizeOptionalCollapsed(value == null ? null : String.valueOf(value));

        if (bankName == null || bankName.length() > MAX_BANK_NAME_LENGTH) {
            return;
        }

        if (!bankName.matches(".*\\p{L}.*")) {
            return;
        }

        bankNames.add(bankName);
    }

    private void validateDeliveryRefundBankInfo(
            String bankName,
            String bankAccountNumber,
            String bankAccountHolder
    ) {
        if (bankName == null) {
            throw badRequest("Vui lòng chọn ngân hàng nhận hoàn tiền");
        }

        if (!isSupportedBankName(bankName)) {
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
            throw badRequest("Vui lòng nhập số tài khoản nhận hoàn tiền");
        }

        if (!bankAccountNumber.matches("^[0-9]{6,30}$")) {
            throw badRequest("Số tài khoản chỉ được nhập số, cho phép khoảng trắng khi nhập và phải từ 6 đến 30 chữ số");
        }

        if (bankAccountNumber.matches("^0+$")) {
            throw badRequest("Số tài khoản không hợp lệ");
        }

        if (bankAccountHolder == null) {
            throw badRequest("Vui lòng nhập tên chủ tài khoản nhận hoàn tiền");
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

        if (!isSupportedBankName(bankName)) {
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

    private BigDecimal getOrderShippingFee(Order order) {
        if (order == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return moneyOrZero(order.getShippingFee()).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateReturnShippingFee(
            Order order,
            List<OrderItem> orderItems,
            List<ReturnItemPayload> returnItemPayloads,
            Integer returnType,
            String reason
    ) {
        if (!isFullOrderReturnPayloads(orderItems, returnItemPayloads)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if (!isShopFaultReturnReason(returnType, reason)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return getOrderShippingFee(order);
    }

    private BigDecimal calculateReturnShippingFeeForExistingRequest(
            Order order,
            List<ReturnRequestItem> returnItems,
            ReturnRequest returnRequest
    ) {
        if (returnRequest == null || returnItems == null || returnItems.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());

        if (!isFullOrderReturnRequestItems(orderItems, returnItems)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if (!isShopFaultReturnReason(returnRequest.getReturnType(), returnRequest.getReason())) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return getOrderShippingFee(order);
    }

    private BigDecimal resolveReturnRefundAmount(
            ReturnRequest returnRequest,
            List<ReturnRequestItem> returnItems,
            BigDecimal returnShippingFee
    ) {
        if (returnRequest == null) {
            return null;
        }

        BigDecimal storedRefundAmount = moneyOrZero(returnRequest.getRefundAmount())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal shippingFee = moneyOrZero(returnShippingFee)
                .setScale(2, RoundingMode.HALF_UP);

        if (shippingFee.compareTo(BigDecimal.ZERO) <= 0) {
            return storedRefundAmount;
        }

        BigDecimal itemRefundTotal = sumReturnItemRefundAmount(returnItems);
        BigDecimal expectedTotalWithShipping = itemRefundTotal.add(shippingFee)
                .setScale(2, RoundingMode.HALF_UP);

        if (storedRefundAmount.compareTo(expectedTotalWithShipping) >= 0) {
            return storedRefundAmount;
        }

        if (storedRefundAmount.compareTo(itemRefundTotal) <= 0) {
            return storedRefundAmount.add(shippingFee).setScale(2, RoundingMode.HALF_UP);
        }

        return storedRefundAmount;
    }

    private BigDecimal sumReturnItemRefundAmount(List<ReturnRequestItem> returnItems) {
        if (returnItems == null || returnItems.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return returnItems.stream()
                .filter(item -> item != null)
                .map(ReturnRequestItem::getRefundAmount)
                .map(this::moneyOrZero)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isFullOrderReturnPayloads(
            List<OrderItem> orderItems,
            List<ReturnItemPayload> returnItemPayloads
    ) {
        if (orderItems == null || orderItems.isEmpty()
                || returnItemPayloads == null || returnItemPayloads.isEmpty()) {
            return false;
        }

        Map<Integer, Integer> returnQuantityByOrderItemId = new HashMap<>();
        for (ReturnItemPayload payload : returnItemPayloads) {
            if (payload != null && payload.orderItemId() != null) {
                returnQuantityByOrderItemId.put(payload.orderItemId(), payload.quantity());
            }
        }

        return isFullOrderReturnByQuantityMap(orderItems, returnQuantityByOrderItemId);
    }

    private boolean isFullOrderReturnByQuantityMap(
            List<OrderItem> orderItems,
            Map<Integer, Integer> returnQuantityByOrderItemId
    ) {
        if (orderItems == null || orderItems.isEmpty()
                || returnQuantityByOrderItemId == null || returnQuantityByOrderItemId.isEmpty()) {
            return false;
        }

        for (OrderItem item : orderItems) {
            if (item == null || item.getId() == null) {
                continue;
            }

            Integer orderedQuantity = item.getQuantity();
            if (orderedQuantity == null || orderedQuantity <= 0) {
                continue;
            }

            Integer returnQuantity = returnQuantityByOrderItemId.get(item.getId());
            if (returnQuantity == null || !returnQuantity.equals(orderedQuantity)) {
                return false;
            }
        }

        return true;
    }

    private boolean isFullOrderReturnRequestItems(
            List<OrderItem> orderItems,
            List<ReturnRequestItem> returnItems
    ) {
        if (orderItems == null || orderItems.isEmpty()
                || returnItems == null || returnItems.isEmpty()) {
            return false;
        }

        Map<Integer, Integer> returnQuantityByOrderItemId = new HashMap<>();
        for (ReturnRequestItem returnItem : returnItems) {
            if (returnItem != null
                    && returnItem.getOrderItem() != null
                    && returnItem.getOrderItem().getId() != null) {
                returnQuantityByOrderItemId.put(
                        returnItem.getOrderItem().getId(),
                        returnItem.getReturnQuantity()
                );
            }
        }

        return isFullOrderReturnByQuantityMap(orderItems, returnQuantityByOrderItemId);
    }

    private boolean isShopFaultReturnReason(Integer returnType, String reason) {
        String cleanReason = normalizeOptionalCollapsed(reason);

        if (cleanReason == null || cleanReason.isEmpty()) {
            return false;
        }

        if (Integer.valueOf(RETURN_TYPE_RECEIVED_WITH_PROBLEM_VALUE).equals(returnType)) {
            return RECEIVED_PROBLEM_REASONS.contains(cleanReason);
        }

        if (Integer.valueOf(RETURN_TYPE_NOT_RECEIVED_OR_MISSING_VALUE).equals(returnType)) {
            return NOT_RECEIVED_OR_MISSING_REASONS.contains(cleanReason);
        }

        return false;
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
        long totalImageSize = 0L;

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

                totalImageSize += file.getSize();
                if (totalImageSize > MAX_TOTAL_RETURN_IMAGE_SIZE) {
                    throw badRequest("Tổng dung lượng hình ảnh không được vượt quá 10MB");
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
                    throw badRequest("Video không được vượt quá 10MB");
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