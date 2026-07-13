package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.PromotionVariant;
import org.example.datn_sd69.entity.PromotionVariantId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface PromotionVariantRepository extends JpaRepository<PromotionVariant, PromotionVariantId> {

    /**
     * Lấy chi tiết biến thể trong 1 chiến dịch.
     *
     * Dùng cho màn hình admin xem/sửa chiến dịch.
     */
    @Query("""
        SELECT pv
        FROM PromotionVariant pv
        JOIN FETCH pv.promotion p
        JOIN FETCH pv.productVariant v
        JOIN FETCH v.product product
        JOIN FETCH v.capacity capacity
        JOIN FETCH v.bottleType bottleType
        WHERE p.id = :promotionId
        ORDER BY product.name ASC, v.sku ASC
    """)
    List<PromotionVariant> findDetailByPromotionId(Integer promotionId);

    /**
     * Xóa danh sách biến thể cũ khi cập nhật chiến dịch.
     */
    void deleteByPromotion_Id(Integer promotionId);

    /**
     * Kiểm tra 1 biến thể có bị trùng thời gian với chiến dịch khác không.
     *
     * Logic overlap chuẩn:
     * Campaign A overlap Campaign B khi:
     * A.start < B.end AND A.end > B.start
     */
    @Query("""
        SELECT COUNT(pv)
        FROM PromotionVariant pv
        JOIN pv.promotion p
        WHERE pv.productVariant.id = :productVariantId
          AND COALESCE(p.isDeleted, false) = false
          AND p.status = 1
          AND (:ignorePromotionId IS NULL OR p.id <> :ignorePromotionId)
          AND p.startDate < :endDate
          AND p.endDate > :startDate
    """)
    long countOverlapPromotion(
            Integer productVariantId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer ignorePromotionId
    );

    /**
     * Public Flash Sale có phân trang.
     *
     * Không filter ở service sau khi đã phân trang.
     * Filter trực tiếp ở query để page không bị hụt sản phẩm.
     *
     * Điều kiện sản phẩm được hiển thị ngoài trang chủ:
     * - Promotion chưa xóa mềm
     * - Promotion đang bật
     * - StartDate <= now < EndDate
     * - ProductVariant chưa xóa mềm
     * - ProductVariant đang hoạt động
     * - Product đang hoạt động
     * - Còn hàng
     * - Đã tới ngày sản xuất
     * - Chưa hết hạn sử dụng
     */
    @Query(
            value = """
                SELECT pv
                FROM PromotionVariant pv
                JOIN FETCH pv.promotion p
                JOIN FETCH pv.productVariant v
                JOIN FETCH v.product product
                JOIN FETCH v.capacity capacity
                JOIN FETCH v.bottleType bottleType
                WHERE COALESCE(p.isDeleted, false) = false
                  AND p.status = 1
                  AND p.startDate <= :now
                  AND p.endDate > :now
                  AND COALESCE(v.isDeleted, false) = false
                  AND v.status = 1
                  AND product.status = 1
                  AND v.stockQuantity > 0
                  AND v.manufacturingDate <= :today
                  AND v.expirationDate >= :today
                ORDER BY p.endDate ASC, product.name ASC, v.sku ASC
            """,
            countQuery = """
                SELECT COUNT(pv)
                FROM PromotionVariant pv
                JOIN pv.promotion p
                JOIN pv.productVariant v
                JOIN v.product product
                WHERE COALESCE(p.isDeleted, false) = false
                  AND p.status = 1
                  AND p.startDate <= :now
                  AND p.endDate > :now
                  AND COALESCE(v.isDeleted, false) = false
                  AND v.status = 1
                  AND product.status = 1
                  AND v.stockQuantity > 0
                  AND v.manufacturingDate <= :today
                  AND v.expirationDate >= :today
            """
    )
    Page<PromotionVariant> findActiveFlashSaleVariants(
            LocalDateTime now,
            LocalDate today,
            Pageable pageable
    );
}