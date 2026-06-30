package org.example.datn_sd69.modules.category.service;

import org.example.datn_sd69.entity.Category;
import org.example.datn_sd69.modules.category.dto.request.CategoryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {
    List<Category> getAll();
    Category getById(Integer id);
    Category create(CategoryRequest request); // Đổi ở đây
    Category update(Integer id, CategoryRequest request); // Đổi ở đây
    void delete(Integer id);
    List<Category> search(String keyword);
    Page<Category> getAll(Pageable pageable);
    Page<Category> search(String keyword, Pageable pageable);
}
