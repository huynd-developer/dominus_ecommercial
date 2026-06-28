export interface Brand {
  id?: number;
  name: string;
  description: string;
  status: number; // 0: Ẩn, 1: Hiện (Validate theo nghiệp vụ task)
}