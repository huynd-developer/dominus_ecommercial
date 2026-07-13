package org.example.datn_sd69.modules.promotion.service;

import org.example.datn_sd69.modules.promotion.dto.request.PromotionRequest;
import org.example.datn_sd69.modules.promotion.dto.response.FlashSaleProductResponse;
import org.example.datn_sd69.modules.promotion.dto.response.PromotionProductVariantOptionResponse;
import org.example.datn_sd69.modules.promotion.dto.response.PromotionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface PromotionService {

    Page<PromotionResponse> getAll(String keyword, Integer status, Pageable pageable);

    PromotionResponse getById(Integer id);

    PromotionResponse create(PromotionRequest request);

    PromotionResponse update(Integer id, PromotionRequest request);

    PromotionResponse changeStatus(Integer id, Integer status);

    void softDelete(Integer id);

    int syncExpiredPromotions();

    List<FlashSaleProductResponse> getActiveFlashSaleProducts();

    Page<PromotionProductVariantOptionResponse> searchProductVariantsForPromotion(
            String keyword,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer ignorePromotionId,
            Pageable pageable
    );
}