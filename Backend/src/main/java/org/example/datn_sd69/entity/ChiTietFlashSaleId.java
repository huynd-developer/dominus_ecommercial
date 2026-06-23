package org.example.datn_sd69.entity;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChiTietFlashSaleId implements Serializable {
    private Integer chuongTrinhFlashSale;
    private Integer bienTheSanPham;
}