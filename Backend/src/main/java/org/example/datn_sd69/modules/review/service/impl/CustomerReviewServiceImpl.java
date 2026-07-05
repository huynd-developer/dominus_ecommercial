package org.example.datn_sd69.modules.review.service.impl;


import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Brand;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.entity.Product;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.entity.Review;
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
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CustomerReviewServiceImpl implements CustomerReviewService {

    private static final int ORDER_STATUS_COMPLETED = 3;
    private static final int MAX_COMMENT_LENGTH = 1000;

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

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

        if (!Objects.equals(order.getStatus(), ORDER_STATUS_COMPLETED)) {
            throw badRequest("Chỉ được đánh giá khi đơn hàng đã hoàn thành");
        }

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

        Review review = new Review();
        review.setUser(currentUser);
        review.setOrderItem(orderItem);
        review.setRating(request.rating());
        review.setComment(normalizeComment(request.comment()));
        review.setCreatedAt(LocalDateTime.now());
        review.setIsDeleted(false);

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

    private ReviewResponse mapToReviewResponse(Review review) {
        OrderItem orderItem = review.getOrderItem();
        Order order = orderItem != null ? orderItem.getOrder() : null;
        ProductVariant variant = orderItem != null ? orderItem.getProductVariant() : null;
        Product product = variant != null ? variant.getProduct() : null;
        Brand brand = product != null ? product.getBrand() : null;

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
                review.getCreatedAt()
        );
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
        boolean canReview = completed && hasProduct && !reviewed;

        String message;

        if (!completed) {
            message = "Chỉ được đánh giá khi đơn hàng đã hoàn thành";
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
