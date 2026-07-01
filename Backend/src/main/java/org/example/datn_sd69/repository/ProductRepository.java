package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    boolean existsByBrandIdAndStatusNot(Integer brandId, Integer status);
}
