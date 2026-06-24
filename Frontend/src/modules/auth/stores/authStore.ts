import { defineStore } from 'pinia';
import api from '@/common/api';

interface AuthState {
  token: string | null;
  role: string | null;
  name: string | null;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem('token'),
    role: localStorage.getItem('role'),
    name: localStorage.getItem('name'),
  }),

  getters: {
    isAuthenticated: (state) => !!state.token,
    isAdminSection: (state) => ['OWNER', 'MANAGER', 'CASHIER'].includes(state.role || ''),
  },

  actions: {
    // Luồng đăng nhập Customer
    async loginCustomer(payload: any) {
      try {
        const response = await api.post('/auth/login/customer', payload);
        this.setAuthData(response.data);
        return { success: true };
      } catch (error: any) {
        return { success: false, message: error.response?.data?.message || 'Đăng nhập thất bại!' };
      }
    },

    // Luồng đăng nhập Nhân viên / Admin
    async loginEmployee(payload: any) {
      try {
        const response = await api.post('/auth/login/employee', payload);
        this.setAuthData(response.data);
        return { success: true };
      } catch (error: any) {
        return { success: false, message: error.response?.data?.message || 'Đăng nhập quản trị thất bại!' };
      }
    },

    // Luồng đăng ký tài khoản Customer
    async registerCustomer(payload: any) {
      try {
        const response = await api.post('/auth/register', payload);
        return { success: true, message: response.data.message };
      } catch (error: any) {
        const errorData = error.response?.data;
        // Nếu BE trả về cụm lỗi Validation Map (MethodArgumentNotValidException)
        if (typeof errorData === 'object' && !errorData.message) {
          return { success: false, validationErrors: errorData };
        }
        return { success: false, message: errorData?.message || 'Đăng ký thất bại!' };
      }
    },

    // Helper thiết lập dữ liệu sạch
    // Helper thiết lập dữ liệu sạch
    setAuthData(data: any) { // Đổi tạm thành any để log không bị lỗi ép kiểu ép cấu trúc
      // 👇 CHÈN DÒNG NÀY VÀO ĐỂ XEM BACKEND ĐANG TRẢ VỀ CÁI GÌ 👇
      console.log("📦 Cấu trúc JSON thực tế từ Backend:", data);

      // Tạm thời giữ nguyên để chạy thử xem log ra sao
      this.token = data.token;
      this.role = data.role;
      this.name = data.name;
      localStorage.setItem('token', data.token || '');
      localStorage.setItem('role', data.role || '');
      localStorage.setItem('name', data.name || '');
    },

    // Đăng xuất xóa sạch dấu vết
    logout() {
      this.token = null;
      this.role = null;
      this.name = null;
      localStorage.clear();
    },
  },
});