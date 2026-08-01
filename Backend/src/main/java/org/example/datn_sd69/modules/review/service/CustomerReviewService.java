package org.example.datn_sd69.modules.review.service;

import org.example.datn_sd69.modules.review.dto.request.CreateReviewRequest;
import org.example.datn_sd69.modules.review.dto.response.ReviewResponse;
import org.example.datn_sd69.modules.review.dto.response.ReviewableOrderItemResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CustomerReviewService {

    ReviewResponse createReview(CreateReviewRequest request);

    ReviewResponse updateReview(
            Integer reviewId,
            Integer rating,
            String comment,
            List<MultipartFile> mediaFiles,
            List<Integer> deletedMediaIds
    );

    List<ReviewResponse> getMyReviews();

    List<ReviewableOrderItemResponse> getReviewableItemsByOrder(Integer orderId);
}