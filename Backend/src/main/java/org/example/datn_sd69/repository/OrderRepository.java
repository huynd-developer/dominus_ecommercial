package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    // ==========================
    // CUSTOMER
    // ==========================

    List<Order> findByCustomer_UserIdOrderByCreatedAtDesc(Integer customerId);

    Optional<Order> findByIdAndCustomer_UserId(
            Integer orderId,
            Integer customerId
    );

    // ==========================
    // ADMIN LIST
    // ==========================

//    Page<Order> findByIsDeletedFalse(Pageable pageable);
//
//    Page<Order> findByStatusAndIsDeletedFalse(
//            Integer status,
//            Pageable pageable
//    );

    List<Order> findByOrderType(String orderType);

    List<Order> findByOrderTypeIn(List<String> orderTypes);

    // ==========================
    // ADMIN SEARCH
    // ==========================

    @Query("""
    SELECT o
    FROM Order o
    WHERE (
        :keyword IS NULL
        OR :keyword = ''
        OR LOWER(o.customerName)
            LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR o.customerPhone
            LIKE CONCAT('%', :keyword, '%')
    )
    ORDER BY o.createdAt DESC
""")
    Page<Order> search(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
SELECT o
FROM Order o
WHERE (
    :keyword IS NULL
    OR :keyword = ''
    OR LOWER(o.customerName)
        LIKE LOWER(CONCAT('%', :keyword, '%'))
    OR o.customerPhone
        LIKE CONCAT('%', :keyword, '%')
)
AND (
    :status IS NULL
    OR o.status = :status
)
AND (
    :orderType IS NULL
    OR :orderType = ''
    OR o.orderType = :orderType
)
""")
    Page<Order> searchAdminOrders(
            @Param("keyword") String keyword,
            @Param("status") Integer status,
            @Param("orderType") String orderType,
            Pageable pageable
    );

    // ==========================
    // ORDER DETAIL
    // ==========================

    @Query("""
        SELECT o
        FROM Order o
        LEFT JOIN FETCH o.customer
        LEFT JOIN FETCH o.voucher
        LEFT JOIN FETCH o.cashier
        WHERE o.id = :id
    """)
    Optional<Order> findDetailById(
            @Param("id") Integer id
    );

    // ==========================
    // REPORT
    // ==========================

    @Query(value = """
            SELECT COALESCE(SUM(o.FinalAmount), 0)
            FROM Orders o
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
            FROM Orders o
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
            FROM Orders o
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

    List<Order> findByStatusAndCreatedAtBefore(
            Integer status,
            LocalDateTime createdAt
    );
}