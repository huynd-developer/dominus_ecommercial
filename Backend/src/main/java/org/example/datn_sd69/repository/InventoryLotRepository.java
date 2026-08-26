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


    // ================= GENERIC INVENTORY LOT SEARCH =================

    /**
     * Danh sách lô dùng chung cho module quản lý lô.
     *
     * GIỮ NGUYÊN logic hiện tại.
     * Không lọc Product/ProductVariant đã xóa tại đây vì dữ liệu
     * lô lịch sử vẫn phải có thể được tra cứu.
     */
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


    // ================= STOCK ADJUSTMENT CANDIDATES =================

    /**
     * Danh sách lô dùng RIÊNG khi tạo phiếu kiểm kê.
     *
     * Quy tắc:
     * - Product và ProductVariant chưa xóa -> được chọn bình thường.
     * - Nếu Product hoặc ProductVariant đã xóa nhưng lô vẫn còn tồn vật lý
     *   (QuantityOnHand > 0) -> vẫn phải hiển thị để có thể kiểm kê/xử lý tồn.
     * - Product/SKU đã xóa và tồn = 0 -> không đưa vào phiếu kiểm kê mới.
     *
     * Không dùng SellableQuantity vì kiểm kê phải bao gồm cả hàng hết hạn.
     * Không thay đổi dữ liệu InventoryLot.
     */
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

                    INNER JOIN dbo.ProductVariant PV
                        ON PV.Id = LS.ProductVariantId

                    INNER JOIN dbo.Product P
                        ON P.Id = PV.ProductId

                    LEFT JOIN dbo.GoodsReceiptItem GRI
                        ON GRI.Id = L.GoodsReceiptItemId

                    LEFT JOIN dbo.GoodsReceipt GR
                        ON GR.Id = GRI.GoodsReceiptId

                    LEFT JOIN dbo.Capacity C
                        ON C.Id = PV.CapacityId

                    LEFT JOIN dbo.BottleType BT
                        ON BT.Id = PV.BottleTypeId

                    OUTER APPLY (
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
                    ) ImageData

                    WHERE
                        (
                            (
                                ISNULL(P.IsDeleted, 0) = 0
                                AND ISNULL(PV.IsDeleted, 0) = 0
                            )
                            OR LS.QuantityOnHand > 0
                        )

                        AND (
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

            countQuery = """
                    SELECT COUNT(*)

                    FROM dbo.vw_InventoryLotStatus LS

                    INNER JOIN dbo.InventoryLot L
                        ON L.Id = LS.InventoryLotId

                    INNER JOIN dbo.ProductVariant PV
                        ON PV.Id = LS.ProductVariantId

                    INNER JOIN dbo.Product P
                        ON P.Id = PV.ProductId

                    LEFT JOIN dbo.GoodsReceiptItem GRI
                        ON GRI.Id = L.GoodsReceiptItemId

                    LEFT JOIN dbo.GoodsReceipt GR
                        ON GR.Id = GRI.GoodsReceiptId

                    WHERE
                        (
                            (
                                ISNULL(P.IsDeleted, 0) = 0
                                AND ISNULL(PV.IsDeleted, 0) = 0
                            )
                            OR LS.QuantityOnHand > 0
                        )

                        AND (
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
    Page<InventoryLotViewProjection> searchAuditCandidates(
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


    // ================= DETAIL =================

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

                    WHERE LS.InventoryLotId = :id
                    """,
            nativeQuery = true
    )
    Optional<InventoryLotViewProjection> findViewById(
            @Param("id")
            Integer id
    );
}