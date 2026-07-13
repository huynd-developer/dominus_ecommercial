import api from "@/common/api";

export interface FavoriteToggleRequest {
  productVariantId: number;
}

export interface FavoriteToggleResponse {
  productVariantId: number;
  favorited: boolean;
  message: string;
}

export interface FavoriteCheckResponse {
  productVariantId: number;
  favorited: boolean;
}

export interface FavoriteItemResponse {
  favoriteId: number;
  productVariantId: number;
  productId: number;
  productName: string;
  brandName: string | null;
  sku: string | null;
  price: number;
  stockQuantity: number;
  capacityValue: number | null;
  bottleTypeName: string | null;
  imageUrl: string | null;
  createdAt: string;
}

export const favoriteService = {
  getFavorites() {
    return api.get<FavoriteItemResponse[]>("/customer/favorites");
  },

  toggleFavorite(productVariantId: number) {
    return api.post<FavoriteToggleResponse>("/customer/favorites/toggle", {
      productVariantId,
    });
  },

  checkFavorite(productVariantId: number) {
    return api.get<FavoriteCheckResponse>(
      `/customer/favorites/check/${productVariantId}`
    );
  },

  removeByVariant(productVariantId: number) {
    return api.delete(`/customer/favorites/variant/${productVariantId}`);
  },
};