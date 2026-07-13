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

    PromotionResponse changeStatus(Integer id, Integer status);

    void softDelete(Integer id);

    int syncExpiredPromotions();

    Page<FlashSaleProductResponse> getActiveFlashSaleProducts(Pageable pageable);

    Page<PromotionProductVariantOptionResponse> searchProductVariantsForPromotion(
            String keyword,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer ignorePromotionId,
            Pageable pageable
    );
}