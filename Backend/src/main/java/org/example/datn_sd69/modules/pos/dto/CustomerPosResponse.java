package org.example.datn_sd69.modules.pos.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerPosResponse {

    private Integer customerId;
    private String name;
    private String phone;
    private String email;
    private String customerRank;
    private Integer loyaltyPoints;
}