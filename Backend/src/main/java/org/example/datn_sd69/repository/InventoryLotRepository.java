package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.InventoryLot;
import org.example.datn_sd69.repository.projection.InventoryLotViewProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InventoryLotRepository
        extends JpaRepository<InventoryLot, Integer> {

    // ================= POS / FEFO =================

    /**
     * Tồn có thể bán thật của SKU.
     * ExpirationDate hôm nay vẫn được coi là còn hạn.
     */
    @Query(
            value = """
                    SELECT COALESCE(SUM(L.QuantityOnHand), 0)
                    FROM dbo.InventoryLot L
                    WHERE L.ProductVariantId = :productVariantId
                      AND L.QuantityOnHand > 0
                      AND L.ExpirationDate >= CAST(GETDATE() AS DATE)
                    """,
            nativeQuery = true
    )
    Integer getSellableQuantityByVariantId(
            @Param("productVariantId") Integer productVariantId
    );

    /**
     * Tồn còn nằm trong lot đã hết hạn.
     * Chỉ dùng để hiển thị lý do không bán được ở POS.
     */
    @Query(
            value = """
                    SELECT COALESCE(SUM(L.QuantityOnHand), 0)
                    FROM dbo.InventoryLot L
                    WHERE L.ProductVariantId = :productVariantId
                      AND L.QuantityOnHand > 0
                      AND L.ExpirationDate < CAST(GETDATE() AS DATE)
                    """,
            nativeQuery = true
    )
    Integer getExpiredOnHandQuantityByVariantId(
            @Param("productVariantId") Integer productVariantId
    );

    /**
     * Lot bán tiếp theo theo FEFO để map ngày compatibility cho POS.
     */
    @Query(
            value = """
                    SELECT TOP 1 L.*
                    FROM dbo.InventoryLot L
                    WHERE L.ProductVariantId = :productVariantId
                      AND L.QuantityOnHand > 0
                      AND L.ExpirationDate >= CAST(GETDATE() AS DATE)
                    ORDER BY
                        L.ExpirationDate ASC,
                        L.ReceivedDate ASC,
                        L.Id ASC
                    """,
            nativeQuery = true
    )
    Optional<InventoryLot> findNextSellableLot(
            @Param("productVariantId") Integer productVariantId
    );

    /**
     * FEFO có khóa để checkout đồng thời không cùng phân bổ một lượng tồn.
     */
    @Query(
            value = """
                    SELECT L.*
                    FROM dbo.InventoryLot L
                    WITH (UPDLOCK, ROWLOCK, HOLDLOCK)
                    WHERE L.ProductVariantId = :productVariantId
                      AND L.QuantityOnHand > 0
                      AND L.ExpirationDate >= CAST(GETDATE() AS DATE)
                    ORDER BY
                        L.ExpirationDate ASC,
                        L.ReceivedDate ASC,
                        L.Id ASC
                    """,
            nativeQuery = true
    )
    List<InventoryLot> findSellableLotsForUpdateFefo(
            @Param("productVariantId") Integer productVariantId
    );


    @Query(
            value = """
                    SELECT
                        LS.InventoryLotId AS id,
                        LS.ProductVariantId AS productVariantId,
                        LS.Sku AS sku,
                        LS.ProductName AS productName,

                        ImageData.ImageUrl AS imageUrl,

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
                        GRI.UnitCost AS unitCost,
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

                    /*
                     * Chỉ bổ sung để xác định ProductId của SKU.
                     * Không ảnh hưởng dữ liệu tồn kho / lô.
                     */
                    LEFT JOIN dbo.ProductVariant PV
                        ON PV.Id = LS.ProductVariantId

                    /*
                     * Lấy duy nhất 1 ảnh cho sản phẩm:
                     * 1. Ưu tiên IsPrimary = 1
                     * 2. Nếu không có ảnh primary thì lấy ảnh đầu tiên
                     *
                     * OUTER APPLY giúp SKU không có ảnh vẫn được trả về.
                     * TOP 1 tránh nhân bản dòng lô khi sản phẩm có nhiều ảnh.
                     */
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
                            OR LOWER(LS.LotCode)
                                LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(LS.Sku)
                                LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(LS.ProductName)
                                LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(COALESCE(GR.ReceiptNo, ''))
                                LIKE LOWER(CONCAT('%', :keyword, '%'))
                        )

                        AND (
                            :productVariantId IS NULL
                            OR LS.ProductVariantId = :productVariantId
                        )

                        AND (
                            :isExpired IS NULL
                            OR LS.IsExpired = :isExpired
                        )

                        AND (
                            :isNearExpiry IS NULL
                            OR LS.IsNearExpiry = :isNearExpiry
                        )

                        AND (
                            :hasStock IS NULL
                            OR (
                                :hasStock = 1
                                AND LS.QuantityOnHand > 0
                            )
                            OR (
                                :hasStock = 0
                                AND LS.QuantityOnHand = 0
                            )
                        )

                        AND (
                            :expirationFrom IS NULL
                            OR LS.ExpirationDate >= :expirationFrom
                        )

                        AND (
                            :expirationTo IS NULL
                            OR LS.ExpirationDate <= :expirationTo
                        )

                    ORDER BY
                        CASE
                            WHEN LS.IsExpired = 1 THEN 1
                            ELSE 0
                        END ASC,
                        LS.ExpirationDate ASC,
                        LS.ReceivedDate ASC,
                        LS.InventoryLotId ASC
                    """,

            /*
             * GIỮ NGUYÊN countQuery.
             *
             * Không cần JOIN ảnh ở đây vì countQuery chỉ dùng
             * để tính tổng số bản ghi cho pagination.
             */
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
                            OR LOWER(LS.LotCode)
                                LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(LS.Sku)
                                LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(LS.ProductName)
                                LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(COALESCE(GR.ReceiptNo, ''))
                                LIKE LOWER(CONCAT('%', :keyword, '%'))
                        )

                        AND (
                            :productVariantId IS NULL
                            OR LS.ProductVariantId = :productVariantId
                        )

                        AND (
                            :isExpired IS NULL
                            OR LS.IsExpired = :isExpired
                        )

                        AND (
                            :isNearExpiry IS NULL
                            OR LS.IsNearExpiry = :isNearExpiry
                        )

                        AND (
                            :hasStock IS NULL
                            OR (
                                :hasStock = 1
                                AND LS.QuantityOnHand > 0
                            )
                            OR (
                                :hasStock = 0
                                AND LS.QuantityOnHand = 0
                            )
                        )

                        AND (
                            :expirationFrom IS NULL
                            OR LS.ExpirationDate >= :expirationFrom
                        )

                        AND (
                            :expirationTo IS NULL
                            OR LS.ExpirationDate <= :expirationTo
                        )
                    """,

            nativeQuery = true
    )
    Page<InventoryLotViewProjection> search(
            @Param("keyword")
            String keyword,

            @Param("productVariantId")
            Integer productVariantId,

            @Param("isExpired")
            Boolean isExpired,

            @Param("isNearExpiry")
            Boolean isNearExpiry,

            @Param("hasStock")
            Boolean hasStock,

            @Param("expirationFrom")
            LocalDate expirationFrom,

            @Param("expirationTo")
            LocalDate expirationTo,

            Pageable pageable
    );


    @Query(
            value = """
                    SELECT
                        LS.InventoryLotId AS id,
                        LS.ProductVariantId AS productVariantId,
                        LS.Sku AS sku,
                        LS.ProductName AS productName,

                        ImageData.ImageUrl AS imageUrl,

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
                        GRI.UnitCost AS unitCost,
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

                    /*
                     * Chỉ bổ sung để lấy ProductId từ SKU.
                     */
                    LEFT JOIN dbo.ProductVariant PV
                        ON PV.Id = LS.ProductVariantId

                    /*
                     * Ảnh primary trước.
                     * Không có primary thì lấy ảnh đầu tiên.
                     */
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

                    WHERE LS.InventoryLotId = :id
                    """,
            nativeQuery = true
    )
    Optional<InventoryLotViewProjection> findViewById(
            @Param("id")
            Integer id
    );
}
