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

            int cancelledCount = 0;

            for (Order candidate : timeoutOrders) {
                if (candidate == null || candidate.getId() == null) {
                    continue;
                }

                /*
                 * Khóa lại đúng Order trước khi hủy.
                 * VNPay callback cũng khóa Order nên scheduler không thể hủy một
                 * đơn vừa được ghi nhận thanh toán ở transaction song song.
                 */
                Order order = orderRepository.findDetailByIdForUpdate(candidate.getId())
                        .orElse(null);

                if (order == null
                        || !Integer.valueOf(0).equals(order.getStatus())
                        || Boolean.TRUE.equals(order.getIsPaymentReported())) {
                    continue;
                }

                /*
                 * Đơn ONLINE PENDING chưa SALE_OUT InventoryLot.
                 * Không hoàn ProductVariant.stockQuantity, không tạo StockMovement.
                 *
                 * Voucher đã reserve từ lúc tạo Order nên auto-cancel phải hoàn
                 * đúng 1 lượt trong cùng transaction.
                 */
                restoreVoucherUsage(order);

                order.setStatus(4);
                order.setCancelReason(
                        "Hệ thống tự động hủy do quá hạn 15 phút không thanh toán"
                );
                order.setCancelledAt(LocalDateTime.now());

                orderRepository.save(order);
                cancelledCount++;
            }

            if (cancelledCount > 0) {
                System.out.println(
                        "CronJob: Tự động hủy "
                                + cancelledCount
                                + " đơn hàng treo thanh toán (Quá 15 phút)."
                );
            }
        }
    }

    private void restoreVoucherUsage(Order order) {
        if (order == null
                || order.getVoucher() == null
                || order.getVoucher().getId() == null) {
            return;
        }

        Voucher lockedVoucher = voucherRepo
                .findByIdForUpdate(order.getVoucher().getId())
                .orElse(null);

        if (lockedVoucher == null) {
            return;
        }

        int usedCount = lockedVoucher.getUsedCount() != null
                ? lockedVoucher.getUsedCount()
                : 0;

        if (usedCount > 0) {
            lockedVoucher.setUsedCount(usedCount - 1);
            voucherRepo.save(lockedVoucher);
        }
    }

    // TASK 2: ĐỒNG BỘ TRẠNG THÁI FLASH SALE & VOUCHER (MỖI 1 PHÚT)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void syncPromotionAndVoucherStatus() {

        LocalDateTime now = LocalDateTime.now();

        /*
         * FLASH SALE / PROMOTION:
         * Giữ nguyên logic đã sửa trước đó: không auto 0 -> 1,
         * chỉ tự động tắt khi hết hạn.
         */
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
         * - status = 1: Admin cho phép sử dụng.
         * - status = 0: Admin tạm dừng/khóa, hoặc hệ thống đã kết thúc voucher.
         * - hiệu lực thực tế còn phụ thuộc startDate <= now < endDate.
         *
         * KHÔNG tự động chuyển status 0 -> 1.
         * Nếu làm vậy Voucher vừa bị Admin tạm dừng sẽ tự bật lại sau tối đa 1 phút.
         *
         * Chỉ giữ auto-end tại SystemJobScheduler để tránh 2 scheduler cùng sửa Voucher.
         */
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
                            + " mã Voucher đã hết hạn."
            );
        }
    }
}
