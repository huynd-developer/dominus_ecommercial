export interface Brand {
  id: number;
  brandName?: string;
  name?: string;
}

export interface Category {
  id: number;
  categoryName?: string;
  name?: string;
}

export interface Concentration {
  id: number;
  concentrationName?: string;
  name?: string;
}

export interface BottleType {
  id: number;
  bottleTypeName?: string;
  name?: string;
}

export interface Capacity {
  id: number;
  value?: number;
  name?: string;
}

export interface FragranceFamily {
  id: number;
  familyName?: string;
  name?: string;
}

export interface ProductImage {
  id: number;
  imageUrl: string;
  isPrimary: boolean;
}

export interface ProductVariant {
  id?: number;
  capacityId: number;
  capacityName?: string;
  bottleTypeId: number;
  bottleTypeName?: string;
  sku?: string;
  price: number;

  /**
   * Nguồn tồn kho thật từ InventoryLot/vw_ProductVariantInventory.
   */
  totalQuantity?: number;
  sellableQuantity?: number;

  /**
   * Compatibility fields. FE Product không dùng stockQuantity làm nguồn tồn kho.
   * manufacturingDate/expirationDate hiện được BE map từ lot FEFO tiếp theo,
   * không còn đọc ProductVariant legacy.
   */
  stockQuantity: number;
  manufacturingDate: string | null;
  expirationDate: string | null;

  status: number;
}

export interface Product {
  id: number;

  /**
   * Snapshot revision của dữ liệu Product/SKU có thể chỉnh sửa.
   * FE gửi lại qua expectedRevision khi PUT để BE phát hiện stale form.
   */
  revision?: string | null;

  name: string;
  description?: string;
  brandId: number;
  brandName?: string;
  categoryId: number;
  categoryName?: string;
  concentrationId: number;
  concentrationName?: string;
  gender: number;
  isNiche: boolean;
  status: number;

  /**
   * Trạng thái xóa mềm của Product.
   */
  isDeleted?: boolean;

  primaryImageUrl?: string | null;
  rating?: number;
  reviewCount?: number;
  images?: ProductImage[];
  fragranceFamilies: FragranceFamily[];
  variants: ProductVariant[];
}

export interface ProductVariantRequest {
  id?: number;
  capacityId: number;
  bottleTypeId: number;
  sku?: string;
  price: number;
  status: number;
}

export interface ProductRequestDTO {
  name: string;
  description?: string;
  brandId: number;
  categoryId: number;
  concentrationId: number;
  gender?: number;
  isNiche?: boolean;
  status?: number;
  fragranceFamilyIds: number[];
  variants: ProductVariantRequest[];

  /**
   * Chỉ gửi khi update. Create/clone để undefined.
   */
  expectedRevision?: string | null;
}

export interface ProductResponse extends Product {}
