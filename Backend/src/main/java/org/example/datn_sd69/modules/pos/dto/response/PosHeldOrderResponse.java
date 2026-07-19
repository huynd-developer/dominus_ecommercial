package org.example.datn_sd69.modules.pos.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PosHeldOrderResponse {

    private Integer orderId;
    private String status;
    private String paymentMethod;

    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;

    private LocalDateTime createdAt;

    private Integer cashierId;
    private String cashierName;

    private Integer customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;

    private Boolean ownOrder;
    private Boolean canOpen;
    private Boolean canCheckout;
    private Boolean canTransfer;
    private Boolean canCancel;
}
