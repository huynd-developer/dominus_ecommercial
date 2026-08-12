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
    private String lotCode;
    private Integer quantity;
    private BigDecimal unitCost;
    private LocalDate manufacturedDate;
    private LocalDate receivedDate;
    private LocalDate expirationDate;
    private String note;
}
