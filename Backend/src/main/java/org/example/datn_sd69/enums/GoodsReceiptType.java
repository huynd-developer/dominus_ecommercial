package org.example.datn_sd69.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum GoodsReceiptType {

    /**
     * Phiếu nhập kho bình thường.
     */
    NORMAL_RECEIPT(
            (byte) 1,
            "Nhập kho bình thường"
    ),

    /**
     * Phiếu tạo tồn đầu kỳ.
     *
     * Dữ liệu cũ trong DB đang sử dụng ReceiptType = 2,
     * vì vậy enum phải hỗ trợ code này.
     */
    OPENING_BALANCE(
            (byte) 2,
            "Tồn đầu kỳ"
    );

    private final byte code;
    private final String label;

    GoodsReceiptType(
            byte code,
            String label
    ) {
        this.code = code;
        this.label = label;
    }

    public static GoodsReceiptType fromCode(Byte code) {

        if (code == null) {
            return null;
        }

        return Arrays.stream(values())
                .filter(value -> value.code == code)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Loại phiếu nhập không hợp lệ: " + code
                        )
                );
    }
}