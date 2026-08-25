package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.InventoryLot;
import org.example.datn_sd69.repository.projection.InventoryLotStatusProjection;
import org.example.datn_sd69.repository.projection.InventoryOverviewProjection;
import org.example.datn_sd69.repository.projection.InventorySummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface InventoryQueryRepository
        extends Repository<InventoryLot, Integer> {

    /*
     * =========================================================
     * DASHBOARD SUMMARY
     * =========================================================
     */

    @Query(
            value = """
                    SELECT
                        COUNT_BIG(*) AS totalSku,

                        COALESCE(
                            SUM(
                                CONVERT(
                                    BIGINT,
                                    CASE
                                        WHEN TotalQuantity > 0 THEN 1
                                        ELSE 0
                                    END
                                )
                            ),
                            0
                        ) AS inStockSku,

                        COALESCE(
                            SUM(
                                CONVERT(
                                    BIGINT,
                                    CASE
                                        WHEN TotalQuantity = 0 THEN 1
                                        ELSE 0
                                    END
                                )
                            ),
                            0
                        ) AS outOfStockSku,

                        COALESCE(SUM(TotalQuantity), 0)
                            AS totalQuantity,

                        COALESCE(SUM(SellableQuantity), 0)
                            AS sellableQuantity,

                        COALESCE(SUM(NearExpiryQuantity), 0)
                            AS nearExpiryQuantity,

                        COALESCE(SUM(ExpiredQuantity), 0)
                            AS expiredQuantity

                    FROM dbo.vw_ProductVariantInventory
                    """,
            nativeQuery = true
    )
    InventorySummaryProjection getInventorySummary();


    /*
     * =========================================================
     * TỔNG QUAN THEO SKU
     * =========================================================
     */

    @Query(
            value = """
                    SELECT
                        v.ProductVariantId AS productVariantId,
                        v.Sku AS sku,
                        v.ProductName AS productName,
                        productImage.ImageUrl AS imageUrl,
                        c.Value AS capacityValue,
                        bt.Name AS bottleTypeName,
                        v.TotalQuantity AS totalQuantity,
                        v.SellableQuantity AS sellableQuantity,
                        v.NearExpiryQuantity AS nearExpiryQuantity,
                        v.ExpiredQuantity AS expiredQuantity

                    FROM dbo.vw_ProductVariantInventory v
                    LEFT JOIN dbo.ProductVariant pv
                        ON pv.Id = v.ProductVariantId
                    LEFT JOIN dbo.Product p
                        ON p.Id = pv.ProductId
                    LEFT JOIN dbo.Capacity c
                        ON c.Id = pv.CapacityId
                    LEFT JOIN dbo.BottleType bt
                        ON bt.Id = pv.BottleTypeId
                    OUTER APPLY (
                        SELECT TOP 1
                            pi.ImageUrl
                        FROM dbo.ProductImage pi
                        WHERE pi.ProductId = pv.ProductId
                        ORDER BY
                            CASE WHEN ISNULL(pi.IsPrimary, 0) = 1 THEN 0 ELSE 1 END,
                            pi.Id ASC
                    ) productImage

                    WHERE
                        (
                            :keyword IS NULL
                            OR :keyword = ''
                            OR v.Sku LIKE CONCAT('%', :keyword, '%')
                            OR v.ProductName LIKE CONCAT('%', :keyword, '%')
                        )

                        AND
                        (
                            :nearExpiryFlag IS NULL

                            OR (
                                :nearExpiryFlag = 1
                                AND v.NearExpiryQuantity > 0
                            )

                            OR (
                                :nearExpiryFlag = 0
                                AND v.NearExpiryQuantity = 0
                            )
                        )

                        AND
                        (
                            :expiredFlag IS NULL

                            OR (
                                :expiredFlag = 1
                                AND v.ExpiredQuantity > 0
                            )

                            OR (
                                :expiredFlag = 0
                                AND v.ExpiredQuantity = 0
                            )
                        )

                        AND
                        (
                            :stockStatus IS NULL
                            OR :stockStatus = 'ALL'

                            OR (
                                :stockStatus = 'IN_STOCK'
                                AND v.TotalQuantity > 0
                            )

                            OR (
                                :stockStatus = 'OUT_OF_STOCK'
                                AND v.TotalQuantity = 0
                            )
                        )

                        AND
                        (
                            :selectableOnlyFlag = 0

                            OR (
                                :selectableOnlyFlag = 1
                                AND pv.Id IS NOT NULL
                                AND p.Id IS NOT NULL
                                AND ISNULL(pv.IsDeleted, 0) = 0
                                AND ISNULL(p.IsDeleted, 0) = 0
                            )
                        )

                    ORDER BY
                        v.ProductName ASC,
                        v.Sku ASC
                    """,

            countQuery = """
                    SELECT COUNT(*)

                    FROM dbo.vw_ProductVariantInventory v
                    LEFT JOIN dbo.ProductVariant pv
                        ON pv.Id = v.ProductVariantId
                    LEFT JOIN dbo.Product p
                        ON p.Id = pv.ProductId

                    WHERE
                        (
                            :keyword IS NULL
                            OR :keyword = ''
                            OR v.Sku LIKE CONCAT('%', :keyword, '%')
                            OR v.ProductName LIKE CONCAT('%', :keyword, '%')
                        )

                        AND
                        (
                            :nearExpiryFlag IS NULL

                            OR (
                                :nearExpiryFlag = 1
                                AND v.NearExpiryQuantity > 0
                            )

                            OR (
                                :nearExpiryFlag = 0
                                AND v.NearExpiryQuantity = 0
                            )
                        )

                        AND
                        (
                            :expiredFlag IS NULL

                            OR (
                                :expiredFlag = 1
                                AND v.ExpiredQuantity > 0
                            )

                            OR (
                                :expiredFlag = 0
                                AND v.ExpiredQuantity = 0
                            )
                        )

                        AND
                        (
                            :stockStatus IS NULL
                            OR :stockStatus = 'ALL'

                            OR (
                                :stockStatus = 'IN_STOCK'
                                AND v.TotalQuantity > 0
                            )

                            OR (
                                :stockStatus = 'OUT_OF_STOCK'
                                AND v.TotalQuantity = 0
                            )
                        )

                        AND
                        (
                            :selectableOnlyFlag = 0

                            OR (
                                :selectableOnlyFlag = 1
                                AND pv.Id IS NOT NULL
                                AND p.Id IS NOT NULL
                                AND ISNULL(pv.IsDeleted, 0) = 0
                                AND ISNULL(p.IsDeleted, 0) = 0
                            )
                        )
                    """,

            nativeQuery = true
    )
    Page<InventoryOverviewProjection> findInventoryOverview(

            @Param("keyword")
            String keyword,

            @Param("nearExpiryFlag")
            Integer nearExpiryFlag,

            @Param("expiredFlag")
            Integer expiredFlag,

            @Param("selectableOnlyFlag")
            Integer selectableOnlyFlag,

            @Param("stockStatus")
            String stockStatus,

            Pageable pageable
    );


    /*
     * =========================================================
     * LÔ SẮP HẾT HẠN
     * =========================================================
     */

    @Query(
            value = """
                    SELECT
                        v.InventoryLotId AS inventoryLotId,
                        v.ProductVariantId AS productVariantId,
                        v.Sku AS sku,
                        v.ProductName AS productName,
                        productImage.ImageUrl AS imageUrl,
                        v.LotCode AS lotCode,
                        v.ManufacturedDate AS manufacturedDate,
                        v.ReceivedDate AS receivedDate,
                        v.ExpirationDate AS expirationDate,
                        v.InitialQuantity AS initialQuantity,
                        v.QuantityOnHand AS quantityOnHand,
                        v.DaysToExpiry AS daysToExpiry,
                        v.IsExpired AS expired,
                        v.IsNearExpiry AS nearExpiry,
                        v.SellableQuantity AS sellableQuantity

                    FROM dbo.vw_InventoryLotStatus v
                    LEFT JOIN dbo.ProductVariant pv
                        ON pv.Id = v.ProductVariantId
                    OUTER APPLY (
                        SELECT TOP 1
                            pi.ImageUrl
                        FROM dbo.ProductImage pi
                        WHERE pi.ProductId = pv.ProductId
                        ORDER BY
                            CASE WHEN ISNULL(pi.IsPrimary, 0) = 1 THEN 0 ELSE 1 END,
                            pi.Id ASC
                    ) productImage

                    WHERE
                        v.IsNearExpiry = 1
                        AND v.IsExpired = 0
                        AND v.QuantityOnHand > 0

                        AND
                        (
                            :keyword IS NULL
                            OR :keyword = ''
                            OR v.Sku LIKE CONCAT('%', :keyword, '%')
                            OR v.ProductName LIKE CONCAT('%', :keyword, '%')
                            OR v.LotCode LIKE CONCAT('%', :keyword, '%')
                        )

                    ORDER BY
                        v.ExpirationDate ASC,
                        v.ProductName ASC,
                        v.Sku ASC
                    """,

            countQuery = """
                    SELECT COUNT(*)

                    FROM dbo.vw_InventoryLotStatus

                    WHERE
                        IsNearExpiry = 1
                        AND IsExpired = 0
                        AND QuantityOnHand > 0

                        AND
                        (
                            :keyword IS NULL
                            OR :keyword = ''
                            OR Sku LIKE CONCAT('%', :keyword, '%')
                            OR ProductName LIKE CONCAT('%', :keyword, '%')
                            OR LotCode LIKE CONCAT('%', :keyword, '%')
                        )
                    """,

            nativeQuery = true
    )
    Page<InventoryLotStatusProjection> findNearExpiryLots(
            @Param("keyword") String keyword,
            Pageable pageable
    );


    /*
     * =========================================================
     * LÔ ĐÃ HẾT HẠN
     * =========================================================
     */

    @Query(
            value = """
                    SELECT
                        v.InventoryLotId AS inventoryLotId,
                        v.ProductVariantId AS productVariantId,
                        v.Sku AS sku,
                        v.ProductName AS productName,
                        productImage.ImageUrl AS imageUrl,
                        v.LotCode AS lotCode,
                        v.ManufacturedDate AS manufacturedDate,
                        v.ReceivedDate AS receivedDate,
                        v.ExpirationDate AS expirationDate,
                        v.InitialQuantity AS initialQuantity,
                        v.QuantityOnHand AS quantityOnHand,
                        v.DaysToExpiry AS daysToExpiry,
                        v.IsExpired AS expired,
                        v.IsNearExpiry AS nearExpiry,
                        v.SellableQuantity AS sellableQuantity

                    FROM dbo.vw_InventoryLotStatus v
                    LEFT JOIN dbo.ProductVariant pv
                        ON pv.Id = v.ProductVariantId
                    OUTER APPLY (
                        SELECT TOP 1
                            pi.ImageUrl
                        FROM dbo.ProductImage pi
                        WHERE pi.ProductId = pv.ProductId
                        ORDER BY
                            CASE WHEN ISNULL(pi.IsPrimary, 0) = 1 THEN 0 ELSE 1 END,
                            pi.Id ASC
                    ) productImage

                    WHERE
                        v.IsExpired = 1
                        AND v.QuantityOnHand > 0

                        AND
                        (
                            :keyword IS NULL
                            OR :keyword = ''
                            OR v.Sku LIKE CONCAT('%', :keyword, '%')
                            OR v.ProductName LIKE CONCAT('%', :keyword, '%')
                            OR v.LotCode LIKE CONCAT('%', :keyword, '%')
                        )

                    ORDER BY
                        v.ExpirationDate ASC,
                        v.ProductName ASC,
                        v.Sku ASC
                    """,

            countQuery = """
                    SELECT COUNT(*)

                    FROM dbo.vw_InventoryLotStatus

                    WHERE
                        IsExpired = 1
                        AND QuantityOnHand > 0

                        AND
                        (
                            :keyword IS NULL
                            OR :keyword = ''
                            OR Sku LIKE CONCAT('%', :keyword, '%')
                            OR ProductName LIKE CONCAT('%', :keyword, '%')
                            OR LotCode LIKE CONCAT('%', :keyword, '%')
                        )
                    """,

            nativeQuery = true
    )
    Page<InventoryLotStatusProjection> findExpiredLots(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}