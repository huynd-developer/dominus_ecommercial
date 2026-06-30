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
@RequestMapping("/api/brand")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    // 1. Xem danh sách (Ai cũng xem được)
    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(Map.of(
                "status", "Thành công",
                "message", "Lấy danh sách thương hiệu phân trang thành công",
                "data", brandService.getBrandsWithPagination(page, size)
        ));
    }

    // ĐÃ THÊM: Tìm kiếm Brand theo ID (Ai cũng xem được)
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
            return ResponseEntity.ok(Map.of(
                    "message", "Đã khóa thương hiệu thành công!"
            ));
        } catch (RuntimeException e) {
            // Bắt lỗi "kẹt sản phẩm" và trả về status 400 kèm thông báo JSON
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            // Bắt các lỗi không xác định khác
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Đã có lỗi hệ thống xảy ra!"));
        }
    }

    // 5. Upload Logo (Đã thêm Validate và đồng bộ kiểu trả về JSON)
    @PreAuthorize("hasAnyAuthority('Manager', 'Owner')") // Thêm phân quyền cho đồng bộ
    @PostMapping("/upload-logo")
    public ResponseEntity<?> uploadLogo(@RequestParam("file") MultipartFile file) {
        // Validate file rỗng
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "Vui lòng chọn một file ảnh để upload!"
            ));
        }

        // Validate định dạng ảnh
        String contentType = file.getContentType();
        List<String> allowedTypes = Arrays.asList("image/jpeg", "image/png", "image/jpg", "image/webp");
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "Định dạng file không hợp lệ! Chỉ chấp nhận ảnh (jpg, jpeg, png, webp)."
            ));
        }

        // Validate dung lượng (Tối đa 5MB)
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