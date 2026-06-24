import axios from 'axios';

// 1. Cấu hình các giá trị mặc định toàn hệ thống
axios.defaults.baseURL = 'http://localhost:8080/api/v1';
axios.defaults.headers.common['Content-Type'] = 'application/json';

// 2. INTERCEPTOR CHO REQUEST: Tự động "đóng dấu" Token vào mọi request gửi đi
axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    
    // Nếu có token trong máy thì tự động đính kèm vào Header Authorization
    if (token && token !== 'null' && token !== 'undefined') {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 3. INTERCEPTOR CHO RESPONSE: Tự động xử lý khi Token hết hạn
axios.interceptors.response.use(
  (response) => response,
  (error) => {
    // Nếu Backend trả về 401 (Hết hạn phiên đăng nhập hoặc token lỏ)
    if (error.response && error.response.status === 401) {
      console.warn('[Axios Global] Phát hiện phiên đăng nhập hết hạn. Đang dọn dẹp bộ nhớ...');
      
      localStorage.removeItem('accessToken');
      localStorage.removeItem('currentUser');
      
      // Nếu đang ở phân hệ Admin thì đá thẳng cổ ra màn hình Login
      if (window.location.pathname.startsWith('/admin')) {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);