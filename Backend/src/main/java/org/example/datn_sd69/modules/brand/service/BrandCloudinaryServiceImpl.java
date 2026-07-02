package org.example.datn_sd69.modules.brand.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface BrandCloudinaryServiceImpl {
    String uploadFile(MultipartFile file) throws IOException;
}
