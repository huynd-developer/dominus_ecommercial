package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface PromotionRepository extends JpaRepository<Promotion, Integer> {

    @Query("""
        SELECT p
        FROM Promotion p
        WHERE COALESCE(p.isDeleted, false) = false
          AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:status IS NULL OR p.status = :status)
        ORDER BY p.id DESC
    """)
    Page<Promotion> search(
            String keyword,
            Integer status,
            Pageable pageable
    );

    /**
     * Background Job dùng method này để tự tắt chiến dịch hết hạn.
     *
     * Chỉ đổi Promotion.Status = 0.
     * Không sửa ProductVariant.Price để tránh sai báo cáo/doanh thu/lịch sử đơn.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Promotion p
        SET p.status = 0
        WHERE COALESCE(p.isDeleted, false) = false
          AND p.status = 1
          AND p.endDate <= :now
    """)
    int disableExpiredPromotions(LocalDateTime now);
}