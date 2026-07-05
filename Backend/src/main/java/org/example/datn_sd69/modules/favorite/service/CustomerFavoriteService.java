package org.example.datn_sd69.modules.favorite.service;

import org.example.datn_sd69.modules.favorite.dto.AddFavoriteRequest;
import org.example.datn_sd69.modules.favorite.dto.FavoriteResponse;

import java.util.List;

public interface CustomerFavoriteService {

    List<FavoriteResponse> getFavorites();

    FavoriteResponse addFavorite(AddFavoriteRequest request);

    void deleteFavorite(Integer favoriteId);

    void deleteFavoriteByVariant(Integer productVariantId);
}