package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.FragranceFamily;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FragranceFamilyRepository extends JpaRepository<FragranceFamily, Integer> {

    // Lấy tất cả bản ghi chưa bị xóa
    List<FragranceFamily> findByIsDeletedFalse();

    // Lấy tất cả bản ghi chưa bị xóa (có phân trang)
    Page<FragranceFamily> findByIsDeletedFalse(Pageable pageable);

    // Lấy các bản ghi chưa bị xóa VÀ có trạng thái cụ thể (Dành cho Public API)
    Page<FragranceFamily> findByStatusAndIsDeletedFalse(Integer status, Pageable pageable);

    // Dùng để quét trùng lặp tên nhóm hương không phân biệt chữ hoa chữ thường
    Optional<FragranceFamily> findByName(String name);

    @Query("SELECT f FROM FragranceFamily f WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<FragranceFamily> searchByName(@Param("keyword") String keyword, Pageable pageable);
}