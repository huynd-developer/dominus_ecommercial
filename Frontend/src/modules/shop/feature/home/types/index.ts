export interface ProductVariant {
  id?: number;
  Id?: number;
  variantId?: number;
  productVariantId?: number;
  price?: number;
  stock?: number;
  stockQuantity?: number;
  availableQuantity?: number;
  quantity?: number;
  status?: number;
}

export interface Product {
  id: number;
  productId?: number;
  productVariantId?: number;
  variantId?: number;

  name: string;
  brand: string;
  image?: string;
  imageUrl?: string;
  color?: string;

  price?: number;
  salePrice: number;
  originalPrice: number;
  discountPercent: number;

  rating: number;
  reviewCount: number;
  reviews?: number;

  stock?: number;
  stockQuantity?: number;
  availableQuantity?: number;
  status?: number;

  isFlashSale?: boolean;
  isNew?: boolean;
  isFeatured?: boolean;

  variants?: ProductVariant[];
}