package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {

    /**
     * Lấy Variant theo SKU
     */
    @Query("""
            SELECT pv FROM ProductVariant pv
            LEFT JOIN FETCH pv.product p
            LEFT JOIN FETCH p.brand
            LEFT JOIN FETCH pv.capacity
            LEFT JOIN FETCH pv.bottleType
            WHERE pv.sku = :sku
              AND pv.status = 1
              AND pv.isDeleted = false
            """)
    Optional<ProductVariant> findBySkuWithDetails(@Param("sku") String sku);

    /**
     * Check SKU khi thêm
     */
    boolean existsBySkuIgnoreCase(String sku);

    /**
     * Check SKU khi sửa
     */
    boolean existsBySkuIgnoreCaseAndIdNot(
            String sku,
            Integer id
    );

    /**
     * Admin
     */
    Page<ProductVariant> findByIsDeletedFalse(Pageable pageable);

    /**
     * Search SKU
     */
    Page<ProductVariant> findBySkuContainingIgnoreCaseAndIsDeletedFalse(
            String keyword,
            Pageable pageable
    );

    /**
     * Variant theo Product
     */
    Page<ProductVariant> findByProductIdAndIsDeletedFalse(
            Integer productId,
            Pageable pageable
    );

    /**
     * Search Variant theo Product
     */
    Page<ProductVariant> findByProductIdAndSkuContainingIgnoreCaseAndIsDeletedFalse(
            Integer productId,
            String keyword,
            Pageable pageable
    );

    /**
     * Public
     */
    List<ProductVariant> findByProductIdAndStatusAndIsDeletedFalse(
            Integer productId,
            Integer status
    );

}