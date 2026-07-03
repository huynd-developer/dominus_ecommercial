// src/services/brand.service.ts
import api from '@/common/api'; 
import type { BrandRequest } from '../types/brand.type';

export const brandService = {
  getAll(config?: any) {
    return api.get('/admin/brands', config);
  },
  
  getById(id: number) {
    return api.get(`/admin/brands/${id}`);
  },

  create(data: BrandRequest) {
    return api.post('/admin/brands', data);
  },

  update(id: number, data: BrandRequest) {
    return api.put(`/admin/brands/${id}`, data);
  },

  delete(id: number) {
    return api.delete(`/admin/brands/${id}`);
  },

  // HÀM MỚI: Upload ảnh
  uploadLogo(file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/admin/brands/upload-logo', formData, {
      headers: {
        'Content-Type': 'multipart/form-data' // Bắt buộc khi gửi file
      }
    });
  }
};