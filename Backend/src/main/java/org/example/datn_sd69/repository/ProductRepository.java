package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    /* ================= CHECK ================= */

    boolean existsByBrandIdAndIsDeletedFalse(Integer brandId);

    boolean existsByCategoryIdAndIsDeletedFalse(Integer categoryId);

    /* ================= ADMIN ================= */

    Page<Product> findByIsDeletedFalse(Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseAndIsDeletedFalse(
            String keyword,
            Pageable pageable
    );

    /* ================= PUBLIC ================= */

    Page<Product> findByStatusAndIsDeletedFalse(
            Integer status,
            Pageable pageable
    );

    Page<Product> findByNameContainingIgnoreCaseAndStatusAndIsDeletedFalse(
            String keyword,
            Integer status,
            Pageable pageable
    );

    /* ================= DETAIL ================= */

    @EntityGraph(attributePaths = {
            "brand",
            "category",
            "concentration"
    })
    Optional<Product> findByIdAndIsDeletedFalse(Integer id);

    @Query("""
            SELECT DISTINCT p
            FROM Product p
            LEFT JOIN FETCH p.variants v
            LEFT JOIN FETCH v.capacity
            LEFT JOIN FETCH v.bottleType
            WHERE p.id = :id
              AND p.isDeleted = false
            """)
    Optional<Product> findDetailWithVariants(Integer id);

}