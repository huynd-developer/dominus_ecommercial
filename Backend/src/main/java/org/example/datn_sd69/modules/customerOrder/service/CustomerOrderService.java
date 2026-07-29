package org.example.datn_sd69.modules.customerOrder.service;

import org.example.datn_sd69.modules.customerOrder.dto.CustomerOrderResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CustomerOrderService {

    List<CustomerOrderResponse> getMyOrders();

    CustomerOrderResponse getOrderDetail(Integer orderId);

    void cancelOrder(Integer orderId);

    void requestReturnOrder(
            Integer orderId,
            String returnType,
            String reason,
            String description,
            String email,
            String refundMethod,
            String bankName,
            String bankAccountNumber,
            String bankAccountHolder,
            String returnItemsJson,
            List<MultipartFile> mediaFiles
    );

    void cancelReturnRequest(Integer orderId);
}
