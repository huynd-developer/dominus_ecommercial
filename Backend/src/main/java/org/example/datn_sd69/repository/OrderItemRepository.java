package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.modules.report.projection.BestSellingProductProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    @Query("""
            SELECT oi FROM OrderItem oi
            LEFT JOIN FETCH oi.productVariant pv
            WHERE oi.order.id = :orderId
            """)
    List<OrderItem> findByOrderId(@Param("orderId") Integer orderId);

    @Query(value = """
            SELECT COALESCE(SUM(oi.Quantity), 0)
            FROM OrderItem oi
            INNER JOIN Orders o ON o.Id = oi.OrderId
            WHERE o.Status = :status
              AND o.CreatedAt >= :fromDate
              AND o.CreatedAt < :toDate
            """, nativeQuery = true)
    Long sumSoldQuantityByCompletedOrders(
            @Param("status") Integer status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    @Query(value = """
            SELECT
                p.Id AS productId,
                p.Name AS productName,
                b.Name AS brandName,
                COALESCE(SUM(oi.Quantity), 0) AS totalSold,
                COALESCE(SUM(oi.FinalPrice), 0) AS revenue,
                MAX(oi.Image) AS imageUrl
            FROM OrderItem oi
            INNER JOIN Orders o ON o.Id = oi.OrderId
            INNER JOIN ProductVariant pv ON pv.Id = oi.ProductVariantId
            INNER JOIN Product p ON p.Id = pv.ProductId
            INNER JOIN Brand b ON b.Id = p.BrandId
            WHERE o.Status = :status
              AND o.CreatedAt >= :fromDate
              AND o.CreatedAt < :toDate
            GROUP BY p.Id, p.Name, b.Name
            ORDER BY SUM(oi.Quantity) DESC, SUM(oi.FinalPrice) DESC
            """, nativeQuery = true)
    List<BestSellingProductProjection> findBestSellingProducts(
            @Param("status") Integer status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );
    List<OrderItem> findByOrder_Id(Integer orderId);

    List<OrderItem> findByOrder_IdOrderByIdAsc(Integer orderId);

}