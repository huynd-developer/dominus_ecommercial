package org.example.datn_sd69.modules.orderAdmin.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.modules.orderAdmin.service.OrderService;
import org.example.datn_sd69.repository.OrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCleanupJob {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    /**
     * Mỗi 5 phút kiểm tra
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void autoCancelExpiredOrders() {

        LocalDateTime expireTime = LocalDateTime.now().minusHours(24);

        List<Order> orders =
                orderRepository.findByStatusAndCreatedAtBefore(
                        0,
                        expireTime
                );

        for (Order order : orders) {

            try {
                orderService.cancelOrder(order.getId());

                log.info(
                        "Auto cancel order id={}",
                        order.getId()
                );

            } catch (Exception e) {

                log.error(
                        "Auto cancel fail order id={}",
                        order.getId(),
                        e
                );
            }
        }
    }
}