package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Integer id);

    Page<ProductVariant> findByIsDeletedFalse(Pageable pageable);

    Page<ProductVariant> findBySkuContainingIgnoreCaseAndIsDeletedFalse(
            String keyword,
            Pageable pageable
    );

    Page<ProductVariant> findByProductIdAndIsDeletedFalse(
            Integer productId,
            Pageable pageable
    );

    Page<ProductVariant> findByProductIdAndSkuContainingIgnoreCaseAndIsDeletedFalse(
            Integer productId,
            String keyword,
            Pageable pageable
    );

    List<ProductVariant> findByProductIdAndStatusAndIsDeletedFalse(
            Integer productId,
            Integer status
    );

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

    List<ProductVariant> findByProductIdAndIsDeletedFalse(Integer productId);
}