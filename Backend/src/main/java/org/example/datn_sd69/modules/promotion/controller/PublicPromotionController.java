package org.example.datn_sd69.modules.promotion.controller;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.promotion.service.PromotionService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/promotions")
public class PublicPromotionController {

    private final PromotionService promotionService;

    @GetMapping("/flash-sale")
    public ResponseEntity<?> getFlashSaleProducts(
            @PageableDefault(size = 8) Pageable pageable,
            @RequestParam(defaultValue = "false") boolean includeTiming
    ) {
        /*
         * Caller cũ không yêu cầu timing:
         * giữ nguyên 100% response Page hiện tại để không phá contract.
         */
        if (!includeTiming) {
            return ResponseEntity.ok(
                    promotionService.getActiveFlashSaleProducts(pageable)
            );
        }

        /*
         * Lấy mốc sắp bắt đầu TRƯỚC.
         *
         * Thứ tự này cố ý để tránh race đúng tại StartDate:
         * nếu thời gian chuyển sang active giữa hai query thì FE vẫn không bị
         * rơi vào trường hợp vừa không có active data vừa mất nextStartDate.
         */
        LocalDateTime nextStartDate =
                promotionService.getNextFlashSaleStartDate();

        var page =
                promotionService.getActiveFlashSaleProducts(pageable);

        /*
         * Không dùng Map.of(...) vì nextStartDate có thể null.
         */
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("content", page.getContent());
        response.put("totalElements", page.getTotalElements());
        response.put("totalPages", page.getTotalPages());
        response.put("number", page.getNumber());
        response.put("size", page.getSize());

        /*
         * null nghĩa là hiện không còn Flash Sale tương lai nào cần schedule.
         */
        response.put("nextStartDate", nextStartDate);

        return ResponseEntity.ok(response);
    }
}