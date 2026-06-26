package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Integer> {
    // Tìm kiếm theo tên (không phân biệt hoa thường) và Phân trang
    Page<Brand> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
