package org.example.datn_sd69.modules.product.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductVariantRequest {

    /**
     * Product cha
     */
    @NotNull(message = "Vui lòng chọn sản phẩm")
    private Integer productId;

    /**
     * Dung tích
     */
    @NotNull(message = "Vui lòng chọn dung tích")
    private Integer capacityId;

    /**
     * Loại chai
     */
    @NotNull(message = "Vui lòng chọn loại chai")
    private Integer bottleTypeId;

    /**
     * SKU
     * Có thể nhập hoặc để trống để hệ thống tự sinh
     */
    private String sku;

    /**
     * Giá bán
     */
    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0", message = "Giá bán phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    /**
     * Tồn kho
     */
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng phải >= 0")
    private Integer stockQuantity;

    /**
     * Trạng thái
     */
    @NotNull(message = "Vui lòng chọn trạng thái")
    @Min(0)
    @Max(1)
    private Integer status;

}