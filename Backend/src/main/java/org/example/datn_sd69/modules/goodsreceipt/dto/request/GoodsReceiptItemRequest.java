package org.example.datn_sd69.modules.goodsreceipt.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class GoodsReceiptItemRequest {

    public static final int MAX_QUANTITY_PER_LINE = 1_000_000;
    public static final String MAX_UNIT_COST = "1000000000.00";

    @NotNull(message = "ProductVariantId không được để trống")
    @Positive(message = "ProductVariantId phải lớn hơn 0")
    private Integer productVariantId;

    /*
     * Phiếu nhập thường:
     * - FE không cần gửi.
     * - GoodsReceiptServiceImpl tự sinh mã lô.
     *
     * Opening Balance:
     * - Vẫn có thể set mã lô vào DTO này.
     *
     * Vì vậy giữ field nhưng KHÔNG dùng @NotBlank.
     */
    @Size(max = 100, message = "Mã lô không được vượt quá 100 ký tự")
    private String lotCode;

    @NotNull(message = "Số lượng không được để trống")
    @Positive(message = "Số lượng phải lớn hơn 0")
    @Max(
            value = MAX_QUANTITY_PER_LINE,
            message = "Số lượng mỗi SKU không được vượt quá 1.000.000"
    )
    private Integer quantity;

    /*
     * Đơn giá được phép để trống theo logic hiện tại.
     * Nếu có nhập:
     * - không âm;
     * - tối đa 2 chữ số thập phân;
     * - giới hạn ở 1 tỷ đồng/đơn vị để tránh dữ liệu nhập sai bất thường.
     */
    @DecimalMin(
            value = "0.00",
            inclusive = true,
            message = "Đơn giá nhập phải lớn hơn hoặc bằng 0"
    )
    @DecimalMax(
            value = MAX_UNIT_COST,
            inclusive = true,
            message = "Đơn giá nhập không được vượt quá 1.000.000.000"
    )
    @Digits(
            integer = 10,
            fraction = 2,
            message = "Đơn giá nhập chỉ được tối đa 10 chữ số phần nguyên và 2 chữ số thập phân"
    )
    private BigDecimal unitCost;

    private LocalDate manufacturedDate;

    /*
     * Phiếu nhập thường:
     * - FE không gửi.
     * - GoodsReceiptServiceImpl tự lấy ngày tạo phiếu.
     *
     * Opening Balance:
     * - Vẫn có thể set receivedDate.
     *
     * Vì vậy giữ field nhưng KHÔNG dùng @NotNull.
     */
    private LocalDate receivedDate;

    @NotNull(message = "Hạn sử dụng không được để trống")
    private LocalDate expirationDate;

    @Size(
            max = 500,
            message = "Ghi chú dòng không được vượt quá 500 ký tự"
    )
    private String note;
}
