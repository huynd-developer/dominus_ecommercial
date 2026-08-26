package org.example.datn_sd69.repository;

import jakarta.persistence.LockModeType;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.repository.projection.ProductVariantInventoryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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

    // ================= Goods Receipt =================

    /**
     * Chỉ dùng cho giao dịch Phiếu nhập mới / sửa DRAFT.
     *
     * Không được dùng method này để đọc phiếu nhập cũ hoặc lịch sử kho,
     * vì dữ liệu lịch sử vẫn phải đọc được dù Product/ProductVariant
     * đã bị soft-delete.
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
        WHERE v.id IN :ids
          AND COALESCE(v.isDeleted, false) = false
          AND COALESCE(p.isDeleted, false) = false
    """)
    List<ProductVariant> findAvailableForGoodsReceiptByIds(
            @Param("ids") Collection<Integer> ids
    );

    // ================= Inventory =================

    /**
     * Đọc tồn vật lý trực tiếp từ InventoryLot.
     *
     * - totalQuantity: tổng QuantityOnHand của mọi lot.
     * - sellableQuantity: chỉ lot còn số lượng và HSD >= hôm nay.
     *
     * Không đọc ProductVariant.StockQuantity và không phụ thuộc ngày legacy của SKU.
     */
    @Query(
            value = """
                SELECT
                    CAST(
                        COALESCE(
                            SUM(CONVERT(BIGINT, L.QuantityOnHand)),
                            0
                        )
                        AS BIGINT
                    ) AS totalQuantity,

                    CAST(
                        COALESCE(
                            SUM(
                                CASE
                                    WHEN L.QuantityOnHand > 0
                                     AND L.ExpirationDate >= CAST(GETDATE() AS DATE)
                                    THEN CONVERT(BIGINT, L.QuantityOnHand)
                                    ELSE 0
                                END
                            ),
                            0
                        )
                        AS BIGINT
                    ) AS sellableQuantity

                FROM dbo.InventoryLot L
                WHERE L.ProductVariantId = :variantId
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

    /**
     * Khóa các SKU theo thứ tự ID trước khi Promotion kiểm tra overlap.
     * Nhờ vậy hai request tạo/sửa/bật campaign cùng SKU không thể cùng lúc
     * đều nhìn thấy overlap = 0 rồi cùng ghi dữ liệu.
     *
     * Không thay đổi query đọc SKU của Product/POS/Cart.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT v
        FROM ProductVariant v
        WHERE v.id IN :ids
        ORDER BY v.id ASC
    """)
    List<ProductVariant> findAllByIdInForPromotionUpdate(
            @Param("ids") List<Integer> ids
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

    // Kiểm tra xem có biến thể nào đang sử dụng loại chai này không (bỏ qua các biến thể đã xóa)
    boolean existsByBottleType_IdAndIsDeletedFalse(Integer bottleTypeId);

    // Kiểm tra xem có biến thể nào đang sử dụng dung tích này không (bỏ qua các biến thể đã xóa)
    boolean existsByCapacity_IdAndIsDeletedFalse(Integer capacityId);
}