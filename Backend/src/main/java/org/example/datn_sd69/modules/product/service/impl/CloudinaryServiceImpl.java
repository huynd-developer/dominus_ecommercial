package org.example.datn_sd69.modules.product.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.product.service.CloudinaryService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    public String uploadImage(MultipartFile file) {

        try {

            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "products"
                    )
            );

            return uploadResult.get("secure_url").toString();

        } catch (Exception e) {

            throw new RuntimeException("Upload ảnh thất bại");

        }

    }

    @Override
    public void deleteImage(String imageUrl) {

        try {

            String publicId = extractPublicId(imageUrl);

            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.emptyMap()
            );

        } catch (Exception ignored) {

        }

    }

    private String extractPublicId(String url) {

        String[] arr = url.split("/");

        String file = arr[arr.length - 1];

        return "products/" + file.substring(0, file.lastIndexOf("."));

    }

}