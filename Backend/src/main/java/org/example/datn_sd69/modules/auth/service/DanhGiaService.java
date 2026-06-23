package org.example.datn_sd69.modules.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.ChiTietDonHang;
import org.example.datn_sd69.entity.DanhGia;
import org.example.datn_sd69.entity.DonHang;
import org.example.datn_sd69.modules.auth.dto.DanhGiaRequest;
import org.example.datn_sd69.repository.ChiTietDonHangRepository;
import org.example.datn_sd69.repository.DanhGiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DanhGiaService {

    private final DanhGiaRepository danhGiaRepository;
    private final ChiTietDonHangRepository chiTietDonHangRepository;

    @Transactional
    public DanhGia taoDanhGia(Long khachHangId, DanhGiaRequest request) {

        ChiTietDonHang ctdh = chiTietDonHangRepository.findById(request.getChiTietDonHangId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chi tiết đơn hàng."));

        DonHang donHang = ctdh.getDonHang();

        if (!donHang.getKhachHang().getId().equals(khachHangId)) {
            throw new SecurityException("Bạn không có quyền đánh giá đơn hàng của người khác.");
        }

        if (donHang.getTrangThaiDonHang() == null || donHang.getTrangThaiDonHang() != 4) {
            throw new IllegalStateException("Bạn chỉ có thể đánh giá sau khi đơn hàng đã được giao thành công.");
        }


        if (danhGiaRepository.existsByChiTietDonHang_Id(request.getChiTietDonHangId())) {
            throw new IllegalStateException("Sản phẩm này trong đơn hàng đã được bạn đánh giá rồi.");
        }

        DanhGia danhGiaMoi = DanhGia.builder()
                .chiTietDonHang(ctdh)
                .diemSoSao(request.getDiemSoSao())
                .binhLuan(request.getBinhLuan())
                .ngayDanhGia(LocalDateTime.now())
                .build();

        return danhGiaRepository.save(danhGiaMoi);
    }
}
