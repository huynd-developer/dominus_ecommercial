package org.example.datn_sd69.modules.review.service;

import org.example.datn_sd69.modules.review.dto.response.ProductReviewSummaryResponse;
import org.example.datn_sd69.modules.review.dto.response.PublicProductReviewResponse;

import java.util.List;

public interface PublicProductReviewService {

    List<PublicProductReviewResponse> getReviewsByProduct(Integer productId);

    ProductReviewSummaryResponse getReviewSummary(Integer productId);
}