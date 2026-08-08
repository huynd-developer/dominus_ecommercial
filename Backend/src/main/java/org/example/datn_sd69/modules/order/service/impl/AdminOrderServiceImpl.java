package org.example.datn_sd69.modules.order.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.OrderDeliveryEvidence;
import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.entity.ReturnRequest;
import org.example.datn_sd69.entity.ReturnRequestItem;
import org.example.datn_sd69.entity.ReturnRequestMedia;
import org.example.datn_sd69.modules.order.dto.response.AdminOrderItemResponse;
import org.example.datn_sd69.modules.order.dto.response.AdminReturnItemResponse;
import org.example.datn_sd69.modules.order.dto.response.AdminOrderResponse;
import org.example.datn_sd69.modules.order.dto.response.AdminOrderStatusCountResponse;
import org.example.datn_sd69.modules.order.dto.request.AdminCancelOrderRequest;
import org.example.datn_sd69.modules.order.dto.request.MarkDeliveryCompletedRequest;
import org.example.datn_sd69.modules.order.dto.request.MarkDeliveryFailedRequest;
import org.example.datn_sd69.modules.order.dto.request.RejectReturnRequest;
import org.example.datn_sd69.modules.order.service.AdminOrderService;
import org.example.datn_sd69.modules.order.service.OrderMailService;
import org.example.datn_sd69.repository.OrderDeliveryEvidenceRepository;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.example.datn_sd69.repository.OrderRepository;
import org.example.datn_sd69.repository.ReturnRequestItemRepository;
import org.example.datn_sd69.repository.ReturnRequestMediaRepository;
import org.example.datn_sd69.repository.ReturnRequestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

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

    private static final int RETURN_TYPE_RECEIVED_WITH_PROBLEM_VALUE = 1;
    private static final int RETURN_TYPE_NOT_RECEIVED_OR_MISSING_VALUE = 2;

    private static final int REFUND_METHOD_BANK_TRANSFER_VALUE = 1;
    private static final int REFUND_METHOD_STORE_VALUE = 2;

    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int MEDIA_TYPE_VIDEO = 2;

    private static final int DELIVERY_EVIDENCE_TYPE_SUCCESS = 1;
    private static final int DELIVERY_EVIDENCE_TYPE_FAILED = 2;

    private static final Set<String> DELIVERY_FAILED_REASONS = Set.of(
            "Không liên hệ được khách hàng",
            "Khách từ chối nhận hàng",
            "Sai hoặc thiếu địa chỉ giao hàng",
            "Không có người nhận hàng",
            "Hàng bị hư hỏng khi giao",
            "Khách hẹn giao lại nhưng shop không thể tiếp tục giao",
            "Khác"
    );

    private static final Set<String> DELIVERY_FAILED_REQUIRES_EVIDENCE_REASONS = Set.of(
            "Không liên hệ được khách hàng",
            "Khách từ chối nhận hàng",
            "Sai hoặc thiếu địa chỉ giao hàng",
            "Hàng bị hư hỏng khi giao",
            "Khác"
    );

    private static final int MAX_DELIVERY_IMAGE_COUNT = 2;

    private static final Set<String> ALLOWED_DELIVERY_IMAGE_EXTENSIONS = Set.of(
            ".jpg",
            ".jpeg",
            ".png",
            ".webp"
    );

    private static final long MAX_DELIVERY_EVIDENCE_TOTAL_SIZE = 10L * 1024L * 1024L;

    private static final String DELIVERY_EVIDENCE_CLOUDINARY_FOLDER = "order-delivery-evidence";

    private static final Set<Integer> VALID_ORDER_STATUSES = Set.of(
            STATUS_PENDING,
            STATUS_CONFIRMED,
            STATUS_SHIPPING,
            STATUS_COMPLETED,
            STATUS_CANCELLED,
            STATUS_DELIVERY_FAILED,
            STATUS_RETURN_REQUESTED,
            STATUS_RETURN_COMPLETED,
            STATUS_AWAITING_REFUND
    );

    private static final Set<String> SUPPORTED_ORDER_TYPES = Set.of(
            "ONLINE",
            "IN_STORE"
    );

    private static final Set<String> ADMIN_CANCEL_REASONS = Set.of(
            "Khách yêu cầu hủy đơn",
            "Không liên hệ được khách hàng",
            "Thông tin nhận hàng không hợp lệ",
            "Sản phẩm tạm hết hàng",
            "Khách đặt trùng đơn",
            "Đơn hàng có dấu hiệu bất thường",
            "Sai giá / sai thông tin sản phẩm",
            "Khác"
    );

    private final OrderRepository orderRepository;
    private final OrderDeliveryEvidenceRepository orderDeliveryEvidenceRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnRequestItemRepository returnRequestItemRepository;
    private final ReturnRequestMediaRepository returnRequestMediaRepository;
    private final UserRepository userRepository;
    private final Cloudinary cloudinary;
    private final EntityManager entityManager;
    private final OrderMailService orderMailService;

    private record KeywordDateRange(
            String keyword,
            LocalDateTime fromDateTime,
            LocalDateTime toDateTime
    ) {
    }

    private record AdminActorInfo(
            String displayName,
            String email
    ) {
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminOrderResponse> getOrders(
            String keyword,
            Integer status,
            String orderType,
            String paymentMethod,
            LocalDate fromDate,
            LocalDate toDate,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            Pageable pageable
    ) {
        validateAmountRange(minAmount, maxAmount);
        validateDateRange(fromDate, toDate);

        KeywordDateRange keywordDateRange = resolveKeywordDateRange(keyword);

        LocalDateTime requestFromDateTime =
                fromDate == null ? null : fromDate.atStartOfDay();

        LocalDateTime requestToDateTime =
                toDate == null ? null : toDate.plusDays(1).atStartOfDay();

        LocalDateTime finalFromDateTime =
                requestFromDateTime != null
                        ? requestFromDateTime
                        : keywordDateRange.fromDateTime();

        LocalDateTime finalToDateTime =
                requestToDateTime != null
                        ? requestToDateTime
                        : keywordDateRange.toDateTime();

        Page<Order> orderPage = orderRepository.searchAdminOrders(
                normalizeKeyword(keywordDateRange.keyword()),
                normalizeSearchStatus(status),
                normalizeOrderType(orderType),
                normalizePaymentMethod(paymentMethod),
                finalFromDateTime,
                finalToDateTime,
                minAmount,
                maxAmount,
                pageable
        );

        List<AdminOrderResponse> responses = orderPage.getContent()
                .stream()
                .sorted(this::compareOrdersForAdminList)
                .map(order -> mapOrderToResponse(order, false))
                .toList();

        return new PageImpl<>(responses, pageable, orderPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderResponse getOrderDetail(Integer orderId) {
        Order order = findOrderOrThrow(orderId);
        return mapOrderToResponse(order, true);
    }

    @Override
    @Transactional
    public AdminOrderResponse confirmOrder(Integer orderId) {
        Order order = findOrderOrThrow(orderId);

        if (safeStatus(order) != STATUS_PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ được xác nhận đơn hàng khi đơn còn ở trạng thái chờ xác nhận"
            );
        }

        deductStockWhenConfirm(order);

        order.setStatus(STATUS_CONFIRMED);
        Order savedOrder = orderRepository.save(order);

        orderMailService.sendOrderConfirmed(savedOrder);

        return mapOrderToResponse(savedOrder, true);
    }

    @Override
    @Transactional
    public AdminOrderResponse markDeliveryCompleted(
            Integer orderId,
            MarkDeliveryCompletedRequest request
    ) {
        Order order = findOrderOrThrow(orderId);

        if (safeStatus(order) != STATUS_SHIPPING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ được xác nhận giao hàng thành công khi đơn đang giao hàng"
            );
        }

        List<MultipartFile> files = request == null ? List.of() : request.getFiles();
        validateDeliveryEvidenceFiles(files, true);

        LocalDateTime now = LocalDateTime.now();

        order.setStatus(STATUS_COMPLETED);
        order.setCompletedAt(now);
        order.setDeliveryCompletedByName(getCurrentAdminDisplayName());

        Order savedOrder = orderRepository.save(order);

        saveDeliveryEvidenceFiles(savedOrder, files, DELIVERY_EVIDENCE_TYPE_SUCCESS);
        orderMailService.sendDeliveryCompleted(savedOrder);

        return mapOrderToResponse(savedOrder, true);
    }

    @Override
    @Transactional
    public AdminOrderResponse markDeliveryFailed(
            Integer orderId,
            MarkDeliveryFailedRequest request
    ) {
        Order order = findOrderOrThrow(orderId);

        if (safeStatus(order) != STATUS_SHIPPING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ được xác nhận giao hàng thất bại khi đơn đang giao hàng"
            );
        }

        String reason = normalizeDeliveryFailedReason(request);
        String description = normalizeDeliveryFailedDescription(request);

        if ("Khác".equals(reason) && description == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng nhập mô tả chi tiết khi chọn lý do Khác"
            );
        }

        List<MultipartFile> files = request == null ? List.of() : request.getFiles();
        boolean evidenceRequired = DELIVERY_FAILED_REQUIRES_EVIDENCE_REASONS.contains(reason);
        validateDeliveryEvidenceFiles(files, evidenceRequired);

        LocalDateTime now = LocalDateTime.now();

        order.setStatus(STATUS_DELIVERY_FAILED);
        order.setDeliveryFailedReason(reason);
        order.setDeliveryFailedDescription(description);
        order.setDeliveryFailedAt(now);
        order.setDeliveryFailedByName(getCurrentAdminDisplayName());

        applyDeliveryFailedRefundInfo(order);

        Order savedOrder = orderRepository.save(order);

        saveDeliveryEvidenceFiles(savedOrder, files, DELIVERY_EVIDENCE_TYPE_FAILED);
        orderMailService.sendDeliveryFailed(savedOrder);

        return mapOrderToResponse(savedOrder, true);
    }

    @Override
    @Transactional
    public AdminOrderResponse markDeliveryRefunded(Integer orderId) {
        Order order = findOrderOrThrow(orderId);

        if (safeStatus(order) != STATUS_DELIVERY_FAILED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ được xác nhận hoàn tiền cho đơn giao hàng thất bại"
            );
        }

        BigDecimal refundAmount = defaultMoney(order.getDeliveryRefundAmount());

        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đơn hàng này không phát sinh hoàn tiền giao thất bại"
            );
        }

        if (!hasDeliveryRefundBankInfo(order)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Khách chưa cung cấp đủ thông tin tài khoản hoàn tiền"
            );
        }

        if (order.getDeliveryRefundedAt() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đơn hàng này đã được xác nhận hoàn tiền"
            );
        }

        if (shouldRestoreStockAfterRefund()) {
            restoreStockWhenDeliveryRefunded(order);
        }

        order.setDeliveryRefundedAt(LocalDateTime.now());
        order.setDeliveryRefundedByName(getCurrentAdminDisplayName());

        Order savedOrder = orderRepository.save(order);

        orderMailService.sendDeliveryRefunded(savedOrder);

        return mapOrderToResponse(savedOrder, true);
    }

    @Override
    @Transactional
    public AdminOrderResponse cancelOrder(Integer orderId, AdminCancelOrderRequest request) {
        Order order = findOrderOrThrow(orderId);

        if (safeStatus(order) != STATUS_PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ được hủy đơn hàng khi đơn còn ở trạng thái chờ xác nhận"
            );
        }

        String cancelReason = normalizeAdminCancelReason(request);

        /*
         * LUỒNG NGHIỆP VỤ: Đơn Chờ xác nhận chưa trừ kho -> Hủy TUYỆT ĐỐI KHÔNG CỘNG KHO
         */
        if (isPrepaidPaymentMethod(order.getPaymentMethod())) {
            order.setStatus(STATUS_AWAITING_REFUND);
            order.setDeliveryRefundAmount(defaultMoney(order.getFinalAmount()));
        } else {
            order.setStatus(STATUS_CANCELLED);
            order.setDeliveryRefundAmount(null);
        }

        order.setCancelReason(cancelReason);
        order.setCancelledAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        orderMailService.sendOrderCancelled(savedOrder, cancelReason);

        return mapOrderToResponse(savedOrder, true);
    }

    @Override
    @Transactional
    public AdminOrderResponse markCancelRefunded(Integer orderId) {
        Order order = findOrderOrThrow(orderId);

        if (safeStatus(order) != STATUS_AWAITING_REFUND) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ được xác nhận hoàn tiền cho đơn đã hủy đang chờ hoàn tiền"
            );
        }

        // Ép buộc Admin chỉ được bấm xác nhận hoàn tiền khi khách đã điền Bank Info
        if (!hasDeliveryRefundBankInfo(order)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Khách chưa cung cấp đủ thông tin tài khoản hoàn tiền"
            );
        }

        /*
         * Xác nhận tiền đã chuyển xong -> Đẩy từ 8 về 4, ghi dấu vết.
         * Tuyệt đối KHÔNG CỘNG KHO.
         */
        order.setStatus(STATUS_CANCELLED);
        order.setDeliveryRefundedAt(LocalDateTime.now());
        order.setDeliveryRefundedByName(getCurrentAdminDisplayName());

        Order savedOrder = orderRepository.save(order);

        return mapOrderToResponse(savedOrder, true);
    }

    @Override
    @Transactional
    public AdminOrderResponse acceptReturnRequest(Integer orderId) {
        Order order = findOrderOrThrow(orderId);
        ReturnRequest returnRequest = getActiveReturnRequestForAdmin(order);
        List<ReturnRequestItem> returnItems = getReturnItemsOrThrow(returnRequest);

        if (!areAllReturnItemsPending(returnItems)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ yêu cầu hoàn hàng đang chờ xử lý mới được chấp nhận"
            );
        }

        for (ReturnRequestItem item : returnItems) {
            item.setStatus(RETURN_ITEM_STATUS_ACCEPTED);
            item.setRejectReason(null);
        }

        returnRequestItemRepository.saveAll(returnItems);

        Order savedOrder = orderRepository.save(order);
        orderMailService.sendReturnAccepted(savedOrder);

        return mapOrderToResponse(savedOrder, true);
    }

    @Override
    @Transactional
    public AdminOrderResponse rejectReturnRequest(Integer orderId, RejectReturnRequest request) {
        Order order = findOrderOrThrow(orderId);
        ReturnRequest returnRequest = getActiveReturnRequestForAdmin(order);
        List<ReturnRequestItem> returnItems = getReturnItemsOrThrow(returnRequest);

        if (!areAllReturnItemsPending(returnItems)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ yêu cầu hoàn hàng đang chờ xử lý mới được từ chối"
            );
        }

        String rejectReason = normalizeRejectReason(request);

        for (ReturnRequestItem item : returnItems) {
            item.setStatus(RETURN_ITEM_STATUS_REJECTED);
            item.setRejectReason(rejectReason);
        }

        returnRequestItemRepository.saveAll(returnItems);

        order.setStatus(STATUS_COMPLETED);
        Order savedOrder = orderRepository.save(order);

        orderMailService.sendReturnRejected(savedOrder, rejectReason);

        return mapOrderToResponse(savedOrder, true);
    }

    @Override
    @Transactional
    public AdminOrderResponse markReturnRefunded(Integer orderId) {
        Order order = findOrderOrThrow(orderId);
        ReturnRequest returnRequest = getActiveReturnRequestForAdmin(order);
        List<ReturnRequestItem> returnItems = getReturnItemsOrThrow(returnRequest);

        if (hasReturnItemStatus(returnItems, RETURN_ITEM_STATUS_REJECTED)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Yêu cầu hoàn hàng đã bị từ chối, không thể hoàn tiền"
            );
        }

        if (hasReturnItemStatus(returnItems, RETURN_ITEM_STATUS_PENDING)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Phải chấp nhận yêu cầu hoàn hàng trước khi xác nhận đã hoàn tiền"
            );
        }

        if (!areAllReturnItemsAccepted(returnItems)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ yêu cầu hoàn hàng đã được chấp nhận mới được chuyển sang đã hoàn tiền"
            );
        }

        if (shouldRestoreStockAfterRefund()) {
            restoreStockWhenReturnRefunded(returnItems);
        }

        returnItems.forEach(item -> item.setStatus(RETURN_ITEM_STATUS_COMPLETED));
        returnRequestItemRepository.saveAll(returnItems);

        order.setStatus(STATUS_RETURN_COMPLETED);
        Order savedOrder = orderRepository.save(order);

        orderMailService.sendReturnRefunded(savedOrder);

        return mapOrderToResponse(savedOrder, true);
    }

    private ReturnRequest getActiveReturnRequestForAdmin(Order order) {
        if (safeStatus(order) != STATUS_RETURN_REQUESTED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đơn hàng không ở trạng thái yêu cầu hoàn hàng"
            );
        }

        ReturnRequest returnRequest = getLatestReturnRequest(order);

        if (returnRequest == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không tìm thấy yêu cầu hoàn hàng của đơn này"
            );
        }

        return returnRequest;
    }

    private List<ReturnRequestItem> getReturnItemsOrThrow(ReturnRequest returnRequest) {
        if (returnRequest == null || returnRequest.getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không tìm thấy yêu cầu hoàn hàng của đơn này"
            );
        }

        List<ReturnRequestItem> returnItems =
                returnRequestItemRepository.findByReturnRequest_IdWithOrderItemDetail(returnRequest.getId());

        if (returnItems == null || returnItems.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Yêu cầu hoàn hàng chưa có sản phẩm cần hoàn"
            );
        }

        return returnItems;
    }

    private boolean areAllReturnItemsPending(List<ReturnRequestItem> returnItems) {
        return returnItems != null
                && !returnItems.isEmpty()
                && returnItems.stream().allMatch(item -> item != null
                && Integer.valueOf(RETURN_ITEM_STATUS_PENDING).equals(item.getStatus()));
    }

    private boolean areAllReturnItemsAccepted(List<ReturnRequestItem> returnItems) {
        return returnItems != null
                && !returnItems.isEmpty()
                && returnItems.stream().allMatch(item -> item != null
                && Integer.valueOf(RETURN_ITEM_STATUS_ACCEPTED).equals(item.getStatus()));
    }

    private boolean hasReturnItemStatus(List<ReturnRequestItem> returnItems, Integer status) {
        return returnItems != null
                && returnItems.stream().anyMatch(item -> item != null
                && Integer.valueOf(status).equals(item.getStatus()));
    }

    private boolean shouldRestoreStockAfterRefund() {
        try {
            if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
                return false;
            }

            HttpServletRequest request = attributes.getRequest();

            if (request == null) {
                return false;
            }

            String rawValue = normalizeOptionalText(request.getHeader("X-Restore-Stock"));

            if (rawValue == null) {
                rawValue = normalizeOptionalText(request.getParameter("restoreStock"));
            }

            if (rawValue == null) {
                return false;
            }

            String normalized = rawValue.trim().toLowerCase(Locale.ROOT);

            return normalized.equals("true")
                    || normalized.equals("1")
                    || normalized.equals("yes")
                    || normalized.equals("y");
        } catch (Exception ignored) {
            return false;
        }
    }

    private String normalizeAdminCancelReason(AdminCancelOrderRequest request) {
        if (request == null || request.reason() == null || request.reason().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng chọn lý do hủy đơn"
            );
        }

        String reason = request.reason()
                .trim()
                .replaceAll("[\r\n\t]+", " ")
                .replaceAll("\\s{2,}", " ");

        if (!ADMIN_CANCEL_REASONS.contains(reason)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Lý do hủy đơn không hợp lệ"
            );
        }

        String description = request.description() == null
                ? null
                : request.description()
                .trim()
                .replaceAll("[\r\n\t]+", " ")
                .replaceAll("\\s{2,}", " ");

        if (description != null && description.isEmpty()) {
            description = null;
        }

        if ("Khác".equals(reason) && description == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng nhập mô tả chi tiết khi chọn lý do Khác"
            );
        }

        if (description != null && description.length() < 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mô tả hủy đơn phải có ít nhất 5 ký tự"
            );
        }

        String fullReason = description == null ? reason : reason + " - " + description;

        if (fullReason.length() > 255) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Lý do hủy đơn không được vượt quá 255 ký tự"
            );
        }

        return fullReason;
    }


    private void deductStockWhenConfirm(Order order) {
        if (order == null || order.getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đơn hàng không hợp lệ"
            );
        }

        List<OrderItem> orderItems = orderItemRepository.findDetailByOrderId(order.getId());

        if (orderItems == null || orderItems.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Đơn hàng chưa có sản phẩm để xác nhận"
            );
        }

        for (OrderItem item : orderItems) {
            if (item == null || item.getProductVariant() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Đơn hàng có sản phẩm không hợp lệ"
                );
            }

            ProductVariant variant = item.getProductVariant();
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();

            if (quantity <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Số lượng sản phẩm trong đơn không hợp lệ"
                );
            }

            int currentStock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();

            if (currentStock < quantity) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sản phẩm " + resolveOrderItemSku(item, variant)
                                + " chỉ còn " + currentStock
                                + " trong kho, không đủ để xác nhận đơn"
                );
            }
        }

        for (OrderItem item : orderItems) {
            ProductVariant variant = item.getProductVariant();
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            int currentStock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();

            variant.setStockQuantity(currentStock - quantity);
            entityManager.merge(variant);
        }
    }

    private String resolveOrderItemSku(OrderItem item, ProductVariant variant) {
        String itemSku = item == null ? null : normalizeOptionalText(item.getSku());

        if (itemSku != null) {
            return itemSku;
        }

        String variantSku = variant == null ? null : normalizeOptionalText(variant.getSku());

        return variantSku == null ? "không xác định" : variantSku;
    }

    private void restoreStockWhenAdminCancel(Order order) {
        if (order == null || order.getId() == null) {
            return;
        }

        List<OrderItem> orderItems = orderItemRepository.findDetailByOrderId(order.getId());

        if (orderItems == null || orderItems.isEmpty()) {
            return;
        }

        for (OrderItem item : orderItems) {
            restoreStockByOrderItemQuantity(item);
        }
    }

    private void restoreStockWhenDeliveryRefunded(Order order) {
        if (order == null || order.getId() == null) {
            return;
        }

        List<OrderItem> orderItems = orderItemRepository.findDetailByOrderId(order.getId());

        if (orderItems == null || orderItems.isEmpty()) {
            return;
        }

        for (OrderItem item : orderItems) {
            restoreStockByOrderItemQuantity(item);
        }
    }

    private void restoreStockWhenReturnRefunded(List<ReturnRequestItem> returnItems) {
        if (returnItems == null || returnItems.isEmpty()) {
            return;
        }

        for (ReturnRequestItem returnItem : returnItems) {
            if (returnItem == null || returnItem.getOrderItem() == null) {
                continue;
            }

            OrderItem orderItem = returnItem.getOrderItem();

            if (orderItem.getProductVariant() == null) {
                continue;
            }

            int quantity = returnItem.getReturnQuantity() == null ? 0 : returnItem.getReturnQuantity();

            if (quantity <= 0) {
                continue;
            }

            ProductVariant variant = orderItem.getProductVariant();
            int currentStock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
            variant.setStockQuantity(currentStock + quantity);
        }
    }

    private void restoreStockByOrderItemQuantity(OrderItem item) {
        if (item == null || item.getProductVariant() == null) {
            return;
        }

        ProductVariant variant = item.getProductVariant();
        int quantity = item.getQuantity() == null ? 0 : item.getQuantity();

        if (quantity <= 0) {
            return;
        }

        int currentStock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
        variant.setStockQuantity(currentStock + quantity);
    }

    private String normalizeRejectReason(RejectReturnRequest request) {
        if (request == null || request.reason() == null || request.reason().trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng nhập lý do từ chối hoàn hàng"
            );
        }

        String reason = request.reason()
                .trim()
                .replaceAll("[\r\n\t]+", " ")
                .replaceAll("\\s{2,}", " ");

        if (reason.length() < 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Lý do từ chối phải có ít nhất 5 ký tự"
            );
        }

        if (reason.length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Lý do từ chối không được vượt quá 500 ký tự"
            );
        }

        return reason;
    }

    private String normalizeDeliveryFailedReason(MarkDeliveryFailedRequest request) {
        String reason = request == null ? null : normalizeOptionalText(request.getReason());

        if (reason == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng chọn lý do giao hàng thất bại"
            );
        }

        if (!DELIVERY_FAILED_REASONS.contains(reason)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Lý do giao hàng thất bại không hợp lệ"
            );
        }

        return reason;
    }

    private String normalizeDeliveryFailedDescription(MarkDeliveryFailedRequest request) {
        String description = request == null ? null : normalizeOptionalText(request.getDescription());

        if (description == null) {
            return null;
        }

        if (description.length() < 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mô tả giao hàng thất bại phải có ít nhất 5 ký tự"
            );
        }

        if (description.length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mô tả giao hàng thất bại không được vượt quá 500 ký tự"
            );
        }

        return description;
    }

    private void validateDeliveryEvidenceFiles(
            List<MultipartFile> mediaFiles,
            boolean required
    ) {
        List<MultipartFile> files = normalizeMultipartFiles(mediaFiles);

        if (required && files.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng tải lên ảnh minh chứng giao hàng"
            );
        }

        if (files.size() > MAX_DELIVERY_IMAGE_COUNT) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ được tải tối đa 2 ảnh minh chứng giao hàng"
            );
        }

        long totalSize = files.stream()
                .mapToLong(MultipartFile::getSize)
                .sum();

        if (totalSize > MAX_DELIVERY_EVIDENCE_TOTAL_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tổng dung lượng ảnh minh chứng không được vượt quá 10MB"
            );
        }

        for (MultipartFile file : files) {
            validateSingleDeliveryEvidenceImage(file);
        }
    }

    private void validateSingleDeliveryEvidenceImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }

        String filename = file.getOriginalFilename() == null
                ? ""
                : file.getOriginalFilename().trim();
        String extension = getFileExtension(filename);

        if (extension.isBlank() || !ALLOWED_DELIVERY_IMAGE_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ảnh minh chứng chỉ hỗ trợ JPG, JPEG, PNG hoặc WEBP"
            );
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().trim().toLowerCase(Locale.ROOT);

        if (!contentType.isBlank() && !contentType.startsWith("image/")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ được tải lên file ảnh minh chứng"
            );
        }

    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }

        String cleanFilename = Paths.get(filename).getFileName().toString();
        int dotIndex = cleanFilename.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == cleanFilename.length() - 1) {
            return "";
        }

        return cleanFilename.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private void saveDeliveryEvidenceFiles(
            Order order,
            List<MultipartFile> mediaFiles,
            Integer evidenceType
    ) {
        List<MultipartFile> files = normalizeMultipartFiles(mediaFiles);

        if (files.isEmpty()) {
            return;
        }

        List<OrderDeliveryEvidence> evidences = new ArrayList<>();

        for (MultipartFile file : files) {
            String imageUrl = uploadDeliveryEvidenceFile(file);

            OrderDeliveryEvidence evidence = new OrderDeliveryEvidence();
            evidence.setOrder(order);
            evidence.setEvidenceType(evidenceType);
            evidence.setImageUrl(imageUrl);
            evidence.setCreatedAt(LocalDateTime.now());

            evidences.add(evidence);
        }

        orderDeliveryEvidenceRepository.saveAll(evidences);
    }

    private String uploadDeliveryEvidenceFile(MultipartFile file) {
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder",
                            DELIVERY_EVIDENCE_CLOUDINARY_FOLDER,
                            "resource_type",
                            "image"
                    )
            );

            Object secureUrl = uploadResult.get("secure_url");

            if (secureUrl == null || secureUrl.toString().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Không lấy được đường dẫn ảnh minh chứng sau khi tải lên"
                );
            }

            return secureUrl.toString();
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể tải ảnh minh chứng giao hàng"
            );
        }
    }

    private List<MultipartFile> normalizeMultipartFiles(List<MultipartFile> mediaFiles) {
        if (mediaFiles == null || mediaFiles.isEmpty()) {
            return List.of();
        }

        return mediaFiles.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
    }

    private List<String> getDeliveryEvidenceUrls(Order order, Integer evidenceType) {
        if (order == null || order.getId() == null) {
            return new ArrayList<>();
        }

        return orderDeliveryEvidenceRepository
                .findByOrder_IdAndEvidenceTypeOrderByCreatedAtAsc(order.getId(), evidenceType)
                .stream()
                .map(OrderDeliveryEvidence::getImageUrl)
                .filter(url -> url != null && !url.trim().isEmpty())
                .toList();
    }

    private String getCurrentAdminDisplayName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return "Nhân viên cửa hàng";
        }

        String email = normalizeOptionalText(authentication.getName());

        if (email == null) {
            return "Nhân viên cửa hàng";
        }

        return resolveUserDisplayNameByEmail(email);
    }

    private String resolveUserDisplayNameByEmail(String email) {
        String cleanEmail = normalizeOptionalText(email);

        if (cleanEmail == null) {
            return "Nhân viên cửa hàng";
        }

        return formatAdminActorInfo(findAdminActorInfo(cleanEmail), cleanEmail);
    }

    private AdminActorInfo findAdminActorInfo(String principalName) {
        String cleanPrincipalName = normalizeOptionalText(principalName);

        if (cleanPrincipalName == null) {
            return new AdminActorInfo(null, null);
        }

        try {
            User user = userRepository.findByEmailIgnoreCase(cleanPrincipalName).orElse(null);

            if (user != null) {
                return new AdminActorInfo(
                        normalizeOptionalText(user.getName()),
                        normalizeOptionalText(user.getEmail())
                );
            }
        } catch (Exception ignored) {
        }

        try {
            List<?> result = entityManager
                    .createNativeQuery("SELECT TOP 1 Name, Email FROM dbo.Users WHERE LOWER(Email) = LOWER(:principalName)")
                    .setParameter("principalName", cleanPrincipalName)
                    .getResultList();

            if (!result.isEmpty()) {
                Object row = result.get(0);

                if (row instanceof Object[] values) {
                    String displayName = values.length > 0 ? normalizeObjectText(values[0]) : null;
                    String email = values.length > 1 ? normalizeObjectText(values[1]) : null;

                    return new AdminActorInfo(displayName, email);
                }
            }
        } catch (Exception ignored) {
        }

        if (isEmailLike(cleanPrincipalName)) {
            return new AdminActorInfo(null, cleanPrincipalName);
        }

        return new AdminActorInfo(cleanPrincipalName, null);
    }

    private String normalizeObjectText(Object value) {
        if (value == null) {
            return null;
        }

        return normalizeOptionalText(value.toString());
    }

    private String normalizeActorDisplayName(String value) {
        String cleanValue = normalizeActorText(value);

        if (cleanValue == null) {
            return null;
        }

        String extractedEmail = extractEmail(cleanValue);

        if (cleanValue.contains("\n")) {
            String[] lines = cleanValue.split("\\R+");
            String displayName = lines.length > 0 ? normalizeOptionalText(lines[0]) : null;
            String email = extractedEmail;

            if (email != null) {
                AdminActorInfo actorInfo = findAdminActorInfo(email);
                String resolvedName = normalizeOptionalText(actorInfo.displayName());

                if (resolvedName != null
                        && !resolvedName.equalsIgnoreCase(email)
                        && (displayName == null || "Nhân viên cửa hàng".equalsIgnoreCase(displayName))) {
                    displayName = resolvedName;
                }
            }

            if (displayName != null && email != null && !displayName.equalsIgnoreCase(email)) {
                return truncateDeliveryActorInfo(displayName + "\n" + email);
            }

            if (email != null) {
                return resolveUserDisplayNameByEmail(email);
            }

            return truncateDeliveryActorInfo(cleanValue);
        }

        if (isEmailLike(cleanValue)) {
            return resolveUserDisplayNameByEmail(cleanValue);
        }

        if (extractedEmail != null) {
            AdminActorInfo actorInfo = findAdminActorInfo(extractedEmail);
            String resolvedName = normalizeOptionalText(actorInfo.displayName());

            if (resolvedName != null && !resolvedName.equalsIgnoreCase(extractedEmail)) {
                return truncateDeliveryActorInfo(resolvedName + "\n" + extractedEmail);
            }
        }

        return truncateDeliveryActorInfo(cleanValue);
    }

    private String formatAdminActorInfo(AdminActorInfo actorInfo, String fallbackValue) {
        String fallback = normalizeOptionalText(fallbackValue);
        String displayName = actorInfo == null ? null : normalizeOptionalText(actorInfo.displayName());
        String email = actorInfo == null ? null : normalizeOptionalText(actorInfo.email());

        if (displayName != null && email != null && !displayName.equalsIgnoreCase(email)) {
            return truncateDeliveryActorInfo(displayName + "\n" + email);
        }

        if (displayName != null) {
            return truncateDeliveryActorInfo(displayName);
        }

        if (email != null) {
            return truncateDeliveryActorInfo("Nhân viên cửa hàng\n" + email);
        }

        if (fallback != null) {
            return truncateDeliveryActorInfo(fallback);
        }

        return "Nhân viên cửa hàng";
    }

    private String truncateDeliveryActorInfo(String value) {
        String cleanValue = normalizeActorText(value);

        if (cleanValue == null) {
            return "Nhân viên cửa hàng";
        }

        if (cleanValue.length() <= 255) {
            return cleanValue;
        }

        return cleanValue.substring(0, 255).trim();
    }

    private String normalizeActorText(String value) {
        if (value == null) {
            return null;
        }

        String cleanValue = value
                .trim()
                .replaceAll("\\r\\n|\\r", "\n")
                .replaceAll("\\t+", " ");

        String[] lines = cleanValue.split("\\n+");
        List<String> cleanLines = new ArrayList<>();

        for (String line : lines) {
            String cleanLine = normalizeOptionalText(line);

            if (cleanLine != null) {
                cleanLines.add(cleanLine);
            }
        }

        if (cleanLines.isEmpty()) {
            return null;
        }

        return String.join("\n", cleanLines);
    }

    private String extractEmail(String value) {
        String cleanValue = normalizeOptionalText(value);

        if (cleanValue == null) {
            return null;
        }

        String[] parts = cleanValue.split("\\s+");

        for (String part : parts) {
            String cleanPart = part
                    .trim()
                    .replaceAll("^[<({\\[]+", "")
                    .replaceAll("[>)}\\],.;:]+$", "");

            if (isEmailLike(cleanPart)) {
                return cleanPart;
            }
        }

        return null;
    }

    private boolean isEmailLike(String value) {
        String cleanValue = normalizeOptionalText(value);

        return cleanValue != null
                && cleanValue.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private void applyDeliveryFailedRefundInfo(Order order) {
        if (order == null) {
            return;
        }

        if (isPrepaidPaymentMethod(order.getPaymentMethod())) {
            BigDecimal refundAmount = defaultMoney(order.getFinalAmount());

            if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Số tiền cần hoàn không hợp lệ"
                );
            }

            order.setDeliveryRefundAmount(refundAmount);
            order.setDeliveryRefundedAt(null);
            order.setDeliveryRefundedByName(null);
            return;
        }

        order.setDeliveryRefundAmount(null);
        order.setDeliveryRefundBankName(null);
        order.setDeliveryRefundBankAccountNumber(null);
        order.setDeliveryRefundBankAccountHolder(null);
        order.setDeliveryRefundedAt(null);
        order.setDeliveryRefundedByName(null);
    }

    private boolean isPrepaidPaymentMethod(String paymentMethod) {
        String method = normalizeOptionalText(paymentMethod);

        if (method == null) {
            return false;
        }

        String upperMethod = method.toUpperCase(Locale.ROOT);

        if (upperMethod.contains("COD")) {
            return false;
        }

        return upperMethod.contains("VNPAY")
                || upperMethod.contains("VIETQR")
                || upperMethod.contains("QR")
                || upperMethod.contains("BANK")
                || upperMethod.contains("TRANSFER")
                || upperMethod.contains("MOMO");
    }

    private boolean hasDeliveryRefundBankInfo(Order order) {
        if (order == null) {
            return false;
        }

        return normalizeOptionalText(order.getDeliveryRefundBankName()) != null
                && normalizeOptionalText(order.getDeliveryRefundBankAccountNumber()) != null
                && normalizeOptionalText(order.getDeliveryRefundBankAccountHolder()) != null;
    }

    private boolean isDeliveryRefundRequired(Order order) {
        return order != null
                && defaultMoney(order.getDeliveryRefundAmount()).compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean canMarkDeliveryRefunded(Order order) {
        return order != null
                && safeStatus(order) == STATUS_DELIVERY_FAILED
                && isDeliveryRefundRequired(order)
                && hasDeliveryRefundBankInfo(order)
                && order.getDeliveryRefundedAt() == null;
    }

    private AdminOrderResponse mapOrderToResponse(Order order, boolean includeItems) {
        AdminOrderResponse response = new AdminOrderResponse();

        response.setOrderId(order.getId());
        response.setOrderCode(formatOrderCode(order.getId()));
        response.setOrderType(order.getOrderType());

        if (order.getCustomer() != null) {
            response.setCustomerId(order.getCustomer().getUserId());
        }

        response.setCustomerName(order.getCustomerName());
        response.setCustomerPhone(order.getCustomerPhone());
        response.setShippingAddress(order.getShippingAddress());

        if (order.getCashier() != null) {
            response.setCashierId(order.getCashier().getUserId());
        }

        if (order.getVoucher() != null) {
            String voucherCode = order.getVoucher().getCode();

            response.setVoucher(
                    new AdminOrderResponse.VoucherInfo(
                            order.getVoucher().getId(),
                            voucherCode,
                            voucherCode
                    )
            );
        }

        response.setTotalAmount(defaultMoney(order.getTotalAmount()));
        response.setDiscountAmount(defaultMoney(order.getDiscountAmount()));
        response.setFinalAmount(defaultMoney(order.getFinalAmount()));
        response.setShippingFee(defaultMoney(order.getShippingFee()));

        response.setPaymentMethod(order.getPaymentMethod());
        response.setStatus(order.getStatus());
        response.setStatusText(getStatusText(order.getStatus()));
        response.setCreatedAt(order.getCreatedAt());
        response.setCompletedAt(order.getCompletedAt());
        response.setDeliveryCompletedByName(normalizeActorDisplayName(order.getDeliveryCompletedByName()));
        response.setDeliveryFailedReason(normalizeOptionalText(order.getDeliveryFailedReason()));
        response.setDeliveryFailedDescription(normalizeOptionalText(order.getDeliveryFailedDescription()));
        response.setDeliveryFailedAt(order.getDeliveryFailedAt());
        response.setDeliveryFailedByName(normalizeActorDisplayName(order.getDeliveryFailedByName()));
        response.setDeliveryRefundAmount(order.getDeliveryRefundAmount());
        response.setDeliveryRefundBankName(normalizeOptionalText(order.getDeliveryRefundBankName()));
        response.setDeliveryRefundBankAccountNumber(normalizeOptionalText(order.getDeliveryRefundBankAccountNumber()));
        response.setDeliveryRefundBankAccountHolder(normalizeOptionalText(order.getDeliveryRefundBankAccountHolder()));
        response.setDeliveryRefundedAt(order.getDeliveryRefundedAt());
        response.setDeliveryRefundedByName(normalizeActorDisplayName(order.getDeliveryRefundedByName()));
        response.setDeliveryRefundRequired(isDeliveryRefundRequired(order));
        response.setDeliveryRefundBankInfoProvided(hasDeliveryRefundBankInfo(order));
        response.setDeliveryRefundCompleted(order.getDeliveryRefundedAt() != null);
        response.setCanMarkDeliveryRefunded(canMarkDeliveryRefunded(order));
        response.setDeliverySuccessMediaUrls(getDeliveryEvidenceUrls(order, DELIVERY_EVIDENCE_TYPE_SUCCESS));
        response.setDeliveryFailedMediaUrls(getDeliveryEvidenceUrls(order, DELIVERY_EVIDENCE_TYPE_FAILED));
        response.setCancelReason(normalizeOptionalText(order.getCancelReason()));
        response.setCancelledAt(order.getCancelledAt());

        response.setIsPaymentReported(order.getIsPaymentReported() != null && order.getIsPaymentReported());

        applyLatestReturnRequestInfo(response, order, includeItems);

        if (includeItems) {
            var items = orderItemRepository.findDetailByOrderId(order.getId());

            var itemResponses = items
                    .stream()
                    .map(this::mapOrderItemToResponse)
                    .toList();

            response.setItems(itemResponses);
            response.setTotalQuantity(
                    itemResponses.stream()
                            .mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                            .sum()
            );
        } else {
            response.setItems(new ArrayList<>());
            response.setTotalQuantity(null);
        }

        return response;
    }

    private int compareOrdersForAdminList(Order first, Order second) {
        boolean firstReturnRequested = safeStatus(first) == STATUS_RETURN_REQUESTED;
        boolean secondReturnRequested = safeStatus(second) == STATUS_RETURN_REQUESTED;

        if (firstReturnRequested != secondReturnRequested) {
            return firstReturnRequested ? -1 : 1;
        }

        LocalDateTime firstTime = firstReturnRequested
                ? getLatestReturnRequestedAt(first)
                : getOrderSortTime(first);

        LocalDateTime secondTime = secondReturnRequested
                ? getLatestReturnRequestedAt(second)
                : getOrderSortTime(second);

        int timeCompare = Comparator
                .nullsLast(LocalDateTime::compareTo)
                .compare(secondTime, firstTime);

        if (timeCompare != 0) {
            return timeCompare;
        }

        return Integer.compare(
                second == null || second.getId() == null ? 0 : second.getId(),
                first == null || first.getId() == null ? 0 : first.getId()
        );
    }

    private LocalDateTime getOrderSortTime(Order order) {
        if (order == null) {
            return null;
        }

        if (order.getCompletedAt() != null) {
            return order.getCompletedAt();
        }

        return order.getCreatedAt();
    }

    private LocalDateTime getLatestReturnRequestedAt(Order order) {
        ReturnRequest returnRequest = getLatestReturnRequest(order);

        if (returnRequest != null && returnRequest.getCreatedAt() != null) {
            return returnRequest.getCreatedAt();
        }

        return getOrderSortTime(order);
    }

    private ReturnRequest getLatestReturnRequest(Order order) {
        if (order == null || order.getId() == null) {
            return null;
        }

        return returnRequestRepository
                .findTopByOrder_IdOrderByCreatedAtDesc(order.getId())
                .orElse(null);
    }

    private void applyLatestReturnRequestInfo(
            AdminOrderResponse response,
            Order order,
            boolean includeItems
    ) {
        if (response == null || order == null) {
            return;
        }

        ReturnRequest returnRequest = getLatestReturnRequest(order);

        if (returnRequest == null || !isReturnOrder(order, returnRequest)) {
            response.setCanAcceptReturn(false);
            response.setCanRejectReturn(false);
            response.setCanMarkReturnRefunded(false);
            return;
        }

        response.setReturnType(formatReturnType(returnRequest.getReturnType()));
        response.setReturnReason(returnRequest.getReason());
        response.setReturnDescription(returnRequest.getDescription());
        response.setReturnRequestedAt(returnRequest.getCreatedAt());
        response.setRefundMethod(formatRefundMethod(returnRequest.getRefundMethod()));

        List<ReturnRequestItem> returnItems =
                returnRequestItemRepository.findByReturnRequest_IdWithOrderItemDetail(returnRequest.getId());

        BigDecimal returnShippingFee = resolveReturnShippingFee(order, returnRequest);
        BigDecimal returnRefundAmount = resolveReturnRefundAmount(returnRequest, returnItems, returnShippingFee);

        response.setReturnRefundAmount(returnRefundAmount);
        response.setRefundAmount(returnRefundAmount);
        response.setReturnShippingFee(returnShippingFee);
        response.setBankName(returnRequest.getBankName());
        response.setBankAccountNumber(returnRequest.getBankAccountNumber());
        response.setBankAccountHolder(returnRequest.getBankAccountHolder());

        String processStatus = resolveReturnProcessStatus(returnItems);
        response.setReturnProcessStatus(processStatus);
        response.setReturnProcessStatusText(getReturnProcessStatusText(processStatus));
        response.setReturnRejectReason(resolveReturnRejectReason(returnItems));

        boolean canProcess = safeStatus(order) == STATUS_RETURN_REQUESTED;
        response.setCanAcceptReturn(canProcess && areAllReturnItemsPending(returnItems));
        response.setCanRejectReturn(canProcess && areAllReturnItemsPending(returnItems));
        response.setCanMarkReturnRefunded(canProcess && areAllReturnItemsAccepted(returnItems));

        if (order.getCustomer() != null && order.getCustomer().getUser() != null) {
            response.setReturnEmail(order.getCustomer().getUser().getEmail());
        }

        List<ReturnRequestMedia> medias =
                returnRequestMediaRepository.findByReturnRequest_Id(returnRequest.getId());

        response.setReturnImages(
                medias.stream()
                        .filter(media -> media != null && Integer.valueOf(MEDIA_TYPE_IMAGE).equals(media.getMediaType()))
                        .map(ReturnRequestMedia::getMediaUrl)
                        .map(this::cleanImageUrl)
                        .filter(value -> value != null)
                        .toList()
        );

        response.setReturnVideos(
                medias.stream()
                        .filter(media -> media != null && Integer.valueOf(MEDIA_TYPE_VIDEO).equals(media.getMediaType()))
                        .map(ReturnRequestMedia::getMediaUrl)
                        .map(this::cleanImageUrl)
                        .filter(value -> value != null)
                        .toList()
        );

        if (includeItems) {
            List<OrderItem> detailOrderItems = orderItemRepository.findDetailByOrderId(order.getId());

            response.setReturnItems(
                    returnItems.stream()
                            .map(returnItem -> mapReturnItemToResponse(returnItem, detailOrderItems))
                            .toList()
            );
        } else {
            response.setReturnItems(new ArrayList<>());
        }
    }

    private boolean isReturnOrder(Order order, ReturnRequest returnRequest) {
        int status = safeStatus(order);

        return status == STATUS_RETURN_REQUESTED
                || status == STATUS_RETURN_COMPLETED
                || returnRequest != null;
    }

    private BigDecimal resolveReturnShippingFee(Order order, ReturnRequest returnRequest) {
        if (order == null || returnRequest == null) {
            return BigDecimal.ZERO;
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
        List<ReturnRequestItem> returnItems = returnRequestItemRepository.findByReturnRequest_IdWithOrderItemDetail(returnRequest.getId());

        if (!isFullOrderReturn(orderItems, returnItems)) {
            return BigDecimal.ZERO;
        }

        if (!isShopFaultReturnReason(returnRequest.getReturnType(), returnRequest.getReason())) {
            return BigDecimal.ZERO;
        }

        return defaultMoney(order.getShippingFee());
    }

    private boolean isFullOrderReturn(List<OrderItem> orderItems, List<ReturnRequestItem> returnItems) {
        if (orderItems == null || orderItems.isEmpty() || returnItems == null || returnItems.isEmpty()) {
            return false;
        }

        for (OrderItem orderItem : orderItems) {
            if (orderItem == null || orderItem.getId() == null) {
                continue;
            }

            Integer orderedQuantity = orderItem.getQuantity();
            if (orderedQuantity == null || orderedQuantity <= 0) {
                continue;
            }

            Integer returnQuantity = returnItems.stream()
                    .filter(item -> item != null
                            && item.getOrderItem() != null
                            && orderItem.getId().equals(item.getOrderItem().getId()))
                    .map(ReturnRequestItem::getReturnQuantity)
                    .findFirst()
                    .orElse(null);

            if (returnQuantity == null || !returnQuantity.equals(orderedQuantity)) {
                return false;
            }
        }

        return true;
    }

    private boolean isShopFaultReturnReason(Integer returnType, String reason) {
        String cleanReason = reason == null ? "" : reason.trim();

        if (cleanReason.isEmpty()) {
            return false;
        }

        if (Integer.valueOf(RETURN_TYPE_RECEIVED_WITH_PROBLEM_VALUE).equals(returnType)) {
            return cleanReason.equals("Thiếu hàng")
                    || cleanReason.equals("Người bán gửi sai hàng")
                    || cleanReason.startsWith("Hàng bể vỡ")
                    || cleanReason.equals("Hàng lỗi, không hoạt động")
                    || cleanReason.equals("Hàng hết hạn sử dụng")
                    || cleanReason.equals("Khác với mô tả")
                    || cleanReason.equals("Hàng đã qua sử dụng")
                    || cleanReason.equals("Hàng giả, nhái");
        }

        if (Integer.valueOf(RETURN_TYPE_NOT_RECEIVED_OR_MISSING_VALUE).equals(returnType)) {
            return cleanReason.equals("Chưa nhận được hàng")
                    || cleanReason.equals("Thiếu hàng")
                    || cleanReason.equals("Thùng hàng rỗng");
        }

        return false;
    }

    private BigDecimal resolveReturnRefundAmount(
            ReturnRequest returnRequest,
            List<ReturnRequestItem> returnItems,
            BigDecimal returnShippingFee
    ) {
        if (returnRequest == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal storedRefundAmount = defaultMoney(returnRequest.getRefundAmount());
        BigDecimal shippingFee = defaultMoney(returnShippingFee);

        if (shippingFee.compareTo(BigDecimal.ZERO) <= 0) {
            return storedRefundAmount;
        }

        BigDecimal itemRefundTotal = returnItems == null
                ? BigDecimal.ZERO
                : returnItems.stream()
                .filter(item -> item != null)
                .map(ReturnRequestItem::getRefundAmount)
                .map(this::defaultMoney)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expectedTotalWithShipping = itemRefundTotal.add(shippingFee);

        if (storedRefundAmount.compareTo(expectedTotalWithShipping) >= 0) {
            return storedRefundAmount;
        }

        if (storedRefundAmount.compareTo(itemRefundTotal) <= 0) {
            return storedRefundAmount.add(shippingFee);
        }

        return storedRefundAmount;
    }

    private String formatReturnType(Integer returnType) {
        if (returnType == null) {
            return null;
        }

        return switch (returnType) {
            case RETURN_TYPE_RECEIVED_WITH_PROBLEM_VALUE -> "RECEIVED_WITH_PROBLEM";
            case RETURN_TYPE_NOT_RECEIVED_OR_MISSING_VALUE -> "NOT_RECEIVED_OR_MISSING";
            default -> "UNKNOWN";
        };
    }

    private String formatRefundMethod(Integer refundMethod) {
        if (refundMethod == null) {
            return null;
        }

        return switch (refundMethod) {
            case REFUND_METHOD_BANK_TRANSFER_VALUE -> "BANK_TRANSFER";
            case REFUND_METHOD_STORE_VALUE -> "STORE";
            default -> "UNKNOWN";
        };
    }

    private AdminReturnItemResponse mapReturnItemToResponse(ReturnRequestItem returnItem) {
        return mapReturnItemToResponse(returnItem, List.of());
    }

    private AdminReturnItemResponse mapReturnItemToResponse(
            ReturnRequestItem returnItem,
            List<OrderItem> detailOrderItems
    ) {
        AdminReturnItemResponse response = new AdminReturnItemResponse();

        if (returnItem == null) {
            return response;
        }

        response.setReturnRequestItemId(returnItem.getId());
        response.setReturnQuantity(returnItem.getReturnQuantity() == null ? 0 : returnItem.getReturnQuantity());
        response.setRefundAmount(defaultMoney(returnItem.getRefundAmount()));
        response.setStatus(returnItem.getStatus());
        response.setStatusText(getReturnItemStatusText(returnItem.getStatus()));
        response.setRejectReason(returnItem.getRejectReason());

        OrderItem orderItem = resolveReturnOrderItem(returnItem, detailOrderItems);

        if (orderItem == null) {
            return response;
        }

        int returnQuantity = response.getReturnQuantity() == null ? 0 : response.getReturnQuantity();
        BigDecimal unitOriginalPrice = defaultMoney(orderItem.getOriginalPrice());
        BigDecimal unitDiscountAmount = defaultMoney(orderItem.getDiscountAmount());
        BigDecimal unitFinalPrice = defaultMoney(orderItem.getFinalPrice());
        BigDecimal refundAmount = defaultMoney(returnItem.getRefundAmount());

        BigDecimal itemOriginalAmount = unitOriginalPrice.multiply(BigDecimal.valueOf(returnQuantity));
        BigDecimal itemDiscountAmount = unitDiscountAmount.multiply(BigDecimal.valueOf(returnQuantity));
        BigDecimal itemFinalAmount = unitFinalPrice.multiply(BigDecimal.valueOf(returnQuantity));
        BigDecimal voucherAllocatedAmount = itemFinalAmount.subtract(refundAmount).max(BigDecimal.ZERO);

        response.setOrderItemId(orderItem.getId());
        response.setOrderedQuantity(orderItem.getQuantity() == null ? 0 : orderItem.getQuantity());
        response.setUnitOriginalPrice(unitOriginalPrice);
        response.setUnitDiscountAmount(unitDiscountAmount);
        response.setUnitFinalPrice(unitFinalPrice);
        response.setItemOriginalAmount(itemOriginalAmount);
        response.setItemDiscountAmount(itemDiscountAmount);
        response.setItemAmount(itemFinalAmount);
        response.setVoucherAllocatedAmount(voucherAllocatedAmount);

        ProductVariant variant = orderItem.getProductVariant();

        if (variant == null) {
            response.setImageUrl(cleanImageUrl(orderItem.getImage()));
            return response;
        }

        response.setProductVariantId(variant.getId());
        response.setSku(variant.getSku());
        response.setImageUrl(resolveOrderItemImage(orderItem, variant));

        if (variant.getProduct() != null) {
            response.setProductId(variant.getProduct().getId());
            response.setProductName(variant.getProduct().getName());

            if (variant.getProduct().getBrand() != null) {
                response.setBrandName(variant.getProduct().getBrand().getName());
            }
        }

        if (variant.getCapacity() != null && variant.getCapacity().getValue() != null) {
            response.setCapacity(formatCapacity(variant.getCapacity().getValue()));
        }

        if (variant.getBottleType() != null) {
            response.setBottleType(variant.getBottleType().getName());
        }

        return response;
    }

    private OrderItem resolveReturnOrderItem(
            ReturnRequestItem returnItem,
            List<OrderItem> detailOrderItems
    ) {
        if (returnItem == null) {
            return null;
        }

        OrderItem originalOrderItem = returnItem.getOrderItem();

        if (originalOrderItem == null || originalOrderItem.getId() == null) {
            return originalOrderItem;
        }

        if (detailOrderItems == null || detailOrderItems.isEmpty()) {
            return originalOrderItem;
        }

        for (OrderItem detailOrderItem : detailOrderItems) {
            if (detailOrderItem == null || detailOrderItem.getId() == null) {
                continue;
            }

            if (!detailOrderItem.getId().equals(originalOrderItem.getId())) {
                continue;
            }

            return detailOrderItem;
        }

        return originalOrderItem;
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
            default -> "Không xác định";
        };
    }

    private String resolveReturnProcessStatus(List<ReturnRequestItem> returnItems) {
        if (returnItems == null || returnItems.isEmpty()) {
            return "UNKNOWN";
        }

        boolean allPending = returnItems.stream()
                .allMatch(item -> item != null
                        && Integer.valueOf(RETURN_ITEM_STATUS_PENDING).equals(item.getStatus()));
        boolean allAccepted = returnItems.stream()
                .allMatch(item -> item != null
                        && Integer.valueOf(RETURN_ITEM_STATUS_ACCEPTED).equals(item.getStatus()));
        boolean allRejected = returnItems.stream()
                .allMatch(item -> item != null
                        && Integer.valueOf(RETURN_ITEM_STATUS_REJECTED).equals(item.getStatus()));
        boolean allCompleted = returnItems.stream()
                .allMatch(item -> item != null
                        && Integer.valueOf(RETURN_ITEM_STATUS_COMPLETED).equals(item.getStatus()));

        if (allPending) {
            return "PENDING";
        }

        if (allAccepted) {
            return "ACCEPTED";
        }

        if (allRejected) {
            return "REJECTED";
        }

        if (allCompleted) {
            return "REFUNDED";
        }

        return "PARTIAL";
    }

    private String getReturnProcessStatusText(String processStatus) {
        if (processStatus == null) {
            return "Không xác định";
        }

        return switch (processStatus) {
            case "PENDING" -> "Chờ xử lý";
            case "ACCEPTED" -> "Đã chấp nhận / Chờ hoàn tiền";
            case "REJECTED" -> "Từ chối hoàn hàng";
            case "REFUNDED" -> "Đã xử lý hoàn tiền";
            case "PARTIAL" -> "Đang xử lý một phần";
            default -> "Không xác định";
        };
    }

    private String resolveReturnRejectReason(List<ReturnRequestItem> returnItems) {
        if (returnItems == null || returnItems.isEmpty()) {
            return null;
        }

        return returnItems.stream()
                .map(ReturnRequestItem::getRejectReason)
                .filter(reason -> reason != null && !reason.trim().isEmpty())
                .map(String::trim)
                .findFirst()
                .orElse(null);
    }

    private List<String> parseMediaString(String mediaStr) {
        if (mediaStr == null || mediaStr.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String[] parts = mediaStr.split(",");
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            String cleaned = p.trim();
            if (!cleaned.isEmpty()) {
                list.add(cleaned);
            }
        }
        return list;
    }

    private AdminOrderItemResponse mapOrderItemToResponse(OrderItem item) {
        AdminOrderItemResponse response = new AdminOrderItemResponse();

        response.setOrderItemId(item.getId());
        response.setQuantity(item.getQuantity());
        response.setOriginalPrice(defaultMoney(item.getOriginalPrice()));
        response.setDiscountAmount(defaultMoney(item.getDiscountAmount()));
        response.setFinalPrice(defaultMoney(item.getFinalPrice()));
        response.setNote(item.getNote());

        BigDecimal lineTotal = defaultMoney(item.getFinalPrice())
                .multiply(BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity()));

        response.setLineTotal(lineTotal);

        ProductVariant variant = item.getProductVariant();

        if (variant == null) {
            response.setImageUrl(cleanImageUrl(item.getImage()));
            return response;
        }

        response.setProductVariantId(variant.getId());
        response.setSku(variant.getSku());

        if (variant.getProduct() != null) {
            response.setProductName(variant.getProduct().getName());
        }

        if (variant.getCapacity() != null && variant.getCapacity().getValue() != null) {
            response.setCapacity(formatCapacity(variant.getCapacity().getValue()));
        }

        if (variant.getBottleType() != null) {
            response.setBottleType(variant.getBottleType().getName());
        }

        response.setImageUrl(resolveOrderItemImage(item, variant));

        return response;
    }

    private String resolveOrderItemImage(OrderItem item, ProductVariant variant) {
        String savedImage = cleanImageUrl(item == null ? null : item.getImage());

        if (savedImage != null) {
            return savedImage;
        }

        return resolveProductImageByVariant(variant);
    }

    private String resolveProductImageByVariant(ProductVariant variant) {
        if (variant == null || variant.getProduct() == null || variant.getProduct().getId() == null) {
            return null;
        }

        try {
            return entityManager.createQuery(
                            """
                                    SELECT img.imageUrl
                                    FROM ProductImage img
                                    WHERE img.product.id = :productId
                                      AND img.imageUrl IS NOT NULL
                                      AND img.imageUrl <> ''
                                    ORDER BY img.id ASC
                                    """,
                            String.class
                    )
                    .setParameter("productId", variant.getProduct().getId())
                    .setMaxResults(1)
                    .getResultStream()
                    .map(this::cleanImageUrl)
                    .filter(value -> value != null)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String cleanImageUrl(String imageUrl) {
        if (imageUrl == null) {
            return null;
        }

        String cleanValue = imageUrl.trim();

        return cleanValue.isEmpty() ? null : cleanValue;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String cleanValue = value
                .trim()
                .replaceAll("[\r\n\t]+", " ")
                .replaceAll("\\s{2,}", " ");

        return cleanValue.isEmpty() ? null : cleanValue;
    }

    private Order findOrderOrThrow(Integer orderId) {
        if (orderId == null || orderId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Mã đơn hàng không hợp lệ"
            );
        }

        return orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng"
                ));
    }

    private Integer normalizeSearchStatus(Integer status) {
        if (status == null) {
            return null;
        }

        if (!VALID_ORDER_STATUSES.contains(status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Trạng thái tìm kiếm không hợp lệ"
            );
        }

        return status;
    }

    private String normalizeOrderType(String orderType) {
        if (orderType == null || orderType.trim().isEmpty()) {
            return null;
        }

        String normalized = orderType.trim().toUpperCase(Locale.ROOT);

        if ("ALL".equals(normalized) || "TAT_CA".equals(normalized)) {
            return null;
        }

        if ("INSTORE".equals(normalized) || "IN-STORE".equals(normalized) || "OFFLINE".equals(normalized)) {
            normalized = "IN_STORE";
        }

        if (!SUPPORTED_ORDER_TYPES.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Loại đơn hàng không hợp lệ. Chỉ hỗ trợ ONLINE, IN_STORE hoặc POS"
            );
        }

        return normalized;
    }

    private String normalizePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            return null;
        }

        String normalized = paymentMethod.trim().toUpperCase(Locale.ROOT);

        if ("ALL".equals(normalized) || "TAT_CA".equals(normalized)) {
            return null;
        }

        return normalized;
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ngày bắt đầu không được sau ngày kết thúc"
            );
        }
    }

    private void validateAmountRange(BigDecimal minAmount, BigDecimal maxAmount) {
        if (minAmount != null && minAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số tiền tối thiểu không hợp lệ"
            );
        }

        if (maxAmount != null && maxAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số tiền tối đa không hợp lệ"
            );
        }

        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Số tiền tối thiểu không được lớn hơn số tiền tối đa"
            );
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        String trimmed = keyword.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        String upper = trimmed.toUpperCase(Locale.ROOT);

        if (upper.matches("^DH0*\\d+$")) {
            String numberPart = upper.replaceFirst("^DH0*", "");

            return numberPart.isEmpty() ? trimmed : numberPart;
        }

        if (trimmed.matches("^#0*\\d+$")) {
            String numberPart = trimmed.replaceFirst("^#0*", "");

            return numberPart.isEmpty() ? trimmed : numberPart;
        }

        return trimmed;
    }

    private KeywordDateRange resolveKeywordDateRange(String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);

        if (normalizedKeyword == null) {
            return new KeywordDateRange(null, null, null);
        }

        String value = normalizedKeyword.trim();

        DateTimeFormatter dateTimeFormatter =
                DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

        DateTimeFormatter dateFormatterSlash =
                DateTimeFormatter.ofPattern("dd/MM/yyyy");

        DateTimeFormatter dateFormatterDash =
                DateTimeFormatter.ofPattern("dd-MM-yyyy");

        try {
            LocalDateTime dateTime = LocalDateTime.parse(value, dateTimeFormatter);

            return new KeywordDateRange(
                    null,
                    dateTime,
                    dateTime.plusSeconds(1)
            );
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalDate date = LocalDate.parse(value, dateFormatterSlash);

            return new KeywordDateRange(
                    null,
                    date.atStartOfDay(),
                    date.plusDays(1).atStartOfDay()
            );
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalDate date = LocalDate.parse(value, dateFormatterDash);

            return new KeywordDateRange(
                    null,
                    date.atStartOfDay(),
                    date.plusDays(1).atStartOfDay()
            );
        } catch (DateTimeParseException ignored) {
        }

        return new KeywordDateRange(normalizedKeyword, null, null);
    }

    private Integer safeStatus(Order order) {
        return order.getStatus() == null ? STATUS_PENDING : order.getStatus();
    }

    private String getStatusText(Integer status) {
        if (status == null) {
            return "Chờ xác nhận";
        }

        return switch (status) {
            case STATUS_PENDING -> "Chờ xác nhận";
            case STATUS_CONFIRMED -> "Đã xác nhận / Đang chuẩn bị hàng";
            case STATUS_SHIPPING -> "Đang giao hàng";
            case STATUS_COMPLETED -> "Hoàn thành";
            case STATUS_CANCELLED -> "Đã hủy";
            case STATUS_DELIVERY_FAILED -> "Giao hàng thất bại";
            case STATUS_RETURN_REQUESTED -> "Yêu cầu hoàn hàng / đổi trả";
            case STATUS_RETURN_COMPLETED -> "Hoàn hàng / đổi trả hoàn tất";
            case STATUS_AWAITING_REFUND -> "Đã hủy / Chờ hoàn tiền";
            default -> "Không xác định";
        };
    }

    private String formatOrderCode(Integer orderId) {
        if (orderId == null) {
            return "DH000000";
        }

        return "DH" + String.format("%06d", orderId);
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String formatCapacity(Double value) {
        if (value == null) {
            return "-";
        }

        if (value % 1 == 0) {
            return value.intValue() + "ml";
        }

        return value + "ml";
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderStatusCountResponse getStatusCounts(
            String keyword,
            String orderType,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        validateDateRange(fromDate, toDate);

        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedOrderType = normalizeOrderType(orderType);

        var fromDateTime = fromDate == null ? null : fromDate.atStartOfDay();
        var toDateTime = toDate == null ? null : toDate.plusDays(1).atStartOfDay();

        return new AdminOrderStatusCountResponse(
                countOrderByStatus(normalizedKeyword, null, normalizedOrderType, fromDateTime, toDateTime),
                countOrderByStatus(normalizedKeyword, STATUS_PENDING, normalizedOrderType, fromDateTime, toDateTime),
                countOrderByStatus(normalizedKeyword, STATUS_CONFIRMED, normalizedOrderType, fromDateTime, toDateTime),
                countOrderByStatus(normalizedKeyword, STATUS_SHIPPING, normalizedOrderType, fromDateTime, toDateTime),
                countOrderByStatus(normalizedKeyword, STATUS_COMPLETED, normalizedOrderType, fromDateTime, toDateTime),
                countOrderByStatus(normalizedKeyword, STATUS_CANCELLED, normalizedOrderType, fromDateTime, toDateTime)
                        + countOrderByStatus(normalizedKeyword, STATUS_AWAITING_REFUND, normalizedOrderType, fromDateTime, toDateTime),
                countOrderByStatus(normalizedKeyword, STATUS_DELIVERY_FAILED, normalizedOrderType, fromDateTime, toDateTime),
                countOrderByStatus(normalizedKeyword, STATUS_RETURN_REQUESTED, normalizedOrderType, fromDateTime, toDateTime),
                countOrderByStatus(normalizedKeyword, STATUS_RETURN_COMPLETED, normalizedOrderType, fromDateTime, toDateTime)
        );
    }

    private long countOrderByStatus(
            String keyword,
            Integer status,
            String orderType,
            java.time.LocalDateTime fromDateTime,
            java.time.LocalDateTime toDateTime
    ) {
        Long count = orderRepository.countAdminOrdersForTabs(
                keyword,
                status,
                orderType,
                fromDateTime,
                toDateTime
        );

        return count == null ? 0L : count;
    }
}