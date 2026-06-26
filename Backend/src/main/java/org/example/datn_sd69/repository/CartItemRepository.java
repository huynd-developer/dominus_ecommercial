package org.example.datn_sd69.repository;

import org.example.datn_sd69.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    // Hàm này check xem sản phẩm đã có trong giỏ chưa
    Optional<CartItem> findByCartIdAndProductVariantId(Integer cartId, Integer productVariantId);
}