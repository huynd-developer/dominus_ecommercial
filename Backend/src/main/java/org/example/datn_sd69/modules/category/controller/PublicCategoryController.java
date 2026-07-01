package org.example.datn_sd69.modules.category.controller;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Category;
import org.example.datn_sd69.modules.category.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories") // ĐƯỜNG DẪN PUBLIC CHO KHÁCH (Không có chữ admin)
@RequiredArgsConstructor
public class PublicCategoryController {

    private final CategoryService categoryService;

    // Khách hàng xem danh mục đang hoạt động (Trạng thái = 1)
    @GetMapping
    public ResponseEntity<Page<Category>> getActiveCategories(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size) {

        return ResponseEntity.ok(categoryService.getActiveCategories(keyword, page, size));
    }
}