// 1. Khai báo kiểu dữ liệu ngay tại đây (Không cần thư mục types nữa)
export interface Variant {
  id: number;
  capacity: string;
  price: number;
  stock: number;
}

export interface Product {
  id: number;
  brand: string;
  name: string;
  price: number;
  oldPrice: number;
  image: string;
  rating: number;
  description: string;
  variants: Variant[];
}

// 2. Chứa cục data giả
export const mockProductList: Product[] = [
  {
    id: 1, brand: 'Chanel', name: 'Bleu de Chanel Parfum', price: 4250000, oldPrice: 5300000, rating: 4.8,
    image: 'https://images.unsplash.com/photo-1526413232644-8a40f41ce931?auto=format&fit=crop&q=80&w=400',
    description: 'Bleu de Chanel Parfum là sự thể hiện đậm đà và sâu lắng nhất của sự tự do.\nHương thơm nam tính, thanh lịch với chiều sâu của gỗ tuyết tùng.\nNew Caledonia kết hợp cùng sự ấm áp của hoắc hương và labdanum.\nMột mùi hương tạo dấu ấn mạnh mẽ và bền bỉ.',
    variants: [
      { id: 1, capacity: '10ml', price: 600000, stock: 999 }, 
      { id: 2, capacity: '50ml', price: 4250000, stock: 999 }, 
      { id: 3, capacity: '100ml', price: 6500000, stock: 999 }
    ]
  },
  {
    id: 2, brand: 'Dior', name: 'Sauvage Elixir', price: 3500000, oldPrice: 4200000, rating: 4.9,
    image: 'https://images.unsplash.com/photo-1594035910387-fea47794261f?auto=format&fit=crop&q=80&w=400',
    description: 'Dior Sauvage Elixir nồng nàn quyến rũ.',
    variants: [
      { id: 4, capacity: '10ml', price: 550000, stock: 50 }, 
      { id: 5, capacity: '60ml', price: 3500000, stock: 100 }
    ]
  },
  {
    id: 3, brand: 'Tom Ford', name: 'Oud Wood Eau de Parfum', price: 6800000, oldPrice: 7500000, rating: 4.7,
    image: 'https://images.unsplash.com/photo-1588405748880-12d1d2a59f75?auto=format&fit=crop&q=80&w=400',
    description: 'Gỗ trầm hương quyền lực.',
    variants: [
      { id: 6, capacity: '50ml', price: 6800000, stock: 20 }
    ]
  }
];