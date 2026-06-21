package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Long> {

    // Tìm khách hàng theo Email
    Optional<KhachHang> findByEmail(String email);

    // Kiểm tra xem Email đã tồn tại trong database chưa (dùng cho Đăng ký)
    boolean existsByEmail(String email);

}
