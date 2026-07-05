// src/modules/admin/feature/bottleType/services/bottle-type.service.ts
import api from '@/common/api'; 
import type { BottleTypeRequest } from '../types/bottle-type.type';

export const bottleTypeService = {
  getAll(config?: any) {
    return api.get('/admin/bottle-types', config);
  },
  
  getById(id: number) {
    return api.get(`/admin/bottle-types/${id}`);
  },

  create(data: BottleTypeRequest) {
    return api.post('/admin/bottle-types', data);
  },

  update(id: number, data: BottleTypeRequest) {
    return api.put(`/admin/bottle-types/${id}`, data);
  },

  delete(id: number) {
    return api.delete('/admin/bottle-types/' + id);
  }
};