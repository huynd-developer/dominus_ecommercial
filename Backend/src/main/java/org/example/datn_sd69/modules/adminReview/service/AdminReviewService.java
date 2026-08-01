package org.example.datn_sd69.modules.adminReview.service;

import org.example.datn_sd69.modules.adminReview.dto.response.AdminReviewResponse;
import org.springframework.data.domain.Page;

public interface AdminReviewService {
    Page<AdminReviewResponse> getFilteredReviews(Integer status, Integer rating, Boolean hasMedia, String keyword, int page, int size);

    void approveReview(Integer id);
    void rejectReview(Integer id, String reason);
    void hideReview(Integer id);
}
