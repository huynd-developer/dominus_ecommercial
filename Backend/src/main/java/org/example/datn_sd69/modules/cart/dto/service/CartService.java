package org.example.datn_sd69.modules.cart.dto.service;

import org.example.datn_sd69.entity.Cart;
import org.example.datn_sd69.entity.CartItem;
import org.example.datn_sd69.entity.Customer;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.repository.CartItemRepository;
import org.example.datn_sd69.repository.CartRepository;
import org.example.datn_sd69.repository.CustomerRepository;
import org.example.datn_sd69.repository.ProductVariantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
// ... import các entity và repository

@Service
public class CartService {
    @Autowired private CartRepository cartRepo;
    @Autowired private CartItemRepository cartItemRepo;
    @Autowired private ProductVariantRepository variantRepo;
    @Autowired private CustomerRepository customerRepo;

    @Transactional
    public CartItem addVariantToCart(Integer customerId, Integer variantId, Integer quantity) {
        // CartService.java
        Cart cart = cartRepo.findByCustomerUserId(customerId)
                .orElseGet(() -> {
                    Customer customer = customerRepo.findById(customerId)
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy dữ liệu Khách hàng!"));
                    Cart newCart = new Cart();
                    newCart.setCustomer(customer);
                    return cartRepo.save(newCart);
                });

        ProductVariant variant = variantRepo.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Biến thể không tồn tại"));

        if (quantity > variant.getStockQuantity()) {
            throw new RuntimeException("Vượt quá tồn kho hiện tại (" + variant.getStockQuantity() + ")");
        }

        Optional<CartItem> existingItem = cartItemRepo.findByCartIdAndProductVariantId(cart.getId(), variantId);

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;
            if (newQuantity > variant.getStockQuantity()) {
                throw new RuntimeException("Tổng số lượng trong giỏ vượt quá tồn kho!");
            }
            item.setQuantity(newQuantity);
            return cartItemRepo.save(item);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductVariant(variant);
            newItem.setQuantity(quantity);
            return cartItemRepo.save(newItem);
        }
    }

    @Transactional
    public CartItem updateCartItemQuantity(Integer cartItemId, Integer newQuantity) {
        CartItem item = cartItemRepo.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy item trong giỏ"));

        if (newQuantity <= 0) {
            cartItemRepo.delete(item);
            return null;
        }

        ProductVariant variant = item.getProductVariant();
        if (newQuantity > variant.getStockQuantity()) {
            throw new RuntimeException("Chỉ còn " + variant.getStockQuantity() + " sản phẩm trong kho!");
        }

        item.setQuantity(newQuantity);
        return cartItemRepo.save(item);
    }

    @Transactional
    public void removeCartItem(Integer cartItemId) {
        cartItemRepo.deleteById(cartItemId);
    }

    // Thêm hàm getCartByCustomerId để phục vụ api lấy giỏ hàng
    public Cart getCartByCustomerId(Integer customerId) {
        System.out.println("DEBUG: Đang tìm giỏ hàng cho CustomerId = " + customerId);
        return cartRepo.findByCustomerUserId(customerId).orElse(null);
    }

}
