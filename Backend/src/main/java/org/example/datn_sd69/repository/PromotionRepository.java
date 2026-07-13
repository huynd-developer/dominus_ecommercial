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
    Page<Promotion> search(String keyword, Integer status, Pageable pageable);

    @Modifying
    @Query("""
        UPDATE Promotion p
        SET p.status = 0
        WHERE COALESCE(p.isDeleted, false) = false
          AND p.status = 1
          AND p.endDate <= :now
    """)
    int disableExpiredPromotions(LocalDateTime now);
}