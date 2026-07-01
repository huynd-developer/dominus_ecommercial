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

    // 1. Dùng cho hàm getAll() không phân trang
    List<Capacity> findByStatusNot(Integer status);

    // 2. Dùng cho hàm getAll() có phân trang (Admin)
    Page<Capacity> findByStatusNot(Integer status, Pageable pageable);

    // 3. Dùng cho hàm getActiveCapacities() có phân trang (Khách)
    Page<Capacity> findByStatus(Integer status, Pageable pageable);

    // 4. Dùng để check trùng lặp khi Thêm/Sửa dung tích
    Optional<Capacity> findByValue(Double value);
}