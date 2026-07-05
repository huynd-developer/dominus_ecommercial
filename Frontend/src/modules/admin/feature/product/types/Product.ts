export interface ProductImage {
    id?: number;
    imageUrl: string;
    isPrimary: boolean;
}

export interface ProductVariant {
    id?: number;

    productId?: number;

    capacityId: number;
    capacityValue?: number;

    bottleTypeId: number;
    bottleTypeName?: string;

    sku: string;

    price: number;

    stockQuantity: number;

    status: number;
}

export interface Product {

    id?: number;

    name: string;

    brandId: number | null;
    brandName?: string;

    categoryId: number | null;
    categoryName?: string;

    concentrationId: number | null;
    concentrationName?: string;

    description: string;

    gender: number;

    isNiche: boolean;

    status: number;

    images: ProductImage[];

    variants: ProductVariant[];
}

export interface ProductPage {

    content: Product[];

    totalElements: number;

    totalPages: number;

    number: number;

    size: number;

    first: boolean;

    last: boolean;

    empty: boolean;

}