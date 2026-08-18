export interface ProductVariant {

  id?: number;

  Id?: number;

  variantId?: number;

  productVariantId?: number;

  price?: number;

  stock?: number;

  /**
   * LEGACY compatibility. Không dùng làm tồn nghiệp vụ.
   */
  stockQuantity?: number;

  /**
   * Tồn có thể bán thật của SKU từ InventoryLot.
   */
  sellableQuantity?: number;

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

  /**
   * LEGACY compatibility. Không dùng làm tồn nghiệp vụ.
   */
  stockQuantity?: number;

  /**
   * Tổng tồn có thể bán của dữ liệu Product khi API có trả về.
   */
  sellableQuantity?: number;

  availableQuantity?: number;

  status?: number;

  isFlashSale?: boolean;

  isNew?: boolean;

  isFeatured?: boolean;

  variants?: ProductVariant[];

}