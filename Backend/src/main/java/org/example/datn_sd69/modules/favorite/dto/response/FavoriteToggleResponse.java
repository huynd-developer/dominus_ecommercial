package org.example.datn_sd69.modules.favorite.dto.response;

public record FavoriteToggleResponse(
        Integer productVariantId,
        Boolean favorited,
        String message
) {
}