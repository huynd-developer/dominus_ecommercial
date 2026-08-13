package org.example.datn_sd69.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum InventoryLotLockActionType {
    LOCK((byte) 1, "Khóa lô"),
    UNLOCK((byte) 2, "Mở khóa lô");

    private final byte code;
    private final String label;

    InventoryLotLockActionType(byte code, String label) {
        this.code = code;
        this.label = label;
    }

    public static InventoryLotLockActionType fromCode(Byte code) {
        if (code == null) {
            return null;
        }

        return Arrays.stream(values())
                .filter(value -> value.code == code)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Loại thao tác khóa lô không hợp lệ: " + code)
                );
    }
}
