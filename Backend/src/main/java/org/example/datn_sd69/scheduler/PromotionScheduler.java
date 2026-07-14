package org.example.datn_sd69.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.datn_sd69.modules.promotion.service.PromotionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PromotionScheduler {

    private final PromotionService promotionService;

    /**
     * Cứ mỗi 60 giây quét chiến dịch hết hạn.
     * Giá Flash Sale vẫn được tính real-time theo StartDate / EndDate khi gọi API.
     */
    @Scheduled(fixedDelay = 60000)
    public void syncExpiredPromotions() {
        int updatedCount = promotionService.syncExpiredPromotions();

        if (updatedCount > 0) {
            log.info("Đã tự động tắt {} chiến dịch khuyến mãi hết hạn", updatedCount);
        }
    }
}
