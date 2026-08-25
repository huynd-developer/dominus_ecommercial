package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.InventoryLot;
import org.example.datn_sd69.repository.projection.ExpiryAlertSummaryProjection;
import org.example.datn_sd69.repository.projection.ExpiryAlertViewProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ExpiryAlertRepository
        extends Repository<InventoryLot, Integer> {

    @Query(
            value = """
                    SELECT
                        LS.InventoryLotId AS id,
                        LS.ProductVariantId AS productVariantId,
                        LS.Sku AS sku,
                        LS.ProductName AS productName,
                        ImageData.ImageUrl AS imageUrl,
                        C.Value AS capacityValue,
                        BT.Name AS bottleTypeName,
                        LS.LotCode AS lotCode,
                        LS.QuantityOnHand AS quantityOnHand,
                        LS.SellableQuantity AS sellableQuantity,
                        LS.ExpirationDate AS expirationDate,
                        LS.DaysToExpiry AS daysToExpiry,
                        LS.IsNearExpiry AS isNearExpiry,
                        LS.IsExpired AS isExpired
                    FROM dbo.vw_InventoryLotStatus LS

                    LEFT JOIN dbo.ProductVariant PV
                        ON PV.Id = LS.ProductVariantId

                    LEFT JOIN dbo.Capacity C
                        ON C.Id = PV.CapacityId

                    LEFT JOIN dbo.BottleType BT
                        ON BT.Id = PV.BottleTypeId

                    OUTER APPLY (
                        SELECT TOP 1
                            PI.ImageUrl
                        FROM dbo.ProductImage PI
                        WHERE PI.ProductId = PV.ProductId
                        ORDER BY
                            CASE
                                WHEN PI.IsPrimary = 1 THEN 0
                                ELSE 1
                            END,
                            PI.Id ASC
                    ) ImageData

                    WHERE
                        (
                            :keyword IS NULL
                            OR LOWER(LS.Sku)
                                LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(LS.ProductName)
                                LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(LS.LotCode)
                                LIKE LOWER(CONCAT('%', :keyword, '%'))
                        )

                        AND
                        (
                            (
                                :groupName = 'NEAR_EXPIRY'
                                AND LS.IsNearExpiry = 1
                                AND LS.IsExpired = 0
                                AND LS.QuantityOnHand > 0
                            )

                            OR
                            (
                                :groupName = 'EXPIRED'
                                AND LS.IsExpired = 1
                                AND LS.QuantityOnHand > 0
                            )

                            OR
                            (
                                :groupName = 'ALL'
                                AND
                                (
                                    (
                                        LS.IsNearExpiry = 1
                                        AND LS.IsExpired = 0
                                        AND LS.QuantityOnHand > 0
                                    )

                                    OR
                                    (
                                        LS.IsExpired = 1
                                        AND LS.QuantityOnHand > 0
                                    )
                                )
                            )
                        )

                        AND (
                            :fromDays IS NULL
                            OR LS.DaysToExpiry >= :fromDays
                        )

                        AND (
                            :toDays IS NULL
                            OR LS.DaysToExpiry <= :toDays
                        )

                    ORDER BY
                        LS.DaysToExpiry ASC,
                        LS.InventoryLotId DESC
                    """,

            countQuery = """
                    SELECT COUNT(*)
                    FROM dbo.vw_InventoryLotStatus LS
                    WHERE
                        (
                            :keyword IS NULL
                            OR LOWER(LS.Sku)
                                LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(LS.ProductName)
                                LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(LS.LotCode)
                                LIKE LOWER(CONCAT('%', :keyword, '%'))
                        )

                        AND
                        (
                            (
                                :groupName = 'NEAR_EXPIRY'
                                AND LS.IsNearExpiry = 1
                                AND LS.IsExpired = 0
                                AND LS.QuantityOnHand > 0
                            )

                            OR
                            (
                                :groupName = 'EXPIRED'
                                AND LS.IsExpired = 1
                                AND LS.QuantityOnHand > 0
                            )

                            OR
                            (
                                :groupName = 'ALL'
                                AND
                                (
                                    (
                                        LS.IsNearExpiry = 1
                                        AND LS.IsExpired = 0
                                        AND LS.QuantityOnHand > 0
                                    )

                                    OR
                                    (
                                        LS.IsExpired = 1
                                        AND LS.QuantityOnHand > 0
                                    )
                                )
                            )
                        )

                        AND (
                            :fromDays IS NULL
                            OR LS.DaysToExpiry >= :fromDays
                        )

                        AND (
                            :toDays IS NULL
                            OR LS.DaysToExpiry <= :toDays
                        )
                    """,

            nativeQuery = true
    )
    Page<ExpiryAlertViewProjection> search(
            @Param("groupName") String groupName,

            @Param("keyword") String keyword,

            @Param("fromDays") Integer fromDays,

            @Param("toDays") Integer toDays,

            Pageable pageable
    );


    @Query(
            value = """
                    SELECT
                        C.ExpiryWarningDays AS warningDays,

                        COALESCE(
                            SUM(
                                CASE
                                    WHEN
                                        LS.IsNearExpiry = 1
                                        AND LS.IsExpired = 0
                                        AND LS.QuantityOnHand > 0
                                    THEN CONVERT(BIGINT, 1)
                                    ELSE CONVERT(BIGINT, 0)
                                END
                            ),
                            0
                        ) AS nearExpiryLotCount,

                        COALESCE(
                            SUM(
                                CASE
                                    WHEN
                                        LS.IsNearExpiry = 1
                                        AND LS.IsExpired = 0
                                        AND LS.QuantityOnHand > 0
                                    THEN CONVERT(BIGINT, LS.QuantityOnHand)
                                    ELSE CONVERT(BIGINT, 0)
                                END
                            ),
                            0
                        ) AS nearExpiryQuantity,

                        COALESCE(
                            SUM(
                                CASE
                                    WHEN
                                        LS.IsExpired = 1
                                        AND LS.QuantityOnHand > 0
                                    THEN CONVERT(BIGINT, 1)
                                    ELSE CONVERT(BIGINT, 0)
                                END
                            ),
                            0
                        ) AS expiredLotCount,

                        COALESCE(
                            SUM(
                                CASE
                                    WHEN
                                        LS.IsExpired = 1
                                        AND LS.QuantityOnHand > 0
                                    THEN CONVERT(BIGINT, LS.QuantityOnHand)
                                    ELSE CONVERT(BIGINT, 0)
                                END
                            ),
                            0
                        ) AS expiredQuantity

                    FROM dbo.InventoryConfig C

                    LEFT JOIN dbo.vw_InventoryLotStatus LS
                        ON 1 = 1

                    WHERE C.Id = 1

                    GROUP BY C.ExpiryWarningDays
                    """,
            nativeQuery = true
    )
    ExpiryAlertSummaryProjection getSummary();
}