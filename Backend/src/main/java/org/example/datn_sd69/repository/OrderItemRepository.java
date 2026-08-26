package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.Order;
import org.example.datn_sd69.entity.OrderItem;
import org.example.datn_sd69.modules.report.projection.BestSellingProductProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    /**
     * Dùng cho xử lý đơn, hoàn kho khi VNPay thất bại/hủy đơn.
     */
    @Query("""
        SELECT oi
        FROM OrderItem oi
        LEFT JOIN FETCH oi.productVariant pv
        WHERE oi.order.id = :orderId
        ORDER BY oi.id ASC
    """)
    List<OrderItem> findByOrderId(@Param("orderId") Integer orderId);

    List<OrderItem> findByOrder_Id(Integer orderId);

    List<OrderItem> findByOrder_IdOrderByIdAsc(Integer orderId);

    /**
     * Dùng cho màn hình Admin xem chi tiết đơn hàng.
     *
     * Dùng LEFT JOIN FETCH để nếu productVariant/capacity/bottleType bị null
     * thì item vẫn hiển thị, không bị mất khỏi chi tiết đơn.
     */
    @Query("""
        SELECT oi
        FROM OrderItem oi
        LEFT JOIN FETCH oi.productVariant pv
        LEFT JOIN FETCH pv.product p
        LEFT JOIN FETCH p.brand b
        LEFT JOIN FETCH pv.capacity c
        LEFT JOIN FETCH pv.bottleType bt
        WHERE oi.order.id = :orderId
        ORDER BY oi.id ASC
    """)
    List<OrderItem> findDetailByOrderId(@Param("orderId") Integer orderId);

    /**
     * Query cũ giữ nguyên để không ảnh hưởng caller hiện tại ngoài Report.
     */
    @Query(value = """
        SELECT COALESCE(SUM(oi.Quantity), 0)
        FROM [OrderItem] oi
        INNER JOIN [Orders] o ON o.Id = oi.OrderId
        WHERE o.Status = :status
          AND o.CompletedAt >= :fromDate
          AND o.CompletedAt < :toDate
    """, nativeQuery = true)
    Long sumSoldQuantityByCompletedOrders(
            @Param("status") Integer status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    /**
     * Query cũ giữ nguyên để không đổi contract/caller hiện có.
     */
    @Query(value = """
        SELECT
            p.Id AS productId,
            p.Name AS productName,
            COALESCE(b.Name, N'Không rõ thương hiệu') AS brandName,
            COALESCE(SUM(oi.Quantity), 0) AS totalSold,
            COALESCE(SUM(oi.FinalPrice * oi.Quantity), 0) AS revenue,
            MAX(oi.Image) AS imageUrl
        FROM [OrderItem] oi
        INNER JOIN [Orders] o ON o.Id = oi.OrderId
        INNER JOIN [ProductVariant] pv ON pv.Id = oi.ProductVariantId
        INNER JOIN [Product] p ON p.Id = pv.ProductId
        LEFT JOIN [Brand] b ON b.Id = p.BrandId
        WHERE o.Status = :status
          AND o.CompletedAt >= :fromDate
          AND o.CompletedAt < :toDate
        GROUP BY p.Id, p.Name, b.Name
        ORDER BY COALESCE(SUM(oi.Quantity), 0) DESC,
                 COALESCE(SUM(oi.FinalPrice * oi.Quantity), 0) DESC
    """, nativeQuery = true)
    List<BestSellingProductProjection> findBestSellingProducts(
            @Param("status") Integer status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    /*
     * =========================================================
     * OWNER REPORT - QUERY RIÊNG
     * =========================================================
     *
     * CompletedAt là mốc giao dịch bán đã hoàn tất.
     * Không lọc Status hiện tại vì đơn có thể đã chuyển sang trạng thái return.
     */
    @Query(value = """
        SELECT COALESCE(SUM(oi.Quantity), 0)
        FROM [OrderItem] oi
        INNER JOIN [Orders] o ON o.Id = oi.OrderId
        WHERE o.CompletedAt IS NOT NULL
          AND o.CompletedAt >= :fromDate
          AND o.CompletedAt < :toDate
          AND UPPER(LTRIM(RTRIM(COALESCE(o.OrderType, ''))))
              IN ('ONLINE', 'IN_STORE', 'POS')
    """, nativeQuery = true)
    Long sumSoldQuantityForOwnerReport(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    /**
     * Top sản phẩm bán chạy theo lượng bán GROSS trong kỳ bán.
     *
     * Revenue của từng dòng được trừ phần Voucher toàn đơn phân bổ theo tỷ lệ:
     *   line = FinalPrice * Quantity
     *   allocatedVoucher = Order.DiscountAmount * line / Order.TotalAmount
     *   lineRevenue = line - allocatedVoucher
     *
     * FinalPrice đã chứa giảm Flash Sale nhưng không chứa Voucher toàn đơn.
     *
     * LEFT JOIN ProductVariant/Product để OrderItem lịch sử vẫn còn được thống kê
     * khi ProductVariantId đã bị ON DELETE SET NULL. Khi đó dùng ProductName snapshot.
     */
    @Query(value = """
        SELECT
            p.Id AS productId,
            p.Name AS productName,
            COALESCE(b.Name, N'Không rõ thương hiệu') AS brandName,
            CASE 
                WHEN c.Value IS NOT NULL THEN 
                    (
                        CASE 
                            WHEN c.Value = ROUND(c.Value, 0) THEN CAST(CAST(c.Value AS INT) AS VARCHAR(50)) + ' ml'
                            ELSE CAST(c.Value AS VARCHAR(50)) + ' ml'
                        END
                        + COALESCE(
                            CASE 
                                WHEN bt.Name IS NOT NULL AND LTRIM(RTRIM(bt.Name)) <> '' THEN ' - ' + bt.Name
                                WHEN oi.BottleTypeName IS NOT NULL AND LTRIM(RTRIM(oi.BottleTypeName)) <> '' THEN ' - ' + oi.BottleTypeName
                                ELSE NULL
                            END,
                            CASE 
                                WHEN pv.Sku IS NOT NULL AND LTRIM(RTRIM(pv.Sku)) <> '' THEN ' (' + pv.Sku + ')'
                                ELSE ''
                            END
                        )
                    )
                WHEN oi.CapacityName IS NOT NULL AND LTRIM(RTRIM(oi.CapacityName)) <> '' THEN 
                    (
                        CASE 
                            WHEN oi.CapacityName NOT LIKE '%ml%' THEN oi.CapacityName + ' ml'
                            ELSE oi.CapacityName
                        END
                        + COALESCE(
                            CASE 
                                WHEN oi.BottleTypeName IS NOT NULL AND LTRIM(RTRIM(oi.BottleTypeName)) <> '' THEN ' - ' + oi.BottleTypeName
                                ELSE NULL
                            END,
                            ''
                        )
                    )
                ELSE COALESCE(oi.BottleTypeName, pv.Sku, N'')
            END AS capacityName,
            COALESCE(SUM(oi.Quantity), 0) AS totalSold,
            COALESCE(SUM(
                CASE
                    WHEN ISNULL(o.TotalAmount, 0) > 0 THEN
                        (oi.FinalPrice * oi.Quantity)
                        - (
                            ISNULL(o.DiscountAmount, 0)
                            * (oi.FinalPrice * oi.Quantity)
                            / NULLIF(o.TotalAmount, 0)
                        )
                    ELSE (oi.FinalPrice * oi.Quantity)
                END
            ), 0) AS revenue,
            MAX(oi.Image) AS imageUrl
        FROM [OrderItem] oi
        INNER JOIN [Orders] o ON o.Id = oi.OrderId
        INNER JOIN [ProductVariant] pv ON pv.Id = oi.ProductVariantId
        INNER JOIN [Product] p ON p.Id = pv.ProductId AND p.IsDeleted = 0
        LEFT JOIN [Brand] b ON b.Id = p.BrandId
        LEFT JOIN [Capacity] c ON c.Id = pv.CapacityId
        LEFT JOIN [BottleType] bt ON bt.Id = pv.BottleTypeId
        WHERE o.CompletedAt IS NOT NULL
          AND o.CompletedAt >= :fromDate
          AND o.CompletedAt < :toDate
          AND UPPER(LTRIM(RTRIM(COALESCE(o.OrderType, ''))))
              IN ('ONLINE', 'IN_STORE', 'POS')
        GROUP BY
            p.Id,
            p.Name,
            b.Name,
            c.Value,
            oi.CapacityName,
            bt.Name,
            oi.BottleTypeName,
            pv.Sku
        ORDER BY totalSold DESC, revenue DESC
    """, nativeQuery = true)
    List<BestSellingProductProjection> findBestSellingProductsForOwnerReport(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    /**
     * Dùng cho lịch sử đơn hàng / chi tiết đơn hàng phía khách.
     */
    @Query("""
        SELECT oi
        FROM OrderItem oi
        LEFT JOIN FETCH oi.productVariant pv
        LEFT JOIN FETCH pv.product p
        LEFT JOIN FETCH p.brand b
        LEFT JOIN FETCH pv.capacity c
        LEFT JOIN FETCH pv.bottleType bt
        WHERE oi.order.id = :orderId
        ORDER BY oi.id ASC
    """)
    List<OrderItem> findByOrderIdWithVariant(@Param("orderId") Integer orderId);

    List<OrderItem> findByOrder(Order order);
}
