package org.example.datn_sd69.repository;

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
     * Dùng LEFT JOIN FETCH để nếu capacity/bottleType bị null
     * thì item vẫn hiển thị, không bị mất khỏi chi tiết đơn.
     */
    @Query("""
        SELECT oi
        FROM OrderItem oi
        JOIN FETCH oi.productVariant pv
        LEFT JOIN FETCH pv.product p
        LEFT JOIN FETCH pv.capacity c
        LEFT JOIN FETCH pv.bottleType bt
        WHERE oi.order.id = :orderId
        ORDER BY oi.id ASC
    """)
    List<OrderItem> findDetailByOrderId(@Param("orderId") Integer orderId);

    /**
     * Tổng số lượng sản phẩm đã bán.
     *
     * Chỉ tính đơn hoàn thành.
     */
    @Query(value = """
        SELECT COALESCE(SUM(oi.Quantity), 0)
        FROM [OrderItem] oi
        INNER JOIN [Orders] o ON o.Id = oi.OrderId
        WHERE o.Status = :status
          AND o.CreatedAt >= :fromDate
          AND o.CreatedAt < :toDate
    """, nativeQuery = true)
    Long sumSoldQuantityByCompletedOrders(
            @Param("status") Integer status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    /**
     * Top sản phẩm bán chạy.
     *
     * Lưu ý:
     * Sau khi sửa checkout, OrderItem.FinalPrice là giá cuối / 1 sản phẩm.
     * Vì vậy doanh thu dòng phải tính:
     *
     * FinalPrice * Quantity
     *
     * Không được SUM(FinalPrice) vì sẽ sai khi quantity > 1.
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
          AND o.CreatedAt >= :fromDate
          AND o.CreatedAt < :toDate
        GROUP BY p.Id, p.Name, b.Name
        ORDER BY COALESCE(SUM(oi.Quantity), 0) DESC,
                 COALESCE(SUM(oi.FinalPrice * oi.Quantity), 0) DESC
    """, nativeQuery = true)
    List<BestSellingProductProjection> findBestSellingProducts(
            @Param("status") Integer status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );

    List<OrderItem> findAllByOrder_Id(Integer orderId);

    /**
     * Dùng cho lịch sử đơn hàng / chi tiết đơn hàng phía khách.
     */
    @Query("""
        SELECT oi
        FROM OrderItem oi
        JOIN FETCH oi.productVariant pv
        LEFT JOIN FETCH pv.product p
        LEFT JOIN FETCH pv.capacity c
        LEFT JOIN FETCH pv.bottleType bt
        WHERE oi.order.id = :orderId
        ORDER BY oi.id ASC
    """)
    List<OrderItem> findByOrderIdWithVariant(@Param("orderId") Integer orderId);
}