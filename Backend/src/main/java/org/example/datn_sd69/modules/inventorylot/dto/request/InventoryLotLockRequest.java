package org.example.datn_sd69.modules.inventorylot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryLotLockRequest {

    @NotBlank(message = "Bắt buộc nhập lý do khóa lô")
    @Size(max = 500, message = "Lý do khóa lô không được vượt quá 500 ký tự")
    private String reason;
}
