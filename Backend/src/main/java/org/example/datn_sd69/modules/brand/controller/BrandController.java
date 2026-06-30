package org.example.datn_sd69.modules.brand.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.brand.dto.request.BrandRequest;
import org.example.datn_sd69.modules.brand.service.BrandService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/thuong-hieu")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    // 1. Xem danh sách (Ai cũng xem được)
    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(Map.of(
                "status", "Thành công",
                "message", "Lấy danh sách thương hiệu thành công",
                "data", brandService.getAllBrands()
        ));
    }
        
    // 2. Thêm mới Brand (Chỉ MANAGER và OWNER)
    @PreAuthorize("hasAnyAuthority('Manager', 'Owner')")
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody BrandRequest request) {
        return ResponseEntity.ok(Map.of(
                "status", "Thành công",
                "message", "Tạo thương hiệu mới thành công",
                "data", brandService.createBrand(request)
        ));
    }

    // 3. Cập nhật Brand (Chỉ MANAGER và OWNER)
    @PreAuthorize("hasAnyAuthority('Manager', 'Owner')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Integer id,
            @Valid @RequestBody BrandRequest request) {
        return ResponseEntity.ok(Map.of(
                "status", "Thành công",
                "message", "Cập nhật thương hiệu thành công",
                "data", brandService.updateBrand(id, request)
        ));
    }

    // 4. Xóa mềm Brand (Chỉ OWNER)
    @PreAuthorize("hasAuthority('Owner')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        try {
            brandService.deleteBrand(id);
            return ResponseEntity.ok(java.util.Map.of(
                    "message", "Đã khóa thương hiệu thành công!"
            ));
        } catch (RuntimeException e) {
            // Bắt lỗi "kẹt sản phẩm" và trả về status 400 kèm thông báo JSON
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("message", e.getMessage()));
        } catch (Exception e) {
            // Bắt các lỗi không xác định khác
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("message", "Đã có lỗi hệ thống xảy ra!"));
        }
    }
}