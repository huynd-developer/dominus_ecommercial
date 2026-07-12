import api from "@/common/api";
import type {
  AddFavoriteRequest,
  ChangePasswordRequest,
  CreateReviewRequest,
  CustomerOrderResponse,
  CustomerProfileResponse,
  FavoriteResponse,
  ReviewResponse,
  ReviewableOrderItemResponse,
  UpdateCustomerProfileRequest,
} from "../types/profile.type";

export const customerProfileService = {
  getProfile() {
    return api.get<CustomerProfileResponse>("/customer/profile");
  },

  updateProfile(data: UpdateCustomerProfileRequest) {
    return api.put<CustomerProfileResponse>("/customer/profile", data);
  },

  uploadAvatar(file: File) {
    const formData = new FormData();
    formData.append("file", file);

    return api.put<CustomerProfileResponse>(
      "/customer/profile/avatar",
      formData
    );
  },

  changePassword(data: ChangePasswordRequest) {
    return api.put<{ message: string }>(
      "/customer/profile/change-password",
      data
    );
  },

  getFavorites() {
    return api.get<FavoriteResponse[]>("/customer/favorites");
  },

  addFavorite(data: AddFavoriteRequest) {
    return api.post<FavoriteResponse>("/customer/favorites", data);
  },

  deleteFavorite(favoriteId: number) {
    return api.delete<{ message: string }>(
      `/customer/favorites/${favoriteId}`
    );
  },

  deleteFavoriteByVariant(productVariantId: number) {
    return api.delete<{ message: string }>(
      `/customer/favorites/variant/${productVariantId}`
    );
  },

  getOrders() {
    return api.get<CustomerOrderResponse[]>("/customer/orders");
  },

  getOrderDetail(orderId: number) {
    return api.get<CustomerOrderResponse>(`/customer/orders/${orderId}`);
  },

  cancelOrder(orderId: number) {
    return api.patch<{ message: string }>(
      `/customer/orders/${orderId}/cancel`
    );
  },

  getReviewableItemsByOrder(orderId: number) {
    return api.get<ReviewableOrderItemResponse[]>(
      `/customer/reviews/orders/${orderId}/items`
    );
  },

  createReview(data: CreateReviewRequest) {
    return api.post<ReviewResponse>("/customer/reviews", data);
  },

  getMyReviews() {
    return api.get<ReviewResponse[]>("/customer/reviews/my");
  },
};