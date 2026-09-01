package org.example.datn_sd69.repository;

import jakarta.persistence.LockModeType;
import org.example.datn_sd69.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    @EntityGraph(attributePaths = {
            "customer",
            "customer.user",
            "cashier",
            "cashier.user",
            "voucher"
    })
    @Query(
            value = """
                        SELECT o
                        FROM Order o
                        LEFT JOIN o.voucher voucher
                        WHERE
                            (
                                :keyword IS NULL
                                OR TRIM(:keyword) = ''

                                OR LOWER(COALESCE(o.customerName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                OR LOWER(COALESCE(o.customerPhone, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                OR LOWER(COALESCE(o.shippingAddress, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))

                                OR LOWER(COALESCE(o.paymentMethod, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                OR LOWER(COALESCE(o.orderType, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                OR LOWER(COALESCE(voucher.code, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))

                                OR CAST(o.id AS string) LIKE CONCAT('%', :keyword, '%')
                                OR CONCAT('#', CAST(o.id AS string)) LIKE CONCAT('%', :keyword, '%')

                                OR LOWER(CONCAT('DH', CAST(o.id AS string))) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                OR LOWER(CONCAT('DH0', CAST(o.id AS string))) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                OR LOWER(CONCAT('DH00', CAST(o.id AS string))) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                OR LOWER(CONCAT('DH000', CAST(o.id AS string))) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                OR LOWER(CONCAT('DH0000', CAST(o.id AS string))) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                OR LOWER(CONCAT('DH00000', CAST(o.id AS string))) LIKE LOWER(CONCAT('%', :keyword, '%'))

                                OR EXISTS (
                                    SELECT 1
                                    FROM OrderItem oi
                                    LEFT JOIN oi.productVariant pv
                                    LEFT JOIN pv.product product
                                    LEFT JOIN pv.capacity capacity
                                    LEFT JOIN pv.bottleType bottleType
                                    WHERE oi.order = o
                                      AND (
                                          LOWER(COALESCE(product.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                          OR LOWER(COALESCE(pv.sku, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                          OR LOWER(COALESCE(bottleType.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                          OR CAST(capacity.value AS string) LIKE CONCAT('%', :keyword, '%')
                                          OR LOWER(COALESCE(oi.note, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                      )
                                )
                            )

                            AND (:status IS NULL OR o.status = :status)

                            AND (
                                :orderType IS NULL
                                OR TRIM(:orderType) = ''
                                OR UPPER(COALESCE(o.orderType, '')) = UPPER(:orderType)
                            )

                            AND (
                                :paymentMethod IS NULL
                                OR TRIM(:paymentMethod) = ''
                                OR UPPER(:paymentMethod) = 'ALL'
                                OR (
                                    UPPER(:paymentMethod) = 'MIXED'
                                    AND UPPER(COALESCE(o.paymentMethod, '')) LIKE 'MIXED%'
                                )
                                OR UPPER(COALESCE(o.paymentMethod, '')) = UPPER(:paymentMethod)
                            )

                            AND (:fromDate IS NULL OR o.createdAt >= :fromDate)
                            AND (:toDate IS NULL OR o.createdAt < :toDate)

                            AND (:minAmount IS NULL OR o.finalAmount >= :minAmount)
                            AND (:maxAmount IS NULL OR o.finalAmount <= :maxAmount)

                            AND (
                                o.paymentMethod IS NULL
                                OR UPPER(o.paymentMethod) <> 'HOLD'
                            )
                            AND NOT (
                                UPPER(COALESCE(o.orderType, '')) = 'ONLINE'
                                AND (o.isPaymentReported IS NULL OR o.isPaymentReported = false)
                                AND UPPER(COALESCE(o.paymentMethod, '')) IN (
                                    'VNPAY',
                                    'VIETQR',
                                    'MIXED_VNPAY',
                                    'MIXED_VIETQR'
                                )
                                AND o.status IN (0, 4)
                            )
                        ORDER BY
                                                     CASE WHEN o.status = 0 THEN 0 ELSE 1 END ASC,
                                                     o.createdAt DESC,
                                                     o.id DESC
                    """,
            countQuery = """
                                SELECT COUNT(o)
                                FROM Order o
                                LEFT JOIN o.voucher voucher
                                WHERE
                                    (
                                        :keyword IS NULL
                                        OR TRIM(:keyword) = ''

                                        OR LOWER(COALESCE(o.customerName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                        OR LOWER(COALESCE(o.customerPhone, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                        OR LOWER(COALESCE(o.shippingAddress, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))

                                        OR LOWER(COALESCE(o.paymentMethod, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                        OR LOWER(COALESCE(o.orderType, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                        OR LOWER(COALESCE(voucher.code, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))

                                        OR CAST(o.id AS string) LIKE CONCAT('%', :keyword, '%')
                                        OR CONCAT('#', CAST(o.id AS string)) LIKE CONCAT('%', :keyword, '%')

                                        OR LOWER(CONCAT('DH', CAST(o.id AS string))) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                        OR LOWER(CONCAT('DH0', CAST(o.id AS string))) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                        OR LOWER(CONCAT('DH00', CAST(o.id AS string))) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                        OR LOWER(CONCAT('DH000', CAST(o.id AS string))) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                        OR LOWER(CONCAT('DH0000', CAST(o.id AS string))) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                        OR LOWER(CONCAT('DH00000', CAST(o.id AS string))) LIKE LOWER(CONCAT('%', :keyword, '%'))

                                        OR EXISTS (
                                            SELECT 1
                                            FROM OrderItem oi
                                            LEFT JOIN oi.productVariant pv
                                            LEFT JOIN pv.product product
                                            LEFT JOIN pv.capacity capacity
                                            LEFT JOIN pv.bottleType bottleType
                                            WHERE oi.order = o
                                              AND (
                                                  LOWER(COALESCE(product.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                                  OR LOWER(COALESCE(pv.sku, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                                  OR LOWER(COALESCE(bottleType.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                                  OR CAST(capacity.value AS string) LIKE CONCAT('%', :keyword, '%')
                                                  OR LOWER(COALESCE(oi.note, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                              )
                                        )
                                    )

                                    AND (:status IS NULL OR o.status = :status)

                                    AND (
                                        :orderType IS NULL
                                        OR TRIM(:orderType) = ''
                                        OR UPPER(COALESCE(o.orderType, '')) = UPPER(:orderType)
                                    )

                                    AND (
                                        :paymentMethod IS NULL
                                        OR TRIM(:paymentMethod) = ''
                                        OR UPPER(:paymentMethod) = 'ALL'
                                        OR (
                                            UPPER(:paymentMethod) = 'MIXED'
                                            AND UPPER(COALESCE(o.paymentMethod, '')) LIKE 'MIXED%'
                                        )
                                        OR UPPER(COALESCE(o.paymentMethod, '')) = UPPER(:paymentMethod)
                                    )

                                    AND (:fromDate IS NULL OR o.createdAt >= :fromDate)
                                    AND (:toDate IS NULL OR o.createdAt < :toDate)

                                    AND (:minAmount IS NULL OR o.finalAmount >= :minAmount)
                                    AND (:maxAmount IS NULL OR o.finalAmount <= :maxAmount)

                                    AND (
                        o.paymentMethod IS NULL
                        OR UPPER(o.paymentMethod) <> 'HOLD'
                    )

                    AND NOT (
                        UPPER(COALESCE(o.orderType, '')) = 'ONLINE'
                        AND (o.isPaymentReported IS NULL OR o.isPaymentReported = false)
                        AND UPPER(COALESCE(o.paymentMethod, '')) IN (
                            'VNPAY',
                            'VIETQR',
                            'MIXED_VNPAY',
                            'MIXED_VIETQR'
                        )
                        AND o.status IN (0, 4)
                    )
                    """
    )
    Page<Order> searchAdminOrders(
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("orderType") String orderType,
            @Param("paymentMethod") String paymentMethod,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            Pageable pageable
    );

    @Query("""
                SELECT COUNT(o)
                FROM Order o
                LEFT JOIN o.voucher voucher
                WHERE
                    (
                        :keyword IS NULL
                        OR TRIM(:keyword) = ''

                        OR LOWER(COALESCE(o.customerName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(COALESCE(o.customerPhone, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(COALESCE(o.shippingAddress, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))

                        OR LOWER(COALESCE(o.paymentMethod, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(COALESCE(o.orderType, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(COALESCE(voucher.code, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))

                        OR CAST(o.id AS string) LIKE CONCAT('%', :keyword, '%')
                        OR CONCAT('#', CAST(o.id AS string)) LIKE CONCAT('%', :keyword, '%')

                        OR LOWER(CONCAT('DH', CAST(o.id AS string))) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(CONCAT('DH0', CAST(o.id AS string))) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(CONCAT('DH00', CAST(o.id AS string))) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(CONCAT('DH000', CAST(o.id AS string))) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(CONCAT('DH0000', CAST(o.id AS string))) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(CONCAT('DH00000', CAST(o.id AS string))) LIKE LOWER(CONCAT('%', :keyword, '%'))

                        OR EXISTS (
                            SELECT 1
                            FROM OrderItem oi
                            LEFT JOIN oi.productVariant pv
                            LEFT JOIN pv.product product
                            LEFT JOIN pv.capacity capacity
                            LEFT JOIN pv.bottleType bottleType
                            WHERE oi.order = o
                              AND (
                                  LOWER(COALESCE(product.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                  OR LOWER(COALESCE(pv.sku, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                  OR LOWER(COALESCE(bottleType.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                  OR CAST(capacity.value AS string) LIKE CONCAT('%', :keyword, '%')
                                  OR LOWER(COALESCE(oi.note, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                              )
                        )
                    )

                    AND (:status IS NULL OR o.status = :status)

                    AND (
                        :orderType IS NULL
                        OR TRIM(:orderType) = ''
                        OR UPPER(COALESCE(o.orderType, '')) = UPPER(:orderType)
                    )

                    AND (:fromDate IS NULL OR o.createdAt >= :fromDate)
                    AND (:toDate IS NULL OR o.createdAt < :toDate)

                    AND (
                o.paymentMethod IS NULL
                OR UPPER(o.paymentMethod) <> 'HOLD'
            )

            AND NOT (
                UPPER(COALESCE(o.orderType, '')) = 'ONLINE'
                AND (o.isPaymentReported IS NULL OR o.isPaymentReported = false)
                AND UPPER(COALESCE(o.paymentMethod, '')) IN (
                    'VNPAY',
                    'VIETQR',
                    'MIXED_VNPAY',
                    'MIXED_VIETQR'
                )
                AND o.status IN (0, 4)
            )
            """)
    Long countAdminOrdersForTabs(
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("orderType") String orderType,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    List<Order> findByCustomer_UserIdOrderByCreatedAtDesc(Integer customerId);

    Optional<Order> findByIdAndCustomer_UserId(Integer orderId, Integer customerId);

    /**
     * Khóa đúng row Orders thuộc khách hàng hiện tại cho các mutation Customer Order.
     * Giữ nguyên query đọc thường ở trên cho GET/list/detail.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT o
                FROM Order o
                WHERE o.id = :orderId
                  AND o.customer.userId = :customerId
            """)
    Optional<Order> findByIdAndCustomer_UserIdForUpdate(
            @Param("orderId") Integer orderId,
            @Param("customerId") Integer customerId
    );

    @EntityGraph(attributePaths = {
            "customer",
            "customer.user",
            "cashier",
            "cashier.user",
            "voucher"
    })
    @Query("""
                SELECT o
                FROM Order o
                WHERE o.status = 0
                  AND UPPER(o.paymentMethod) = 'HOLD'
                  AND UPPER(o.orderType) IN ('POS', 'IN_STORE')
                  AND (:cashierId IS NULL OR o.cashier.userId = :cashierId)
                ORDER BY o.createdAt DESC
            """)
    List<Order> findHeldOrders(@Param("cashierId") Integer cashierId);

    @EntityGraph(attributePaths = {
            "customer",
            "customer.user",
            "cashier",
            "cashier.user",
            "voucher"
    })
    @Query("""
                SELECT o
                FROM Order o
                WHERE o.id = :orderId
            """)
    Optional<Order> findDetailById(@Param("orderId") Integer orderId);

    /**
     * Khóa row Orders cho các mutation POS để hai request đồng thời
     * không cùng confirm/cancel/rollback một hóa đơn.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "customer",
            "customer.user",
            "cashier",
            "cashier.user",
            "voucher"
    })
    @Query("""
                SELECT o
                FROM Order o
                WHERE o.id = :orderId
            """)
    Optional<Order> findDetailByIdForUpdate(
            @Param("orderId") Integer orderId
    );

    @EntityGraph(attributePaths = {
            "customer",
            "customer.user",
            "cashier",
            "cashier.user",
            "voucher"
    })
    @Query("""
                SELECT o
                FROM Order o
                WHERE o.id = :orderId
                  AND o.status = 0
                  AND UPPER(o.paymentMethod) = 'HOLD'
                  AND UPPER(o.orderType) IN ('POS', 'IN_STORE')
            """)
    Optional<Order> findHeldOrderById(@Param("orderId") Integer orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "customer",
            "customer.user",
            "cashier",
            "cashier.user",
            "voucher"
    })
    @Query("""
                SELECT o
                FROM Order o
                WHERE o.id = :orderId
                  AND o.status = 0
                  AND UPPER(o.paymentMethod) = 'HOLD'
                  AND UPPER(o.orderType) IN ('POS', 'IN_STORE')
            """)
    Optional<Order> findHeldOrderByIdForUpdate(
            @Param("orderId") Integer orderId
    );

    List<Order> findByStatusAndCreatedAtBefore(
            Integer status,
            LocalDateTime createdAt
    );

    @EntityGraph(attributePaths = {
            "customer",
            "customer.user",
            "cashier",
            "cashier.user",
            "voucher"
    })
    @Query("""
                SELECT o
                FROM Order o
                WHERE o.status = 0
                  AND UPPER(o.paymentMethod) = 'HOLD'
                  AND UPPER(o.orderType) IN ('POS', 'IN_STORE')
                  AND o.customerPhone = :phone
                ORDER BY o.createdAt DESC
            """)
    List<Order> findActiveHeldOrdersByCustomerPhone(@Param("phone") String phone);

    @EntityGraph(attributePaths = {
            "customer",
            "customer.user",
            "cashier",
            "cashier.user",
            "voucher"
    })
    @Query("""
                SELECT o
                FROM Order o
                WHERE o.id = :orderId
                  AND o.status = 0
                  AND UPPER(o.orderType) IN ('POS', 'IN_STORE')
                  AND UPPER(o.paymentMethod) IN (
                      'VNPAY',
                      'VIETQR',
                      'MIXED',
                      'MIXED_VNPAY',
                      'MIXED_VIETQR'
                  )
            """)
    Optional<Order> findPendingPaymentOrderById(@Param("orderId") Integer orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "customer",
            "customer.user",
            "cashier",
            "cashier.user",
            "voucher"
    })
    @Query("""
                SELECT o
                FROM Order o
                WHERE o.id = :orderId
                  AND o.status = 0
                  AND UPPER(o.orderType) IN ('POS', 'IN_STORE')
                  AND UPPER(o.paymentMethod) IN (
                      'VNPAY',
                      'VIETQR',
                      'MIXED',
                      'MIXED_VNPAY',
                      'MIXED_VIETQR'
                  )
            """)
    Optional<Order> findPendingPaymentOrderByIdForUpdate(
            @Param("orderId") Integer orderId
    );

    @Query(value = """
                SELECT COALESCE(SUM(o.FinalAmount - ISNULL(o.Shippingfee, 0)), 0)
                FROM [Orders] o
                WHERE o.Status = :status
                  AND o.CompletedAt >= :fromDate
                  AND o.CompletedAt < :toDate
            """, nativeQuery = true)
    BigDecimal sumFinalAmountByStatusAndCompletedAtBetween(
            @Param("status") Integer status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query(value = """
                SELECT COUNT(o.Id)
                FROM [Orders] o
                WHERE o.Status = :status
                  AND o.CompletedAt >= :fromDate
                  AND o.CompletedAt < :toDate
            """, nativeQuery = true)
    Long countOrdersByStatusAndCompletedAtBetween(
            @Param("status") Integer status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query(value = """
                SELECT *
                FROM [Orders] o
                WHERE o.Status = :status
                  AND o.CompletedAt >= :fromDate
                  AND o.CompletedAt < :toDate
                ORDER BY o.CompletedAt ASC
            """, nativeQuery = true)
    List<Order> findCompletedOrdersForChart(
            @Param("status") Integer status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query("""
                SELECT o.orderType, COALESCE(SUM(o.finalAmount - COALESCE(o.shippingFee, 0)), 0), COUNT(o)
                FROM Order o
                WHERE o.status = :status
                  AND o.completedAt >= :fromDate
                  AND o.completedAt < :toDate
                GROUP BY o.orderType
            """)
    List<Object[]> getSummaryBreakdownByOrderType(
            @Param("status") Integer status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    /*
     * =========================================================
     * OWNER REPORT - QUERY RIÊNG
     * =========================================================
     *
     * Không sửa semantics các query cũ vì OrderRepository đang được dùng bởi
     * Admin Order, Customer Order, POS và payment callback.
     *
     * Một giao dịch bán được ghi nhận khi CompletedAt != NULL.
     * Sau đó Order có thể chuyển 3 -> 6 -> 7 do return, nhưng giao dịch bán
     * lịch sử vẫn phải còn trong báo cáo.
     *
     * Doanh thu bán hàng:
     * - ONLINE: FinalAmount đã gồm shipping -> loại Shippingfee.
     * - POS/IN_STORE: FinalAmount không có shipping -> dùng nguyên FinalAmount.
     *
     * Chỉ nhận các OrderType nghiệp vụ hợp lệ của hệ thống để dữ liệu test/null
     * không bị gộp nhầm vào POS.
     */
    @Query(value = """
                SELECT COALESCE(SUM(
                    CASE
                        WHEN UPPER(LTRIM(RTRIM(COALESCE(o.OrderType, '')))) = 'ONLINE'
                            THEN o.FinalAmount - ISNULL(o.Shippingfee, 0)
                        ELSE o.FinalAmount
                    END
                ), 0)
                FROM [Orders] o
                WHERE o.CompletedAt IS NOT NULL
                  AND o.CompletedAt >= :fromDate
                  AND o.CompletedAt < :toDate
                  AND UPPER(LTRIM(RTRIM(COALESCE(o.OrderType, ''))))
                      IN ('ONLINE', 'IN_STORE', 'POS')
            """, nativeQuery = true)
    BigDecimal sumGrossSalesRevenueForOwnerReport(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query(value = """
                SELECT COUNT(o.Id)
                FROM [Orders] o
                WHERE o.CompletedAt IS NOT NULL
                  AND o.CompletedAt >= :fromDate
                  AND o.CompletedAt < :toDate
                  AND UPPER(LTRIM(RTRIM(COALESCE(o.OrderType, ''))))
                      IN ('ONLINE', 'IN_STORE', 'POS')
            """, nativeQuery = true)
    Long countCompletedSalesForOwnerReport(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query(value = """
                SELECT *
                FROM [Orders] o
                WHERE o.CompletedAt IS NOT NULL
                  AND o.CompletedAt >= :fromDate
                  AND o.CompletedAt < :toDate
                  AND UPPER(LTRIM(RTRIM(COALESCE(o.OrderType, ''))))
                      IN ('ONLINE', 'IN_STORE', 'POS')
                ORDER BY o.CompletedAt ASC, o.Id ASC
            """, nativeQuery = true)
    List<Order> findSalesForOwnerReportChart(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query(value = """
                SELECT
                    UPPER(LTRIM(RTRIM(o.OrderType))) AS orderType,
                    COALESCE(SUM(
                        CASE
                            WHEN UPPER(LTRIM(RTRIM(o.OrderType))) = 'ONLINE'
                                THEN o.FinalAmount - ISNULL(o.Shippingfee, 0)
                            ELSE o.FinalAmount
                        END
                    ), 0) AS revenue,
                    COUNT(o.Id) AS totalOrders
                FROM [Orders] o
                WHERE o.CompletedAt IS NOT NULL
                  AND o.CompletedAt >= :fromDate
                  AND o.CompletedAt < :toDate
                  AND UPPER(LTRIM(RTRIM(COALESCE(o.OrderType, ''))))
                      IN ('ONLINE', 'IN_STORE', 'POS')
                GROUP BY UPPER(LTRIM(RTRIM(o.OrderType)))
            """, nativeQuery = true)
    List<Object[]> getOwnerReportSalesBreakdownByOrderType(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    // --- JOB TỰ ĐỘNG HỦY ĐƠN ONLINE TRẢ TRƯỚC CHƯA THANH TOÁN ---
    @Query("""
                SELECT o FROM Order o
                WHERE o.status = 0
                  AND UPPER(o.orderType) = 'ONLINE'
                  AND (o.isPaymentReported IS NULL OR o.isPaymentReported = false)
                  AND UPPER(o.paymentMethod) IN ('VNPAY', 'VIETQR', 'MIXED_VNPAY', 'MIXED_VIETQR')
                  AND o.createdAt <= :timeoutThreshold
            """)
    List<Order> findUnpaidPrepaidOrders(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);
}