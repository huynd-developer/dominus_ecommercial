package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.StockMovement;
import org.example.datn_sd69.repository.projection.StockMovementViewProjection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockMovementRepository
        extends JpaRepository<StockMovement, Integer> {

    // ================= POS rollback =================

    /**
     * Lấy đúng các SALE_OUT theo chứng từ để hoàn về chính lot đã xuất.
     */
    @Query("""
            SELECT sm
            FROM StockMovement sm
            JOIN FETCH sm.inventoryLot lot
            WHERE sm.referenceType = :referenceType
              AND sm.referenceId = :referenceId
              AND sm.movementType = :movementType
            ORDER BY sm.id ASC
            """)
    List<StockMovement> findByReference(
            @Param("referenceType") String referenceType,
            @Param("referenceId") Long referenceId,
            @Param("movementType") Byte movementType
    );


    @Query(
            value = """
                    SELECT
                        SM.Id AS Id,

                        IL.Id AS InventoryLotId,

                        IL.ProductVariantId AS ProductVariantId,

                        PV.Sku AS Sku,

                        P.Name AS ProductName,

                        PIMG.ImageUrl AS ImageUrl,

                        C.Value AS CapacityValue,

                        BT.Name AS BottleTypeName,

                        IL.LotCode AS LotCode,

                        SM.MovementType AS MovementType,

                        SM.QuantityChange AS QuantityChange,

                        SM.QuantityBefore AS QuantityBefore,

                        SM.QuantityAfter AS QuantityAfter,

                        SM.ReferenceType AS ReferenceType,

                        SM.ReferenceId AS ReferenceId,

                        SM.ReferenceLineId AS ReferenceLineId,

                        SM.Reason AS Reason,

                        U.Id AS CreatedById,

                        U.Name AS CreatedByName,

                        SM.CreatedAt AS CreatedAt

                    FROM dbo.StockMovement SM

                    INNER JOIN dbo.InventoryLot IL
                        ON IL.Id = SM.InventoryLotId

                    INNER JOIN dbo.ProductVariant PV
                        ON PV.Id = IL.ProductVariantId

                    LEFT JOIN dbo.Capacity C
                        ON C.Id = PV.CapacityId

                    LEFT JOIN dbo.BottleType BT
                        ON BT.Id = PV.BottleTypeId

                    INNER JOIN dbo.Product P
                        ON P.Id = PV.ProductId

                    OUTER APPLY
                    (
                        SELECT TOP 1
                            PI.ImageUrl
                        FROM dbo.ProductImage PI
                        WHERE PI.ProductId = P.Id
                        ORDER BY
                            CASE
                                WHEN PI.IsPrimary = 1 THEN 0
                                ELSE 1
                            END,
                            PI.Id ASC
                    ) PIMG

                    INNER JOIN dbo.Users U
                        ON U.Id = SM.CreatedBy

                    WHERE
                        (
                            :keyword IS NULL

                            OR PV.Sku LIKE
                                CONCAT('%', :keyword, '%')

                            OR P.Name LIKE
                                CONCAT('%', :keyword, '%')

                            OR IL.LotCode LIKE
                                CONCAT('%', :keyword, '%')
                        )

                        AND
                        (
                            :inventoryLotId IS NULL

                            OR SM.InventoryLotId =
                                :inventoryLotId
                        )

                        AND
                        (
                            :movementType IS NULL

                            OR SM.MovementType =
                                :movementType
                        )

                        AND
                        (
                            :createdBy IS NULL

                            OR SM.CreatedBy =
                                :createdBy
                        )

                        AND
                        (
                            :referenceType IS NULL

                            OR SM.ReferenceType =
                                :referenceType
                        )

                        AND
                        (
                            :referenceId IS NULL

                            OR SM.ReferenceId =
                                :referenceId
                        )

                        AND
                        (
                            :fromDateTime IS NULL

                            OR SM.CreatedAt >=
                                :fromDateTime
                        )

                        AND
                        (
                            :toDateTime IS NULL

                            OR SM.CreatedAt <
                                :toDateTime
                        )

                    ORDER BY
                        SM.CreatedAt DESC,
                        SM.Id DESC
                    """,

            countQuery = """
                    SELECT
                        COUNT(*)

                    FROM dbo.StockMovement SM

                    INNER JOIN dbo.InventoryLot IL
                        ON IL.Id = SM.InventoryLotId

                    INNER JOIN dbo.ProductVariant PV
                        ON PV.Id = IL.ProductVariantId

                    INNER JOIN dbo.Product P
                        ON P.Id = PV.ProductId

                    INNER JOIN dbo.Users U
                        ON U.Id = SM.CreatedBy

                    WHERE
                        (
                            :keyword IS NULL

                            OR PV.Sku LIKE
                                CONCAT('%', :keyword, '%')

                            OR P.Name LIKE
                                CONCAT('%', :keyword, '%')

                            OR IL.LotCode LIKE
                                CONCAT('%', :keyword, '%')
                        )

                        AND
                        (
                            :inventoryLotId IS NULL

                            OR SM.InventoryLotId =
                                :inventoryLotId
                        )

                        AND
                        (
                            :movementType IS NULL

                            OR SM.MovementType =
                                :movementType
                        )

                        AND
                        (
                            :createdBy IS NULL

                            OR SM.CreatedBy =
                                :createdBy
                        )

                        AND
                        (
                            :referenceType IS NULL

                            OR SM.ReferenceType =
                                :referenceType
                        )

                        AND
                        (
                            :referenceId IS NULL

                            OR SM.ReferenceId =
                                :referenceId
                        )

                        AND
                        (
                            :fromDateTime IS NULL

                            OR SM.CreatedAt >=
                                :fromDateTime
                        )

                        AND
                        (
                            :toDateTime IS NULL

                            OR SM.CreatedAt <
                                :toDateTime
                        )
                    """,

            nativeQuery = true
    )
    Page<StockMovementViewProjection> search(

            @Param("keyword")
            String keyword,

            @Param("inventoryLotId")
            Integer inventoryLotId,

            @Param("movementType")
            Byte movementType,

            @Param("createdBy")
            Integer createdBy,

            @Param("referenceType")
            String referenceType,

            @Param("referenceId")
            Integer referenceId,

            @Param("fromDateTime")
            LocalDateTime fromDateTime,

            @Param("toDateTime")
            LocalDateTime toDateTime,

            Pageable pageable
    );


    @Query(
            value = """
                    SELECT
                        SM.Id AS Id,

                        IL.Id AS InventoryLotId,

                        IL.ProductVariantId AS ProductVariantId,

                        PV.Sku AS Sku,

                        P.Name AS ProductName,

                        PIMG.ImageUrl AS ImageUrl,

                        C.Value AS CapacityValue,

                        BT.Name AS BottleTypeName,

                        IL.LotCode AS LotCode,

                        SM.MovementType AS MovementType,

                        SM.QuantityChange AS QuantityChange,

                        SM.QuantityBefore AS QuantityBefore,

                        SM.QuantityAfter AS QuantityAfter,

                        SM.ReferenceType AS ReferenceType,

                        SM.ReferenceId AS ReferenceId,

                        SM.ReferenceLineId AS ReferenceLineId,

                        SM.Reason AS Reason,

                        U.Id AS CreatedById,

                        U.Name AS CreatedByName,

                        SM.CreatedAt AS CreatedAt

                    FROM dbo.StockMovement SM

                    INNER JOIN dbo.InventoryLot IL
                        ON IL.Id = SM.InventoryLotId

                    INNER JOIN dbo.ProductVariant PV
                        ON PV.Id = IL.ProductVariantId

                    LEFT JOIN dbo.Capacity C
                        ON C.Id = PV.CapacityId

                    LEFT JOIN dbo.BottleType BT
                        ON BT.Id = PV.BottleTypeId

                    INNER JOIN dbo.Product P
                        ON P.Id = PV.ProductId

                    OUTER APPLY
                    (
                        SELECT TOP 1
                            PI.ImageUrl
                        FROM dbo.ProductImage PI
                        WHERE PI.ProductId = P.Id
                        ORDER BY
                            CASE
                                WHEN PI.IsPrimary = 1 THEN 0
                                ELSE 1
                            END,
                            PI.Id ASC
                    ) PIMG

                    INNER JOIN dbo.Users U
                        ON U.Id = SM.CreatedBy

                    WHERE SM.Id = :id
                    """,
            nativeQuery = true
    )
    Optional<StockMovementViewProjection> findViewById(
            @Param("id") Integer id
    );
}