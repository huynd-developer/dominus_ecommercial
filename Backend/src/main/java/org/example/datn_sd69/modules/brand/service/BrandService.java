package org.example.datn_sd69.modules.brand.service;

import org.example.datn_sd69.entity.Brand;
import org.example.datn_sd69.modules.brand.dto.request.BrandRequest;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface BrandService {
    List<Brand> getAllBrands();
    Brand createBrand(BrandRequest request);
    Brand updateBrand(Integer id, BrandRequest request);
    void deleteBrand(Integer id);
    String uploadLogo(MultipartFile file) throws IOException;
    Brand getBrandById(Integer id);
    Page<Brand> getBrandsWithPagination(int page, int size);
}