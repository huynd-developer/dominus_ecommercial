package org.example.datn_sd69.enums;

import lombok.Getter;

@Getter
public enum StockAdjustmentStatus {

    DRAFT((byte) 0, "Lưu tạm"),
    PENDING_APPROVAL((byte) 1, "Chờ duyệt"),
    APPROVED((byte) 2, "Đã phê duyệt"),
    REJECTED((byte) 3, "Đã từ chối"),
    CANCELLED((byte) 4, "Đã hủy");

    private final byte code;
    private final String label;

    StockAdjustmentStatus(byte code, String label) {
        this.code = code;
        this.label = label;
    }

    public static StockAdjustmentStatus fromCode(Byte code) {
        if (code == null) {
            return null;
        }

        for (StockAdjustmentStatus value : values()) {
            if (value.code == code) {
                return value;
            }
        }

        throw new IllegalArgumentException(
                "StockAdjustmentStatus không hợp lệ: " + code
        );
    }
}