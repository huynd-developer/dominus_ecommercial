export interface Brand {
  id: number;
  name: string;
  description: string;
  status: number; 
}

// Interface dùng lúc gửi request Thêm/Sửa (không có ID)
export interface BrandRequest {
  name: string;
  description: string;
  status: number;
}