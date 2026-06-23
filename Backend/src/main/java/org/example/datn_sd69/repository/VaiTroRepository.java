package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.VaiTro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VaiTroRepository extends JpaRepository<VaiTro, Integer> {
    // Thêm hàm tìm kiếm VaiTro theo tên
    Optional<VaiTro> findByTenVaiTro(String tenVaiTro);
}