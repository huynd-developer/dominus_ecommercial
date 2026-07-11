package org.example.datn_sd69.modules.pos.dto.request;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PosTransferHeldOrderRequest {

    @NotNull(message = "Nhân viên nhận phiếu không được để trống")
    private Integer targetEmployeeId;
}
