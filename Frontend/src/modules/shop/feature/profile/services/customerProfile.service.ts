import api from "@/common/api";
import type {
  AddFavoriteRequest,
  ChangePasswordRequest,
  CreateReviewRequest,
  CustomerOrderResponse,
  CustomerProfileResponse,
  FavoriteResponse,
  ReturnRequestSubmitPayload,
  ReviewResponse,
  ReviewableOrderItemResponse,
  SubmitDeliveryRefundBankRequest,
  UpdateCustomerProfileRequest,
  UpdateReviewRequest,
} from "../types/profile.type";

type ProfileAddressItem = {
  fullAddress?: string;
  address?: string;
  shippingAddress?: string;
  isDefault?: boolean;
  default?: boolean;
  selected?: boolean;
};

const collapseSpaces = (value: string) => {
  return String(value || "").trim().replace(/\s{2,}/g, " ");
};

/**
 * BE hiện tại chỉ nhận 1 address dạng text 5-200 ký tự.
 * Không được gửi JSON array vào address vì sẽ lỗi validate.
 */
const pickAddressFromList = (addresses: unknown): string => {
  if (!Array.isArray(addresses) || addresses.length === 0) {
    return "";
  }

  const selectedAddress =
    addresses.find((item: ProfileAddressItem) => {
      return Boolean(item?.isDefault || item?.default || item?.selected);
    }) || addresses[0];

  return collapseSpaces(
    String(
      selectedAddress?.fullAddress ||
        selectedAddress?.address ||
        selectedAddress?.shippingAddress ||
        ""
    )
  );
};

const normalizeAddressForProfileApi = (address: unknown): string => {
  if (Array.isArray(address)) {
    return pickAddressFromList(address);
  }

  const rawAddress = collapseSpaces(String(address || ""));

  if (!rawAddress) {
    return "";
  }

  if (rawAddress.startsWith("[")) {
    try {
      const parsedAddress = JSON.parse(rawAddress);
      return pickAddressFromList(parsedAddress);
    } catch {
      return rawAddress;
    }
  }

  if (rawAddress.startsWith("{")) {
    try {
      const parsedAddress = JSON.parse(rawAddress);
      return collapseSpaces(
        String(
          parsedAddress?.fullAddress ||
            parsedAddress?.address ||
            parsedAddress?.shippingAddress ||
            ""
        )
      );
    } catch {
      return rawAddress;
    }
  }

  return rawAddress;
};

const normalizeUpdateProfilePayload = (
  data: UpdateCustomerProfileRequest
): UpdateCustomerProfileRequest => {
  return {
    ...data,
    address: normalizeAddressForProfileApi((data as any).address),
  };
};

const appendReviewFormData = (
  formData: FormData,
  data: CreateReviewRequest | UpdateReviewRequest
) => {
  formData.append("rating", String(data.rating));

  if (data.comment) {
    formData.append("comment", data.comment);
  }

  const files = data.mediaFiles || data.files || [];
  files.forEach((file) => {
    formData.append("mediaFiles", file);
  });

  if ("deletedMediaIds" in data && data.deletedMediaIds) {
    data.deletedMediaIds.forEach((mediaId) => {
      formData.append("deletedMediaIds", String(mediaId));
    });
  }
};

const buildCreateReviewFormData = (data: CreateReviewRequest) => {
  const formData = new FormData();
  formData.append("orderItemId", String(data.orderItemId));
  appendReviewFormData(formData, data);
  return formData;
};

const buildUpdateReviewFormData = (data: UpdateReviewRequest) => {
  const formData = new FormData();
  appendReviewFormData(formData, data);
  return formData;
};

export const customerProfileService = {
  getProfile() {
    return api.get<CustomerProfileResponse>("/customer/profile");
  },

  updateProfile(data: UpdateCustomerProfileRequest) {
    const payload = normalizeUpdateProfilePayload(data);

    return api.put<CustomerProfileResponse>("/customer/profile", payload);
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
    return api.delete<{ message: string }>(`/customer/favorites/${favoriteId}`);
  },

  deleteFavoriteByVariant(productVariantId: number) {
    return api.delete<{ message: string }>(
      `/customer/favorites/variant/${productVariantId}`
    );
  },

  /**
   * BE:
   * GET /api/customer/orders
   */
  getOrders() {
    return api.get<CustomerOrderResponse[]>("/customer/orders");
  },

  /**
   * BE:
   * GET /api/customer/orders/{orderId}
   */
  getOrderDetail(orderId: number) {
    return api.get<CustomerOrderResponse>(`/customer/orders/${orderId}`);
  },

  /**
   * BE:
   * PATCH /api/customer/orders/{orderId}/cancel
   */
  cancelOrder(orderId: number) {
    return api.patch<{ message: string }>(`/customer/orders/${orderId}/cancel`);
  },

  /**
   * BE:
   * PATCH /api/customer/orders/{orderId}/delivery-refund-bank
   * Chỉ gửi 1 lần; BE sẽ chặn nếu khách đã cung cấp thông tin hoàn tiền.
   */
  submitDeliveryRefundBank(
    orderId: number,
    data: SubmitDeliveryRefundBankRequest
  ) {
    return api.patch<CustomerOrderResponse>(
      `/customer/orders/${orderId}/delivery-refund-bank`,
      data
    );
  },

  /**
   * BE:
   * PUT /api/customer/orders/{orderId}/request-return
   */
  requestReturnOrder(orderId: number, data: ReturnRequestSubmitPayload) {
    const formData = new FormData();

    formData.append("returnType", data.returnType);
    formData.append("reason", data.reason);
    formData.append("description", data.description || "");
    formData.append("email", data.email || "");
    formData.append("refundMethod", data.refundMethod);

    if (data.refundMethod === "BANK_TRANSFER") {
      formData.append("bankName", data.bankName || "");
      formData.append("bankAccountNumber", data.bankAccountNumber || "");
      formData.append("bankAccountHolder", data.bankAccountHolder || "");
    }

    formData.append("returnItems", JSON.stringify(data.returnItems || []));

    data.files.forEach((file) => {
      formData.append("mediaFiles", file);
    });

    return api.put<{ message: string }>(
      `/customer/orders/${orderId}/request-return`,
      formData
    );
  },

  /**
   * BE:
   * PUT /api/customer/orders/{orderId}/cancel-return
   */
  cancelReturnRequest(orderId: number) {
    return api.put<{ message: string }>(
      `/customer/orders/${orderId}/cancel-return`
    );
  },

  getReviewableItemsByOrder(orderId: number) {
    return api.get<ReviewableOrderItemResponse[]>(
      `/customer/reviews/orders/${orderId}/items`
    );
  },

  createReview(data: CreateReviewRequest | FormData) {
    const payload =
      data instanceof FormData ? data : buildCreateReviewFormData(data);

    return api.post<ReviewResponse>("/customer/reviews", payload);
  },

  updateReview(reviewId: number, data: UpdateReviewRequest | FormData) {
    const payload =
      data instanceof FormData ? data : buildUpdateReviewFormData(data);

    return api.patch<ReviewResponse>(`/customer/reviews/${reviewId}`, payload);
  },

  getMyReviews() {
    return api.get<ReviewResponse[]>("/customer/reviews/my");
  },
};