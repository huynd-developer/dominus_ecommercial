package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository
        extends JpaRepository<ProductImage,Integer> {

    List<ProductImage> findByProductId(Integer productId);

    @Query("""
            select pi
            from ProductImage pi
            where pi.product.id=:productId
            and pi.isPrimary=true
            """)
    Optional<ProductImage> findPrimaryByProductId(
            @Param("productId") Integer productId
    );

    @Query("""
            select pi
            from ProductImage pi
            where pi.product.id=:productId
            order by pi.id
            """)
    Optional<ProductImage> findFirstByProductId(
            @Param("productId") Integer productId
    );

}