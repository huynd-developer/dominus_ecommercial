package org.example.datn_sd69.modules.pos.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Locale;

@Data
public class PosItemRequest {

    @NotBlank(message = "SKU sản phẩm không được để trống")
    private String sku;

    @NotNull(message = "Số lượng sản phẩm không được để trống")
    @Min(value = 1, message = "Số lượng sản phẩm phải lớn hơn 0")
    @Max(value = 10, message = "Mỗi sản phẩm chỉ được mua tối đa 10 lọ trong một đơn hàng")
    private Integer quantity;

    public void setSku(String sku) {
        if (sku == null || sku.trim().isBlank()) {
            this.sku = null;
            return;
        }

        this.sku = sku.trim().toUpperCase(Locale.ROOT);
    }
}