package org.example.datn_sd69.modules.brand.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Brand;
import org.example.datn_sd69.modules.brand.dto.request.BrandRequest;
import org.example.datn_sd69.modules.brand.service.BrandService;
import org.example.datn_sd69.modules.brand.service.CloudinaryService;
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
    private final CloudinaryService cloudinaryService;

    @Override
    public List<Brand> getAllBrands() {
        return brandRepository.findAll();
    }

    @Override
    @Transactional
    public Brand createBrand(BrandRequest request) {
        if (brandRepository.existsByNameIgnoreCaseAndStatusNot(request.getName().trim(), 0)) {
            throw new RuntimeException("Thương hiệu '" + request.getName() + "' đã tồn tại và đang hoạt động!");
        }

        Brand brand = new Brand();
        brand.setName(request.getName().trim());
        brand.setDescription(request.getDescription());
        brand.setStatus(request.getStatus() != null ? request.getStatus() : 1);

        return brandRepository.save(brand);
    }

    @Override
    @Transactional
    public Brand updateBrand(Integer id, BrandRequest request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu!"));

        if (brandRepository.existsByNameIgnoreCaseAndIdNotAndStatusNot(request.getName().trim(), id, 0)) {
            throw new RuntimeException("Tên thương hiệu đã bị trùng với một hãng khác đang hoạt động!");
        }

        brand.setName(request.getName().trim());
        brand.setDescription(request.getDescription());
        brand.setStatus(request.getStatus());

        return brandRepository.save(brand);
    }

    @Override
    @Transactional
    public void deleteBrand(Integer id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu!"));

        if (productRepository.existsByBrandIdAndStatusNot(id, 0)) {
            throw new RuntimeException("Không thể khóa! Đang có sản phẩm hoạt động thuộc thương hiệu này.");
        }

        brand.setStatus(0);
        brandRepository.save(brand);
    }

    @Override
    public String uploadLogo(MultipartFile file) throws IOException {
        return cloudinaryService.uploadFile(file);
    }

    @Override
    public Brand getBrandById(Integer id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu với ID: " + id));
    }

    // 1. Hàm cho Admin: Kết hợp Tìm kiếm và Phân trang (Lấy tất cả trạng thái)
    @Override
    public Page<Brand> getBrandsWithPagination(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        if (keyword != null && !keyword.trim().isEmpty()) {
            // Dòng 97: Sửa thành gọi hàm mới, truyền thêm tham số 0 (nghĩa là loại bỏ status = 0)
            return brandRepository.findByNameContainingIgnoreCaseAndStatusNot(keyword.trim(), 0, pageable);
        }

        // Dòng 99: Thay findAll() bằng hàm mới, cũng truyền tham số 0 để loại bỏ đồ đã xóa
        return brandRepository.findByStatusNot(0, pageable);
    }

    // 2. Hàm cho Khách: Lấy danh sách đang hoạt động (Status = 1)
    @Override
    public Page<Brand> getActiveBrands(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        if (keyword != null && !keyword.trim().isEmpty()) {
            return brandRepository.findByNameContainingIgnoreCaseAndStatus(keyword.trim(), 1, pageable);
        }
        return brandRepository.findByStatus(1, pageable);
    }
}