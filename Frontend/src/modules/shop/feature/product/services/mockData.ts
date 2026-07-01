
// import bleu from '@/assets/images/bleu.png'
// import sauvage from '@/assets/images/sauvage.png'
// import tomford from '@/assets/images/tomford.png'

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
    id: 1, 
    brand: 'Dior', 
    name: 'Dior Sauvage', 
    price: 3500000, 
    oldPrice: 4000000, 
    rating: 4.9,
    image: '/sauvage.png', 
    description: 'Nước hoa nam tính mạnh mẽ.',
    variants: [
      { id: 6, capacity: '10ml', price: 380000, stock: 100 },
      { id: 7, capacity: '50ml', price: 2400000, stock: 30 },
      { id: 1, capacity: '100ml', price: 3500000, stock: 50 } // ID 1 gốc
    ]
  },
  {
    id: 2, 
    brand: 'Chanel', 
    name: 'Chanel No 5', 
    price: 4200000, 
    oldPrice: 4500000, 
    rating: 4.8,
    image: '/bleu.png', 
    description: 'Huyền thoại nước hoa nữ quý phái.',
    variants: [
      { id: 8, capacity: '10ml', price: 450000, stock: 80 },
      { id: 2, capacity: '50ml', price: 4200000, stock: 20 }, // ID 2 gốc
      { id: 9, capacity: '100ml', price: 5800000, stock: 15 }
    ]
  },
  {
    id: 3, 
    brand: 'Tom Ford', 
    name: 'Tom Ford Oud Wood', 
    price: 600000, 
    oldPrice: 750000, 
    rating: 4.7,
    image: '/tomford.png', 
    description: 'Hương gỗ trầm hương sang trọng bậc nhất.',
    variants: [
      { id: 3, capacity: '10ml', price: 600000, stock: 100 }, // ID 3 gốc
      { id: 10, capacity: '50ml', price: 3500000, stock: 25 },
      { id: 11, capacity: '100ml', price: 6200000, stock: 12 }
    ]
  },
  {
    id: 4, 
    brand: 'Creed', 
    name: 'Creed Aventus', 
    price: 6500000, 
    oldPrice: 7000000, 
    rating: 5.0,
    image: '/creed.png', 
    description: 'Vua nước hoa nam hoàng gia Niche.',
    variants: [
      { id: 12, capacity: '10ml', price: 750000, stock: 50 },
      { id: 13, capacity: '50ml', price: 4200000, stock: 20 },
      { id: 4, capacity: '100ml', price: 6500000, stock: 5 } // ID 4 gốc
    ]
  },
  {
    id: 5, 
    brand: 'Jo Malone', 
    name: 'Jo Malone English Pear', 
    price: 2100000, 
    oldPrice: 2500000, 
    rating: 4.6,
    image: '/jomalone.png', 
    description: 'Hương quả lê tươi mát và hoa lan nam phi thanh lịch.',
    variants: [
      { id: 14, capacity: '10ml', price: 350000, stock: 70 },
      { id: 5, capacity: '30ml', price: 2100000, stock: 30 }, // ID 5 gốc
      { id: 15, capacity: '100ml', price: 3900000, stock: 20 }
    ]
  }
];