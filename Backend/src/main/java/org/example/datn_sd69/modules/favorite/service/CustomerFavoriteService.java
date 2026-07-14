package org.example.datn_sd69.modules.favorite.service;

import org.example.datn_sd69.modules.favorite.dto.request.AddFavoriteRequest;
import org.example.datn_sd69.modules.favorite.dto.response.FavoriteResponse;
import org.example.datn_sd69.modules.favorite.dto.response.FavoriteToggleResponse;

import java.util.List;

public interface CustomerFavoriteService {

    List<FavoriteResponse> getFavorites();

    FavoriteResponse addFavorite(AddFavoriteRequest request);

    FavoriteToggleResponse toggleFavorite(AddFavoriteRequest request);

    Boolean checkFavorite(Integer productVariantId);

    void deleteFavorite(Integer favoriteId);

    void deleteFavoriteByVariant(Integer productVariantId);
}