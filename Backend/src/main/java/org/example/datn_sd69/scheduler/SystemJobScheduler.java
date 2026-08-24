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

    // TASK 2: ĐỒNG BỘ TRẠNG THÁI FLASH SALE & VOUCHER (MỖI 1 PHÚT)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void syncPromotionAndVoucherStatus() {

        LocalDateTime now = LocalDateTime.now();

        /*
         * FLASH SALE / PROMOTION:
         *
         * Không tự động chuyển status 0 -> 1.
         *
         * Quy ước hiện tại của module Promotion:
         * - status = 1: Admin cho phép chiến dịch hoạt động.
         * - status = 0: Admin đã chủ động tắt/tạm dừng, hoặc chiến dịch đã kết thúc.
         * - activeNow được quyết định thêm bởi khoảng thời gian
         *   startDate <= now < endDate.
         *
         * Promotion được tạo mới với status = 1. Vì vậy campaign tương lai
         * không cần scheduler "bật" khi tới giờ; các API Flash Sale chỉ xem nó
         * là active khi thời gian thực sự nằm trong khoảng chạy.
         *
         * Nếu scheduler tự bật mọi Promotion status = 0 đang nằm trong thời gian
         * chạy thì một campaign vừa bị Admin tạm dừng sẽ bị bật lại sau tối đa
         * 1 phút. Đó là sai nghiệp vụ.
         */

        // 1. Chỉ tự động tắt Flash Sale đã quá hạn kết thúc.
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
                            + " chiến dịch Flash Sale/Promotion đã hết hạn."
            );
        }

        /*
         * VOUCHER:
         * Giữ nguyên hoàn toàn logic hiện tại.
         * Voucher sẽ được audit/sửa riêng ở module Voucher, không thay đổi
         * nghiệp vụ Voucher trong bản fix Promotion này.
         */

        // 2. Bật Voucher
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

        // 3. Tắt Voucher
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