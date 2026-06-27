package org.example.datn_sd69.modules.brand.service;

import org.example.datn_sd69.entity.Brand;
import org.example.datn_sd69.modules.brand.dto.request.BrandRequest;

import java.util.List;

public interface BrandService {
    List<Brand> getAllBrands();
    Brand createBrand(BrandRequest request);
    Brand updateBrand(Integer id, BrandRequest request);
    void deleteBrand(Integer id);
}