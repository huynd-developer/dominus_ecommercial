export interface Category {
  id: number;
  name: string;
  status: number; 
}

// Interface dùng lúc gửi request Thêm/Sửa (không có ID)
export interface CategoryRequest {
  name: string;
  status: number;
}