package org.example.datn_sd69.modules.product.service;

import org.example.datn_sd69.modules.product.dto.request.ProductRequest;
import org.example.datn_sd69.modules.product.dto.response.ProductImageResponse;
import org.example.datn_sd69.modules.product.dto.response.ProductResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ProductService {

    ProductResponse getProductById(Integer id);

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Integer id,
                                  ProductRequest request);

    void deleteProduct(Integer id);

    Map<String, Object> getAllProducts(int page,
                                       int size);

    Map<String, Object> getAllProductsAdmin(int page,
                                            int size);

    String uploadImage(
            Integer productId,
            MultipartFile file,
            Boolean isPrimary
    ) throws Exception;

    void deleteProductImage(Integer imageId);

    void setPrimaryImage(Integer productId,
                         Integer imageId);

    List<ProductImageResponse> getProductImages(
            Integer productId
    );
}