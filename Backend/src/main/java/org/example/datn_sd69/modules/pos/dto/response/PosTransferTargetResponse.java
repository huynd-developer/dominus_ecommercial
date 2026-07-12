package org.example.datn_sd69.modules.pos.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PosTransferTargetResponse {
    private Integer employeeId;

    private String employeeCode;
    private String name;
    private String email;
    private String phone;
    private String roleName;
    private String jobTitle;
}