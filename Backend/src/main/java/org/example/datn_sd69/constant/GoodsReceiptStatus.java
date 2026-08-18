package org.example.datn_sd69.constant;

public final class GoodsReceiptStatus {

    private GoodsReceiptStatus() {
    }

    public static final byte DRAFT = 0;
    public static final byte PENDING_APPROVAL = 1;
    public static final byte APPROVED = 2;
    public static final byte REJECTED = 3;
    public static final byte CANCELLED = 4;
}