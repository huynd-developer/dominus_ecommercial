import { ref, computed } from 'vue';
import { defineStore } from 'pinia';
import request from '@/common/utils/request';

// Định nghĩa các interface rõ ràng để tránh lỗi Type Check hiển thị trên IDE
interface UserProfile {
  username: string;
  role?: string;
}

interface LoginPayload {
  loginId: string;
  password: string;
}

interface RegisterPayload {
  username: string;
  fullName: string;
  email: string;
  phone: string;
  password: string;
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserProfile | null>(null);
  const token = ref<string | null>(localStorage.getItem('accessToken'));
  const isLoggedIn = computed(() => !!token.value);

  // 1. Hàm giải mã JWT chống sập luồng (Sửa lỗi DOMException của hàm atob)
  function getRoleFromJwt(tokenString: string | null): string {
    if (!tokenString || typeof tokenString !== 'string' || tokenString.includes('[object')) return 'USER';
    try {
      const parts = tokenString.split('.');
      if (parts.length < 2) return 'USER';
      
      let base64Url = parts[1] || ''; 
      if (!base64Url) return 'USER'; 
      
      let base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      
      while (base64.length % 4) {
        base64 += '=';
      }

      const jsonPayload = decodeURIComponent(
        window.atob(base64)
          .split('')
          .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      
      const payload = JSON.parse(jsonPayload);
      let rawRole = payload.role || payload.roles || payload.authorities || payload.auth;
      if (Array.isArray(rawRole) && rawRole.length > 0) {
        rawRole = rawRole[0].authority || rawRole[0];
      }

      if (rawRole && typeof rawRole === 'string') {
        return rawRole.toUpperCase().replace('ROLE_', '');
      }
    } catch (e) {
      console.error("⚠️ Lỗi không thể giải mã cấu trúc JWT Token:", e);
    }
    return 'USER';
  }

  // 2. Hàm xử lý lưu trữ trạng thái khi Đăng nhập thành công
  function processLoginSuccess(tokenString: string, usernameStr?: string) {
    token.value = tokenString;
    localStorage.setItem('accessToken', tokenString);

    const extractedRole = getRoleFromJwt(tokenString);
    const finalUsername = usernameStr || 'User';

    user.value = { 
      username: finalUsername,
      role: extractedRole
    }; 

    localStorage.setItem('currentUser', JSON.stringify({ 
      fullName: finalUsername,
      role: extractedRole 
    }));
    
    window.dispatchEvent(new Event('user-logged-in'));
    return { success: true, message: "Đăng nhập thành công" };
  }

  // 3. ĐÃ SỬA: Hàm bóc tách nội dung lỗi từ Backend để khớp với GlobalExceptionHandler
  function parseBackendError(error: any): string {
    if (!error.response || !error.response.data) {
      return error.message || 'Lỗi kết nối máy chủ, vui lòng thử lại!';
    }
    
    const errData = error.response.data;

    // Trường hợp 1: Backend trả về một chuỗi thông báo thuần (String)
    if (typeof errData === 'string') {
      return errData;
    }

    // Trường hợp 2: Backend trả về cấu trúc có mảng errors (Spring mặc định)
    if (errData.errors && Array.isArray(errData.errors) && errData.errors.length > 0) {
      return errData.errors[0].defaultMessage || 'Dữ liệu đầu vào không hợp lệ!';
    }

    // Trường hợp 3: Bản tin JSON chứa thuộc tính message
    if (errData.message) {
      return errData.message;
    }

    // Trường hợp 4 (QUAN TRỌNG): Backend trả về Map<String, String> từ GlobalExceptionHandler 
    // Ví dụ: {"email": "Email không đúng định dạng!", "phone": "Lỗi số điện thoại"}
    if (typeof errData === 'object' && Object.keys(errData).length > 0) {
      // Lấy câu thông báo lỗi đầu tiên trong Object để hiển thị
      const firstErrorValue = Object.values(errData)[0];
      if (typeof firstErrorValue === 'string') {
        return firstErrorValue;
      }
    }

    return 'Xảy ra lỗi không xác định từ hệ thống!';
  }

  // -- Đăng Nhập Khách Hàng --
  async function loginCustomer(credentials: LoginPayload) {
    try {
      const payload = {
        email: credentials.loginId,
        password: credentials.password
      };
      const res: any = await request.post('/auth/login/customer', payload);
      
      const tokenStr = typeof res === 'string' ? res : (res?.accessToken || res?.data?.accessToken || res?.data);
      
      if (!tokenStr) return { success: false, message: 'Hệ thống không trả về Token hợp lệ!' };
      return processLoginSuccess(tokenStr, credentials.loginId);
    } catch (error: any) {
      return { success: false, message: parseBackendError(error) };
    }
  }

  // -- Đăng Nhập Nhân Viên --
  async function loginEmployee(credentials: LoginPayload) {
    try {
      const payload = {
        email: credentials.loginId,
        password: credentials.password
      };
      const res: any = await request.post('/auth/login/employee', payload);
      
      const tokenStr = typeof res === 'string' ? res : (res?.accessToken || res?.data?.accessToken || res?.data);
      
      if (!tokenStr) return { success: false, message: 'Hệ thống không trả về Token hợp lệ!' };
      return processLoginSuccess(tokenStr, credentials.loginId);
    } catch (error: any) {
      return { success: false, message: parseBackendError(error) };
    }
  }

  // -- Đăng Ký Khách Hàng --
  async function registerCustomer(registerData: RegisterPayload) {
    try {
      const payload = {
        name: registerData.fullName,
        email: registerData.email,
        phone: registerData.phone,
        password: registerData.password
      };
      const res: any = await request.post('/auth/register', payload);
      const successMsg = typeof res === 'string' ? res : (res?.message || 'Đăng ký tài khoản thành công!');
      return { success: true, message: successMsg };
    } catch (error: any) {
      return { success: false, message: parseBackendError(error) };
    }
  }

  // -- Đăng Xuất --
  function logout() {
    token.value = null;
    user.value = null;
    localStorage.removeItem('accessToken');
    localStorage.removeItem('currentUser'); 
    window.location.href = '/login'; 
  }

  return { user, token, isLoggedIn, loginCustomer, loginEmployee, registerCustomer, logout };
});