package org.example.datn_sd69.repository;

import jakarta.persistence.LockModeType;
import org.example.datn_sd69.entity.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StockAdjustmentRepository
        extends JpaRepository<StockAdjustment, Integer>,
        JpaSpecificationExecutor<StockAdjustment> {

    boolean existsByAdjustmentNo(String adjustmentNo);

    long countByStatus(Byte status);

    /**
     * Khóa phiếu khi submit / approve / reject để tránh xử lý đồng thời.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a
            from StockAdjustment a
            where a.id = :id
            """)
    Optional<StockAdjustment> findByIdForUpdate(
            @Param("id") Integer id
    );
}
