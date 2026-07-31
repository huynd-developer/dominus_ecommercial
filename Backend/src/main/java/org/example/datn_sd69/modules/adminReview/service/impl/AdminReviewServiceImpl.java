package org.example.datn_sd69.modules.adminReview.service.impl;

import jakarta.persistence.criteria.Predicate;
import org.example.datn_sd69.entity.Review;
import org.example.datn_sd69.modules.adminReview.dto.response.AdminReviewMediaResponse;
import org.example.datn_sd69.modules.adminReview.dto.response.AdminReviewResponse;
import org.example.datn_sd69.modules.adminReview.service.AdminReviewService;
import org.example.datn_sd69.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminReviewServiceImpl implements AdminReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Override
    public Page<AdminReviewResponse> getFilteredReviews(Integer status, Integer rating, Boolean hasMedia, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Review> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Luôn lọc các review chưa bị xóa mềm
            predicates.add(cb.equal(root.get("isDeleted"), false));

            if (status != null) {
                predicates.add(cb.equal(root.get("approvalStatus"), status));
            }

            if (rating != null) {
                predicates.add(cb.equal(root.get("rating"), rating));
            }

            if (hasMedia != null) {
                if (hasMedia) {
                    predicates.add(cb.isNotEmpty(root.get("reviewMedias")));
                } else {
                    predicates.add(cb.isEmpty(root.get("reviewMedias")));
                }
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchKey = "%" + keyword.trim().toLowerCase() + "%";

                // ĐÃ SỬA: "fullName" -> "name" và "productDetail" -> "productVariant"
                Predicate searchUser = cb.like(cb.lower(root.get("user").get("name")), searchKey);
                Predicate searchEmail = cb.like(cb.lower(root.get("user").get("email")), searchKey);
                Predicate searchComment = cb.like(cb.lower(root.get("comment")), searchKey);
                Predicate searchProduct = cb.like(cb.lower(root.get("orderItem").get("productVariant").get("product").get("name")), searchKey);
                Predicate searchSku = cb.like(cb.lower(root.get("orderItem").get("productVariant").get("sku")), searchKey);

                predicates.add(cb.or(searchUser, searchEmail, searchComment, searchProduct, searchSku));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return reviewRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void approveReview(Integer id) {
        Review review = reviewRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá!"));

        review.setApprovalStatus(1);
        review.setApprovedAt(LocalDateTime.now());
        review.setRejectedReason(null);
        review.setRejectedAt(null);
        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void rejectReview(Integer id, String reason) {
        Review review = reviewRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá!"));

        review.setApprovalStatus(2);
        review.setRejectedAt(LocalDateTime.now());
        review.setRejectedReason(reason);
        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void hideReview(Integer id) {
        Review review = reviewRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá!"));

        review.setApprovalStatus(3);
        reviewRepository.save(review);
    }

    private AdminReviewResponse mapToResponse(Review review) {
        AdminReviewResponse response = new AdminReviewResponse();
        response.setId(review.getId());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setApprovalStatus(review.getApprovalStatus());
        response.setCreatedAt(review.getCreatedAt());
        response.setApprovedAt(review.getApprovedAt());
        response.setRejectedAt(review.getRejectedAt());
        response.setRejectedReason(review.getRejectedReason());

        if (review.getUser() != null) {
            // ĐÃ SỬA: getFullName() -> getName()
            response.setCustomerName(review.getUser().getName());
            response.setCustomerEmail(review.getUser().getEmail());
        }

        // ĐÃ SỬA: getProductDetail() -> getProductVariant()
        if (review.getOrderItem() != null && review.getOrderItem().getProductVariant() != null) {
            response.setProductName(review.getOrderItem().getProductVariant().getProduct().getName());
            response.setProductSku(review.getOrderItem().getProductVariant().getSku());
        }

        if (review.getReviewMedias() != null) {
            List<AdminReviewMediaResponse> mediaList = review.getReviewMedias().stream()
                    .map(m -> new AdminReviewMediaResponse(m.getId(), m.getMediaUrl(), m.getMediaType()))
                    .collect(Collectors.toList());
            response.setMedia(mediaList);
        }

        return response;
    }
}