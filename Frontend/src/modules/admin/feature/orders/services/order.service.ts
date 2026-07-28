import api from "@/common/api";
import type {
  AdminOrderResponse,
  OrderSearchParams,
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
          page: params.page ?? 0,
          size: params.size ?? 10,
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