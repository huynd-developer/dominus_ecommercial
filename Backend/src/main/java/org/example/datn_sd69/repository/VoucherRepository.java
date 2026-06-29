package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {

    @Query("""
            SELECT v FROM Voucher v
            WHERE v.code = :code
              AND v.status = 1
              AND v.startDate <= :now
              AND v.endDate   >= :now
              AND v.usedCount  < v.usageLimit
            """)
    Optional<Voucher> findValidByCode(@Param("code") String code, @Param("now") LocalDateTime now);
}