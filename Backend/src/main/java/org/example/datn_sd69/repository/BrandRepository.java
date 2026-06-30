package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Integer> {
    boolean existsByNameIgnoreCaseAndStatusNot(String name, Integer status);
    boolean existsByNameIgnoreCaseAndIdNotAndStatusNot(String name, Integer id, Integer status);
    Page<Brand> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
}
