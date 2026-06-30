package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findByNameContainingIgnoreCase(String keyword);
    Page<Category> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
    boolean existsByNameIgnoreCase(String name);

    // 2. Dùng cho lúc Cập nhật (Kiểm tra xem tên này đã bị thằng KHÁC chiếm chưa)
    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);
}