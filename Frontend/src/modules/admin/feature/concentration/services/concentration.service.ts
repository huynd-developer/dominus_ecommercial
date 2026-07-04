import api from '@/common/api';
import type { ConcentrationRequest } from '../types/concentration.type';

const API_URL = '/admin/concentrations';

export const concentrationService = {
  getAll(config?: any) {
    return api.get(API_URL, config);
  },
  
  create(data: ConcentrationRequest) {
    return api.post(API_URL, data);
  },
  
  update(id: number, data: ConcentrationRequest) {
    return api.put(`${API_URL}/${id}`, data);
  },
  
  delete(id: number) {
    return api.delete(`${API_URL}/${id}`);
  }
};