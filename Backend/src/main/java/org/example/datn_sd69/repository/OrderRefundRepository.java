package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.OrderRefund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRefundRepository extends JpaRepository<OrderRefund, Integer> {
    Optional<OrderRefund> findByOrderIdAndRefundType(Integer orderId, String refundType);
}