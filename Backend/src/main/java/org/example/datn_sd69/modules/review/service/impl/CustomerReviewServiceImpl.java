package org.example.datn_sd69.modules.review.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Brand;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.entity.Product;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.entity.Review;
import org.example.datn_sd69.entity.ReviewMedia;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.modules.review.dto.request.CreateReviewRequest;
import org.example.datn_sd69.modules.review.dto.response.ReviewResponse;
import org.example.datn_sd69.modules.review.dto.response.ReviewableOrderItemResponse;
import org.example.datn_sd69.modules.review.service.CustomerReviewService;
import org.example.datn_sd69.repository.OrderItemRepository;
import org.example.datn_sd69.repository.OrderRepository;
import org.example.datn_sd69.repository.ReviewRepository;
import org.example.datn_sd69.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CustomerReviewServiceImpl implements CustomerReviewService {

    private static final int ORDER_STATUS_COMPLETED = 3;
    private static final int MAX_COMMENT_LENGTH = 1000;
    private static final int REVIEW_CREATE_DEADLINE_DAYS = 15;
    private static final int REVIEW_EDIT_DEADLINE_DAYS = 30;
    private static final int MAX_REVIEW_EDIT_COUNT = 1;

    private static final int REVIEW_APPROVAL_PENDING = 0;
    private static final int REVIEW_APPROVAL_APPROVED = 1;
    private static final int REVIEW_APPROVAL_REJECTED = 2;
    private static final int REVIEW_APPROVAL_HIDDEN = 3;

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final Cloudinary cloudinary;

    @Override
    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request) {
        User currentUser = getCurrentUser();

        validateCreateRequest(request);

        OrderItem orderItem = orderItemRepository.findById(request.orderItemId())
                .orElseThrow(() -> badRequest("Không tìm thấy sản phẩm trong đơn hàng"));

        Order order = orderItem.getOrder();

        if (order == null) {
            throw badRequest("Dữ liệu đơn hàng không hợp lệ");
        }

        validateOrderBelongsToCurrentUser(order, currentUser.getId());

        LocalDateTime now = LocalDateTime.now();

        if (!Objects.equals(order.getStatus(), ORDER_STATUS_COMPLETED)) {
            throw badRequest("Chỉ được đánh giá khi đơn hàng đã hoàn thành");
        }

        validateReviewCreateDeadline(order, now);

        if (orderItem.getProductVariant() == null) {
            throw badRequest("Sản phẩm không còn tồn tại nên không thể đánh giá");
        }

        boolean reviewed = reviewRepository.existsByUser_IdAndOrderItem_IdAndIsDeletedFalse(
                currentUser.getId(),
                orderItem.getId()
        );

        if (reviewed) {
            throw badRequest("Bạn đã đánh giá sản phẩm này trong đơn hàng");
        }

        List<MultipartFile> validMediaFiles = getValidMediaFiles(request.mediaFiles());
        boolean hasMedia = !validMediaFiles.isEmpty();

        Review review = new Review();
        review.setUser(currentUser);
        review.setOrderItem(orderItem);
        review.setRating(request.rating());
        review.setComment(normalizeComment(request.comment()));
        review.setCreatedAt(now);
        review.setIsDeleted(false);

        if (hasMedia) {
            review.setApprovalStatus(REVIEW_APPROVAL_PENDING);
            review.setApprovedAt(null);
        } else {
            review.setApprovalStatus(REVIEW_APPROVAL_APPROVED);
            review.setApprovedAt(now);
        }

        review.setRejectedAt(null);
        review.setRejectedReason(null);

        uploadReviewMediaFiles(review, validMediaFiles);

        Review savedReview = reviewRepository.save(review);

        return mapToReviewResponse(savedReview);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(
            Integer reviewId,
            Integer rating,
            String comment,
            List<MultipartFile> mediaFiles
    ) {
        User currentUser = getCurrentUser();

        if (reviewId == null || reviewId <= 0) {
            throw badRequest("reviewId phải là số nguyên dương");
        }

        validateReviewUpdatePayload(rating, comment);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đánh giá"
                ));

        if (Boolean.TRUE.equals(review.getIsDeleted())) {
            throw badRequest("Đánh giá đã bị xóa, không thể chỉnh sửa");
        }

        if (review.getUser() == null || !Objects.equals(review.getUser().getId(), currentUser.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền chỉnh sửa đánh giá này"
            );
        }

        validateReviewEditDeadline(review, LocalDateTime.now());
        validateReviewEditCount(review);

        List<MultipartFile> validMediaFiles = getValidMediaFiles(mediaFiles);
        boolean hasNewMedia = !validMediaFiles.isEmpty();
        LocalDateTime now = LocalDateTime.now();

        review.setRating(rating);
        review.setComment(normalizeComment(comment));
        review.setEditedAt(now);
        review.setEditCount((review.getEditCount() == null ? 0 : review.getEditCount()) + 1);

        if (hasNewMedia) {
            uploadReviewMediaFiles(review, validMediaFiles);
            review.setApprovalStatus(REVIEW_APPROVAL_PENDING);
            review.setApprovedAt(null);
            review.setRejectedAt(null);
            review.setRejectedReason(null);
        } else if ((review.getReviewMedias() == null || review.getReviewMedias().isEmpty())
                && !Objects.equals(review.getApprovalStatus(), REVIEW_APPROVAL_HIDDEN)) {
            review.setApprovalStatus(REVIEW_APPROVAL_APPROVED);
            review.setApprovedAt(now);
            review.setRejectedAt(null);
            review.setRejectedReason(null);
        }

        Review savedReview = reviewRepository.save(review);

        return mapToReviewResponse(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews() {
        User currentUser = getCurrentUser();

        return reviewRepository
                .findByUser_IdAndIsDeletedFalseOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::mapToReviewResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewableOrderItemResponse> getReviewableItemsByOrder(Integer orderId) {
        User currentUser = getCurrentUser();

        if (orderId == null || orderId <= 0) {
            throw badRequest("orderId phải là số nguyên dương");
        }

        Order order = orderRepository.findByIdAndCustomer_UserId(orderId, currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng của bạn"
                ));

        List<OrderItem> orderItems = orderItemRepository.findByOrder_IdOrderByIdAsc(order.getId());

        return orderItems
                .stream()
                .map(orderItem -> mapToReviewableOrderItemResponse(order, orderItem, currentUser.getId()))
                .toList();
    }

    private void validateCreateRequest(CreateReviewRequest request) {
        if (request == null) {
            throw badRequest("Dữ liệu đánh giá không được để trống");
        }

        if (request.orderItemId() == null || request.orderItemId() <= 0) {
            throw badRequest("orderItemId phải là số nguyên dương");
        }

        if (request.rating() == null) {
            throw badRequest("Số sao đánh giá không được để trống");
        }

        if (request.rating() < 1 || request.rating() > 5) {
            throw badRequest("Số sao đánh giá phải từ 1 đến 5");
        }

        if (request.comment() != null && request.comment().length() > MAX_COMMENT_LENGTH) {
            throw badRequest("Bình luận tối đa 1000 ký tự");
        }
    }

    private void validateOrderBelongsToCurrentUser(Order order, Integer currentUserId) {
        if (order.getCustomer() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Đơn hàng này không thuộc tài khoản khách hàng"
            );
        }

        Integer customerId = order.getCustomer().getUserId();

        if (!Objects.equals(customerId, currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn không có quyền đánh giá sản phẩm trong đơn hàng này"
            );
        }
    }

    private String normalizeComment(String comment) {
        if (comment == null) {
            return null;
        }

        String trimmed = comment.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        if (trimmed.length() > MAX_COMMENT_LENGTH) {
            throw badRequest("Bình luận tối đa 1000 ký tự");
        }

        return trimmed;
    }

    private List<MultipartFile> getValidMediaFiles(List<MultipartFile> mediaFiles) {
        if (mediaFiles == null || mediaFiles.isEmpty()) {
            return List.of();
        }

        return mediaFiles.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
    }

    private void uploadReviewMediaFiles(Review review, List<MultipartFile> mediaFiles) {
        if (review == null || mediaFiles == null || mediaFiles.isEmpty()) {
            return;
        }

        for (MultipartFile file : mediaFiles) {
            try {
                Map<?, ?> uploadResult = cloudinary.uploader().upload(
                        file.getBytes(),
                        ObjectUtils.asMap("resource_type", "auto")
                );

                Object secureUrl = uploadResult.get("secure_url");

                if (secureUrl == null || String.valueOf(secureUrl).trim().isEmpty()) {
                    throw new IllegalStateException("Cloudinary không trả về secure_url");
                }

                ReviewMedia media = new ReviewMedia();
                media.setReview(review);
                media.setMediaUrl(String.valueOf(secureUrl));
                media.setMediaType(resolveMediaType(file));

                review.getReviewMedias().add(media);
            } catch (Exception e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Lỗi upload ảnh/video đánh giá: " + e.getMessage(),
                        e
                );
            }
        }
    }

    private String resolveMediaType(MultipartFile file) {
        String contentType = file == null ? null : file.getContentType();

        if (contentType != null && contentType.toLowerCase().startsWith("video/")) {
            return "video";
        }

        return "image";
    }

    private ReviewResponse mapToReviewResponse(Review review) {
        OrderItem orderItem = review.getOrderItem();
        Order order = orderItem != null ? orderItem.getOrder() : null;
        ProductVariant variant = orderItem != null ? orderItem.getProductVariant() : null;
        Product product = variant != null ? variant.getProduct() : null;
        Brand brand = product != null ? product.getBrand() : null;

        List<String> mediaUrls = new ArrayList<>();
        if (review.getReviewMedias() != null && !review.getReviewMedias().isEmpty()) {
            mediaUrls = review.getReviewMedias().stream()
                    .map(ReviewMedia::getMediaUrl)
                    .filter(Objects::nonNull)
                    .toList();
        }

        return new ReviewResponse(
                review.getId(),
                orderItem != null ? orderItem.getId() : null,
                order != null ? order.getId() : null,

                variant != null ? variant.getId() : null,
                product != null ? product.getId() : null,
                product != null ? product.getName() : "Sản phẩm không xác định",
                brand != null ? brand.getName() : null,
                variant != null ? variant.getSku() : null,
                orderItem != null ? orderItem.getImage() : null,

                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getApprovalStatus(),
                getApprovalStatusText(review.getApprovalStatus()),
                review.getApprovedAt(),
                review.getRejectedAt(),
                review.getRejectedReason(),
                mediaUrls
        );
    }

    private String getApprovalStatusText(Integer status) {
        if (status == null) {
            return "Đã hiển thị";
        }

        return switch (status) {
            case REVIEW_APPROVAL_PENDING -> "Đang chờ duyệt ảnh/video";
            case REVIEW_APPROVAL_APPROVED -> "Đã hiển thị";
            case REVIEW_APPROVAL_REJECTED -> "Đánh giá không được duyệt";
            case REVIEW_APPROVAL_HIDDEN -> "Đánh giá đã bị ẩn";
            default -> "Không xác định";
        };
    }

    private ReviewableOrderItemResponse mapToReviewableOrderItemResponse(
            Order order,
            OrderItem orderItem,
            Integer currentUserId
    ) {
        ProductVariant variant = orderItem.getProductVariant();
        Product product = variant != null ? variant.getProduct() : null;
        Brand brand = product != null ? product.getBrand() : null;

        boolean reviewed = reviewRepository.existsByUser_IdAndOrderItem_IdAndIsDeletedFalse(
                currentUserId,
                orderItem.getId()
        );

        boolean completed = Objects.equals(order.getStatus(), ORDER_STATUS_COMPLETED);
        boolean hasProduct = variant != null;
        boolean withinReviewDeadline = isWithinReviewCreateDeadline(order, LocalDateTime.now());
        boolean canReview = completed && hasProduct && withinReviewDeadline && !reviewed;

        String message;

        if (!completed) {
            message = "Chỉ được đánh giá khi đơn hàng đã hoàn thành";
        } else if (!withinReviewDeadline) {
            message = "Đã quá hạn 15 ngày đánh giá sản phẩm";
        } else if (!hasProduct) {
            message = "Sản phẩm không còn tồn tại nên không thể đánh giá";
        } else if (reviewed) {
            message = "Bạn đã đánh giá sản phẩm này";
        } else {
            message = "Có thể đánh giá";
        }

        return new ReviewableOrderItemResponse(
                orderItem.getId(),
                order.getId(),

                variant != null ? variant.getId() : null,
                product != null ? product.getId() : null,
                product != null ? product.getName() : "Sản phẩm không xác định",
                brand != null ? brand.getName() : null,
                variant != null ? variant.getSku() : null,
                orderItem.getImage(),

                order.getStatus(),
                reviewed,
                canReview,
                message
        );
    }

    private void validateReviewUpdatePayload(Integer rating, String comment) {
        if (rating == null) {
            throw badRequest("Số sao đánh giá không được để trống");
        }

        if (rating < 1 || rating > 5) {
            throw badRequest("Số sao đánh giá phải từ 1 đến 5");
        }

        if (comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            throw badRequest("Bình luận tối đa 1000 ký tự");
        }
    }

    private void validateReviewCreateDeadline(Order order, LocalDateTime now) {
        if (!isWithinReviewCreateDeadline(order, now)) {
            throw badRequest("Đã quá hạn 15 ngày đánh giá sản phẩm.");
        }
    }

    private boolean isWithinReviewCreateDeadline(Order order, LocalDateTime now) {
        LocalDateTime completedAt = getOrderCompletedAt(order);

        if (completedAt == null) {
            return false;
        }

        return !now.isAfter(completedAt.plusDays(REVIEW_CREATE_DEADLINE_DAYS));
    }

    private void validateReviewEditDeadline(Review review, LocalDateTime now) {
        LocalDateTime createdAt = review == null ? null : review.getCreatedAt();

        if (createdAt == null) {
            throw badRequest("Đánh giá chưa có thời gian tạo, không thể chỉnh sửa.");
        }

        if (now.isAfter(createdAt.plusDays(REVIEW_EDIT_DEADLINE_DAYS))) {
            throw badRequest("Đã quá hạn 30 ngày chỉnh sửa đánh giá.");
        }
    }

    private void validateReviewEditCount(Review review) {
        Integer editCount = review == null || review.getEditCount() == null
                ? 0
                : review.getEditCount();

        if (editCount >= MAX_REVIEW_EDIT_COUNT) {
            throw badRequest("Bạn chỉ được chỉnh sửa đánh giá 1 lần.");
        }
    }

    private LocalDateTime getOrderCompletedAt(Order order) {
        if (order == null) {
            return null;
        }

        if (order.getCompletedAt() != null) {
            return order.getCompletedAt();
        }

        /*
         * Fallback cho dữ liệu cũ chưa có CompletedAt.
         * Dữ liệu mới vẫn phải set CompletedAt khi đơn chuyển sang Hoàn thành.
         */
        return order.getCreatedAt();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn chưa đăng nhập");
        }

        String email = authentication.getName();

        if (email == null || email.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token không hợp lệ");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Không tìm thấy tài khoản đăng nhập"
                ));
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}