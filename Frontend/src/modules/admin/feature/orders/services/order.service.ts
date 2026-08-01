import api from "@/common/api";
import type {
  AdminCancelOrderRequest,
  AdminOrderResponse,
  AdminOrderStatusCountResponse,
  MarkDeliveryCompletedRequest,
  MarkDeliveryFailedRequest,
  OrderSearchParams,
  OrderStatusCountParams,
  PageResponse,
  RejectReturnRequest,
  UpdateOrderStatusResponse,
} from "../types/order.type";

const ORDER_ADMIN_API = "/admin/orders";

function buildDeliveryCompletedFormData(data: MarkDeliveryCompletedRequest) {
  const formData = new FormData();

  (data.files || []).forEach((file) => {
    if (file) {
      formData.append("files", file);
    }
  });

  return formData;
}

function buildDeliveryFailedFormData(data: MarkDeliveryFailedRequest) {
  const formData = new FormData();

  formData.append("reason", data.reason || "");

  if (data.description) {
    formData.append("description", data.description);
  }

  (data.files || []).forEach((file) => {
    if (file) {
      formData.append("files", file);
    }
  });

  return formData;
}

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

  /**
   * Admin hủy đơn khi đơn còn ở trạng thái Chờ xác nhận.
   * BE sẽ lưu lý do hủy và thời gian hủy để admin/khách xem lại.
   */
  async cancelOrder(orderId: number, data: AdminCancelOrderRequest) {
    const response = await api.patch<AdminOrderResponse>(
      `${ORDER_ADMIN_API}/${orderId}/cancel`,
      data
    );

    return response.data;
  },

  /**
   * Xác nhận giao hàng thành công.
   * Bắt buộc gửi ảnh minh chứng.
   */
  async markDeliveryCompleted(
    orderId: number,
    data: MarkDeliveryCompletedRequest
  ) {
    const response = await api.patch<AdminOrderResponse>(
      `${ORDER_ADMIN_API}/${orderId}/delivery-completed`,
      buildDeliveryCompletedFormData(data),
      {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      }
    );

    return response.data;
  },

  /**
   * Xác nhận giao hàng thất bại.
   * Bắt buộc có lý do, mô tả nếu chọn Khác, ảnh nếu lý do nhạy cảm.
   */
  async markDeliveryFailed(orderId: number, data: MarkDeliveryFailedRequest) {
    const response = await api.patch<AdminOrderResponse>(
      `${ORDER_ADMIN_API}/${orderId}/delivery-failed`,
      buildDeliveryFailedFormData(data),
      {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      }
    );

    return response.data;
  },

  /**
   * Admin xác nhận đã hoàn tiền thực tế cho đơn giao hàng thất bại.
   */
  async markDeliveryRefunded(orderId: number) {
    const response = await api.patch<AdminOrderResponse>(
      `${ORDER_ADMIN_API}/${orderId}/delivery-refunded`
    );

    return response.data;
  },

  /**
   * Admin chấp nhận yêu cầu hoàn hàng.
   * Sau bước này mới được bấm "Đã hoàn tiền".
   */
  async acceptReturn(orderId: number) {
    const response = await api.patch<AdminOrderResponse>(
      `${ORDER_ADMIN_API}/${orderId}/return-accepted`
    );

    return response.data;
  },

  /**
   * Admin từ chối yêu cầu hoàn hàng.
   * BE bắt buộc có lý do để khách hàng nhìn được vì sao bị từ chối.
   */
  async rejectReturn(orderId: number, data: RejectReturnRequest) {
    const response = await api.patch<AdminOrderResponse>(
      `${ORDER_ADMIN_API}/${orderId}/return-rejected`,
      data
    );

    return response.data;
  },

  /**
   * Admin xác nhận đã hoàn tiền thực tế cho khách.
   * BE chỉ cho chạy khi yêu cầu hoàn đã được chấp nhận.
   */
  async markReturnRefunded(orderId: number) {
    const response = await api.patch<AdminOrderResponse>(
      `${ORDER_ADMIN_API}/${orderId}/return-refunded`
    );

    return response.data;
  },
};