package org.example.datn_sd69.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum GoodsReceiptStatus {
    DRAFT((byte) 0, "Lưu tạm"),
    PENDING_APPROVAL((byte) 1, "Chờ duyệt"),
    APPROVED((byte) 2, "Đã duyệt"),
    REJECTED((byte) 3, "Từ chối"),
    CANCELLED((byte) 4, "Đã hủy");

    private final byte code;
    private final String label;

    GoodsReceiptStatus(byte code, String label) {
        this.code = code;
        this.label = label;
    }

    public static GoodsReceiptStatus fromCode(Byte code) {
        if (code == null) {
            return null;
        }

        return Arrays.stream(values())
                .filter(value -> value.code == code)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Trạng thái phiếu nhập không hợp lệ: " + code)
                );
    }
}
