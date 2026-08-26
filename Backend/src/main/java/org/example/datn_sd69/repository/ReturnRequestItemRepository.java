package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.ReturnRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ReturnRequestItemRepository extends JpaRepository<ReturnRequestItem, Integer> {

    List<ReturnRequestItem> findByReturnRequest_Id(Integer returnRequestId);

    /**
     * Dùng cho admin xem/xử lý hoàn hàng.
     *
     * Fetch sẵn OrderItem -> ProductVariant -> Product/Brand/Capacity/BottleType
     * để bảng sản phẩm hoàn không bị mất tên sản phẩm, SKU, dung tích, loại chai.
     */
    @Query("""
        SELECT DISTINCT item
        FROM ReturnRequestItem item
        LEFT JOIN FETCH item.orderItem oi
        LEFT JOIN FETCH oi.productVariant pv
        LEFT JOIN FETCH pv.product p
        LEFT JOIN FETCH p.brand b
        LEFT JOIN FETCH pv.capacity c
        LEFT JOIN FETCH pv.bottleType bt
        WHERE item.returnRequest.id = :returnRequestId
        ORDER BY item.id ASC
    """)
    List<ReturnRequestItem> findByReturnRequest_IdWithOrderItemDetail(
            @Param("returnRequestId") Integer returnRequestId
    );

    boolean existsByReturnRequest_Order_IdAndStatus(Integer orderId, Integer status);

    List<ReturnRequestItem> findByReturnRequest_Order_IdAndStatus(Integer orderId, Integer status);

    /*
     * =========================================================
     * OWNER REPORT - QUERY RIÊNG
     * =========================================================
     *
     * Chỉ trừ tiền hoàn khi từng ReturnRequestItem đã thực sự hoàn tất:
     * Status = 3 và RefundedAt nằm trong kỳ báo cáo.
     *
     * Dùng item.refundAmount thay vì ReturnRequest.refundAmount vì tổng request
     * có thể chứa cả phần shipping được hoàn, trong khi doanh thu bán hàng của
     * Owner Report đã loại shipping ngay từ đầu.
     */
    @Query("""
        SELECT COALESCE(SUM(item.refundAmount), 0)
        FROM ReturnRequestItem item
        JOIN item.returnRequest rr
        JOIN rr.order o
        WHERE item.status = 3
          AND item.refundedAt IS NOT NULL
          AND item.refundedAt >= :fromDate
          AND item.refundedAt < :toDate
          AND UPPER(TRIM(o.orderType)) IN ('ONLINE', 'IN_STORE', 'POS')
    """)
    BigDecimal sumCompletedProductRefundAmountForOwnerReport(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query("""
        SELECT o.orderType, COALESCE(SUM(item.refundAmount), 0)
        FROM ReturnRequestItem item
        JOIN item.returnRequest rr
        JOIN rr.order o
        WHERE item.status = 3
          AND item.refundedAt IS NOT NULL
          AND item.refundedAt >= :fromDate
          AND item.refundedAt < :toDate
          AND UPPER(TRIM(o.orderType)) IN ('ONLINE', 'IN_STORE', 'POS')
        GROUP BY o.orderType
    """)
    List<Object[]> getOwnerReportRefundBreakdownByOrderType(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query("""
        SELECT item
        FROM ReturnRequestItem item
        JOIN item.returnRequest rr
        JOIN rr.order o
        WHERE item.status = 3
          AND item.refundedAt IS NOT NULL
          AND item.refundedAt >= :fromDate
          AND item.refundedAt < :toDate
          AND UPPER(TRIM(o.orderType)) IN ('ONLINE', 'IN_STORE', 'POS')
        ORDER BY item.refundedAt ASC, item.id ASC
    """)
    List<ReturnRequestItem> findCompletedProductRefundsForOwnerReportChart(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
}
