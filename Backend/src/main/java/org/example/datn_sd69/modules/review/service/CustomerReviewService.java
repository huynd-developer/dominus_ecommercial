package org.example.datn_sd69.modules.review.service;


import org.example.datn_sd69.modules.review.dto.request.CreateReviewRequest;
import org.example.datn_sd69.modules.review.dto.response.ReviewResponse;
import org.example.datn_sd69.modules.review.dto.response.ReviewableOrderItemResponse;

import java.util.List;

public interface CustomerReviewService {

    ReviewResponse createReview(CreateReviewRequest request);

    List<ReviewResponse> getMyReviews();

    List<ReviewableOrderItemResponse> getReviewableItemsByOrder(Integer orderId);
}
