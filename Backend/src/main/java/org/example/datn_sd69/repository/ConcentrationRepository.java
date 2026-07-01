package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Concentration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConcentrationRepository extends JpaRepository<Concentration, Integer> {
    List<Concentration> findByStatusNot(Integer status);

    Page<Concentration> findByStatusNot(Integer status, Pageable pageable);

    Page<Concentration> findByStatus(Integer status, Pageable pageable);

    // Dùng để check trùng lặp tên (không phân biệt hoa thường)
    Optional<Concentration> findByNameIgnoreCase(String name);
}
