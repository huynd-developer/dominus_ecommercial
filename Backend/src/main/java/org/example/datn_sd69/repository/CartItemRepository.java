package org.example.datn_sd69.repository;

import jakarta.transaction.Transactional;
import org.example.datn_sd69.entity.CartItem;
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

    // Hàm lấy ra danh sách tất cả CartItem nằm trong một Giỏ hàng cụ thể
    List<CartItem> findByCartId(Integer cartId);

    /**
     * Lấy ảnh đại diện của Product từ ProductVariant trong giỏ hàng.
     *
     * API /v1/customer/cart/my-cart đang trả imageUrl = null vì CartItem.thumbnailUrl
     * thường không được FE truyền khi thêm giỏ. Vì vậy cần lấy ảnh trực tiếp từ
     * ProductImage theo ProductId của ProductVariant.
     *
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
                    ORDER BY pi.Id ASC
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