package org.example.datn_sd69.modules.orderAdmin.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException() {
        super("Không tìm thấy đơn hàng.");
    }

    public OrderNotFoundException(String message) {
        super(message);
    }

}
