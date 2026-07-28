import api from "@/common/api";
import type {
  AdminOrderResponse,
  AdminOrderStatusCountResponse,
  OrderSearchParams,
  OrderStatusCountParams,
  PageResponse,
  UpdateOrderStatusResponse,
} from "../types/order.type";

const ORDER_ADMIN_API = "/admin/orders";

export const orderService = {
  async getOrders(params: OrderSearchParams) {
    const response = await api.get<PageResponse<AdminOrderResponse>>(
      ORDER_ADMIN_API,
      {
        params: {
          keyword: params.keyword || undefined,

          status:
            params.status === null || params.status === undefined
              ? undefined
              : params.status,

          orderType: params.orderType || undefined,

          fromDate: params.fromDate || undefined,
          toDate: params.toDate || undefined,

          page: params.page ?? 0,
          size: params.size ?? 10,
        },
      }
    );

    return response.data;
  },

  async getStatusCounts(params: OrderStatusCountParams) {
    const response = await api.get<AdminOrderStatusCountResponse>(
      `${ORDER_ADMIN_API}/status-counts`,
      {
        params: {
          keyword: params.keyword || undefined,
          orderType: params.orderType || undefined,

          fromDate: params.fromDate || undefined,
          toDate: params.toDate || undefined,
        },
      }
    );

    return response.data;
  },

  async getOrderDetail(orderId: number) {
    const response = await api.get<AdminOrderResponse>(
      `${ORDER_ADMIN_API}/${orderId}`
    );

    return response.data;
  },

  async updateOrderStatus(orderId: number, status: number) {
    const response = await api.patch<UpdateOrderStatusResponse>(
      `${ORDER_ADMIN_API}/${orderId}/status`,
      {
        status,
      }
    );

    return response.data;
  },
};