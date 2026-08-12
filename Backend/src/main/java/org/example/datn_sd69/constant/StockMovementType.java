package org.example.datn_sd69.constant;

public final class StockMovementType {

    private StockMovementType() {
    }

    public static final byte RECEIPT_IN = 1;
    public static final byte OPENING_IN = 2;

    public static final byte SALE_OUT = 3;

    public static final byte RETURN_IN = 4;

    public static final byte ADJUST_IN = 5;
    public static final byte ADJUST_OUT = 6;

    public static final byte DISPOSAL_OUT = 7;
}