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
    private Integer id; // Cột ID này sẽ được lấy từ BaseEntity
    private String name;
    private String description;
    private Integer status;

    // Hàm khởi tạo giúp convert siêu nhanh từ Entity sang DTO trong Service
    public BrandResponse(Brand brand) {
        this.id = brand.getId();
        this.name = brand.getName();
        this.description = brand.getDescription();
        this.status = brand.getStatus();
    }
}
