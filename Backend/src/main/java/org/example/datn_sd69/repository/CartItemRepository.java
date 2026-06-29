package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    // Hàm tìm kiếm CartItem theo id của Giỏ hàng và id của Biến thể (Đã dùng ở hàm addVariantToCart)
    Optional<CartItem> findByCartIdAndProductVariantId(Integer cartId, Integer productVariantId);

    // Hàm lấy ra danh sách tất cả CartItem nằm trong một Giỏ hàng cụ thể
    List<CartItem> findByCartId(Integer cartId);
}