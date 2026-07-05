package org.example.datn_sd69.modules.orderStatus.service;

import org.example.datn_sd69.entity.Order;

public interface AdminOrderStatusService {

    Order updateStatus(Integer orderId, Integer newStatus);
}
