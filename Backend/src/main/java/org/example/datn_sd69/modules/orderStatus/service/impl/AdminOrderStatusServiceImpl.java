package org.example.datn_sd69.modules.orderStatus.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.modules.loyalty.service.LoyaltyPointService;
import org.example.datn_sd69.modules.order.service.AdminOrderService;
import org.example.datn_sd69.modules.orderStatus.service.AdminOrderStatusService;
import org.example.datn_sd69.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminOrderStatusServiceImpl implements AdminOrderStatusService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_CONFIRMED = 1;
    private static final int STATUS_SHIPPING = 2;
    private static final int STATUS_COMPLETED = 3;
    private static final int STATUS_CANCELLED = 4;
    private static final int STATUS_DELIVERY_FAILED = 5;
    private static final int STATUS_RETURN_REQUESTED = 6;
    private static final int STATUS_RETURN_COMPLETED = 7;

    private static final Set<Integer> VALID_ORDER_STATUSES = Set.of(
            STATUS_PENDING,
            STATUS_CONFIRMED,
            STATUS_SHIPPING,
            STATUS_COMPLETED,
            STATUS_CANCELLED,
            STATUS_DELIVERY_FAILED,
            STATUS_RETURN_REQUESTED,
            STATUS_RETURN_COMPLETED
    );

    private final OrderRepository orderRepository;

    /*
     * Dùng service Order hiện tại làm nguồn nghiệp vụ chuẩn cho các transition
     * có liên quan trực tiếp tới InventoryLot / StockMovement.
     *
     * Đặc biệt:
     * PENDING -> CONFIRMED phải đi qua AdminOrderService.confirmOrder()
     * để:
     * - kiểm tra tồn từ InventoryLot
     * - SALE_OUT theo FEFO
     * - ghi StockMovement
     *
     * Không được tự xử lý ProductVariant.stockQuantity tại service này.
     */
    private final AdminOrderService adminOrderService;

    private final LoyaltyPointService loyaltyPointService;

    @Override
    @Transactional
    public Order updateStatus(Integer orderId, Integer newStatus) {

        validateOrderId(orderId);
        validateNewStatus(newStatus);

        Order order = findOrder(orderId);

        validateNotHoldOrder(order);

        Integer oldStatus = safeStatus(order);

        if (Objects.equals(oldStatus, newStatus)) {
            throw conflict("Đơn hàng đã ở trạng thái này");
        }

        switch (newStatus) {

            /*
             * =========================================================
             * PENDING -> CONFIRMED
             * =========================================================
             *
             * Đây là transition có xuất kho thực tế.
             *
             * Tuyệt đối không:
             * - order.setStatus(CONFIRMED) trực tiếp
             * - đọc ProductVariant.stockQuantity
             * - tự viết lại FEFO tại đây
             *
             * Gọi đúng flow chuẩn hiện có trong AdminOrderService.
             */
            case STATUS_CONFIRMED -> {
                requireCurrentStatus(
                        order,
                        STATUS_PENDING,
                        "Chỉ đơn chờ xác nhận mới được chuyển sang đã xác nhận"
                );

                adminOrderService.confirmOrder(orderId);

                /*
                 * Load lại để response của API /status trả trạng thái mới nhất.
                 */
                return findOrder(orderId);
            }

            /*
             * =========================================================
             * CONFIRMED -> SHIPPING
             * =========================================================
             *
             * Chỉ đổi trạng thái.
             * Không xuất thêm kho vì SALE_OUT đã xảy ra lúc CONFIRMED.
             */
            case STATUS_SHIPPING -> shipOrder(order);

            /*
             * =========================================================
             * SHIPPING -> COMPLETED
             * hoặc RETURN_REQUESTED -> COMPLETED theo legacy workflow
             * =========================================================
             *
             * Giữ nguyên behavior hiện tại của service này.
             * Không có thay đổi InventoryLot ở transition này.
             */
            case STATUS_COMPLETED -> completeOrder(order);

            /*
             * =========================================================
             * CANCEL
             * =========================================================
             *
             * Không cho generic /status tự xử lý.
             *
             * Lý do:
             * - PENDING chưa SALE_OUT -> không hoàn kho.
             * - Flow hủy hiện tại còn xử lý lý do hủy / prepaid refund.
             * - Generic status không có đủ dữ liệu request đó.
             *
             * Phải dùng AdminOrderService.cancelOrder() qua endpoint
             * hủy đơn chuyên dụng.
             */
            case STATUS_CANCELLED -> throw badRequest(
                    "Không được hủy đơn bằng cập nhật trạng thái trực tiếp. "
                            + "Vui lòng sử dụng chức năng Hủy đơn."
            );

            /*
             * =========================================================
             * DELIVERY FAILED
             * =========================================================
             *
             * Không cho generic /status thực hiện vì flow chuẩn cần:
             * - lý do giao thất bại
             * - mô tả
             * - minh chứng
             * - phân biệt COD / trả trước
             * - RETURN_IN đúng SALE_OUT nếu nghiệp vụ yêu cầu hoàn kho
             *
             * Tất cả đã nằm trong AdminOrderService.markDeliveryFailed().
             */
            case STATUS_DELIVERY_FAILED -> throw badRequest(
                    "Không được chuyển sang Giao hàng thất bại bằng cập nhật "
                            + "trạng thái trực tiếp. Vui lòng sử dụng chức năng "
                            + "Giao hàng thất bại."
            );

            /*
             * =========================================================
             * COMPLETED -> RETURN_REQUESTED
             * =========================================================
             *
             * Giữ nguyên logic cũ.
             * Transition này chưa làm thay đổi tồn kho.
             */
            case STATUS_RETURN_REQUESTED -> requestReturnExchange(order);

            /*
             * =========================================================
             * RETURN COMPLETED
             * =========================================================
             *
             * Tuyệt đối không dùng restoreStock(order) kiểu cũ.
             *
             * Partial return hiện tại phải:
             * - lấy ReturnRequestItem
             * - tìm SALE_OUT đúng OrderItem
             * - RETURN_IN đúng InventoryLot gốc
             * - chống hoàn trùng
             *
             * Flow đó đã nằm trong AdminOrderService.
             */
            case STATUS_RETURN_COMPLETED -> throw badRequest(
                    "Không được chuyển thẳng sang Hoàn hàng / đổi trả hoàn tất. "
                            + "Vui lòng xử lý bằng luồng hoàn hàng và xác nhận "
                            + "đã hoàn tiền."
            );

            default -> throw badRequest("Trạng thái đơn hàng không hợp lệ");
        }

        return order;
    }

    /*
     * =========================================================
     * SHIPPING
     * =========================================================
     */
    private void shipOrder(Order order) {

        requireCurrentStatus(
                order,
                STATUS_CONFIRMED,
                "Chỉ đơn đã xác nhận mới được chuyển sang đang giao hàng"
        );

        /*
         * Không tác động InventoryLot.
         *
         * SALE_OUT đã được thực hiện khi xác nhận đơn.
         */
        order.setStatus(STATUS_SHIPPING);
        orderRepository.save(order);
    }

    /*
     * =========================================================
     * COMPLETED
     * =========================================================
     */
    private void completeOrder(Order order) {

        Integer oldStatus = safeStatus(order);

        /*
         * Giữ nguyên behavior legacy:
         * Admin từ chối yêu cầu hoàn bằng cách đưa đơn từ
         * RETURN_REQUESTED về COMPLETED.
         *
         * Không có nghiệp vụ tồn kho tại đây.
         */
        if (oldStatus == STATUS_RETURN_REQUESTED) {
            order.setStatus(STATUS_COMPLETED);
            orderRepository.save(order);
            return;
        }

        requireCurrentStatus(
                order,
                STATUS_SHIPPING,
                "Chỉ đơn đang giao hàng mới được chuyển sang hoàn thành"
        );

        order.setStatus(STATUS_COMPLETED);

        if (order.getCompletedAt() == null) {
            order.setCompletedAt(LocalDateTime.now());
        }

        orderRepository.save(order);

        /*
         * Giữ nguyên logic loyalty cũ.
         * Không thay đổi trong migration InventoryLot.
         */
        loyaltyPointService.applyPointsWhenOrderCompleted(order);

        orderRepository.save(order);
    }

    /*
     * =========================================================
     * RETURN REQUESTED
     * =========================================================
     */
    private void requestReturnExchange(Order order) {

        requireCurrentStatus(
                order,
                STATUS_COMPLETED,
                "Chỉ đơn đã hoàn thành mới được yêu cầu hoàn hàng / đổi trả"
        );

        /*
         * Chỉ đổi trạng thái.
         *
         * Chưa RETURN_IN tại bước yêu cầu hoàn.
         */
        order.setStatus(STATUS_RETURN_REQUESTED);
        orderRepository.save(order);
    }

    /*
     * =========================================================
     * COMMON VALIDATION
     * =========================================================
     */

    private Order findOrder(Integer orderId) {

        return orderRepository.findDetailByIdForUpdate(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy đơn hàng"
                ));
    }

    private void requireCurrentStatus(
            Order order,
            Integer expectedStatus,
            String message
    ) {

        Integer oldStatus = safeStatus(order);

        if (isTerminalStatus(oldStatus)) {
            throw conflict(
                    "Đơn hàng đã ở trạng thái kết thúc, không thể cập nhật tiếp"
            );
        }

        if (!Objects.equals(oldStatus, expectedStatus)) {
            throw conflict(message);
        }
    }

    private boolean isTerminalStatus(Integer status) {

        return status != null
                && (
                status == STATUS_CANCELLED
                        || status == STATUS_DELIVERY_FAILED
                        || status == STATUS_RETURN_COMPLETED
        );
    }

    private void validateOrderId(Integer orderId) {

        if (orderId == null || orderId <= 0) {
            throw badRequest("orderId phải là số nguyên dương");
        }
    }

    private void validateNewStatus(Integer newStatus) {

        if (newStatus == null) {
            throw badRequest(
                    "Trạng thái đơn hàng không được để trống"
            );
        }

        if (!VALID_ORDER_STATUSES.contains(newStatus)) {
            throw badRequest(
                    "Trạng thái đơn hàng không hợp lệ"
            );
        }

        if (newStatus == STATUS_PENDING) {
            throw badRequest(
                    "Không thể cập nhật đơn hàng quay về trạng thái chờ xác nhận"
            );
        }
    }

    private Integer safeStatus(Order order) {

        return order.getStatus() == null
                ? STATUS_PENDING
                : order.getStatus();
    }

    private void validateNotHoldOrder(Order order) {

        if (isHoldOrder(order)) {
            throw badRequest(
                    "Đây là phiếu treo tại quầy. "
                            + "Vui lòng xử lý bằng chức năng POS, "
                            + "không xử lý ở màn quản lý đơn thường"
            );
        }
    }

    private boolean isHoldOrder(Order order) {

        String paymentMethod =
                order.getPaymentMethod() == null
                        ? ""
                        : order.getPaymentMethod()
                        .trim()
                        .toUpperCase(Locale.ROOT);

        String orderType =
                order.getOrderType() == null
                        ? ""
                        : order.getOrderType()
                        .trim()
                        .toUpperCase(Locale.ROOT);

        return "HOLD".equals(paymentMethod)
                && (
                "POS".equals(orderType)
                        || "IN_STORE".equals(orderType)
        );
    }

    private ResponseStatusException conflict(String message) {

        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }

    private ResponseStatusException badRequest(String message) {

        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}
