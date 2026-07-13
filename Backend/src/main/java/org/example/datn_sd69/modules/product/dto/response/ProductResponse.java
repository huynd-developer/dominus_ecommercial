package org.example.datn_sd69.modules.product.dto.response;

import lombok.Data;

import java.math.BigDecimal;
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

    private String primaryImageUrl;

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

        private Integer stockQuantity;

        private Integer status;
    }
}
