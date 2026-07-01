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

    // Check trùng tên lúc thêm mới
    boolean existsByNameIgnoreCase(String name);

    // Check trùng tên lúc cập nhật (Kiểm tra xem tên này đã bị thằng KHÁC chiếm chưa)
    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);

    // --- THÊM VÀO LUỒNG PUBLIC CỦA KHÁCH (Chỉ lấy status = 1) ---
    Page<Category> findByNameContainingIgnoreCaseAndStatus(String keyword, Integer status, Pageable pageable);
    Page<Category> findByStatus(Integer status, Pageable pageable);
}