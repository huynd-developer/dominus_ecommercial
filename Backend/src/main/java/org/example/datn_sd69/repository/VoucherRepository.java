package org.example.datn_sd69.repository;

import jakarta.persistence.LockModeType;
import org.example.datn_sd69.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {

    @Query("""
            SELECT v FROM Voucher v
            WHERE LOWER(v.code) = LOWER(:code)
              AND (v.isDeleted = false OR v.isDeleted IS NULL)
            """)
    Optional<Voucher> findByCodeIgnoreCase(@Param("code") String code);

    Optional<Voucher> findByCode(String code);

    @Query("""
            SELECT v FROM Voucher v
            WHERE LOWER(v.code) = LOWER(:code)
              AND v.status = 1
              AND (v.isDeleted = false OR v.isDeleted IS NULL)
              AND (v.startDate IS NULL OR v.startDate <= :now)
              AND (v.endDate IS NULL OR v.endDate > :now)
              AND (
                    v.usageLimit IS NULL
                    OR v.usageLimit <= 0
                    OR v.usedCount IS NULL
                    OR v.usedCount < v.usageLimit
                  )
            """)
    Optional<Voucher> findValidByCode(
            @Param("code") String code,
            @Param("now") LocalDateTime now
    );

    List<Voucher> findByIsDeletedFalseOrderByIdDesc();

    boolean existsByCode(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Integer id);

    /**
     * Khóa đúng row Voucher trước các mutation Admin.
     * Không filter IsDeleted ở query để service có thể phân biệt rõ
     * voucher không tồn tại và voucher đã bị xóa mềm.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voucher v WHERE v.id = :id")
    Optional<Voucher> findByIdForUpdate(@Param("id") Integer id);

    @Query("""
            SELECT v FROM Voucher v
            WHERE COALESCE(v.isDeleted, false) = false
              AND (:keyword IS NULL OR LOWER(v.code) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR v.status = :status)
            ORDER BY v.id DESC
            """)
    org.springframework.data.domain.Page<Voucher> searchVouchers(
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            org.springframework.data.domain.Pageable pageable
    );

    /*
     * Giữ method findToStart để không làm vỡ caller cũ nếu còn tồn tại,
     * nhưng SystemJobScheduler không còn dùng nó để tự bật Voucher status 0 -> 1.
     */
    @Query("""
        SELECT v FROM Voucher v
        WHERE COALESCE(v.isDeleted, false) = false
          AND v.status = :currentStatus
          AND v.startDate <= :now
          AND v.endDate > :now
    """)
    List<Voucher> findToStart(
            @Param("currentStatus") Integer currentStatus,
            @Param("now") LocalDateTime now
    );

    @Query("""
        SELECT v FROM Voucher v
        WHERE COALESCE(v.isDeleted, false) = false
          AND v.status = :currentStatus
          AND v.endDate <= :now
    """)
    List<Voucher> findToEnd(
            @Param("currentStatus") Integer currentStatus,
            @Param("now") LocalDateTime now
    );
}
