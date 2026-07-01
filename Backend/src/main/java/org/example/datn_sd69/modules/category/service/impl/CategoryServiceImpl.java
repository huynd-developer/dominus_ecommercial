package org.example.datn_sd69.modules.category.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Category;
import org.example.datn_sd69.modules.category.dto.request.CategoryRequest;
import org.example.datn_sd69.modules.category.service.CategoryService;
import org.example.datn_sd69.repository.CategoryRepository;
import org.example.datn_sd69.repository.ProductRepository; // Nhớ import
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository; // Bổ sung cái này để check mồ côi

    @Override
    public List<Category> getAll() {
        return categoryRepository.findByIsDeletedFalse();
    }

    @Override
    public Category getById(Integer id) {
        return categoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục có ID: " + id));
    }

    @Override
    @Transactional
    public Category create(CategoryRequest request) {
        String categoryName = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIsDeletedFalse(categoryName)) {
            throw new RuntimeException("Tên danh mục '" + categoryName + "' đã tồn tại!");
        }
        Category category = new Category();
        category.setName(categoryName);
        category.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        category.setIsDeleted(false);
        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Category update(Integer id, CategoryRequest categoryDetails) {
        Category existingCategory = getById(id);
        String newName = categoryDetails.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNotAndIsDeletedFalse(newName, id)) {
            throw new RuntimeException("Tên danh mục '" + newName + "' đã được sử dụng cho danh mục khác!");
        }
        existingCategory.setName(newName);
        existingCategory.setStatus(categoryDetails.getStatus());
        return categoryRepository.save(existingCategory);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Category category = getById(id);

        // CHẶN XÓA NẾU ĐANG CÓ SẢN PHẨM
        // (Ông cần tạo hàm existsByCategoryIdAndIsDeletedFalse(id) bên ProductRepository nhé)
        if (productRepository.existsByCategoryIdAndIsDeletedFalse(id)) {
            throw new RuntimeException("Không thể ném vào thùng rác! Đang có sản phẩm thuộc danh mục này.");
        }

        // Đổi từ setStatus(0) sang setIsDeleted(true)
        category.setIsDeleted(true);
        categoryRepository.save(category);
    }

    @Override
    public Page<Category> getAll(Pageable pageable) {
        return categoryRepository.findByIsDeletedFalse(pageable);
    }

    @Override
    public Page<Category> search(String keyword, Pageable pageable) {
        return categoryRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse(keyword.trim(), pageable);
    }

    @Override
    public Page<Category> getActiveCategories(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        if (keyword != null && !keyword.trim().isEmpty()) {
            return categoryRepository.findByNameContainingIgnoreCaseAndStatusAndIsDeletedFalse(keyword.trim(), 1, pageable);
        }
        return categoryRepository.findByStatusAndIsDeletedFalse(1, pageable);
    }

    // Hàm search trả về List (nếu ông vẫn dùng)
    @Override
    public List<Category> search(String keyword) {
        return categoryRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse(keyword.trim(), Pageable.unpaged()).getContent();
    }
}