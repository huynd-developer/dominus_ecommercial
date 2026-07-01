package org.example.datn_sd69.modules.brand.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.datn_sd69.entity.Brand;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BrandResponse {
    private Integer id;
    private String name;
    private String description;
    private String logoUrl; // Thêm trường này để hiển thị ảnh
    private Integer status;

    public BrandResponse(Brand brand) {
        this.id = brand.getId();
        this.name = brand.getName();
        this.description = brand.getDescription();
        this.logoUrl = brand.getLogoUrl(); // Map link ảnh từ Entity sang DTO
        this.status = brand.getStatus();
    }
}