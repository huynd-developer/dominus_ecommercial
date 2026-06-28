package org.example.datn_sd69.modules.brand.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Brand;
import org.example.datn_sd69.modules.brand.dto.request.BrandRequest;
import org.example.datn_sd69.modules.brand.service.BrandService;
import org.example.datn_sd69.repository.BrandRepository;
import org.example.datn_sd69.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    @Override
    public List<Brand> getAllBrands() {
        return brandRepository.findAll();
    }

    @Override
    @Transactional
    public Brand createBrand(BrandRequest request) {
        // Kiểm tra xem tên có bị trùng không (bỏ qua những cái đã xóa mềm)
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

        // Kiểm tra trùng tên với hãng khác (loại trừ chính ID đang sửa và những cái đã xóa mềm)
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

        // Chặn xóa nếu đang có sản phẩm thuộc thương hiệu này
        if (productRepository.existsByBrandIdAndStatusNot(id, 0)) {
            throw new RuntimeException("Không thể khóa! Đang có sản phẩm hoạt động thuộc thương hiệu này.");
        }

        // Thực thi Xóa mềm
        brand.setStatus(0);
        brandRepository.save(brand);
    }
}
