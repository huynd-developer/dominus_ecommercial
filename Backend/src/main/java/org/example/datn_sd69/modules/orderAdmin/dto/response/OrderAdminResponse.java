package org.example.datn_sd69.modules.orderAdmin.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderAdminResponse {
    private Integer id;
    private String customerName;
    private String customerPhone;
    private String orderType; // ONLINE hoặc IN_STORE
    private BigDecimal totalAmount;
    private BigDecimal finalAmount;
    private Integer status;
    private LocalDateTime createdAt;
}