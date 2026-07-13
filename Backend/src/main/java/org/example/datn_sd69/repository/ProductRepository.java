package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository
        extends JpaRepository<Product, Integer> {

    Page<Product> findByIsDeletedFalse(
            Pageable pageable);

    Page<Product> findByStatusAndIsDeletedFalse(
            Integer status,
            Pageable pageable);

    boolean existsByBrandIdAndIsDeletedFalse(Integer brandId);

    boolean existsByCategoryIdAndIsDeletedFalse(Integer categoryId);

    boolean existsByConcentrationIdAndIsDeletedFalse(Integer concentrationId);

}