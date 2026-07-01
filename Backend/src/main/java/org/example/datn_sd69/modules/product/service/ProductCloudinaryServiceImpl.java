package org.example.datn_sd69.modules.product.service;

import org.springframework.web.multipart.MultipartFile;

public interface ProductCloudinaryServiceImpl {

    String uploadImage(MultipartFile file);

    void deleteImage(String imageUrl);

}