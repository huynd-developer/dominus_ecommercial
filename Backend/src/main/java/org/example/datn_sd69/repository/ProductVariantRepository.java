package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository
        extends JpaRepository<ProductVariant, Integer> {

    List<ProductVariant> findByProduct_Id(
            Integer productId
    );

    void deleteByProduct_Id(
            Integer productId
    );

    Optional<ProductVariant> findBySkuIgnoreCaseAndIsDeletedFalse(
            String sku
    );
}