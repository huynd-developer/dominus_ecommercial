package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.PromotionVariant;
import org.example.datn_sd69.entity.PromotionVariantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    @Query("""
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
        ORDER BY p.endDate ASC, product.name ASC
    """)
    List<PromotionVariant> findActiveFlashSaleVariants(LocalDateTime now);
}