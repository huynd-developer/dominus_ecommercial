package org.example.datn_sd69.enums;

import lombok.Getter;

@Getter
public enum StockMovementType {

    RECEIPT_IN((byte) 1, "Nhập kho"),

    OPENING_IN((byte) 2, "Tồn đầu kỳ"),

    SALE_OUT((byte) 3, "Xuất bán"),

    RETURN_IN((byte) 4, "Nhập trả hàng"),

    ADJUST_IN((byte) 5, "Điều chỉnh tăng"),

    ADJUST_OUT((byte) 6, "Điều chỉnh giảm"),

    DISPOSAL_OUT((byte) 7, "Xuất hủy");

    private final Byte code;

    private final String label;

    StockMovementType(
            Byte code,
            String label
    ) {
        this.code = code;
        this.label = label;
    }

    public static StockMovementType fromCode(Byte code) {

        if (code == null) {
            return null;
        }

        for (StockMovementType type : values()) {

            if (type.code.equals(code)) {
                return type;
            }
        }

        return null;
    }
}