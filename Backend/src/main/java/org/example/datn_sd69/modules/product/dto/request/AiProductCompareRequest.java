package org.example.datn_sd69.modules.product.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AiProductCompareRequest {

    @NotEmpty(message = "Danh sách sản phẩm không được để trống")
    @Size(
            min = 2,
            max = 3,
            message = "Chỉ được so sánh từ 2 đến 3 sản phẩm"
    )
    private List<
            @NotNull(message = "ProductId không được để trống")
            @Positive(message = "ProductId phải lớn hơn 0")
                    Integer
            > productIds;
}