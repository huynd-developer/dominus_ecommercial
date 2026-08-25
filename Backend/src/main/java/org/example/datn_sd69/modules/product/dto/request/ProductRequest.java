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

    /**
     * Revision snapshot dùng khi cập nhật để phát hiện form cũ/stale.
     * Nullable để giữ compatibility cho client cũ; FE admin mới phải gửi field này khi PUT.
     */
    @Size(max = 64, message = "Revision sản phẩm không hợp lệ")
    private String expectedRevision;

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

        private String sku;

        @NotNull(message = "Giá không được để trống")
        @DecimalMin(
                value = "0",
                inclusive = true,
                message = "Giá phải lớn hơn hoặc bằng 0"
        )
        private BigDecimal price;

        /**
         * LEGACY.
         * Không dùng ProductVariant.StockQuantity làm tồn kho thực tế nữa.
         * Tồn kho được quản lý theo InventoryLot.
         */
        private Integer stockQuantity;

        /**
         * LEGACY.
         * NSX thuộc từng lô nhập, không thuộc SKU.
         */
        private LocalDate manufacturingDate;

        /**
         * LEGACY.
         * HSD thuộc từng lô nhập, không thuộc SKU.
         */
        private LocalDate expirationDate;

        private Integer status = 1;
    }
}
