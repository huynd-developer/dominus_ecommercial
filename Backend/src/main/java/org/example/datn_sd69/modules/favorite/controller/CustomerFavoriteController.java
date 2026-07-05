package org.example.datn_sd69.modules.favorite.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.datn_sd69.modules.favorite.dto.AddFavoriteRequest;
import org.example.datn_sd69.modules.favorite.service.CustomerFavoriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/customer/favorites")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('USER')")
public class CustomerFavoriteController {

    private final CustomerFavoriteService customerFavoriteService;

    @GetMapping
    public ResponseEntity<?> getFavorites() {
        return ResponseEntity.ok(customerFavoriteService.getFavorites());
    }

    @PostMapping
    public ResponseEntity<?> addFavorite(
            @Valid @RequestBody AddFavoriteRequest request
    ) {
        return ResponseEntity.ok(customerFavoriteService.addFavorite(request));
    }

    @DeleteMapping("/{favoriteId}")
    public ResponseEntity<?> deleteFavorite(@PathVariable Integer favoriteId) {
        customerFavoriteService.deleteFavorite(favoriteId);
        return ResponseEntity.ok(Map.of("message", "Đã xóa sản phẩm khỏi yêu thích"));
    }

    @DeleteMapping("/variant/{productVariantId}")
    public ResponseEntity<?> deleteFavoriteByVariant(
            @PathVariable Integer productVariantId
    ) {
        customerFavoriteService.deleteFavoriteByVariant(productVariantId);
        return ResponseEntity.ok(Map.of("message", "Đã bỏ yêu thích sản phẩm"));
    }
}