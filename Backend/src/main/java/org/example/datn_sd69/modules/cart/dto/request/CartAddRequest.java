package org.example.datn_sd69.modules.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartAddRequest {

    @NotNull(message = "Biến thể sản phẩm không được để trống")
    @Min(value = 1, message = "Biến thể sản phẩm không hợp lệ")
    private Integer productVariantId;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;

    @Size(max = 255, message = "Ghi chú tối đa 255 ký tự")
    private String note;

    @Size(max = 500, message = "Ảnh sản phẩm tối đa 500 ký tự")
    private String thumbnailUrl;
}