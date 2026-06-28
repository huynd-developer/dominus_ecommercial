import type { Brand } from '../types/brand.type';

// Dữ liệu mồi giả lập (đã thêm các trường giả của Sản phẩm và dùng 'as any' để TS không báo lỗi)
let mockData: Brand[] = [
  { 
    id: 1, 
    name: 'Bleu de Chanel Parfum', 
    description: 'Thương hiệu cao cấp Pháp', 
    brandName: 'Chanel',
    fragranceGroup: 'Gỗ - Thơm',
    priceRange: '2.450.000 đ',
    stock: 150,
    status: 1 
  },
  { 
    id: 2, 
    name: 'Miss Dior Blooming Bouquet', 
    description: 'Nước hoa quyến rũ thời thượng',
    brandName: 'Dior',
    fragranceGroup: 'Hoa - Trái cây',
    priceRange: '2.650.000 đ',
    stock: 120,
    status: 1 
  },
  { 
    id: 3, 
    name: 'Sauvage EDP', 
    description: 'Hương thơm nam tính', 
    brandName: 'Dior',
    fragranceGroup: 'Gỗ - Cay nồng',
    priceRange: '2.350.000 đ',
    stock: 200,
    status: 0 
  },
  {
    id: 4,
    name: '1 Million EDT',
    description: 'Hương gỗ ấm áp',
    brandName: 'Paco Rabanne',
    fragranceGroup: 'Gỗ - Ấm',
    priceRange: '2.200.000 đ',
    stock: 180,
    status: 1
  },
  {
    id: 5,
    name: 'Black Opium EDP',
    description: 'Hương ngọt ngào',
    brandName: 'Yves Saint Laurent',
    fragranceGroup: 'Cà phê - Ngọt',
    priceRange: '2.550.000 đ',
    stock: 90,
    status: 1
  }
] as any; // Ép kiểu để bypass lỗi TypeScript

export const brandApi = {
  getAll: async () => {
    return new Promise<Brand[]>((resolve) => setTimeout(() => resolve([...mockData]), 300));
  },
  create: async (data: Brand) => {
    const newBrand = { ...data, id: Math.floor(Math.random() * 1000) };
    mockData.unshift(newBrand);
    return new Promise<Brand>((resolve) => setTimeout(() => resolve(newBrand), 300));
  },
  update: async (id: number, data: Brand) => {
    const index = mockData.findIndex(b => b.id === id);
    if (index !== -1) mockData[index] = { ...data, id } as any;
    return new Promise<Brand>((resolve) => setTimeout(() => resolve(mockData[index] as Brand), 300));
  },
  delete: async (id: number) => {
    mockData = mockData.filter(b => b.id !== id);
    return new Promise<boolean>((resolve) => setTimeout(() => resolve(true), 300));
  }
};