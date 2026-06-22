package org.example.datn_sd69.repository;
import org.example.datn_sd69.entity.DanhSachYeuThich;
import org.example.datn_sd69.entity.KhachHang;
import org.example.datn_sd69.entity.BienTheSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DanhSachYeuThichRepository extends JpaRepository<DanhSachYeuThich, Long> {

    // Tìm xem khách hàng này đã yêu thích biến thể sản phẩm này chưa
    Optional<DanhSachYeuThich> findByKhachHangAndBienTheSanPham(KhachHang khachHang, BienTheSanPham bienTheSanPham);

    // Lấy toàn bộ danh sách yêu thích của 1 khách hàng để hiển thị lên UI Tab "Đã lưu"
    List<DanhSachYeuThich> findByKhachHang(KhachHang khachHang);
}
