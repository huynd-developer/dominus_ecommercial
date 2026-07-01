package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {

    // Ưu tiên ảnh IsPrimary = true
    @Query("SELECT pi FROM ProductImage pi WHERE pi.product.id = :productId AND pi.isPrimary = true")
    Optional<ProductImage> findPrimaryByProductId(@Param("productId") Integer productId);

    // Fallback: lấy ảnh đầu tiên nếu không có primary
    @Query("SELECT pi FROM ProductImage pi WHERE pi.product.id = :productId ORDER BY pi.id ASC")
    Optional<ProductImage> findFirstByProductId(@Param("productId") Integer productId);
}
