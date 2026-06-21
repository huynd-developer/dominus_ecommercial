package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Long> {

    // Kiểm tra trùng lặp qua email
    Optional<KhachHang> findByEmail(String email);

    // Kiểm tra trùng lặp qua số điện thoại
    Optional<KhachHang> findBySoDienThoai(String soDienThoai);

    boolean existsByEmail(String email);

    // 💡 KHUYÊN DÙNG: Thêm luôn dòng này phòng hờ bên AuthService cần check trùng SĐT khi đăng ký nhé!
    boolean existsBySoDienThoai(String soDienThoai);
}
