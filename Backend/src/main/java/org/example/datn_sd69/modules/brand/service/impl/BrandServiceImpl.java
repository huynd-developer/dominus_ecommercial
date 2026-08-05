package org.example.datn_sd69.modules.brand.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Brand;
import org.example.datn_sd69.modules.brand.dto.request.BrandRequest;
import org.example.datn_sd69.modules.brand.service.BrandService;
import org.example.datn_sd69.modules.brand.service.BrandCloudinaryServiceImpl;
import org.example.datn_sd69.repository.BrandRepository;
import org.example.datn_sd69.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final BrandCloudinaryServiceImpl brandCloudinaryServiceImpl;

    @Override
    public List<Brand> getAllBrands() {
        return brandRepository.findByIsDeletedFalse();
    }

    @Override
    @Transactional
    public Brand createBrand(BrandRequest request) {
        // Chuẩn hóa khoảng trắng
        String brandName = request.getName().trim().replaceAll("\\s+", " ");

        if (brandRepository.existsByNameIgnoreCaseAndIsDeletedFalse(brandName)) {
            throw new IllegalArgumentException("Thương hiệu '" + brandName + "' đã tồn tại trong hệ thống!");
        }

        Brand brand = new Brand();
        brand.setName(brandName);
        brand.setDescription(request.getDescription());
        brand.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        brand.setIsDeleted(false);
        brand.setLogoUrl(request.getLogoUrl());
        return brandRepository.save(brand);
    }

    @Override
    @Transactional
    public Brand updateBrand(Integer id, BrandRequest request) {
        Brand brand = brandRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thương hiệu!"));

        // Chuẩn hóa khoảng trắng
        String newName = request.getName().trim().replaceAll("\\s+", " ");

        if (brandRepository.existsByNameIgnoreCaseAndIdNotAndIsDeletedFalse(newName, id)) {
            throw new IllegalArgumentException("Tên thương hiệu đã bị trùng với một hãng khác!");
        }

        brand.setName(newName);
        brand.setDescription(request.getDescription());
        brand.setStatus(request.getStatus());
        brand.setLogoUrl(request.getLogoUrl());
        return brandRepository.save(brand);
    }

    @Override
    @Transactional
    public void deleteBrand(Integer id) {
        Brand brand = brandRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thương hiệu!"));

        if (productRepository.existsByBrandIdAndIsDeletedFalse(id)) {
            throw new IllegalArgumentException("Không thể đưa vào thùng rác! Đang có sản phẩm thuộc thương hiệu này.");
        }

        brand.setIsDeleted(true);
        brandRepository.save(brand);
    }

    @Override
    public String uploadLogo(MultipartFile file) throws IOException {
        return brandCloudinaryServiceImpl.uploadFile(file);
    }

    @Override
    public Brand getBrandById(Integer id) {
        return brandRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thương hiệu với ID: " + id));
    }

    @Override
    public Page<Brand> getBrandsWithPagination(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        if (keyword != null && !keyword.trim().isEmpty()) {
            return brandRepository.findByNameContainingIgnoreCaseAndIsDeletedFalse(keyword.trim().replaceAll("\\s+", " "), pageable);
        }
        return brandRepository.findByIsDeletedFalse(pageable);
    }

    @Override
    public Page<Brand> getActiveBrands(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        if (keyword != null && !keyword.trim().isEmpty()) {
            return brandRepository.findByNameContainingIgnoreCaseAndStatusAndIsDeletedFalse(keyword.trim().replaceAll("\\s+", " "), 1, pageable);
        }
        return brandRepository.findByStatusAndIsDeletedFalse(1, pageable);
    }
}