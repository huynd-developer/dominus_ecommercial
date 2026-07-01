package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.FragranceFamily;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FragranceFamilyRepository extends JpaRepository<FragranceFamily, Integer> {
    List<FragranceFamily> findByStatusNot(Integer status);

    Page<FragranceFamily> findByStatusNot(Integer status, Pageable pageable);

    Page<FragranceFamily> findByStatus(Integer status, Pageable pageable);

    // Dùng để quét trùng lặp tên nhóm hương không phân biệt chữ hoa chữ thường
    Optional<FragranceFamily> findByNameIgnoreCase(String name);
}
