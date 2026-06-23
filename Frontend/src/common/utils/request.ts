import axios, { type InternalAxiosRequestConfig, AxiosError } from 'axios';
const baseURL = 'http://localhost:8080/api';

const request = axios.create({
  baseURL,
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

// Interceptor Request: Tự động gắn Token vào mọi request
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (typeof window !== 'undefined') {
      const token = localStorage.getItem('accessToken');
      if (token) {
        config.headers.set('Authorization', `Bearer ${token}`);
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Interceptor Response: Xử lý lỗi cơ bản (như hết hạn Token)
request.interceptors.response.use(
  (response) => response.data, 
  (error: AxiosError) => {
    // Nếu Backend trả về lỗi 401 (Unauthorized - Chưa đăng nhập hoặc Token hết hạn/sai)
    if (error.response?.status === 401) {
      if (typeof window !== 'undefined') {
        // 1. Dọn dẹp sạch sẽ dữ liệu cũ
        localStorage.removeItem('accessToken');
        localStorage.removeItem('currentUser'); 
        
        // 2. Đá văng người dùng về trang đăng nhập
        window.location.href = '/login'; 
      }
    }
    return Promise.reject(error);
  }
);

export default request;