package org.example.datn_sd69.modules.product.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Data
public class ProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm không được vượt quá 255 ký tự")
    private String name;

    private String description;

    @NotNull(message = "Brand không được để trống")
    private Integer brandId;

    @NotNull(message = "Category không được để trống")
    private Integer categoryId;

    @NotNull(message = "Concentration không được để trống")
    private Integer concentrationId;

    /**
     * 1 = Nam
     * 2 = Nữ
     * 3 = Unisex
     */
    private Integer gender;

    private Boolean isNiche = false;

    private Integer status = 1;

    private Set<Integer> fragranceFamilyIds;

    @Valid
    @NotEmpty(message = "Sản phẩm phải có ít nhất 1 biến thể")
    private List<VariantRequestDTO> variants;

    @Data
    public static class VariantRequestDTO {

        private Integer id;

        @NotNull(message = "Dung tích không được để trống")
        private Integer capacityId;

        @NotNull(message = "Loại chai không được để trống")
        private Integer bottleTypeId;

        @NotBlank(message = "SKU không được để trống")
        @Size(max = 100, message = "SKU không được vượt quá 100 ký tự")
        private String sku;

        @NotNull(message = "Giá không được để trống")
        @DecimalMin(
                value = "0",
                inclusive = true,
                message = "Giá phải lớn hơn hoặc bằng 0"
        )
        private BigDecimal price;

        @NotNull(message = "Số lượng tồn không được để trống")
        @Min(
                value = 0,
                message = "Tồn kho không được âm"
        )
        private Integer stockQuantity;

        private Integer status = 1;
    }
}
