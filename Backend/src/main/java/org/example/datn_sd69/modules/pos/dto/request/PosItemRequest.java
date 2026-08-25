package org.example.datn_sd69.modules.pos.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Locale;

@Data
public class PosItemRequest {

    @NotBlank(message = "SKU sản phẩm không được để trống")
    private String sku;

    @NotNull(message = "Số lượng sản phẩm không được để trống")
    @Min(value = 1, message = "Số lượng sản phẩm phải lớn hơn 0")
    private Integer quantity;

    /*
     * Giá mà FE đang hiển thị cho SKU tại lúc submit.
     * Backend chỉ dùng để phát hiện stale; giá bán thật vẫn lấy ProductVariant.Price.
     */
    @DecimalMin(value = "0.00", message = "Đơn giá dự kiến không được âm")
    @Digits(integer = 18, fraction = 2, message = "Đơn giá dự kiến không hợp lệ")
    private BigDecimal expectedUnitPrice;

    public void setSku(String sku) {
        if (sku == null || sku.trim().isBlank()) {
            this.sku = null;
            return;
        }

        this.sku = sku.trim().toUpperCase(Locale.ROOT);
    }
}
