package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {

    @Query("""
            SELECT pv FROM ProductVariant pv
            LEFT JOIN FETCH pv.product p
            LEFT JOIN FETCH p.brand
            LEFT JOIN FETCH pv.capacity
            LEFT JOIN FETCH pv.bottleType
            WHERE pv.sku = :sku AND pv.status = 1
            """)
    Optional<ProductVariant> findBySkuWithDetails(@Param("sku") String sku);
}