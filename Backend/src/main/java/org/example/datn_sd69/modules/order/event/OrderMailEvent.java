package org.example.datn_sd69.modules.order.event;

public record OrderMailEvent(
        Integer orderId,
        Type type
) {

    public enum Type {
        RETURN_ACCEPTED,
        RETURN_REFUNDED
    }
}