package org.example.datn_sd69.modules.product.service;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.AiUsage;
import org.example.datn_sd69.repository.AiUsageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AiUsageService {

    private static final int DAILY_LIMIT = 5;

    private static final ZoneId BUSINESS_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private final AiUsageRepository aiUsageRepository;

    /**
     * Giữ trước 1 lượt sử dụng AI.
     *
     * Transaction này chỉ tồn tại trong thời gian rất ngắn để:
     * - đọc/khóa quota của đúng user trong ngày;
     * - kiểm tra giới hạn 5 lượt;
     * - tăng UsedCount lên 1;
     * - commit ngay.
     *
     * Không giữ transaction trong lúc chờ Gemini.
     */
    @Transactional(
            isolation = Isolation.SERIALIZABLE,
            rollbackFor = Exception.class
    )
    public Reservation reserve(Integer userId) {

        validateUserId(userId);

        LocalDate today =
                LocalDate.now(BUSINESS_ZONE);

        AiUsage usage =
                aiUsageRepository
                        .findForUpdate(userId, today)
                        .orElseGet(() -> {
                            AiUsage newUsage =
                                    new AiUsage();

                            newUsage.setUserId(userId);
                            newUsage.setUsageDate(today);
                            newUsage.setUsedCount(0);

                            /*
                             * Unique(UserId, UsageDate) ở DB đảm bảo
                             * một user chỉ có một record quota mỗi ngày.
                             */
                            return aiUsageRepository
                                    .saveAndFlush(newUsage);
                        });

        int usedCount =
                usage.getUsedCount() == null
                        ? 0
                        : usage.getUsedCount();

        if (usedCount >= DAILY_LIMIT) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Bạn đã sử dụng hết 5 lượt so sánh bằng AI hôm nay."
            );
        }

        usage.setUsedCount(usedCount + 1);
        aiUsageRepository.save(usage);

        return new Reservation(
                userId,
                today
        );
    }

    /**
     * Hoàn lại 1 lượt nếu Gemini hoặc bước parse kết quả thất bại.
     *
     * Đây là transaction riêng và ngắn.
     * Không ảnh hưởng Product, Inventory, Cart, Order hoặc auth.
     */
    @Transactional(
            isolation = Isolation.SERIALIZABLE,
            rollbackFor = Exception.class
    )
    public void release(Reservation reservation) {

        if (reservation == null) {
            return;
        }

        AiUsage usage =
                aiUsageRepository
                        .findForUpdate(
                                reservation.userId(),
                                reservation.usageDate()
                        )
                        .orElse(null);

        if (usage == null) {
            return;
        }

        int usedCount =
                usage.getUsedCount() == null
                        ? 0
                        : usage.getUsedCount();

        if (usedCount <= 0) {
            return;
        }

        usage.setUsedCount(usedCount - 1);
        aiUsageRepository.save(usage);
    }

    private void validateUserId(Integer userId) {

        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Không xác định được tài khoản đang đăng nhập."
            );
        }
    }

    /**
     * Giữ chính xác ngày đã reserve.
     *
     * Nếu request bắt đầu trước 00:00 và Gemini trả sau 00:00,
     * release vẫn hoàn đúng record của ngày đã trừ lượt.
     */
    public record Reservation(
            Integer userId,
            LocalDate usageDate
    ) {
    }
}