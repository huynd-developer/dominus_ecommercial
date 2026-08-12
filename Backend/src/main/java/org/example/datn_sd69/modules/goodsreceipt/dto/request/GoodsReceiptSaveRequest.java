package org.example.datn_sd69.modules.goodsreceipt.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.example.datn_sd69.enums.GoodsReceiptType;

import java.util.List;

@Getter
@Setter
public class GoodsReceiptSaveRequest {

    @NotNull(message = "Loại phiếu nhập không được để trống")
    private GoodsReceiptType receiptType;

    @Size(max = 1000, message = "Ghi chú phiếu không được vượt quá 1000 ký tự")
    private String note;

    @NotNull(message = "Danh sách sản phẩm không được để trống")
    @Size(min = 1, message = "Phiếu nhập phải có ít nhất một sản phẩm")
    @Valid
    private List<GoodsReceiptItemRequest> items;
}
