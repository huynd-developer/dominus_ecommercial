package org.example.datn_sd69.modules.category.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Category;
import org.example.datn_sd69.modules.category.dto.request.CategoryRequest;
import org.example.datn_sd69.modules.category.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/categories") // ĐƯỜNG DẪN ADMIN
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    // 1. Lấy danh sách danh mục (Kết hợp Tìm kiếm & Phân trang - Admin thấy tất cả)
    @GetMapping
    public ResponseEntity<Page<Category>> getAllCategories(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "99") int size) {

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

    // 3. Thêm mới danh mục
    @PreAuthorize("hasAnyAuthority('MANAGER', 'OWNER')")
    @PostMapping
    public ResponseEntity<Category> createCategory(@Valid @RequestBody CategoryRequest categoryRequest) {
        return ResponseEntity.ok(categoryService.create(categoryRequest));
    }

    // 4. Cập nhật danh mục
    @PreAuthorize("hasAnyAuthority('MANAGER', 'OWNER')")
    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(
            @PathVariable Integer id,
            @Valid @RequestBody CategoryRequest categoryRequest) {
        return ResponseEntity.ok(categoryService.update(id, categoryRequest));
    }

    // 5. Xóa danh mục
    @PreAuthorize("hasAuthority('OWNER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCategory(@PathVariable Integer id) {
        categoryService.delete(id);
        return ResponseEntity.ok("Xóa danh mục thành công!");
    }
}