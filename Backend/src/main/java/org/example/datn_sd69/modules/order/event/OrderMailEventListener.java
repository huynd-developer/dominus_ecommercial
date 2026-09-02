package org.example.datn_sd69.modules.order.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.modules.order.service.OrderMailService;
import org.example.datn_sd69.repository.OrderRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Propagation;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderMailEventListener {

    private final OrderRepository orderRepository;
    private final OrderMailService orderMailService;

    @Async
    @Transactional(
            readOnly = true,
            propagation = Propagation.REQUIRES_NEW
    )
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderMailEvent(OrderMailEvent event) {

        if (event == null
                || event.orderId() == null
                || event.type() == null) {
            return;
        }

        try {
            Order order = orderRepository.findById(event.orderId())
                    .orElse(null);

            if (order == null) {
                log.warn(
                        "Không gửi mail async vì không tìm thấy Order #{}",
                        event.orderId()
                );
                return;
            }

            switch (event.type()) {
                case RETURN_ACCEPTED -> orderMailService.sendReturnAccepted(order);

                case RETURN_REFUNDED -> orderMailService.sendReturnRefunded(order);
            }

        } catch (Exception exception) {
            log.warn(
                    "Không xử lý được mail async cho Order #{}: {}",
                    event.orderId(),
                    exception.getMessage()
            );
        }
    }
}