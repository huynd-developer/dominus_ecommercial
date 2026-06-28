import { defineStore } from 'pinia';
import api from '@/common/api';

interface AuthState {
  token: string | null;
  role: string | null;
  name: string | null;
}

// 1. BỔ SUNG GÓI TYPE CHO TYPESCRIPT
export interface AuthResponse {
  success: boolean;
  message?: string;
  validationErrors?: Record<string, string>;
}

// 2. ÉP KIỂU TRẢ VỀ CỦA HELPER LÀ AuthResponse
const handleAuthError = (error: any, defaultMessage: string): AuthResponse => {
  const errorData = error.response?.data;
  if (typeof errorData === 'object' && !errorData.message) {
    return { success: false, validationErrors: errorData };
  }
  return { success: false, message: errorData?.message || defaultMessage };
};

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem('token'),
    role: localStorage.getItem('role'),
    name: localStorage.getItem('name'),
  }),

  getters: {
    isAuthenticated: (state) => !!state.token,
    
    // ĐÂY LÀ CHỖ QUAN TRỌNG NHẤT ĐỂ FIX LỖI 403:
    isAdminSection: (state) => {
      // Ép tất cả về chữ IN HOA và xóa tiền tố ROLE_ (nếu có)
      const cleanRole = (state.role || '').toUpperCase().replace('ROLE_', '');
      return ['OWNER', 'MANAGER', 'CASHIER'].includes(cleanRole);
    },
  },

  actions: {
    // 3. ÉP KIỂU TRẢ VỀ CHO CÁC HÀM LÀ Promise<AuthResponse>
    async loginCustomer(payload: any): Promise<AuthResponse> {
      try {
        const response = await api.post('/auth/login/customer', payload);
        this.setAuthData(response.data);
        return { success: true }; 
      } catch (error: any) {
        return handleAuthError(error, 'Đăng nhập thất bại!');
      }
    },

    async loginEmployee(payload: any): Promise<AuthResponse> {
      try {
        const response = await api.post('/auth/login/employee', payload);
        this.setAuthData(response.data);
        return { success: true };
      } catch (error: any) {
        return handleAuthError(error, 'Đăng nhập quản trị thất bại!');
      }
    },

    async registerCustomer(payload: any): Promise<AuthResponse> {
      try {
        const response = await api.post('/auth/register', payload);
        return { success: true, message: response.data.message };
      } catch (error: any) {
        return handleAuthError(error, 'Đăng ký thất bại!');
      }
    },

    // Giữ nguyên 100% logic setAuthData
    setAuthData(data: any) { 
      console.log("📦 Cấu trúc JSON thực tế từ Backend:", data);
      
      if (data.token) localStorage.setItem('token', data.token);
      if (data.role) localStorage.setItem('role', data.role);
      if (data.name) localStorage.setItem('name', data.name);

      this.token = data.token || null;
      this.role = data.role || null;
      this.name = data.name || null;
    },

    // Giữ nguyên 100% logic logout
    logout() {
      localStorage.removeItem('token');
      localStorage.removeItem('role');
      localStorage.removeItem('name');

      this.token = null;
      this.role = null;
      this.name = null;
    },
  },
});