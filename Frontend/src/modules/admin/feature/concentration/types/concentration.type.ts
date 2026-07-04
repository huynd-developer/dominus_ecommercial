export interface Concentration {
  id: number;
  name: string;
  status: number; // 1: Đang hoạt động, 0: Tạm ẩn
}

export interface ConcentrationRequest {
  name: string;
  status: number;
}