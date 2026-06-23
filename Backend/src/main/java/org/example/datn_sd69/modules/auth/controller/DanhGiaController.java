package org.example.datn_sd69.modules.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.auth.dto.DanhGiaRequest;
import org.example.datn_sd69.entity.DanhGia;
import org.example.datn_sd69.modules.auth.service.DanhGiaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/danh-gia")
@RequiredArgsConstructor
public class DanhGiaController {

    private final DanhGiaService danhGiaService;

    @PostMapping
    public ResponseEntity<?> guiDanhGiaSanPham(@Valid @RequestBody DanhGiaRequest request) {
        try {
            // KHÔNG DÙNG TOKEN: Gán cứng luôn ID khách hàng hợp lệ trong DB (Ví dụ: 1L)
            Long khachHangId = 1L;

            DanhGia ketQua = danhGiaService.taoDanhGia(khachHangId, request);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Cảm ơn bạn đã đánh giá sản phẩm!",
                    "data", ketQua
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}