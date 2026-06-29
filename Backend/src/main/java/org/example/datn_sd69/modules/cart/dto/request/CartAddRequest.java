package org.example.datn_sd69.modules.cart.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartAddRequest {
    private Integer productVariantId;
    private Integer quantity;
}
