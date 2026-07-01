package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Capacity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CapacityRepository extends JpaRepository<Capacity, Integer> {

    // Lấy tất cả bản ghi chưa bị xóa mềm
    List<Capacity> findByIsDeletedFalse();

    // Phân trang cho Admin: Chỉ lấy các bản ghi chưa bị xóa mềm
    Page<Capacity> findByIsDeletedFalse(Pageable pageable);

    // Phân trang cho Khách: Phải đang hiện (status = 1) VÀ chưa bị xóa mềm (isDeleted = false)
    Page<Capacity> findByStatusAndIsDeletedFalse(Integer status, Pageable pageable);

    // Tìm kiếm chính xác theo giá trị (Quét toàn bộ DB để check trùng tuyệt đối)
    Optional<Capacity> findByValue(Double value);
}