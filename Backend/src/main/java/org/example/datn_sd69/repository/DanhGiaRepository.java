package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.DanhGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DanhGiaRepository extends JpaRepository<DanhGia, Long> {

    boolean existsByChiTietDonHang_Id(Long chiTietDonHangId);
}
