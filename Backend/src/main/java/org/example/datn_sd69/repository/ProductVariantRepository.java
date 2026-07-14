package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
// ==========================
// CRUD cơ bản
// ==========================

    List<ProductVariant> findByProduct_Id(Integer productId);

    @Modifying
    @Query("DELETE FROM ProductVariant v WHERE v.product.id = :productId")
    void deleteByProduct_Id(@Param("productId") Integer productId);


    Page<ProductVariant> findByIsDeletedFalse(Pageable pageable);


    @EntityGraph(attributePaths = {
            "product",
            "product.brand",
            "capacity",
            "bottleType"
    })
    Optional<ProductVariant> findByIdAndIsDeletedFalse(Integer id);

    @EntityGraph(attributePaths = {
            "product",
            "product.brand",
            "capacity",
            "bottleType"
    })
    Optional<ProductVariant> findBySkuIgnoreCaseAndIsDeletedFalse(String sku);

    @Query("""
    select count(v)
    from ProductVariant v
    where v.isDeleted = false
      and v.expirationDate <= :date
""")
    Long countNearExpiredVariants(
            @Param("date") LocalDate date
    );

    @Query("""
    select count(v)
    from ProductVariant v
    where v.isDeleted = false
      and v.expirationDate < CURRENT_DATE
""")
    Long countExpiredVariants();

    @Query("""
    select v
    from ProductVariant v
    where v.isDeleted = false
      and v.expirationDate < CURRENT_DATE
""")
    List<ProductVariant> findExpiredVariants();

// ==========================
// Search dùng cho Promotion
// ==========================

    @EntityGraph(attributePaths = {
            "product",
            "capacity",
            "bottleType"
    })
    @Query("""
    SELECT v
    FROM ProductVariant v
    JOIN v.product p
    WHERE COALESCE(p.isDeleted, false) = false
      AND (:keyword IS NULL
           OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(v.sku) LIKE LOWER(CONCAT('%', :keyword, '%')))
    ORDER BY p.name ASC, v.sku ASC
""")
    Page<ProductVariant> searchVariantsForPromotion(
            @Param("keyword") String keyword,
            Pageable pageable
    );

}
