package org.example.datn_sd69.modules.product.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
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

    /**
     * Không cho phép 1 sản phẩm có nhiều biến thể trùng cả Dung tích và Loại chai.
     * Ví dụ: được phép có nhiều dòng cùng 10ml, nhưng không được có 2 dòng giống hệt cả 10ml và cùng loại chai Fullbox.
     */
    @AssertTrue(message = "Không được phép có 2 biến thể trùng cả Dung tích và Loại chai trong cùng một sản phẩm")
    public boolean isVariantCapacityUnique() {
        if (variants == null || variants.isEmpty()) {
            return true;
        }

        Set<String> variantPairSet = new HashSet<>();

        for (VariantRequestDTO variant : variants) {
            if (variant == null || variant.getCapacityId() == null || variant.getBottleTypeId() == null) {
                continue;
            }

            String pairKey = variant.getCapacityId() + "-" + variant.getBottleTypeId();

            if (!variantPairSet.add(pairKey)) {
                return false;
            }
        }

        return true;
    }

    @Data
    public static class VariantRequestDTO {

        private Integer id;

        @NotNull(message = "Dung tích không được để trống")
        private Integer capacityId;

        @NotNull(message = "Loại chai không được để trống")
        private Integer bottleTypeId;

        // @NotBlank(message = "SKU không được để trống")
        // @Size(max = 100, message = "SKU không được vượt quá 100 ký tự")
        private String sku; // BẮT BUỘC phải giữ lại dòng này để Service còn đọc được dữ liệu

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

        @NotNull(message = "Ngày sản xuất không được để trống")
        private LocalDate manufacturingDate;

        @NotNull(message = "Hạn sử dụng không được để trống")
        private LocalDate expirationDate;

        private Integer status = 1;
    }
}