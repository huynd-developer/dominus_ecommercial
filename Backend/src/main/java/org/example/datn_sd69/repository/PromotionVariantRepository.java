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
     * Flash Sale active chỉ phụ thuộc vào:
     * - Promotion còn hiệu lực
     * - ProductVariant/Product còn được bật và chưa xóa
     *
     * Promotion không dùng ProductVariant.stockQuantity / manufacturingDate /
     * expirationDate để quyết định active.
     *
     * Tồn bán được thật do InventoryLot quản lý và được map ra response ở Service.
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
            """
    )
    Page<PromotionVariant> findActiveFlashSaleVariantsByPromotionTime(
            LocalDateTime now,
            Pageable pageable
    );

    /**
     * Giữ signature cũ để không làm vỡ caller hiện tại.
     * today chỉ còn là compatibility parameter, không tham gia business rule Promotion.
     */
    default Page<PromotionVariant> findActiveFlashSaleVariants(
            LocalDateTime now,
            LocalDate today,
            Pageable pageable
    ) {
        return findActiveFlashSaleVariantsByPromotionTime(now, pageable);
    }

    /**
     * Dùng riêng cho cart/checkout để lấy Promotion active của một biến thể.
     *
     * Query này chỉ quyết định Promotion theo campaign/SKU.
     * Cart/Checkout tự kiểm tồn sellable từ InventoryLot theo nghiệp vụ của chúng.
     */
    @Query("""
        SELECT pv
        FROM PromotionVariant pv
        JOIN FETCH pv.promotion p
        JOIN FETCH pv.productVariant v
        JOIN FETCH v.product product
        WHERE v.id = :productVariantId
          AND COALESCE(p.isDeleted, false) = false
          AND p.status = 1
          AND p.startDate <= :now
          AND p.endDate > :now
          AND COALESCE(v.isDeleted, false) = false
          AND v.status = 1
          AND product.status = 1
        ORDER BY pv.discountPercent DESC, p.endDate ASC
    """)
    List<PromotionVariant> findActivePromotionByProductVariantIdByPromotionTime(
            Integer productVariantId,
            LocalDateTime now
    );

    /**
     * Giữ signature cũ để Cart/Checkout hiện tại không cần sửa theo thay đổi repository.
     */
    default List<PromotionVariant> findActivePromotionByProductVariantId(
            Integer productVariantId,
            LocalDateTime now,
            LocalDate today
    ) {
        return findActivePromotionByProductVariantIdByPromotionTime(
                productVariantId,
                now
        );
    }
}