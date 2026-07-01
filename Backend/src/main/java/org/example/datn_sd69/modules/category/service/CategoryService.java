package org.example.datn_sd69.modules.category.service;

import org.example.datn_sd69.entity.Category;
import org.example.datn_sd69.modules.category.dto.request.CategoryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {
    List<Category> getAll();
    Category getById(Integer id);
    Category create(CategoryRequest request);
    Category update(Integer id, CategoryRequest request);
    void delete(Integer id);
    List<Category> search(String keyword);
    Page<Category> getAll(Pageable pageable);
    Page<Category> search(String keyword, Pageable pageable);

    // --- THÊM DÒNG NÀY: Lấy danh sách cho khách xem (chỉ hàng đang hoạt động) ---
    Page<Category> getActiveCategories(String keyword, int page, int size);
}