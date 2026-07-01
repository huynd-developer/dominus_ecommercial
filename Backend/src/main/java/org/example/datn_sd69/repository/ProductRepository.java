package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    // Check mồ côi cho Brand
    boolean existsByBrandIdAndIsDeletedFalse(Integer brandId);

    // THÊM DÒNG NÀY: Check mồ côi cho Category
    boolean existsByCategoryIdAndIsDeletedFalse(Integer categoryId);
}