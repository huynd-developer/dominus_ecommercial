package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Integer> {

    Optional<Brand> findByIdAndIsDeletedFalse(Integer id);
    List<Brand> findByIsDeletedFalse();

    boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);
    boolean existsByNameIgnoreCaseAndIdNotAndIsDeletedFalse(String name, Integer id);

    Page<Brand> findByIsDeletedFalse(Pageable pageable);
    Page<Brand> findByNameContainingIgnoreCaseAndIsDeletedFalse(String name, Pageable pageable);

    Page<Brand> findByNameContainingIgnoreCaseAndStatusAndIsDeletedFalse(String name, Integer status, Pageable pageable);
    Page<Brand> findByStatusAndIsDeletedFalse(Integer status, Pageable pageable);
}