package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Concentration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConcentrationRepository extends JpaRepository<Concentration, Integer> {

    // Lấy tất cả bản ghi chưa bị xóa
    List<Concentration> findByIsDeletedFalse();

    // Lấy tất cả bản ghi chưa bị xóa (có phân trang)
    Page<Concentration> findByIsDeletedFalse(Pageable pageable);

    // Lấy các bản ghi chưa bị xóa VÀ có trạng thái cụ thể
    Page<Concentration> findByStatusAndIsDeletedFalse(Integer status, Pageable pageable);

    // Dùng để check trùng lặp tên (không phân biệt hoa thường)
    Optional<Concentration> findByNameIgnoreCase(String name);

    // 👇 THÊM HÀM NÀY ĐỂ XỬ LÝ TÌM KIẾM TỪ KHÓA TỪ VUE GỬI LÊN 👇
    @Query("SELECT c FROM Concentration c WHERE c.isDeleted = false AND LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Concentration> searchByName(@Param("keyword") String keyword, Pageable pageable);
}