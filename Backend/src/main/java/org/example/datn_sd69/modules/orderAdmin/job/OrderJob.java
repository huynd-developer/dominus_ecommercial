//package org.example.datn_sd69.modules.orderAdmin.job;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.example.datn_sd69.entity.Order;
//import org.example.datn_sd69.entity.OrderItem;
//import org.example.datn_sd69.entity.ProductVariant;
//import org.example.datn_sd69.repository.OrderItemRepository;
//import org.example.datn_sd69.repository.OrderRepository;
//import org.example.datn_sd69.repository.ProductVariantRepository;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class OrderJob {
//
//    private final OrderRepository orderRepository;
//    private final OrderItemRepository orderItemRepository;
//    private final ProductVariantRepository productVariantRepository;
//
//    /**
//     * Mỗi 30 phút kiểm tra đơn chờ xác nhận quá 24 giờ.
//     */
//    @Transactional
//    @Scheduled(cron = "0 */30 * * * *")
//    public void autoCancelExpiredOrders() {
//
//        LocalDateTime expiredTime = LocalDateTime.now().minusHours(24);
//
//        List<Order> orders = orderRepository
//                .findByStatusAndCreatedAtBeforeAndIsDeletedFalse(0, expiredTime);
//
//        if (orders.isEmpty()) {
//            return;
//        }
//
//        for (Order order : orders) {
//
//            List<OrderItem> items =
//                    orderItemRepository.findByOrder_Id(order.getId());
//
//            for (OrderItem item : items) {
//
//                ProductVariant variant = item.getProductVariant();
//
//                variant.setStockQuantity(
//                        variant.getStockQuantity() + item.getQuantity()
//                );
//
//                productVariantRepository.save(variant);
//            }
//
//            order.setStatus(4);
//
//            orderRepository.save(order);
//
//            log.info("Đã tự động hủy đơn #{}", order.getId());
//        }
//    }
//}