package org.example.datn_sd69.modules.oder.dto.request;

import lombok.Data;

@Data
public class OrderRequest {
    private String customerName;
    private String customerPhone;
    private String shippingAddress;
    private String paymentMethod; // Ví dụ: "COD", "VNPAY"
    private String note;


}
