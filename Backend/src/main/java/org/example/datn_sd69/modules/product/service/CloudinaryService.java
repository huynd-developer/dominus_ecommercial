package org.example.datn_sd69.modules.product.service;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

    String uploadImage(MultipartFile file);

    void deleteImage(String imageUrl);

}