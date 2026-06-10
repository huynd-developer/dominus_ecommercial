// frontend/src/common/types/api.type.ts
export interface ApiResponse<T> {
  timestamp: string;
  status: number;
  message: string;
  data: T;
}