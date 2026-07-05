// src/types/brand.type.ts

export interface BrandRequest {
  name: string;
  description: string;
  status: number;
  logoUrl: string | null; // Thêm trường ảnh
}

export interface Brand {
  id: number;
  name: string;
  description: string;
  status: number;
  logoUrl: string | null; // Thêm trường ảnh
}