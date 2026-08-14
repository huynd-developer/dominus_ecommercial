package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.InventoryLot;
import org.example.datn_sd69.repository.projection.InventoryLotViewProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface InventoryLotRepository extends JpaRepository<InventoryLot, Integer> {

    @Query(
            value = """
                    SELECT
                        LS.InventoryLotId AS id,
                        LS.ProductVariantId AS productVariantId,
                        LS.Sku AS sku,
                        LS.ProductName AS productName,
                        LS.LotCode AS lotCode,
                        LS.ManufacturedDate AS manufacturedDate,
                        LS.ReceivedDate AS receivedDate,
                        LS.ExpirationDate AS expirationDate,
                        LS.DaysToExpiry AS daysToExpiry,
                        LS.InitialQuantity AS initialQuantity,
                        LS.QuantityOnHand AS quantityOnHand,
                        LS.SellableQuantity AS sellableQuantity,
                        LS.IsNearExpiry AS isNearExpiry,
                        LS.IsExpired AS isExpired,
                        L.CreatedBy AS createdById,
                        CU.Name AS createdByName,
                        L.CreatedAt AS createdAt,
                        L.GoodsReceiptItemId AS goodsReceiptItemId,
                        GR.Id AS goodsReceiptId,
                        GR.ReceiptNo AS receiptNo,
                        GR.ReceiptType AS receiptType,
                        GR.Status AS receiptStatus
                    FROM dbo.vw_InventoryLotStatus LS
                    INNER JOIN dbo.InventoryLot L
                        ON L.Id = LS.InventoryLotId
                    INNER JOIN dbo.Users CU
                        ON CU.Id = L.CreatedBy
                    LEFT JOIN dbo.GoodsReceiptItem GRI
                        ON GRI.Id = L.GoodsReceiptItemId
                    LEFT JOIN dbo.GoodsReceipt GR
                        ON GR.Id = GRI.GoodsReceiptId
                    WHERE
                        (
                            :keyword IS NULL
                            OR LOWER(LS.LotCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(LS.Sku) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(LS.ProductName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(COALESCE(GR.ReceiptNo, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        )
                        AND (:productVariantId IS NULL OR LS.ProductVariantId = :productVariantId)
                        AND (:isExpired IS NULL OR LS.IsExpired = :isExpired)
                        AND (:isNearExpiry IS NULL OR LS.IsNearExpiry = :isNearExpiry)
                        AND (
                            :hasStock IS NULL
                            OR (:hasStock = 1 AND LS.QuantityOnHand > 0)
                            OR (:hasStock = 0 AND LS.QuantityOnHand = 0)
                        )
                        AND (:expirationFrom IS NULL OR LS.ExpirationDate >= :expirationFrom)
                        AND (:expirationTo IS NULL OR LS.ExpirationDate <= :expirationTo)
                    ORDER BY
                        LS.ExpirationDate ASC,
                        LS.ReceivedDate DESC,
                        LS.InventoryLotId DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM dbo.vw_InventoryLotStatus LS
                    INNER JOIN dbo.InventoryLot L
                        ON L.Id = LS.InventoryLotId
                    LEFT JOIN dbo.GoodsReceiptItem GRI
                        ON GRI.Id = L.GoodsReceiptItemId
                    LEFT JOIN dbo.GoodsReceipt GR
                        ON GR.Id = GRI.GoodsReceiptId
                    WHERE
                        (
                            :keyword IS NULL
                            OR LOWER(LS.LotCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(LS.Sku) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(LS.ProductName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(COALESCE(GR.ReceiptNo, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        )
                        AND (:productVariantId IS NULL OR LS.ProductVariantId = :productVariantId)
                        AND (:isExpired IS NULL OR LS.IsExpired = :isExpired)
                        AND (:isNearExpiry IS NULL OR LS.IsNearExpiry = :isNearExpiry)
                        AND (
                            :hasStock IS NULL
                            OR (:hasStock = 1 AND LS.QuantityOnHand > 0)
                            OR (:hasStock = 0 AND LS.QuantityOnHand = 0)
                        )
                        AND (:expirationFrom IS NULL OR LS.ExpirationDate >= :expirationFrom)
                        AND (:expirationTo IS NULL OR LS.ExpirationDate <= :expirationTo)
                    """,
            nativeQuery = true
    )
    Page<InventoryLotViewProjection> search(
            @Param("keyword") String keyword,
            @Param("productVariantId") Integer productVariantId,
            @Param("isExpired") Boolean isExpired,
            @Param("isNearExpiry") Boolean isNearExpiry,
            @Param("hasStock") Boolean hasStock,
            @Param("expirationFrom") LocalDate expirationFrom,
            @Param("expirationTo") LocalDate expirationTo,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT
                        LS.InventoryLotId AS id,
                        LS.ProductVariantId AS productVariantId,
                        LS.Sku AS sku,
                        LS.ProductName AS productName,
                        LS.LotCode AS lotCode,
                        LS.ManufacturedDate AS manufacturedDate,
                        LS.ReceivedDate AS receivedDate,
                        LS.ExpirationDate AS expirationDate,
                        LS.DaysToExpiry AS daysToExpiry,
                        LS.InitialQuantity AS initialQuantity,
                        LS.QuantityOnHand AS quantityOnHand,
                        LS.SellableQuantity AS sellableQuantity,
                        LS.IsNearExpiry AS isNearExpiry,
                        LS.IsExpired AS isExpired,
                        L.CreatedBy AS createdById,
                        CU.Name AS createdByName,
                        L.CreatedAt AS createdAt,
                        L.GoodsReceiptItemId AS goodsReceiptItemId,
                        GR.Id AS goodsReceiptId,
                        GR.ReceiptNo AS receiptNo,
                        GR.ReceiptType AS receiptType,
                        GR.Status AS receiptStatus
                    FROM dbo.vw_InventoryLotStatus LS
                    INNER JOIN dbo.InventoryLot L
                        ON L.Id = LS.InventoryLotId
                    INNER JOIN dbo.Users CU
                        ON CU.Id = L.CreatedBy
                    LEFT JOIN dbo.GoodsReceiptItem GRI
                        ON GRI.Id = L.GoodsReceiptItemId
                    LEFT JOIN dbo.GoodsReceipt GR
                        ON GR.Id = GRI.GoodsReceiptId
                    WHERE LS.InventoryLotId = :id
                    """,
            nativeQuery = true
    )
    Optional<InventoryLotViewProjection> findViewById(@Param("id") Integer id);
}