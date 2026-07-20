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

  productName: string | null;
  brandName: string | null;
  sku: string | null;

  capacity: string | null;
  bottleType: string | null;

  manufacturingDate: string | null;
  expirationDate: string | null;

  quantity: number;

  /**
   * Giá gốc tại thời điểm đặt hàng.
   */
  originalPrice: number;

  /**
   * Số tiền giảm trên 1 sản phẩm tại thời điểm đặt hàng.
   */
  discountAmount: number;

  /**
   * Giá cuối cùng trên 1 sản phẩm tại thời điểm đặt hàng.
   */
  finalPrice: number;

  /**
   * Thành tiền dòng = finalPrice * quantity.
   */
  lineTotal: number;

  note: string | null;
  image: string | null;
}

export interface CustomerOrderResponse {
  orderId: number;

  orderType: string | null;

  customerName: string | null;
  customerPhone: string | null;
  shippingAddress: string | null;

  totalAmount: number;
  discountAmount: number;
  finalAmount: number;

  paymentMethod: string | null;

  /**
   * 0 = Chờ xác nhận
   * 1 = Đã xác nhận
   * 2 = Đang giao hàng
   * 3 = Hoàn thành
   * 4 = Đã hủy
   * 5 = Giao hàng thất bại
   * 6 = Yêu cầu hoàn hàng / đổi trả
   * 7 = Hoàn hàng / đổi trả hoàn tất
   */
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
  productName: string | null;
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
  productName: string | null;
  brandName: string | null;
  sku: string | null;
  image: string | null;

  orderStatus: number;
  reviewed: boolean;
  canReview: boolean;
  message: string;
}