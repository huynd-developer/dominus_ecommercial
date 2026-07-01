import api from '@/common/api'; 
import type { Brand, BrandRequest } from '../types/brand.type';

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
  }
};