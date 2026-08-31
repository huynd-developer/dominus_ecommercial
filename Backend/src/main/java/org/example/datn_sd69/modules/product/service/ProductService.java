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

    /*
     * Xóa mềm sản phẩm.
     * Không xóa record Product khỏi database.
     */
    void deleteProduct(Integer id);

    /*
     * Khôi phục sản phẩm đã xóa mềm.
     */
    void restoreProduct(Integer id);

    Map<String, Object> getAllProducts(int page,
                                       int size);

    Map<String, Object> getAllProductsAdmin(int page,
                                            int size);

    /*
     * Danh sách sản phẩm đã xóa mềm để admin có thể khôi phục.
     */
    Map<String, Object> getDeletedProductsAdmin(int page,
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