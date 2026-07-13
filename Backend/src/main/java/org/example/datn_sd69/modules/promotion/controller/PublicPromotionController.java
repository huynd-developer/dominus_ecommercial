package org.example.datn_sd69.modules.promotion.controller;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.promotion.service.PromotionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/promotions")
public class PublicPromotionController {

    private final PromotionService promotionService;

    /**
     * Khung Flash Sale ngoài trang chủ.
     * Chỉ trả sản phẩm thuộc chiến dịch đang bật và đang nằm trong thời gian chạy.
     */
    @GetMapping("/flash-sale")
    public ResponseEntity<?> getFlashSaleProducts() {
        return ResponseEntity.ok(promotionService.getActiveFlashSaleProducts());
    }
}