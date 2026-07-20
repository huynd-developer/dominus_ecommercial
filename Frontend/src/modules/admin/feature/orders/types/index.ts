export interface OrderAdminResponse {
  id: number;
  customerName: string;
  customerPhone: string;
  orderType: 'ONLINE' | 'IN_STORE';
  totalAmount: number;
  finalAmount: number;
  status: number;
  createdAt: string;
}

export interface OrderItemAdminResponse {
  id: number;
  productName: string;
  capacity: string;
  bottleType: string;
  quantity: number;
  originalPrice: number;
  finalPrice: number;
  discountAmount: number;
  image: string;
}

export interface OrderDetailAdminResponse {
  id: number;
  customerName: string;
  customerPhone: string;
  shippingAddress: string;
  orderType: 'ONLINE' | 'IN_STORE';
  totalAmount: number;
  discountAmount: number;
  finalAmount: number;
  paymentMethod: string;
  status: number;
  createdAt: string;
  items: OrderItemAdminResponse[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}