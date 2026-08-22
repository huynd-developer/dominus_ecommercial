package org.example.datn_sd69.repository;

import jakarta.persistence.LockModeType;
import org.example.datn_sd69.entity.AiUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface AiUsageRepository extends JpaRepository<AiUsage, Integer> {

    /*
     * Khóa đúng record quota của user trong ngày để tránh hai request
     * đồng thời cùng đọc một usedCount rồi cùng vượt giới hạn.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a
            FROM AiUsage a
            WHERE a.userId = :userId
              AND a.usageDate = :usageDate
            """)
    Optional<AiUsage> findForUpdate(
            @Param("userId") Integer userId,
            @Param("usageDate") LocalDate usageDate
    );
}