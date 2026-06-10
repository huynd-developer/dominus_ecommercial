// src/services/api.ts
import axios from 'axios';

// Tạo một instance (bản sao) của axios với cấu hình mặc định
const api = axios.create({
    baseURL: 'http://localhost:8080/api/v1', // Trỏ thẳng vào Backend của Huy
    timeout: 10000, // Đợi 10s, nếu BE sập thì báo lỗi
});

export default api;