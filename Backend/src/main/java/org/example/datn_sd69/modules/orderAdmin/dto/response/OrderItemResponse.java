package org.example.datn_sd69.modules.orderAdmin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

    /**
     * ID OrderItem
     */
    private Integer id;

    /**
     * Variant
     */
    private Integer productVariantId;

    /**
     * Product
     */
    private Integer productId;

    /**
     * Tên sản phẩm
     */
    private String productName;

    /**
     * SKU
     */
    private String sku;

    /**
     * Dung tích
     */
    private Integer capacityId;

    private String capacityName;

    /**
     * Loại vỏ
     */
    private Integer bottleTypeId;

    private String bottleTypeName;

    /**
     * Hình ảnh
     */
    private String image;

    /**
     * Số lượng
     */
    private Integer quantity;

    /**
     * Giá gốc
     */
    private BigDecimal originalPrice;

    /**
     * Giảm giá
     */
    private BigDecimal discountAmount;

    /**
     * Giá cuối cùng
     */
    private BigDecimal finalPrice;

    /**
     * Thành tiền của dòng sản phẩm
     */
    private BigDecimal lineTotal;

    /**
     * Ghi chú
     */
    private String note;

}