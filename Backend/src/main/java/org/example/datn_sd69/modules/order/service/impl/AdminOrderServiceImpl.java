package org.example.datn_sd69.modules.order.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.persistence.EntityManager;
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
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
            "Sai địa chỉ giao hàng",
            "Khách hẹn giao lại",
            "Đơn hàng bị thất lạc",
            "Hàng bị hư hỏng khi giao",
            "Khu vực giao hàng không hỗ trợ",
            "Khác"
    );

    private static final Set<String> DELIVERY_FAILED_REQUIRES_EVIDENCE_REASONS = Set.of(
            "Không liên hệ được khách hàng",
            "Khách từ chối nhận hàng",
            "Sai địa chỉ giao hàng",
            "Hàng bị hư hỏng khi giao",
            "Khác"
    );

    private static final Set<String> ALLOWED_DELIVERY_IMAGE_EXTENSIONS = Set.of(
            ".jpg",
            ".jpeg",
            ".png",
            ".webp"
    );

    private static final Set<String> ALLOWED_DELIVERY_VIDEO_EXTENSIONS = Set.of(
            ".mp4",
            ".mov",
            ".webm"
    );

    private static final long MAX_DELIVERY_IMAGE_SIZE = 20L * 1024L * 1024L;
    private static final long MAX_DELIVERY_VIDEO_SIZE = 200L * 1024L * 1024L;

    private static final String DELIVERY_EVIDENCE_CLOUDINARY_FOLDER = "order-delivery-evidence";

    private static final Set<Integer> VALID_ORDER_STATUSES = Set.of(
            STATUS_PENDING,
            STATUS_CONFIRMED,
            STATUS_SHIPPING,
            STATUS_COMPLETED,
            STATUS_CANCELLED,
            STATUS_DELIVERY_FAILED,
            STATUS_RETURN_REQUESTED,
            STATUS_RETURN_COMPLETED
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
    private final Cloudinary cloudinary;
    private final EntityManager entityManager;
    private record KeywordDateRange(
            String keyword,
            LocalDateTime fromDateTime,
            LocalDateTime toDateTime
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

        Order savedOrder = orderRepository.save(order);

        saveDeliveryEvidenceFiles(savedOrder, files, DELIVERY_EVIDENCE_TYPE_FAILED);

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

        restoreStockWhenAdminCancel(order);

        order.setStatus(STATUS_CANCELLED);
        order.setCancelReason(cancelReason);
        order.setCancelledAt(LocalDateTime.now());

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

        return mapOrderToResponse(orderRepository.save(order), true);
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

        /*
         * Khi từ chối hoàn hàng, đơn không còn ở luồng hoàn tiền nữa.
         * Đơn quay lại Hoàn thành, còn lý do từ chối lưu trong ReturnRequestItem
         * để khách/admin vẫn xem được lịch sử xử lý.
         */
        order.setStatus(STATUS_COMPLETED);
        Order savedOrder = orderRepository.save(order);

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

        returnItems.forEach(item -> item.setStatus(RETURN_ITEM_STATUS_COMPLETED));
        returnRequestItemRepository.saveAll(returnItems);

        order.setStatus(STATUS_RETURN_COMPLETED);
        Order savedOrder = orderRepository.save(order);

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

    private void restoreStockWhenAdminCancel(Order order) {
        if (order == null || order.getId() == null) {
            return;
        }

        List<OrderItem> orderItems = orderItemRepository.findDetailByOrderId(order.getId());

        if (orderItems == null || orderItems.isEmpty()) {
            return;
        }

        for (OrderItem item : orderItems) {
            if (item == null || item.getProductVariant() == null) {
                continue;
            }

            ProductVariant variant = item.getProductVariant();
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();

            if (quantity <= 0) {
                continue;
            }

            int currentStock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
            variant.setStockQuantity(currentStock + quantity);
        }
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
                    "Vui lòng tải lên ảnh/video minh chứng giao hàng"
            );
        }

        for (MultipartFile file : files) {
            validateSingleDeliveryEvidenceFile(file);
        }
    }

    private void validateSingleDeliveryEvidenceFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }

        Integer mediaType = resolveDeliveryMediaType(file);
        long maxSize = Integer.valueOf(MEDIA_TYPE_VIDEO).equals(mediaType)
                ? MAX_DELIVERY_VIDEO_SIZE
                : MAX_DELIVERY_IMAGE_SIZE;

        if (file.getSize() > maxSize) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    Integer.valueOf(MEDIA_TYPE_VIDEO).equals(mediaType)
                            ? "Video minh chứng không được vượt quá 200MB"
                            : "Ảnh minh chứng không được vượt quá 20MB"
            );
        }
    }

    private Integer resolveDeliveryMediaType(MultipartFile file) {
        String filename = file == null || file.getOriginalFilename() == null
                ? ""
                : file.getOriginalFilename().trim();

        String extension = getFileExtension(filename);
        String contentType = file == null || file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);

        if (contentType.startsWith("image/") || ALLOWED_DELIVERY_IMAGE_EXTENSIONS.contains(extension)) {
            if (!ALLOWED_DELIVERY_IMAGE_EXTENSIONS.contains(extension)
                    && !".jpg".equals(extension)
                    && !".jpeg".equals(extension)
                    && !".png".equals(extension)
                    && !".webp".equals(extension)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Ảnh minh chứng chỉ hỗ trợ JPG, JPEG, PNG, WEBP"
                );
            }

            return MEDIA_TYPE_IMAGE;
        }

        if (contentType.startsWith("video/") || ALLOWED_DELIVERY_VIDEO_EXTENSIONS.contains(extension)) {
            if (!ALLOWED_DELIVERY_VIDEO_EXTENSIONS.contains(extension)
                    && !".mp4".equals(extension)
                    && !".mov".equals(extension)
                    && !".webm".equals(extension)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Video minh chứng chỉ hỗ trợ MP4, MOV, WEBM"
                );
            }

            return MEDIA_TYPE_VIDEO;
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "File minh chứng chỉ hỗ trợ ảnh JPG/JPEG/PNG/WEBP hoặc video MP4/MOV/WEBM"
        );
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
            Integer mediaType = resolveDeliveryMediaType(file);
            String mediaUrl = uploadDeliveryEvidenceFile(file);

            OrderDeliveryEvidence evidence = new OrderDeliveryEvidence();
            evidence.setOrder(order);
            evidence.setEvidenceType(evidenceType);
            evidence.setMediaType(mediaType);
            evidence.setMediaUrl(mediaUrl);
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
                            "auto"
                    )
            );

            Object secureUrl = uploadResult.get("secure_url");

            if (secureUrl == null || secureUrl.toString().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Không lấy được đường dẫn file minh chứng sau khi tải lên"
                );
            }

            return secureUrl.toString();
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không thể tải file minh chứng giao hàng"
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
                .map(OrderDeliveryEvidence::getMediaUrl)
                .filter(url -> url != null && !url.trim().isEmpty())
                .toList();
    }

    private String getCurrentAdminDisplayName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return "Nhân viên cửa hàng";
        }

        String name = normalizeOptionalText(authentication.getName());

        return name == null ? "Nhân viên cửa hàng" : name;
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
        response.setDeliveryCompletedByName(normalizeOptionalText(order.getDeliveryCompletedByName()));
        response.setDeliveryFailedReason(normalizeOptionalText(order.getDeliveryFailedReason()));
        response.setDeliveryFailedDescription(normalizeOptionalText(order.getDeliveryFailedDescription()));
        response.setDeliveryFailedAt(order.getDeliveryFailedAt());
        response.setDeliveryFailedByName(normalizeOptionalText(order.getDeliveryFailedByName()));
        response.setDeliverySuccessMediaUrls(getDeliveryEvidenceUrls(order, DELIVERY_EVIDENCE_TYPE_SUCCESS));
        response.setDeliveryFailedMediaUrls(getDeliveryEvidenceUrls(order, DELIVERY_EVIDENCE_TYPE_FAILED));
        response.setCancelReason(normalizeOptionalText(order.getCancelReason()));
        response.setCancelledAt(order.getCancelledAt());

        // BỔ SUNG TRƯỜNG NÀY ĐỂ TRẢ VỀ CHO VUE
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
            /*
             * ReturnRequestItem chỉ giữ OrderItem. Với dữ liệu cũ hoặc khi OrderItem
             * bị lazy/proxy thiếu ProductVariant, cần lấy lại danh sách OrderItem detail
             * của đơn để map đúng tên sản phẩm, SKU, dung tích, loại chai và ảnh.
             */
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

            /*
             * Ưu tiên bản detail vì repository findDetailByOrderId thường fetch sẵn
             * ProductVariant/Product/Capacity/BottleType để tránh return item bị mất
             * tên sản phẩm, SKU, dung tích, loại chai.
             */
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

    // --- HÀM HỖ TRỢ PARSE CHUỖI ẢNH/VIDEO THÀNH LIST ---
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
            System.out.println("=== LỖI QUERY ẢNH CHI TIẾT ĐƠN HÀNG: " + e.getMessage());
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
                countOrderByStatus(normalizedKeyword, STATUS_CANCELLED, normalizedOrderType, fromDateTime, toDateTime),
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