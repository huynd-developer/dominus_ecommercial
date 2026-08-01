package org.example.datn_sd69.modules.adminReview.service;

import org.example.datn_sd69.modules.adminReview.dto.response.PublicReviewResponse;
import org.example.datn_sd69.modules.adminReview.dto.request.CreateReviewRequest;
import org.springframework.data.domain.Page;

public interface PublicReviewService {
    Page<PublicReviewResponse> getApprovedReviewsByProduct(Integer productId, int page, int size);

    /**
     * Tạo đánh giá mới - Bắt buộc trạng thái CHỜ DUYỆT (status = 0)
     */
    void createReview(CreateReviewRequest request);
}
