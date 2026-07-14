package org.example.datn_sd69.modules.customerOrder.service;

import org.example.datn_sd69.modules.customerOrder.dto.CustomerOrderResponse;

import java.util.List;

public interface CustomerOrderService {

    List<CustomerOrderResponse> getMyOrders();

    CustomerOrderResponse getOrderDetail(Integer orderId);

    void cancelOrder(Integer orderId);
}