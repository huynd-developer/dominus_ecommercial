package org.example.datn_sd69.modules.product.service;

import org.example.datn_sd69.modules.product.dto.ProductRequest;
import org.example.datn_sd69.modules.product.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {

    Page<ProductResponse> getAll(
            String keyword,
            int page,
            int size
    );

    ProductResponse getById(Integer id);

    ProductResponse create(

            ProductRequest request,

            MultipartFile primaryImage,

            List<MultipartFile> images

    );

    ProductResponse update(

            Integer id,

            ProductRequest request,

            MultipartFile primaryImage,

            List<MultipartFile> images

    );

    void delete(Integer id);

}