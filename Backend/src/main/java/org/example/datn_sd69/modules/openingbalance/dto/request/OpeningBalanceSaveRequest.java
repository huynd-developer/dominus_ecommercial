package org.example.datn_sd69.modules.openingbalance.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OpeningBalanceSaveRequest {

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String note;

    @NotEmpty(message = "Phiếu kiểm kho phải có ít nhất một sản phẩm")
    @Valid
    private List<OpeningBalanceItemRequest> items;
}