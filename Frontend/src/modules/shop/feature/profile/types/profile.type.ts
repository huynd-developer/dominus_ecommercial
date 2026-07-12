export type GenderValue = 0 | 1 | 2 | null;

export interface CustomerProfileResponse {
  userId: number;
  name: string;
  email: string;
  phone: string;
  address: string;
  avatarUrl: string | null;
  status: number;
  createdAt: string;

  customerRank: string;
  loyaltyPoints: number;
  dateOfBirth: string | null;
  gender: GenderValue;
}

export interface UpdateCustomerProfileRequest {
  name: string;
  phone: string;
  address: string;
  dateOfBirth?: string | null;
  gender?: GenderValue;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface AddFavoriteRequest {
  productVariantId: number;
}

export interface FavoriteResponse {
  favoriteId: number;
  productVariantId: number;
  productId: number;
  productName: string;
  brandName: string;
  sku: string;
  price: number;
  stockQuantity: number;
  capacityValue: number | null;
  bottleTypeName: string | null;
  imageUrl: string | null;
  createdAt: string;
}

export interface CustomerOrderItemResponse {
  orderItemId: number;
  productVariantId: number | null;
  productId: number | null;
  productName: string;
  brandName: string | null;
  sku: string | null;
  quantity: number;
  originalPrice: number;
  discountAmount: number;
  finalPrice: number;
  note: string | null;
  image: string | null;
}

export interface CustomerOrderResponse {
  orderId: number;
  orderType: string;
  customerName: string;
  customerPhone: string;
  shippingAddress: string;
  totalAmount: number;
  discountAmount: number;
  finalAmount: number;
  paymentMethod: string;
  status: number;
  statusText: string;
  canCancel: boolean;
  createdAt: string;
  items: CustomerOrderItemResponse[];
}

export interface CreateReviewRequest {
  orderItemId: number;
  rating: number;
  comment?: string | null;
}

export interface ReviewResponse {
  reviewId: number;
  orderItemId: number;
  orderId: number;

  productVariantId: number | null;
  productId: number | null;
  productName: string;
  brandName: string | null;
  sku: string | null;
  image: string | null;

  rating: number;
  comment: string | null;
  createdAt: string;
}

export interface ReviewableOrderItemResponse {
  orderItemId: number;
  orderId: number;

  productVariantId: number | null;
  productId: number | null;
  productName: string;
  brandName: string | null;
  sku: string | null;
  image: string | null;

  orderStatus: number;
  reviewed: boolean;
  canReview: boolean;
  message: string;
}