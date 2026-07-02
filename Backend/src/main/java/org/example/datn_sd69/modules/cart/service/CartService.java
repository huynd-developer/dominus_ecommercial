package org.example.datn_sd69.modules.cart.service;

import org.example.datn_sd69.entity.Cart;
import org.example.datn_sd69.entity.CartItem;
import org.example.datn_sd69.entity.Customer;
import org.example.datn_sd69.entity.ProductVariant;
import org.example.datn_sd69.modules.cart.dto.response.CartItemResponse;
import org.example.datn_sd69.repository.CartItemRepository;
import org.example.datn_sd69.repository.CartRepository;
import org.example.datn_sd69.repository.CustomerRepository;
import org.example.datn_sd69.repository.ProductVariantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartService {
    @Autowired private CartRepository cartRepo;
    @Autowired private CartItemRepository cartItemRepo;
    @Autowired private ProductVariantRepository variantRepo;
    @Autowired private CustomerRepository customerRepo;

    @Transactional
    public void addVariantToCart(Integer customerId, Integer variantId, Integer quantity) {
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
            cartItemRepo.save(item);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductVariant(variant);
            newItem.setQuantity(quantity);
            cartItemRepo.save(newItem);
        }
    }

    @Transactional
    public void updateCartItemQuantity(Integer customerId, Integer cartItemId, Integer newQuantity) {
        CartItem item = cartItemRepo.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy item trong giỏ"));

        // Xác thực quyền: Check xem item này có đúng thuộc về user đang đăng nhập không
        if (!item.getCart().getCustomer().getUserId().equals(customerId)) {
            throw new RuntimeException("Không có quyền thao tác trên giỏ hàng này");
        }

        if (newQuantity <= 0) {
            cartItemRepo.delete(item);
            return;
        }

        ProductVariant variant = item.getProductVariant();
        if (newQuantity > variant.getStockQuantity()) {
            throw new RuntimeException("Chỉ còn " + variant.getStockQuantity() + " sản phẩm trong kho!");
        }

        item.setQuantity(newQuantity);
        cartItemRepo.save(item);
    }

    @Transactional
    public void removeCartItem(Integer customerId, Integer cartItemId) {
        CartItem item = cartItemRepo.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy item trong giỏ"));

        // Xác thực quyền
        if (!item.getCart().getCustomer().getUserId().equals(customerId)) {
            throw new RuntimeException("Không có quyền thao tác trên giỏ hàng này");
        }

        cartItemRepo.delete(item);
    }

    // Đã map sang DTO để trả chính xác Price và StockQuantity theo đúng yêu cầu Task UI
    // Đã map sang DTO để trả chính xác Price và StockQuantity theo đúng yêu cầu Task UI
    public List<CartItemResponse> getCartByCustomerId(Integer customerId) {
        Cart cart = cartRepo.findByCustomerUserId(customerId).orElse(null);
        if (cart == null) return new ArrayList<>();

        return cartItemRepo.findByCartId(cart.getId()).stream().map(item -> {
            CartItemResponse res = new CartItemResponse();
            res.setCartItemId(item.getId());
            res.setProductVariantId(item.getProductVariant().getId());
            res.setSku(item.getProductVariant().getSku());
            res.setQuantity(item.getQuantity());
            res.setProductName(item.getProductVariant().getProduct().getName());

            // Query trực tiếp giá và tồn kho realtime
            res.setPrice(item.getProductVariant().getPrice());
            res.setStockQuantity(item.getProductVariant().getStockQuantity());

            // 👇 ĐÃ FIX: Lấy tên dung tích từ bảng Capacity truyền vào DTO 👇
            // Chú ý: Nếu trong Entity Capacity của m dùng getCapacityName() thay vì getName() thì m đổi lại cho khớp nhé.
            res.setCapacity(item.getProductVariant().getCapacity().getValue().intValue() + "ml");

            return res;
        }).collect(Collectors.toList());
    }
}