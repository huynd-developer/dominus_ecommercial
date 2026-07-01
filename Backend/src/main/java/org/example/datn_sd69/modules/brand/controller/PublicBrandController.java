package org.example.datn_sd69.modules.brand.controller;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.brand.service.BrandService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/brands") // ĐƯỜNG DẪN PUBLIC CHO KHÁCH
@RequiredArgsConstructor
public class PublicBrandController {

    private final BrandService brandService;

    // Xem danh sách cho Khách (Chỉ lấy Status = 1)
    @GetMapping
    public ResponseEntity<?> getActiveBrands(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(Map.of(
                "status", "Thành công",
                "message", "Lấy danh sách thương hiệu đang hoạt động",
                "data", brandService.getActiveBrands(keyword, page, size)
        ));
    }
}