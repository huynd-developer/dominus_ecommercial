import api from '@/common/api'; 
import type { Category, CategoryRequest } from '../types/category.type';

export const categoryService = {
  getAll(config?: any) {
    return api.get('/admin/categories', config);
  },
  
  getById(id: number) {
    return api.get(`/admin/categories/${id}`);
  },

  create(data: CategoryRequest) {
    return api.post('/admin/categories', data);
  },

  update(id: number, data: CategoryRequest) {
    return api.put(`/admin/categories/${id}`, data);
  },

  delete(id: number) {
    return api.delete(`/admin/categories/${id}`);
  }
};