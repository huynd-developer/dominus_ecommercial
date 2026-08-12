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
                            AS expiredQuantity,

                        COALESCE(SUM(LockedQuantity), 0)
                            AS lockedQuantity

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
                        ProductVariantId AS productVariantId,
                        Sku AS sku,
                        ProductName AS productName,
                        TotalQuantity AS totalQuantity,
                        SellableQuantity AS sellableQuantity,
                        NearExpiryQuantity AS nearExpiryQuantity,
                        ExpiredQuantity AS expiredQuantity,
                        LockedQuantity AS lockedQuantity

                    FROM dbo.vw_ProductVariantInventory

                    WHERE
                        (
                            :keyword IS NULL
                            OR :keyword = ''
                            OR Sku LIKE CONCAT('%', :keyword, '%')
                            OR ProductName LIKE CONCAT('%', :keyword, '%')
                        )

                        AND
                        (
                            :nearExpiryFlag IS NULL

                            OR (
                                :nearExpiryFlag = 1
                                AND NearExpiryQuantity > 0
                            )

                            OR (
                                :nearExpiryFlag = 0
                                AND NearExpiryQuantity = 0
                            )
                        )

                        AND
                        (
                            :expiredFlag IS NULL

                            OR (
                                :expiredFlag = 1
                                AND ExpiredQuantity > 0
                            )

                            OR (
                                :expiredFlag = 0
                                AND ExpiredQuantity = 0
                            )
                        )

                        AND
                        (
                            :lockedFlag IS NULL

                            OR (
                                :lockedFlag = 1
                                AND LockedQuantity > 0
                            )

                            OR (
                                :lockedFlag = 0
                                AND LockedQuantity = 0
                            )
                        )

                        AND
                        (
                            :stockStatus IS NULL
                            OR :stockStatus = 'ALL'

                            OR (
                                :stockStatus = 'IN_STOCK'
                                AND TotalQuantity > 0
                            )

                            OR (
                                :stockStatus = 'OUT_OF_STOCK'
                                AND TotalQuantity = 0
                            )
                        )

                    ORDER BY
                        ProductName ASC,
                        Sku ASC
                    """,

            countQuery = """
                    SELECT COUNT(*)

                    FROM dbo.vw_ProductVariantInventory

                    WHERE
                        (
                            :keyword IS NULL
                            OR :keyword = ''
                            OR Sku LIKE CONCAT('%', :keyword, '%')
                            OR ProductName LIKE CONCAT('%', :keyword, '%')
                        )

                        AND
                        (
                            :nearExpiryFlag IS NULL

                            OR (
                                :nearExpiryFlag = 1
                                AND NearExpiryQuantity > 0
                            )

                            OR (
                                :nearExpiryFlag = 0
                                AND NearExpiryQuantity = 0
                            )
                        )

                        AND
                        (
                            :expiredFlag IS NULL

                            OR (
                                :expiredFlag = 1
                                AND ExpiredQuantity > 0
                            )

                            OR (
                                :expiredFlag = 0
                                AND ExpiredQuantity = 0
                            )
                        )

                        AND
                        (
                            :lockedFlag IS NULL

                            OR (
                                :lockedFlag = 1
                                AND LockedQuantity > 0
                            )

                            OR (
                                :lockedFlag = 0
                                AND LockedQuantity = 0
                            )
                        )

                        AND
                        (
                            :stockStatus IS NULL
                            OR :stockStatus = 'ALL'

                            OR (
                                :stockStatus = 'IN_STOCK'
                                AND TotalQuantity > 0
                            )

                            OR (
                                :stockStatus = 'OUT_OF_STOCK'
                                AND TotalQuantity = 0
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

            @Param("lockedFlag")
            Integer lockedFlag,

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
                        InventoryLotId AS inventoryLotId,
                        ProductVariantId AS productVariantId,
                        Sku AS sku,
                        ProductName AS productName,
                        LotCode AS lotCode,
                        ManufacturedDate AS manufacturedDate,
                        ReceivedDate AS receivedDate,
                        ExpirationDate AS expirationDate,
                        InitialQuantity AS initialQuantity,
                        QuantityOnHand AS quantityOnHand,
                        IsLocked AS locked,
                        LockReason AS lockReason,
                        DaysToExpiry AS daysToExpiry,
                        IsExpired AS expired,
                        IsNearExpiry AS nearExpiry,
                        SellableQuantity AS sellableQuantity

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

                    ORDER BY
                        ExpirationDate ASC,
                        ProductName ASC,
                        Sku ASC
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
                        InventoryLotId AS inventoryLotId,
                        ProductVariantId AS productVariantId,
                        Sku AS sku,
                        ProductName AS productName,
                        LotCode AS lotCode,
                        ManufacturedDate AS manufacturedDate,
                        ReceivedDate AS receivedDate,
                        ExpirationDate AS expirationDate,
                        InitialQuantity AS initialQuantity,
                        QuantityOnHand AS quantityOnHand,
                        IsLocked AS locked,
                        LockReason AS lockReason,
                        DaysToExpiry AS daysToExpiry,
                        IsExpired AS expired,
                        IsNearExpiry AS nearExpiry,
                        SellableQuantity AS sellableQuantity

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

                    ORDER BY
                        ExpirationDate ASC,
                        ProductName ASC,
                        Sku ASC
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


    /*
     * =========================================================
     * LÔ ĐANG KHÓA
     * =========================================================
     */

    @Query(
            value = """
                    SELECT
                        InventoryLotId AS inventoryLotId,
                        ProductVariantId AS productVariantId,
                        Sku AS sku,
                        ProductName AS productName,
                        LotCode AS lotCode,
                        ManufacturedDate AS manufacturedDate,
                        ReceivedDate AS receivedDate,
                        ExpirationDate AS expirationDate,
                        InitialQuantity AS initialQuantity,
                        QuantityOnHand AS quantityOnHand,
                        IsLocked AS locked,
                        LockReason AS lockReason,
                        DaysToExpiry AS daysToExpiry,
                        IsExpired AS expired,
                        IsNearExpiry AS nearExpiry,
                        SellableQuantity AS sellableQuantity

                    FROM dbo.vw_InventoryLotStatus

                    WHERE
                        IsLocked = 1

                        AND
                        (
                            :keyword IS NULL
                            OR :keyword = ''
                            OR Sku LIKE CONCAT('%', :keyword, '%')
                            OR ProductName LIKE CONCAT('%', :keyword, '%')
                            OR LotCode LIKE CONCAT('%', :keyword, '%')
                        )

                    ORDER BY
                        ExpirationDate ASC,
                        ProductName ASC,
                        Sku ASC
                    """,

            countQuery = """
                    SELECT COUNT(*)

                    FROM dbo.vw_InventoryLotStatus

                    WHERE
                        IsLocked = 1

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
    Page<InventoryLotStatusProjection> findLockedLots(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}