import { defineStore } from 'pinia';
import api from '@/common/api';

type UserRole = 'OWNER' | 'MANAGER' | 'CASHIER' | 'USER' | '';

interface AuthState {
  token: string | null;
  role: UserRole | null;
  name: string | null;
}

interface BackendLoginResponse {
  token: string;
  role: string;
  name: string;
}

export interface AuthResponse {
  success: boolean;
  message?: string;
  validationErrors?: Record<string, string>;
  token?: string;
  role?: UserRole;
  name?: string;
}

const normalizeRole = (role?: string | null): UserRole => {
  const cleanRole = (role || '')
    .toUpperCase()
    .replace('ROLE_', '')
    .trim();

  if (['OWNER', 'MANAGER', 'CASHIER', 'USER'].includes(cleanRole)) {
    return cleanRole as UserRole;
  }

  return '';
};

const handleAuthError = (error: any, defaultMessage: string): AuthResponse => {
  const errorData = error?.response?.data;

  // Backend mới: { message: "...", errors: { field: "..." } }
  if (errorData?.errors) {
    return {
      success: false,
      message: errorData.message || 'Dữ liệu không hợp lệ',
      validationErrors: errorData.errors,
    };
  }

  // Backend auth hiện tại: { message: "..." }
  if (errorData?.message) {
    return {
      success: false,
      message: errorData.message,
    };
  }

  // Trường hợp validation cũ trả thẳng object field lỗi
  if (typeof errorData === 'object' && errorData !== null) {
    return {
      success: false,
      message: 'Dữ liệu không hợp lệ',
      validationErrors: errorData,
    };
  }

  return {
    success: false,
    message: defaultMessage,
  };
};

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem('token'),
    role: normalizeRole(localStorage.getItem('role')),
    name: localStorage.getItem('name'),
  }),

  getters: {
    isAuthenticated: (state) => !!state.token,

    cleanRole: (state): UserRole => {
      return normalizeRole(state.role);
    },

    isOwner: (state) => normalizeRole(state.role) === 'OWNER',

    isManager: (state) => normalizeRole(state.role) === 'MANAGER',

    isCashier: (state) => normalizeRole(state.role) === 'CASHIER',

    isUser: (state) => normalizeRole(state.role) === 'USER',

    isAdminSection: (state) => {
      const role = normalizeRole(state.role);
      return ['OWNER', 'MANAGER', 'CASHIER'].includes(role);
    },
  },

  actions: {
    async loginCustomer(payload: {
      email: string;
      password: string;
    }): Promise<AuthResponse> {
      try {
        const response = await api.post<BackendLoginResponse>(
          '/auth/login/customer',
          {
            email: payload.email.trim(),
            password: payload.password,
          }
        );

        this.setAuthData(response.data);

        return {
          success: true,
          token: this.token || undefined,
          role: this.role || undefined,
          name: this.name || undefined,
        };
      } catch (error: any) {
        return handleAuthError(error, 'Đăng nhập thất bại!');
      }
    },

    async loginEmployee(payload: {
      email: string;
      password: string;
    }): Promise<AuthResponse> {
      try {
        const response = await api.post<BackendLoginResponse>(
          '/auth/login/employee',
          {
            email: payload.email.trim(),
            password: payload.password,
          }
        );

        this.setAuthData(response.data);

        return {
          success: true,
          token: this.token || undefined,
          role: this.role || undefined,
          name: this.name || undefined,
        };
      } catch (error: any) {
        return handleAuthError(error, 'Đăng nhập quản trị thất bại!');
      }
    },

    async registerCustomer(payload: {
      name: string;
      email: string;
      phone: string;
      password: string;
    }): Promise<AuthResponse> {
      try {
        const response = await api.post<{ message: string }>(
          '/auth/register',
          {
            name: payload.name.trim(),
            email: payload.email.trim().toLowerCase(),
            phone: payload.phone.trim(),
            password: payload.password,
          }
        );

        return {
          success: true,
          message: response.data.message || 'Đăng ký tài khoản thành công!',
        };
      } catch (error: any) {
        return handleAuthError(error, 'Đăng ký thất bại!');
      }
    },

    setAuthData(data: BackendLoginResponse) {
      console.log('📦 Cấu trúc JSON thực tế từ Backend:', data);

      const token = data.token || '';
      const role = normalizeRole(data.role);
      const name = data.name || '';

      if (!token || !role) {
        this.logout();
        return;
      }

      localStorage.setItem('token', token);
      localStorage.setItem('role', role);
      localStorage.setItem('name', name);

      this.token = token;
      this.role = role;
      this.name = name;
    },

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