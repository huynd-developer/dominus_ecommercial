package org.example.datn_sd69.modules.customerOrder.service;

import org.example.datn_sd69.modules.customerOrder.dto.request.SubmitDeliveryRefundBankRequest;
import org.example.datn_sd69.modules.customerOrder.dto.response.CustomerOrderResponse;
import org.example.datn_sd69.modules.order.dto.request.CancelOrderRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CustomerOrderService {

    List<CustomerOrderResponse> getMyOrders();

    CustomerOrderResponse getOrderDetail(Integer orderId);

    void cancelOrder(Integer orderId, CancelOrderRequest request);

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

    CustomerOrderResponse submitDeliveryRefundBank(
            Integer orderId,
            SubmitDeliveryRefundBankRequest request
    );
}