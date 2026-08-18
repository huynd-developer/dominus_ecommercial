package org.example.datn_sd69.modules.product.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
public class ProductResponse {
    private Integer id;

    private String name;

    private String description;

    private Integer brandId;
    private String brandName;

    private Integer categoryId;
    private String categoryName;

    private Integer concentrationId;
    private String concentrationName;

    private Integer gender;

    private Boolean isNiche;

    private Integer status;

    private Double rating;
    private Long reviewCount;

    private String primaryImageUrl;

    private List<ProductImageResponse> images;

    private Set<FragranceFamilyDTO> fragranceFamilies;

    private List<VariantDTO> variants;

    @Data
    public static class FragranceFamilyDTO {
        private Integer id;
        private String name;
    }

    @Data
    public static class VariantDTO {
        private Integer id;
        private Integer capacityId;
        private String capacityName;
        private Integer bottleTypeId;
        private String bottleTypeName;
        private String sku;
        private BigDecimal price;

        /**
         * LEGACY: giữ để không làm vỡ client/module cũ.
         * Không dùng làm nguồn tồn kho thật.
         */
        private Integer stockQuantity;

        /** LEGACY: NSX thật thuộc InventoryLot/GoodsReceiptItem. */
        private LocalDate manufacturingDate;

        /** LEGACY: HSD thật thuộc InventoryLot/GoodsReceiptItem. */
        private LocalDate expirationDate;

        /** Tổng tồn vật lý thật của SKU. */
        private Long totalQuantity;

        /** Số lượng hiện có thể bán của SKU. */
        private Long sellableQuantity;

        private Integer status;
    }
}