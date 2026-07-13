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

    void deleteByPromotion_Id(Integer promotionId);

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
     * Không filter ở service sau khi đã phân trang, vì nếu filter sau pagination
     * thì mỗi page có thể bị hụt sản phẩm.
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
                  AND v.status = 1
                  AND product.status = 1
                  AND v.stockQuantity > 0
                  AND (v.manufacturingDate IS NULL OR v.manufacturingDate <= :today)
                  AND (v.expirationDate IS NULL OR v.expirationDate >= :today)
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
                  AND v.status = 1
                  AND product.status = 1
                  AND v.stockQuantity > 0
                  AND (v.manufacturingDate IS NULL OR v.manufacturingDate <= :today)
                  AND (v.expirationDate IS NULL OR v.expirationDate >= :today)
            """
    )
    Page<PromotionVariant> findActiveFlashSaleVariants(
            LocalDateTime now,
            LocalDate today,
            Pageable pageable
    );
}