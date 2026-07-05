package org.example.datn_sd69.modules.orderStatus.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.modules.loyalty.service.LoyaltyPointService;
import org.example.datn_sd69.modules.orderStatus.service.AdminOrderStatusService;
import org.example.datn_sd69.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminOrderStatusServiceImpl implements AdminOrderStatusService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_CONFIRMED = 1;
    private static final int STATUS_SHIPPING = 2;
    private static final int STATUS_COMPLETED = 3;
    private static final int STATUS_CANCELLED = 4;

    private final OrderRepository orderRepository;
    private final LoyaltyPointService loyaltyPointService;

    @Override
    @Transactional
    public Order updateStatus(Integer orderId, Integer newStatus) {
        if (orderId == null || orderId <= 0) {
            throw badRequest("orderId phải là số nguyên dương");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng"
                ));

        Integer oldStatus = order.getStatus();

        validateOrderStatusTransition(oldStatus, newStatus);

        order.setStatus(newStatus);

        if (!Objects.equals(oldStatus, STATUS_COMPLETED)
                && Objects.equals(newStatus, STATUS_COMPLETED)) {
            loyaltyPointService.applyPointsWhenOrderCompleted(order);
            return order;
        }

        return orderRepository.save(order);
    }

    private void validateOrderStatusTransition(Integer oldStatus, Integer newStatus) {
        if (newStatus == null || newStatus < 0 || newStatus > 4) {
            throw badRequest("Trạng thái đơn hàng không hợp lệ");
        }

        if (oldStatus == null) {
            throw badRequest("Trạng thái hiện tại của đơn hàng không hợp lệ");
        }

        if (Objects.equals(oldStatus, newStatus)) {
            throw badRequest("Đơn hàng đã ở trạng thái này");
        }

        if (Objects.equals(oldStatus, STATUS_COMPLETED)) {
            throw badRequest("Đơn hàng đã hoàn thành nên không thể đổi trạng thái");
        }

        if (Objects.equals(oldStatus, STATUS_CANCELLED)) {
            throw badRequest("Đơn hàng đã hủy nên không thể đổi trạng thái");
        }

        boolean valid = switch (oldStatus) {
            case STATUS_PENDING -> newStatus == STATUS_CONFIRMED || newStatus == STATUS_CANCELLED;
            case STATUS_CONFIRMED -> newStatus == STATUS_SHIPPING || newStatus == STATUS_CANCELLED;
            case STATUS_SHIPPING -> newStatus == STATUS_COMPLETED;
            default -> false;
        };

        if (!valid) {
            throw badRequest("Không thể chuyển trạng thái đơn hàng từ " + oldStatus + " sang " + newStatus);
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
