import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api', // Điền đúng cổng BE của ông
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Lấy thông tin của request vừa gọi
    const originalRequest = error.config;

    // Nếu Backend báo lỗi 401 (Token hết hạn hoặc sai) 
    // VÀ request đó KHÔNG PHẢI là request gọi API đăng nhập
    if (
      error.response && 
      error.response.status === 401 && 
      !originalRequest.url.includes('/login') // <-- Chìa khóa để fix lỗi ở đây
    ) {
      // Xóa dữ liệu cũ
      localStorage.removeItem('token');
      localStorage.removeItem('role');
      localStorage.removeItem('name');
      
      // Đá về trang đăng nhập
      alert('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại!');
      window.location.href = '/login'; 
    }
    
    return Promise.reject(error);
  }
);

export default api;