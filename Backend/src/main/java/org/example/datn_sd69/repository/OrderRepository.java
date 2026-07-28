package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
    @Query("""
        SELECT o
        FROM Order o
        WHERE
            (
                :keyword IS NULL
                OR TRIM(:keyword) = ''
                OR LOWER(o.customerName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(o.customerPhone) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR CAST(o.id AS string) LIKE CONCAT('%', :keyword, '%')
            )
            AND (:status IS NULL OR o.status = :status)
            AND (:orderType IS NULL OR o.orderType = :orderType)
            AND (CAST(:startDate AS timestamp) IS NULL OR o.createdAt >= :startDate)
            AND (CAST(:endDate AS timestamp) IS NULL OR o.createdAt <= :endDate)
        ORDER BY o.createdAt DESC
    """)
    Page<Order> searchAdminOrders(
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("orderType") String orderType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query(value = """
        SELECT COALESCE(SUM(o.FinalAmount), 0)
        FROM [Orders] o
        WHERE o.Status = :status
          AND o.CreatedAt >= :fromDate
          AND o.CreatedAt < :toDate
    """, nativeQuery = true)
    BigDecimal sumFinalAmountByStatusAndCreatedAtBetween(
            @Param("status") Integer status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query(value = """
        SELECT COUNT(o.Id)
        FROM [Orders] o
        WHERE o.Status = :status
          AND o.CreatedAt >= :fromDate
          AND o.CreatedAt < :toDate
    """, nativeQuery = true)
    Long countOrdersByStatusAndCreatedAtBetween(
            @Param("status") Integer status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query(value = """
        SELECT *
        FROM [Orders] o
        WHERE o.Status = :status
          AND o.CreatedAt >= :fromDate
          AND o.CreatedAt < :toDate
        ORDER BY o.CreatedAt ASC
    """, nativeQuery = true)
    List<Order> findCompletedOrdersForChart(
            @Param("status") Integer status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    List<Order> findByCustomer_UserIdOrderByCreatedAtDesc(Integer customerId);

    Optional<Order> findByIdAndCustomer_UserId(Integer orderId, Integer customerId);

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
}