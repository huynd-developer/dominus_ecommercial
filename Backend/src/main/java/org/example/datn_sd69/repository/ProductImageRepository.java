package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository
        extends JpaRepository<ProductImage, Integer> {

    List<ProductImage> findByProduct_Id(
            Integer productId
    );

    Optional<ProductImage> findFirstByProduct_IdAndIsPrimaryTrue(
            Integer productId
    );

    Optional<ProductImage> findFirstByProduct_Id(
            Integer productId
    );
}