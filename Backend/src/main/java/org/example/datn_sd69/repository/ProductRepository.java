package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    // Check Brand đang được sử dụng
    boolean existsByBrandIdAndIsDeletedFalse(Integer brandId);

    // Check Category đang được sử dụng
    boolean existsByCategoryIdAndIsDeletedFalse(Integer categoryId);

    // Admin - tìm kiếm tất cả sản phẩm chưa xóa
    Page<Product> findByNameContainingIgnoreCaseAndIsDeletedFalse(
            String keyword,
            Pageable pageable
    );

    Page<Product> findByIsDeletedFalse(Pageable pageable);

    // Public - chỉ lấy sản phẩm đang hoạt động
    Page<Product> findByStatusAndIsDeletedFalse(
            Integer status,
            Pageable pageable
    );

    Page<Product> findByNameContainingIgnoreCaseAndStatusAndIsDeletedFalse(
            String keyword,
            Integer status,
            Pageable pageable
    );

}