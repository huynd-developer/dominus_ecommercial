package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.GoodsReceipt;
import org.example.datn_sd69.repository.projection.GoodsReceiptListProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Integer> {

    boolean existsByReceiptNo(String receiptNo);

    long countByStatusAndReceiptType(Byte status, Byte receiptType);

    @Query(
            value = """
                    SELECT
                        GR.Id AS id,
                        GR.ReceiptNo AS receiptNo,
                        GR.ReceiptType AS receiptType,
                        GR.Status AS status,
                        GR.Note AS note,
                        GR.CreatedBy AS createdById,
                        U.Name AS createdByName,
                        GR.CreatedAt AS createdAt,
                        GR.SubmittedAt AS submittedAt,
                        GR.ApprovedAt AS approvedAt,
                        GR.RejectedAt AS rejectedAt,
                        GR.CancelledAt AS cancelledAt,
                        CONVERT(BIGINT, COUNT(DISTINCT GRI.ProductVariantId)) AS totalSku,
                        COALESCE(SUM(CONVERT(BIGINT, GRI.Quantity)), 0) AS totalQuantity
                    FROM dbo.GoodsReceipt GR
                    INNER JOIN dbo.Users U
                        ON U.Id = GR.CreatedBy
                    LEFT JOIN dbo.GoodsReceiptItem GRI
                        ON GRI.GoodsReceiptId = GR.Id
                    WHERE
                        (
                            :keyword IS NULL
                            OR LOWER(GR.ReceiptNo) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(U.Name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        )
                        AND (:status IS NULL OR GR.Status = :status)
                        AND (:receiptType IS NULL OR GR.ReceiptType = :receiptType)
                        AND (:createdBy IS NULL OR GR.CreatedBy = :createdBy)
                        AND (:fromDate IS NULL OR GR.CreatedAt >= :fromDate)
                        AND (:toDateExclusive IS NULL OR GR.CreatedAt < :toDateExclusive)
                    GROUP BY
                        GR.Id,
                        GR.ReceiptNo,
                        GR.ReceiptType,
                        GR.Status,
                        GR.Note,
                        GR.CreatedBy,
                        U.Name,
                        GR.CreatedAt,
                        GR.SubmittedAt,
                        GR.ApprovedAt,
                        GR.RejectedAt,
                        GR.CancelledAt
                    ORDER BY GR.CreatedAt DESC, GR.Id DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM dbo.GoodsReceipt GR
                    INNER JOIN dbo.Users U
                        ON U.Id = GR.CreatedBy
                    WHERE
                        (
                            :keyword IS NULL
                            OR LOWER(GR.ReceiptNo) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(U.Name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                        )
                        AND (:status IS NULL OR GR.Status = :status)
                        AND (:receiptType IS NULL OR GR.ReceiptType = :receiptType)
                        AND (:createdBy IS NULL OR GR.CreatedBy = :createdBy)
                        AND (:fromDate IS NULL OR GR.CreatedAt >= :fromDate)
                        AND (:toDateExclusive IS NULL OR GR.CreatedAt < :toDateExclusive)
                    """,
            nativeQuery = true
    )
    Page<GoodsReceiptListProjection> search(
            @Param("keyword") String keyword,
            @Param("status") Byte status,
            @Param("receiptType") Byte receiptType,
            @Param("createdBy") Integer createdBy,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDateExclusive") LocalDateTime toDateExclusive,
            Pageable pageable
    );
}
