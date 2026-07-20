import axios from 'axios';
import type { OrderAdminResponse, OrderDetailAdminResponse, PageResponse } from '../types';

const API_URL = '/api/admin/orders';

// Đính kèm token tự động từ localStorage (nếu có)
const getHeaders = () => {
  const token = localStorage.getItem('token'); 
  return token ? { Authorization: `Bearer ${token}` } : {};
};

export const orderAdminService = {
  // 1. Lấy danh sách đơn hàng có bộ lọc và phân trang
  getOrders: async (params: { keyword?: string; status?: number | string; orderType?: string; page: number; size: number }) => {
    const response = await axios.get<PageResponse<OrderAdminResponse>>(API_URL, { 
      params, 
      headers: getHeaders() 
    });
    return response.data;
  },

  // 2. Xem chi tiết đơn hàng
  getOrderDetail: async (id: number) => {
    const response = await axios.get<OrderDetailAdminResponse>(`${API_URL}/${id}`, { 
      headers: getHeaders() 
    });
    return response.data;
  },

  // 3. Chuyển đổi trạng thái theo luồng dương (0 -> 1 -> 2 -> 3)
  nextStatus: async (id: number) => {
    const response = await axios.put<string>(`${API_URL}/${id}/next-status`, null, { 
      headers: getHeaders() 
    });
    return response.data;
  },

  // 4. Hủy đơn hàng (Status = 4) và hoàn tồn kho
  cancelOrder: async (id: number) => {
    const response = await axios.put<string>(`${API_URL}/${id}/cancel`, null, { 
      headers: getHeaders() 
    });
    return response.data;
  }
};