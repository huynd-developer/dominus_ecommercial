import type { Product } from '../types';

export const mockProducts: Product[] = [
  // --- NHÓM FLASH SALE ---
  {
    id: 1,
    name: 'Bleu De Chanel EDP',
    brand: 'Chanel',
    image: 'https://i.imgur.com/3Z8g8rG.png', // Thay bằng link ảnh thật của bạn
    price: 2625000,
    originalPrice: 3500000,
    discount: 25,
    rating: 5,
    reviews: 128,
    isFlashSale: true
  },
  {
    id: 2,
    name: 'Miss Dior Blooming Bouquet',
    brand: 'Dior',
    image: 'https://i.imgur.com/w8N9Xm8.png',
    price: 2550000,
    originalPrice: 3400000,
    discount: 25,
    rating: 4.8,
    reviews: 98,
    isFlashSale: true
  },
  {
    id: 3,
    name: 'Sauvage EDP',
    brand: 'Dior',
    image: 'https://i.imgur.com/wVfA0gM.png',
    price: 2625000,
    originalPrice: 3500000,
    discount: 25,
    rating: 5,
    reviews: 154,
    isFlashSale: true
  },
  // --- NHÓM NƯỚC HOA MỚI NHẤT ---
  {
    id: 7,
    name: 'Paradoxe EDP',
    brand: 'Prada',
    image: 'https://i.imgur.com/xR8B9zB.png',
    price: 3200000,
    rating: 4.9,
    reviews: 94,
    isNew: true
  },
  {
    id: 8,
    name: 'My Way EDP',
    brand: 'Giorgio Armani',
    image: 'https://i.imgur.com/yT9C9zC.png',
    price: 2950000,
    rating: 4.7,
    reviews: 86,
    isNew: true
  },
  // --- NHÓM SẢN PHẨM NỔI BẬT ---
  {
    id: 13,
    name: 'N°5 Chanel EDP',
    brand: 'Chanel',
    image: 'https://i.imgur.com/zU9D9zD.png',
    price: 3780000,
    rating: 5,
    reviews: 111,
    isFeatured: true
  }
];