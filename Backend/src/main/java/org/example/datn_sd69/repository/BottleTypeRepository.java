package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.BottleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BottleTypeRepository extends JpaRepository<BottleType, Integer> {
    List<BottleType> findByStatusNot(Integer status);

    Page<BottleType> findByStatusNot(Integer status, Pageable pageable);

    Page<BottleType> findByStatus(Integer status, Pageable pageable);

    // Tìm kiếm chính xác tên nhưng không phân biệt chữ hoa chữ thường
    Optional<BottleType> findByNameIgnoreCase(String name);
    Page<BottleType> findByIsDeletedFalse(Pageable pageable);

    Page<BottleType> findByNameContainingIgnoreCaseAndIsDeletedFalse(String name, Pageable pageable);
}
