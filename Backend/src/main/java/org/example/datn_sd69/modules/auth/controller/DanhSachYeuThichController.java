package org.example.datn_sd69.modules.auth.controller;

import org.example.datn_sd69.entity.DanhSachYeuThich;
import org.example.datn_sd69.modules.auth.service.DanhSachYeuThichService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/favorites")
@CrossOrigin(origins = "*")
public class DanhSachYeuThichController {

    private final DanhSachYeuThichService yeuThichService;

    public DanhSachYeuThichController(DanhSachYeuThichService yeuThichService) {
        this.yeuThichService = yeuThichService;
    }

    // ... Hàm POST toggle giữ nguyên

    @GetMapping
    public ResponseEntity<?> getMyFavorites() {
        Long khachHangId = 1L;
        List<DanhSachYeuThich> listRaw = yeuThichService.getDanhSachByKhachHang(khachHangId);

        // Chuyển đổi Entity thành một mảng Map gọn nhẹ, loại bỏ thông tin KhachHang nhạy cảm gây lỗi 403
        List<Map<String, Object>> customList = new ArrayList<>();

        for (DanhSachYeuThich item : listRaw) {
            Map<String, Object> map = new HashMap<>();
            map.clear();
            map.put("id", item.getId());
            map.put("ngayThem", item.getNgayThem());

            // Chỉ lấy thông tin biến thể sản phẩm truyền ra cho UI
            if (item.getBienTheSanPham() != null) {
                map.put("bienTheSanPhamId", item.getBienTheSanPham().getId());
                // map.put("tenBienThe", item.getBienTheSanPham().getTen()); // Bật ra nếu entity của bạn có trường này
                // map.put("gia", item.getBienTheSanPham().getGia());
            }
            customList.add(map);
        }

        return ResponseEntity.ok(customList);
    }
}