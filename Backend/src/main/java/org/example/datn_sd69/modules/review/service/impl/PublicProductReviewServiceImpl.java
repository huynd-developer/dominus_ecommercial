package org.example.datn_sd69.modules.review.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Brand;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.entity.Product;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.entity.Review;
import org.example.datn_sd69.entity.User;
import org.example.datn_sd69.modules.review.dto.response.ProductReviewSummaryResponse;
import org.example.datn_sd69.modules.review.dto.response.PublicProductReviewResponse;
import org.example.datn_sd69.modules.review.service.PublicProductReviewService;
import org.example.datn_sd69.repository.ProductRepository;
import org.example.datn_sd69.repository.ReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PublicProductReviewServiceImpl implements PublicProductReviewService {

    private static final Integer COMPLETED_ORDER_STATUS = 3;

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PublicProductReviewResponse> getReviewsByProduct(Integer productId) {
        validateProduct(productId);

        return getValidPublicReviewsByProductId(productId)
                .stream()
                .map(this::mapToPublicResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductReviewSummaryResponse getReviewSummary(Integer productId) {
        validateProduct(productId);

        List<Review> reviews = getValidPublicReviewsByProductId(productId);

        long total = reviews.size();

        double average = reviews.stream()
                .map(Review::getRating)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        double roundedAverage = BigDecimal.valueOf(average)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();

        return new ProductReviewSummaryResponse(
                productId,
                roundedAverage,
                total,
                countByRating(reviews, 5),
                countByRating(reviews, 4),
                countByRating(reviews, 3),
                countByRating(reviews, 2),
                countByRating(reviews, 1)
        );
    }

    private List<Review> getValidPublicReviewsByProductId(Integer productId) {
        return reviewRepository.findByIsDeletedFalseOrderByCreatedAtDesc()
                .stream()
                .filter(review -> isValidPublicReviewOfProduct(review, productId))
                .toList();
    }

    private boolean isValidPublicReviewOfProduct(Review review, Integer productId) {
        if (review == null) {
            return false;
        }

        if (Boolean.TRUE.equals(review.getIsDeleted())) {
            return false;
        }

        OrderItem orderItem = review.getOrderItem();

        if (orderItem == null) {
            return false;
        }

        Order order = orderItem.getOrder();

        if (order == null || order.getStatus() == null) {
            return false;
        }

        if (!Objects.equals(order.getStatus(), COMPLETED_ORDER_STATUS)) {
            return false;
        }

        ProductVariant variant = orderItem.getProductVariant();

        if (variant == null) {
            return false;
        }

        Product product = variant.getProduct();

        if (product == null || product.getId() == null) {
            return false;
        }

        return Objects.equals(product.getId(), productId);
    }

    private long countByRating(List<Review> reviews, Integer rating) {
        return reviews.stream()
                .filter(review -> Objects.equals(review.getRating(), rating))
                .count();
    }

    private PublicProductReviewResponse mapToPublicResponse(Review review) {
        OrderItem orderItem = review.getOrderItem();
        ProductVariant variant = orderItem != null ? orderItem.getProductVariant() : null;
        Product product = variant != null ? variant.getProduct() : null;
        Brand brand = product != null ? product.getBrand() : null;
        User user = review.getUser();

        return new PublicProductReviewResponse(
                review.getId(),
                product != null ? product.getId() : null,
                variant != null ? variant.getId() : null,
                product != null ? product.getName() : "Sản phẩm không xác định",
                brand != null ? brand.getName() : null,
                variant != null ? variant.getSku() : null,

                maskCustomerName(user != null ? user.getName() : null),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }

    private String maskCustomerName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Khách hàng";
        }

        String trimmed = name.trim();

        if (trimmed.length() <= 1) {
            return trimmed + "*";
        }

        if (trimmed.length() <= 2) {
            return trimmed.charAt(0) + "*";
        }

        return trimmed.charAt(0) + "***" + trimmed.substring(trimmed.length() - 1);
    }

    private void validateProduct(Integer productId) {
        if (productId == null || productId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "productId phải là số nguyên dương"
            );
        }

        if (!productRepository.existsById(productId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy sản phẩm"
            );
        }
    }
}