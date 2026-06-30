package org.example.datn_sd69.modules.category.controller;

<<<<<<< HEAD
import org.example.datn_sd69.modules.category.request.CategoryRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/danh-muc")
@CrossOrigin("*")
public class CategoryController {

    // 1. Xem danh sách Danh mục
    // Ai cũng xem được (Để đổ ra trang chủ cho User chọn)
//    @PreAuthorize("hasAnyAuthority(" +
//            "'OWNER', 'Owner', 'owner', " +
//            "'MANAGER', 'Manager', 'manager', " +
//            "'CASHIER', 'Cashier', 'cashier', " +
//            "'USER', 'User', 'user')")
    @GetMapping
    public ResponseEntity<?> getAll() {
        List<Map<String, Object>> mockCategories = List.of(
                Map.of("id", 1, "name", "Nước hoa Nam", "status", 1),
                Map.of("id", 2, "name", "Nước hoa Nữ", "status", 1),
                Map.of("id", 3, "name", "Nước hoa Unisex", "status", 1)
        );

        return ResponseEntity.ok(Map.of(
                "status", "Thành công",
                "data", mockCategories
        ));
    }

    // 2. Xem chi tiết 1 Danh mục
//    @PreAuthorize("hasAnyAuthority(" +
//            "'OWNER', 'Owner', 'owner', " +
//            "'MANAGER', 'Manager', 'manager', " +
//            "'CASHIER', 'Cashier', 'cashier', " +
//            "'USER', 'User', 'user')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(Map.of(
                "id", id,
                "name", "Danh mục Test " + id,
                "status", 1
        ));
    }

    // 3. Thêm mới Danh mục
    // Chỉ OWNER và MANAGER (Theo đúng Document)
//    @PreAuthorize("hasAnyAuthority(" +
//            "'OWNER', 'Owner', 'owner', " +
//            "'MANAGER', 'Manager', 'manager')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody CategoryRequest request) {
        // Yêu cầu Validate Status (0: Ẩn, 1: Hiện) từ Front-end gửi lên
        return ResponseEntity.ok(Map.of(
                "id", 99,
                "name", request.getName(),
                "status", request.getStatus(),
                "message", "Tạo danh mục mới thành công!"
        ));
    }

    // 4. Cập nhật Danh mục
//    @PreAuthorize("hasAnyAuthority(" +
//            "'OWNER', 'Owner', 'owner', " +
//            "'MANAGER', 'Manager', 'manager')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(Map.of(
                "id", id,
                "name", request.getName(),
                "status", request.getStatus(),
                "message", "Cập nhật danh mục thành công!"
        ));
    }

    // 5. Xóa mềm Danh mục
//    @PreAuthorize("hasAnyAuthority(" +
//            "'OWNER', 'Owner', 'owner', " +
//            "'MANAGER', 'Manager', 'manager')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        return ResponseEntity.ok(Map.of(
                "id", id,
                "status", 0, // 0: Ẩn theo yêu cầu của task
                "message", "Đã xóa mềm (ẩn) danh mục ID " + id
        ));
=======
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Category;
import org.example.datn_sd69.modules.category.dto.request.CategoryRequest;
import org.example.datn_sd69.modules.category.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@CrossOrigin("*")
public class CategoryController {

    private final CategoryService categoryService;

    // 1. Lấy danh sách danh mục (Kết hợp Tìm kiếm & Phân trang)
    @GetMapping
    public ResponseEntity<Page<Category>> getAllCategories(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page, // Spring Boot đếm trang từ 0
            @RequestParam(name = "size", defaultValue = "5") int size) { // Mặc định 5 item/trang

        Pageable pageable = PageRequest.of(page, size);

        if (keyword != null && !keyword.trim().isEmpty()) {
            return ResponseEntity.ok(categoryService.search(keyword, pageable));
        }
        return ResponseEntity.ok(categoryService.getAll(pageable));
    }

    // 2. Lấy chi tiết một danh mục
    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Integer id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    // 3. Thêm mới danh mục (Validate qua DTO)
    @PostMapping
    public ResponseEntity<Category> createCategory(@Valid @RequestBody CategoryRequest categoryRequest) {
        return ResponseEntity.ok(categoryService.create(categoryRequest));
    }

    // 4. Cập nhật danh mục (Validate qua DTO)
    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(
            @PathVariable Integer id,
            @Valid @RequestBody CategoryRequest categoryRequest) {
        return ResponseEntity.ok(categoryService.update(id, categoryRequest));
    }

    // 5. Xóa danh mục
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCategory(@PathVariable Integer id) {
        categoryService.delete(id);
        return ResponseEntity.ok("Xóa danh mục thành công!");
>>>>>>> d70ed35ab485643fffe04e11c47e6690abb1a2ef
    }
}