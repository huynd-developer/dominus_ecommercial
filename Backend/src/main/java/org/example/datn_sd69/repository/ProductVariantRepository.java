package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.repository.projection.ProductVariantInventoryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {

    // ================= CRUD =================

    /**
     * Giữ method cũ vì có thể module khác vẫn đang dùng.
     * Method này có thể trả cả variant đã xóa mềm.
     */
    List<ProductVariant> findByProduct_Id(Integer productId);

    /**
     * Giữ nguyên signature để không làm vỡ caller cũ,
     * nhưng không hard delete ProductVariant nữa.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE ProductVariant v
        SET v.isDeleted = true,
            v.status = 0
        WHERE v.product.id = :productId
    """)
    void deleteByProduct_Id(@Param("productId") Integer productId);

    // ================= SKU =================

    /**
     * Không loại SKU đã xóa mềm để tránh tái sử dụng mã SKU
     * đã có lịch sử kho/chứng từ.
     */
    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Integer id);

    // ================= Paging =================

    Page<ProductVariant> findByIsDeletedFalse(Pageable pageable);

    Page<ProductVariant> findBySkuContainingIgnoreCaseAndIsDeletedFalse(
            String keyword,
            Pageable pageable
    );

    Page<ProductVariant> findByProduct_IdAndIsDeletedFalse(
            Integer productId,
            Pageable pageable
    );

    Page<ProductVariant> findByProduct_IdAndSkuContainingIgnoreCaseAndIsDeletedFalse(
            Integer productId,
            String keyword,
            Pageable pageable
    );

    List<ProductVariant> findByProduct_IdAndStatusAndIsDeletedFalse(
            Integer productId,
            Integer status
    );

    List<ProductVariant> findByProduct_IdAndIsDeletedFalse(Integer productId);

    // ================= Detail =================

    @EntityGraph(attributePaths = {
            "product",
            "product.brand",
            "capacity",
            "bottleType"
    })
    Optional<ProductVariant> findByIdAndIsDeletedFalse(Integer id);

    @EntityGraph(attributePaths = {
            "product",
            "product.brand",
            "capacity",
            "bottleType"
    })
    Optional<ProductVariant> findBySkuAndIsDeletedFalse(String sku);

    // ================= Inventory =================

    /**
     * Đọc tồn kho thật từ view kho.
     * Không ghi ngược về ProductVariant.StockQuantity.
     */
    @Query(
            value = """
                SELECT
                    CAST(COALESCE(V.TotalQuantity, 0) AS BIGINT) AS totalQuantity,
                    CAST(COALESCE(V.SellableQuantity, 0) AS BIGINT) AS sellableQuantity
                FROM dbo.vw_ProductVariantInventory V
                WHERE V.ProductVariantId = :variantId
                """,
            nativeQuery = true
    )
    ProductVariantInventoryProjection findInventoryByVariantId(
            @Param("variantId") Integer variantId
    );

    // ================= Promotion =================

    @EntityGraph(attributePaths = {
            "product",
            "capacity",
            "bottleType"
    })
    @Query("""
        SELECT v
        FROM ProductVariant v
        JOIN v.product p
        WHERE COALESCE(v.isDeleted, false) = false
          AND COALESCE(p.isDeleted, false) = false
          AND (:keyword IS NULL
               OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(v.sku) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY p.name ASC, v.sku ASC
    """)
    Page<ProductVariant> searchVariantsForPromotion(
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "product",
            "product.brand",
            "capacity",
            "bottleType"
    })
    Optional<ProductVariant> findBySkuIgnoreCaseAndIsDeletedFalse(String sku);

    // ================= POS =================

    /**
     * Giữ nguyên logic lọc soft-delete hiện tại.
     * Tồn kho POS sẽ sửa riêng ở module POS.
     */
    @EntityGraph(attributePaths = {
            "product",
            "product.brand",
            "capacity",
            "bottleType"
    })
    @Query("""
        SELECT v
        FROM ProductVariant v
        JOIN v.product p
        WHERE COALESCE(v.isDeleted, false) = false
          AND COALESCE(p.isDeleted, false) = false
        ORDER BY p.name ASC, v.sku ASC
    """)
    List<ProductVariant> findVisibleVariantsForPos();

    @EntityGraph(attributePaths = {
            "product",
            "product.brand",
            "capacity",
            "bottleType"
    })
    @Query("""
        SELECT v
        FROM ProductVariant v
        JOIN v.product p
        WHERE LOWER(v.sku) = LOWER(:sku)
          AND COALESCE(v.isDeleted, false) = false
          AND COALESCE(p.isDeleted, false) = false
    """)
    Optional<ProductVariant> findPosVisibleBySku(@Param("sku") String sku);

    /**
     * Giá thấp nhất chỉ tính variant chưa xóa mềm.
     * Không liên quan tồn kho.
     */
    @Query("""
        SELECT MIN(v.price)
        FROM ProductVariant v
        WHERE v.product.id = :productId
          AND v.status = :status
          AND COALESCE(v.isDeleted, false) = false
          AND COALESCE(v.product.isDeleted, false) = false
    """)
    Double findMinSalePriceByProductIdAndStatus(
            @Param("productId") Integer productId,
            @Param("status") Integer status
    );
}