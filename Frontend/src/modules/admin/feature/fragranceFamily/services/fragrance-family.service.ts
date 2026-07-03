import api from '@/common/api'; // Đường dẫn import api instance của bạn
import type { FragranceFamilyRequest } from '../types/fragrance-family.type';

const API_URL = '/admin/fragrance-families';

export const fragranceFamilyService = {
  getAll(config?: any) {
    return api.get(API_URL, config);
  },
  
  create(data: FragranceFamilyRequest) {
    return api.post(API_URL, data);
  },
  
  update(id: number, data: FragranceFamilyRequest) {
    return api.put(`${API_URL}/${id}`, data);
  },
  
  delete(id: number) {
    return api.delete(`${API_URL}/${id}`);
  }
};