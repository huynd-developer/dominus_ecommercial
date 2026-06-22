package org.example.datn_sd69.modules.auth.service;

import org.example.datn_sd69.entity.DanhSachYeuThich;
import org.example.datn_sd69.entity.KhachHang;
import org.example.datn_sd69.entity.BienTheSanPham;
import org.example.datn_sd69.repository.DanhSachYeuThichRepository;
import org.example.datn_sd69.repository.KhachHangRepository; // Giả định bạn đã có
import org.example.datn_sd69.repository.BienTheSanPhamRepository; // Giả định bạn đã có
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DanhSachYeuThichService {

    private final DanhSachYeuThichRepository yeuThichRepository;
    private final KhachHangRepository khachHangRepository;
    private final BienTheSanPhamRepository bienTheSanPhamRepository;

    public DanhSachYeuThichService(DanhSachYeuThichRepository yeuThichRepository,
                                   KhachHangRepository khachHangRepository,
                                   BienTheSanPhamRepository bienTheSanPhamRepository) {
        this.yeuThichRepository = yeuThichRepository;
        this.khachHangRepository = khachHangRepository;
        this.bienTheSanPhamRepository = bienTheSanPhamRepository;
    }

    @Transactional
    public boolean toggleYeuThich(Long khachHangId, Long bienTheSanPhamId) {
        // 1. Tìm hoặc lấy ra đối tượng KhachHang và BienTheSanPham từ Database
        KhachHang khachHang = khachHangRepository.findById(khachHangId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng!"));
        BienTheSanPham bienTheSanPham = bienTheSanPhamRepository.findById(bienTheSanPhamId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể sản phẩm!"));

        // 2. Kiểm tra xem cặp đôi này đã tồn tại trong bảng DanhSachYeuThichs chưa
        Optional<DanhSachYeuThich> existing = yeuThichRepository.findByKhachHangAndBienTheSanPham(khachHang, bienTheSanPham);

        if (existing.isPresent()) {
            // Đã tồn tại -> Tiến hành XÓA (Bỏ yêu thích)
            yeuThichRepository.delete(existing.get());
            return false; // Trả về false nghĩa là trạng thái hiện tại: Đã bỏ thích
        } else {
            // Chưa tồn tại -> Tiến hành THÊM MỚI sử dụng Builder từ Lombok của bạn
            DanhSachYeuThich mới = DanhSachYeuThich.builder()
                    .khachHang(khachHang)
                    .bienTheSanPham(bienTheSanPham)
                    .ngayThem(LocalDateTime.now())
                    .build();

            yeuThichRepository.save(mới);
            return true; // Trả về true nghĩa là trạng thái hiện tại: Đã yêu thích thành công
        }
    }

    public List<DanhSachYeuThich> getDanhSachByKhachHang(Long khachHangId) {
        KhachHang khachHang = khachHangRepository.findById(khachHangId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng!"));
        return yeuThichRepository.findByKhachHang(khachHang);
    }
}
