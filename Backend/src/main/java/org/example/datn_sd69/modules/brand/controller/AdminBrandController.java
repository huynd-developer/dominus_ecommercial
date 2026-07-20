package org.example.datn_sd69.modules.brand.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.brand.dto.request.BrandRequest;
import org.example.datn_sd69.modules.brand.service.BrandService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/brands") // ĐÃ ĐỔI THÀNH API ADMIN
@RequiredArgsConstructor
public class AdminBrandController {

    private final BrandService brandService;

    // 1. Xem danh sách (Cho Admin, thấy cả trạng thái 0 và 1)
    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "99") int size) {

        return ResponseEntity.ok(Map.of(
                "status", "Thành công",
                "message", "Lấy danh sách thương hiệu phân trang thành công",
                "data", brandService.getBrandsWithPagination(keyword, page, size)
        ));
    }

    // Lấy chi tiết Brand
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(Map.of(
                    "status", "Thành công",
                    "message", "Lấy thông tin thương hiệu thành công",
                    "data", brandService.getBrandById(id)
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage()
            ));
        }
    }

    // 2. Thêm mới Brand
    @PreAuthorize("hasAnyAuthority('MANAGER', 'OWNER')")
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody BrandRequest request) {
        return ResponseEntity.ok(Map.of(
                "status", "Thành công",
                "message", "Tạo thương hiệu mới thành công",
                "data", brandService.createBrand(request)
        ));
    }

    // 3. Cập nhật Brand
    @PreAuthorize("hasAnyAuthority('MANAGER', 'OWNER')")
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

    // 4. Xóa mềm Brand
    @PreAuthorize("hasAuthority('OWNER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        try {
            brandService.deleteBrand(id);
            return ResponseEntity.ok(Map.of(
                    "message", "Đã khóa thương hiệu thành công!"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Đã có lỗi hệ thống xảy ra!"));
        }
    }

    // 5. Upload Logo
    @PreAuthorize("hasAnyAuthority('MANAGER', 'OWNER')")
    @PostMapping("/upload-logo")
    public ResponseEntity<?> uploadLogo(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "Vui lòng chọn một file ảnh để upload!"
            ));
        }

        String contentType = file.getContentType();
        List<String> allowedTypes = Arrays.asList("image/jpeg", "image/png", "image/jpg", "image/webp");
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "Định dạng file không hợp lệ! Chỉ chấp nhận ảnh (jpg, jpeg, png, webp)."
            ));
        }

        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "Dung lượng file quá lớn! Vui lòng chọn ảnh dưới 5MB."
            ));
        }

        try {
            String imageUrl = brandService.uploadLogo(file);
            return ResponseEntity.ok(Map.of(
                    "status", "Thành công",
                    "message", "Upload ảnh thành công",
                    "url", imageUrl
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "Lỗi upload ảnh: " + e.getMessage()
            ));
        }
    }
}