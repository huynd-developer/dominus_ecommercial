package org.example.datn_sd69.modules.inventorylot.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryLotUnlockRequest {

    @Size(max = 500, message = "Lý do mở khóa không được vượt quá 500 ký tự")
    private String reason;
}
