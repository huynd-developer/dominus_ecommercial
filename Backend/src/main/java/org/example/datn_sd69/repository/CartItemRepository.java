package org.example.datn_sd69.repository;

import jakarta.transaction.Transactional;
import org.example.datn_sd69.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {

    // Hàm tìm kiếm CartItem theo id của Giỏ hàng và id của Biến thể (Đã dùng ở hàm addVariantToCart)
    Optional<CartItem> findByCartIdAndProductVariantId(Integer cartId, Integer productVariantId);

    /**
     * GET Cart luôn lấy Product/SKU hiện tại trong cùng lần đọc.
     * Không lưu snapshot giá/status/tồn ở CartItem.
     */
    @EntityGraph(attributePaths = {
            "productVariant",
            "productVariant.product",
            "productVariant.capacity",
            "productVariant.bottleType"
    })
    List<CartItem> findByCartId(Integer cartId);

    /**
     * Lấy ảnh hiện tại của Product từ ProductVariant trong giỏ hàng.
     *
     * Ưu tiên ảnh primary; nếu Product chưa có primary thì lấy ảnh đầu tiên.
     * Chỉ đọc dữ liệu, không ảnh hưởng logic thêm/sửa/xóa giỏ hàng.
     */
    @Query(
            value = """
                    SELECT TOP 1 pi.ImageUrl
                    FROM ProductVariant pv
                    JOIN ProductImage pi ON pi.ProductId = pv.ProductId
                    WHERE pv.Id = :productVariantId
                      AND pi.ImageUrl IS NOT NULL
                      AND LTRIM(RTRIM(pi.ImageUrl)) <> ''
                    ORDER BY
                        CASE
                            WHEN pi.IsPrimary = 1 THEN 0
                            ELSE 1
                        END,
                        pi.Id ASC
                    """,
            nativeQuery = true
    )
    Optional<String> findFirstProductImageUrlByVariantId(
            @Param("productVariantId") Integer productVariantId
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem c WHERE c.productVariant.id = :productVariantId")
    void deleteByProductVariantId(@Param("productVariantId") Integer productVariantId);
}