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

const getCurrentUserRole = (): string => {
  try {
    const currentUser = JSON.parse(localStorage.getItem("currentUser") || "{}");

    return String(
      currentUser.role ||
        currentUser.roleName ||
        currentUser.authority ||
        currentUser.authorities?.[0]?.authority ||
        ""
    )
      .toUpperCase()
      .replace("ROLE_", "");
  } catch {
    return "";
  }
};

const canUseCustomerFavoriteApi = (): boolean => {
  const token =
    localStorage.getItem("token") ||
    localStorage.getItem("accessToken") ||
    localStorage.getItem("jwtToken");

  return !!token && getCurrentUserRole() === "USER";
};

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
    if (!canUseCustomerFavoriteApi()) {
      return Promise.resolve({
        data: {
          productVariantId,
          favorited: false,
        },
      } as any);
    }

    return api
      .get<FavoriteCheckResponse>(
        `/customer/favorites/check/${productVariantId}`
      )
      .catch((error) => {
        if (
          error?.response?.status === 401 ||
          error?.response?.status === 403
        ) {
          return {
            data: {
              productVariantId,
              favorited: false,
            },
          } as any;
        }

        throw error;
      });
  },

  removeByVariant(productVariantId: number) {
    return api.delete(`/customer/favorites/variant/${productVariantId}`);
  },
};