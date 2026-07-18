package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {

    /**
     * Dùng cho POS / Checkout:
     * Tìm voucher theo code, không phân biệt hoa thường.
     *
     * Không check status/date ở query này vì service sẽ tự validate
     * để trả đúng lỗi nghiệp vụ:
     * - mã đã ngừng hoạt động
     * - chưa đến thời gian dùng
     * - đã hết hạn
     * - hết lượt sử dụng
     * - đơn chưa đủ tối thiểu
     */
    @Query("""
            SELECT v FROM Voucher v
            WHERE LOWER(v.code) = LOWER(:code)
              AND (v.isDeleted = false OR v.isDeleted IS NULL)
            """)
    Optional<Voucher> findByCodeIgnoreCase(@Param("code") String code);

    /**
     * Dùng cho luồng cũ nếu còn chỗ nào đang gọi.
     */
    Optional<Voucher> findByCode(String code);

    /**
     * Dùng cho API khách hàng/apply cũ.
     */
    @Query("""
            SELECT v FROM Voucher v
            WHERE LOWER(v.code) = LOWER(:code)
              AND v.status = 1
              AND (v.isDeleted = false OR v.isDeleted IS NULL)
              AND (v.startDate IS NULL OR v.startDate <= :now)
              AND (v.endDate IS NULL OR v.endDate >= :now)
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
}