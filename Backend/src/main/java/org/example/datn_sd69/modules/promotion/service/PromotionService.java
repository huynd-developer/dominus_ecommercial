package org.example.datn_sd69.modules.promotion.service;

import org.example.datn_sd69.modules.promotion.dto.request.PromotionRequest;
import org.example.datn_sd69.modules.promotion.dto.response.FlashSaleProductResponse;
import org.example.datn_sd69.modules.promotion.dto.response.PromotionProductVariantOptionResponse;
import org.example.datn_sd69.modules.promotion.dto.response.PromotionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface PromotionService {

    Page<PromotionResponse> getAll(String keyword, Integer status, Pageable pageable);

    PromotionResponse getById(Integer id);

    PromotionResponse create(PromotionRequest request);

    PromotionResponse update(Integer id, PromotionRequest request);

    /** Giữ signature cũ để không làm vỡ caller khác. */
    PromotionResponse changeStatus(Integer id, Integer status);

    /** Bản stale-safe dành cho Admin FE mới. */
    PromotionResponse changeStatus(Integer id, Integer status, String expectedRevision);

    /** Giữ signature cũ để không làm vỡ caller khác. */
    void softDelete(Integer id);

    /** Bản stale-safe dành cho Admin FE mới. */
    void softDelete(Integer id, String expectedRevision);

    int syncExpiredPromotions();

    Page<FlashSaleProductResponse> getActiveFlashSaleProducts(Pageable pageable);

    /**
     * Thời điểm bắt đầu gần nhất của một Flash Sale tương lai.
     *
     * Dùng cho FE đặt timer tự refresh, không dùng để quyết định Promotion active.
     */
    LocalDateTime getNextFlashSaleStartDate();

    Page<PromotionProductVariantOptionResponse> searchProductVariantsForPromotion(
            String keyword,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer ignorePromotionId,
            Pageable pageable
    );

}
