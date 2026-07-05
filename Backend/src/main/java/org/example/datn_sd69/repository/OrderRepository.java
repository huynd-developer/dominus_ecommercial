package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Order;
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
    List<Order> findByCustomer_UserIdOrderByCreatedAtDesc(Integer customerId);

    Optional<Order> findByIdAndCustomer_UserId(Integer orderId, Integer customerId);

}