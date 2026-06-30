export interface Product {
  id: number;
  name: string;
  brand: string;
  image: string;
  price: number;
  originalPrice?: number;
  discount?: number;
  rating: number;
  reviews: number;
  isFlashSale?: boolean;
  isNew?: boolean;
  isFeatured?: boolean;
}