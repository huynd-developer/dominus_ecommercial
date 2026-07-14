package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    Optional<Category> findByIdAndIsDeletedFalse(Integer id);

    List<Category> findByIsDeletedFalse();

    boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndIdNotAndIsDeletedFalse(String name, Integer id);

    Page<Category> findByIsDeletedFalse(Pageable pageable);

    Page<Category> findByNameContainingIgnoreCaseAndIsDeletedFalse(String name, Pageable pageable);

    // Dành cho khách hàng (Status = 1 và chưa bị xóa)
    Page<Category> findByNameContainingIgnoreCaseAndStatusAndIsDeletedFalse(String name, Integer status, Pageable pageable);

    Page<Category> findByStatusAndIsDeletedFalse(Integer status, Pageable pageable);

}