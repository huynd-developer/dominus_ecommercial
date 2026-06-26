package org.example.datn_sd69.modules.brand.service;

import org.example.datn_sd69.entity.Brand;
import org.example.datn_sd69.modules.brand.request.BrandRequest;
import org.example.datn_sd69.modules.brand.response.BrandResponse;
import org.example.datn_sd69.repository.BrandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service

public class BrandService {
    @Autowired
    private BrandRepository brandRepository;

    // 1. Phân trang & Tìm kiếm
    public Page<BrandResponse> searchAndPaginate(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Brand> brandPage;

        if (keyword == null || keyword.trim().isEmpty()) {
            brandPage = brandRepository.findAll(pageable);
        } else {
            brandPage = brandRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);
        }

        // Map từ Entity sang Response
        return brandPage.map(BrandResponse::new);
    }

    public BrandResponse getById(Integer id) {
        Brand brand = brandRepository.findById(id).orElse(null);
        return brand != null ? new BrandResponse(brand) : null;
    }

    public BrandResponse create(BrandRequest request) {
        Brand brand = new Brand();
        brand.setName(request.getName());
        brand.setDescription(request.getDescription());
        brand.setStatus(request.getStatus() != null ? request.getStatus() : 1);

        Brand saved = brandRepository.save(brand);
        return new BrandResponse(saved);
    }

    public BrandResponse update(Integer id, BrandRequest request) {
        Brand brand = brandRepository.findById(id).orElse(null);
        if (brand == null) {
            return null;
        }
        brand.setName(request.getName());
        brand.setDescription(request.getDescription());
        brand.setStatus(request.getStatus());

        Brand updated = brandRepository.save(brand);
        return new BrandResponse(updated);
    }

    // 2. Xóa mềm (Cập nhật Status = 0)
    public boolean softDelete(Integer id) {
        Brand brand = brandRepository.findById(id).orElse(null);
        if (brand == null) {
            return false;
        }
        brand.setStatus(0); // 0 là Ngừng hoạt động (hoặc bị xóa)
        brandRepository.save(brand);
        return true;
    }
}
