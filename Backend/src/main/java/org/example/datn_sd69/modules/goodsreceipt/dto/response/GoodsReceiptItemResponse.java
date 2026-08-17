package org.example.datn_sd69.modules.goodsreceipt.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptItemResponse {

    private Integer id;

    private Integer productVariantId;

    private String sku;

    private String productName;

    private String imageUrl;

    // Chỉ bổ sung metadata để nhận diện biến thể trên màn chi tiết.
    private Double capacityValue;

    private String bottleTypeName;

    private String lotCode;

    private Integer quantity;

    private BigDecimal unitCost;

    private LocalDate manufacturedDate;

    private LocalDate receivedDate;

    private LocalDate expirationDate;

    // Giữ field để tương thích dữ liệu/API cũ, chỉ không hiển thị ở FE.
    private String note;
}