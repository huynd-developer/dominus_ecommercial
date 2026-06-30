package org.example.datn_sd69.modules.category.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Category;
import org.example.datn_sd69.modules.category.dto.request.CategoryRequest;
import org.example.datn_sd69.modules.category.service.CategoryService;
import org.example.datn_sd69.repository.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<Category> getAll() {
        return categoryRepository.findAll();
    }

    @Override
    public Category getById(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục có ID: " + id));
    }

    @Override
    public Category create(CategoryRequest request) {
        // Chuẩn hóa chuỗi: Cắt khoảng trắng dư thừa
        String categoryName = request.getName().trim();

        // CHECK TRÙNG TÊN LÚC THÊM MỚI
        if (categoryRepository.existsByNameIgnoreCase(categoryName)) {
            throw new RuntimeException("Tên danh mục '" + categoryName + "' đã tồn tại!");
        }

        Category category = new Category();
        category.setName(categoryName);
        category.setStatus(request.getStatus());

        return categoryRepository.save(category);
    }

    @Override
    public Category update(Integer id, CategoryRequest categoryDetails) {
        Category existingCategory = getById(id);
        String newName = categoryDetails.getName().trim();

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(newName, id)) {
            throw new RuntimeException("Tên danh mục '" + newName + "' đã được sử dụng!");
        }

        existingCategory.setName(newName);
        existingCategory.setStatus(categoryDetails.getStatus());

        return categoryRepository.save(existingCategory);
    }

    @Override
    public void delete(Integer id) {
        Category category = getById(id);
        category.setStatus(0);
        categoryRepository.save(category);
    }
    @Override
    public List<Category> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return categoryRepository.findAll();
        }
        return categoryRepository.findByNameContainingIgnoreCase(keyword.trim());
    }

    @Override
    public Page<Category> getAll(Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }

    @Override
    public Page<Category> search(String keyword, Pageable pageable) {
        return categoryRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);
    }
}
