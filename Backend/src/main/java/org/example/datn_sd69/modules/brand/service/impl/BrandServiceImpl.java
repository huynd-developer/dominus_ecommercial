package org.example.datn_sd69.modules.brand.service.impl;

// Đã xóa các import com.cloudinary thừa thãi vì không cần dùng trực tiếp ở đây nữa
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
    private final CloudinaryService cloudinaryService; // Gọi Service trung gian

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

    // CHỈNH LẠI: Gọn gàng 1 dòng duy nhất, phó thác việc upload cho CloudinaryService
    @Override
    public String uploadLogo(MultipartFile file) throws IOException {
        return cloudinaryService.uploadFile(file);
    }
    @Override
    public Brand getBrandById(Integer id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thương hiệu với ID: " + id));
    }
    @Override
    public Page<Brand> getBrandsWithPagination(int page, int size) {
        // Tạo điều kiện phân trang: trang số mấy, số lượng bao nhiêu, sắp xếp ID giảm dần
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        // Hàm findAll(pageable) có sẵn trong JpaRepository sẽ tự xử lý câu lệnh SQL phân trang
        return brandRepository.findAll(pageable);
    }

}