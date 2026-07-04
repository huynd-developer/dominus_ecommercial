import api from '@/common/api'; 
import type { CapacityRequest } from '../types/capacity.type';

export const capacityService = {
  getAll(config?: any) {
    return api.get('/admin/capacities', config);
  },
  
  getById(id: number) {
    return api.get(`/admin/capacities/${id}`);
  },

  create(data: CapacityRequest) {
    return api.post('/admin/capacities', data);
  },

  update(id: number, data: CapacityRequest) {
    return api.put(`/admin/capacities/${id}`, data);
  },

  delete(id: number) {
    return api.delete(`/admin/capacities/${id}`);
  }
};