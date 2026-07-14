package org.example.datn_sd69.modules.loyalty.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Customer;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.modules.loyalty.service.LoyaltyPointService;
import org.example.datn_sd69.repository.CustomerRepository;
import org.example.datn_sd69.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LoyaltyPointServiceImpl implements LoyaltyPointService {

    private static final int ORDER_STATUS_COMPLETED = 3;

    /**
     * Quy đổi điểm:
     * 100.000 VND = 1 điểm
     */
    private static final BigDecimal VND_PER_POINT = BigDecimal.valueOf(100_000);

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public void applyPointsWhenOrderCompleted(Order order) {
        if (order == null) {
            return;
        }

        if (!Objects.equals(order.getStatus(), ORDER_STATUS_COMPLETED)) {
            return;
        }

        if (Boolean.TRUE.equals(order.getLoyaltyPointsApplied())) {
            return;
        }

        Customer customer = order.getCustomer();

        if (customer == null) {
            order.setLoyaltyPointsApplied(true);
            order.setLoyaltyPointsEarned(0);

            if (order.getCompletedAt() == null) {
                order.setCompletedAt(LocalDateTime.now());
            }

            orderRepository.save(order);
            return;
        }

        int earnedPoints = calculateEarnedPoints(order);

        int currentPoints = customer.getLoyaltyPoints() == null
                ? 0
                : customer.getLoyaltyPoints();

        int newTotalPoints = currentPoints + earnedPoints;

        customer.setLoyaltyPoints(newTotalPoints);
        customer.setCustomerRank(calculateRank(newTotalPoints));

        order.setLoyaltyPointsApplied(true);
        order.setLoyaltyPointsEarned(earnedPoints);

        if (order.getCompletedAt() == null) {
            order.setCompletedAt(LocalDateTime.now());
        }

        customerRepository.save(customer);
        orderRepository.save(order);
    }

    @Override
    public int calculateEarnedPoints(Order order) {
        if (order == null || order.getFinalAmount() == null) {
            return 0;
        }

        BigDecimal finalAmount = order.getFinalAmount();

        if (finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        return finalAmount
                .divide(VND_PER_POINT, 0, RoundingMode.DOWN)
                .intValue();
    }

    private String calculateRank(int loyaltyPoints) {
        if (loyaltyPoints >= 1000) {
            return "Diamond";
        }

        if (loyaltyPoints >= 500) {
            return "Gold";
        }

        if (loyaltyPoints >= 200) {
            return "Silver";
        }

        return "Bronze";
    }
}
