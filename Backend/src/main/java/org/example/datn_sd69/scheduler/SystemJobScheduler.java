package org.example.datn_sd69.scheduler;

import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.Promotion;
import org.example.datn_sd69.entity.Voucher;
import org.example.datn_sd69.repository.OrderRepository;
import org.example.datn_sd69.repository.PromotionRepository;
import org.example.datn_sd69.repository.VoucherRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SystemJobScheduler {

    private final OrderRepository orderRepository;
    private final PromotionRepository flashSaleRepo;
    private final VoucherRepository voucherRepo;

    public SystemJobScheduler(
            OrderRepository orderRepository,
            PromotionRepository flashSaleRepo,
            VoucherRepository voucherRepo
    ) {
        this.orderRepository = orderRepository;
        this.flashSaleRepo = flashSaleRepo;
        this.voucherRepo = voucherRepo;
    }

    // TASK 1: QUÉT HỦY ĐƠN CHỜ THANH TOÁN QUÁ HẠN (MỖI 1 PHÚT)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoCancelUnpaidOrders() {

        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(15);

        List<Order> timeoutOrders =
                orderRepository.findUnpaidPrepaidOrders(timeoutThreshold);

        if (!timeoutOrders.isEmpty()) {

            for (Order order : timeoutOrders) {

                /*
                 * Đơn PENDING chưa xuất kho thực tế.
                 *
                 * InventoryLot là nguồn tồn kho duy nhất.
                 * Không hoàn ProductVariant.stockQuantity.
                 * Không tạo StockMovement vì chưa có SALE_OUT.
                 */
                order.setStatus(4);
                order.setCancelReason(
                        "Hệ thống tự động hủy do quá hạn 15 phút không thanh toán"
                );
                order.setCancelledAt(LocalDateTime.now());
            }

            orderRepository.saveAll(timeoutOrders);

            System.out.println(
                    "CronJob: Tự động hủy "
                            + timeoutOrders.size()
                            + " đơn hàng treo thanh toán (Quá 15 phút)."
            );
        }
    }

    // TASK 2: TỰ ĐỘNG BẬT TẮT FLASH SALE & VOUCHER (MỖI 1 PHÚT)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void syncPromotionAndVoucherStatus() {

        LocalDateTime now = LocalDateTime.now();

        // 1. Bật Flash Sale chưa bắt đầu (status = 0) mà đến giờ chạy
        List<Promotion> startingSales =
                flashSaleRepo.findToStart(0, now);

        for (Promotion sale : startingSales) {
            sale.setStatus(1);
        }

        if (!startingSales.isEmpty()) {
            flashSaleRepo.saveAll(startingSales);

            System.out.println(
                    "CronJob: Đã TỰ ĐỘNG BẬT "
                            + startingSales.size()
                            + " chiến dịch Flash Sale/Promotion."
            );
        }

        // 2. Tắt Flash Sale đang chạy (status = 1) mà quá hạn kết thúc
        List<Promotion> endingSales =
                flashSaleRepo.findToEnd(1, now);

        for (Promotion sale : endingSales) {
            sale.setStatus(0);
        }

        if (!endingSales.isEmpty()) {
            flashSaleRepo.saveAll(endingSales);

            System.out.println(
                    "CronJob: Đã TỰ ĐỘNG TẮT "
                            + endingSales.size()
                            + " chiến dịch Flash Sale/Promotion."
            );
        }

        // 3. Bật Voucher
        List<Voucher> startingVouchers =
                voucherRepo.findToStart(0, now);

        for (Voucher voucher : startingVouchers) {
            voucher.setStatus(1);
        }

        if (!startingVouchers.isEmpty()) {
            voucherRepo.saveAll(startingVouchers);

            System.out.println(
                    "CronJob: Đã TỰ ĐỘNG BẬT "
                            + startingVouchers.size()
                            + " mã Voucher."
            );
        }

        // 4. Tắt Voucher
        List<Voucher> endingVouchers =
                voucherRepo.findToEnd(1, now);

        for (Voucher voucher : endingVouchers) {
            voucher.setStatus(0);
        }

        if (!endingVouchers.isEmpty()) {
            voucherRepo.saveAll(endingVouchers);

            System.out.println(
                    "CronJob: Đã TỰ ĐỘNG TẮT "
                            + endingVouchers.size()
                            + " mã Voucher."
            );
        }
    }
}