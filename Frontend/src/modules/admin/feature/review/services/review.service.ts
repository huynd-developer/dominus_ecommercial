import api from '@/common/api';

const API_URL = '/admin/reviews';

export const reviewService = {
  getReviews(params?: any) {
    return api.get(API_URL, { params });
  },
  
  approveReview(id: number) {
    return api.patch(`${API_URL}/${id}/approve`);
  },
  
  rejectReview(id: number, reason: string) {
    return api.patch(`${API_URL}/${id}/reject`, { rejectedReason: reason });
  },
  
  hideReview(id: number) {
    return api.patch(`${API_URL}/${id}/hide`);
  }
};