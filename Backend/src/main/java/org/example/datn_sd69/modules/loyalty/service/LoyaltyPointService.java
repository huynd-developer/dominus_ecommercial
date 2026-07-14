package org.example.datn_sd69.modules.loyalty.service;

import org.example.datn_sd69.entity.Order;

public interface LoyaltyPointService {

    void applyPointsWhenOrderCompleted(Order order);

    int calculateEarnedPoints(Order order);
}
