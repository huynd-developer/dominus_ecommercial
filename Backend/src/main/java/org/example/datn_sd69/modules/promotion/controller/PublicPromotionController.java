package org.example.datn_sd69.modules.promotion.controller;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.promotion.service.PromotionService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/promotions")
public class PublicPromotionController {

    private final PromotionService promotionService;

    /**
     * Khung Flash Sale ngoài trang chủ.
     * Có phân trang.
     *
     * GET /api/promotions/flash-sale?page=0&size=8
     */
    @GetMapping("/flash-sale")
    public ResponseEntity<?> getFlashSaleProducts(
            @PageableDefault(size = 8) Pageable pageable
    ) {
        return ResponseEntity.ok(promotionService.getActiveFlashSaleProducts(pageable));
    }
}