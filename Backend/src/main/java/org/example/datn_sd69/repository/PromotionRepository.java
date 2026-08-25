package org.example.datn_sd69.repository;

import jakarta.persistence.LockModeType;
import org.example.datn_sd69.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
     * Dùng riêng cho mutation Promotion để serialize các thao tác trên cùng chiến dịch.
     * GET/list vẫn dùng query cũ, không bị lock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT p
        FROM Promotion p
        WHERE p.id = :id
    """)
    Optional<Promotion> findByIdForUpdate(@Param("id") Integer id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Promotion p
        SET p.status = 0
        WHERE COALESCE(p.isDeleted, false) = false
          AND p.status = 1
          AND p.endDate <= :now
    """)
    int disableExpiredPromotions(LocalDateTime now);

    // --- HÀM THÊM MỚI DÀNH CHO JOB BẬT/TẮT FLASH SALE TỰ ĐỘNG ---
    @Query("""
        SELECT p FROM Promotion p
        WHERE COALESCE(p.isDeleted, false) = false
          AND p.status = :currentStatus
          AND p.startDate <= :now
          AND p.endDate > :now
    """)
    List<Promotion> findToStart(@Param("currentStatus") Integer currentStatus, @Param("now") LocalDateTime now);

    @Query("""
        SELECT p FROM Promotion p
        WHERE COALESCE(p.isDeleted, false) = false
          AND p.status = :currentStatus
          AND p.endDate <= :now
    """)
    List<Promotion> findToEnd(@Param("currentStatus") Integer currentStatus, @Param("now") LocalDateTime now);
}
