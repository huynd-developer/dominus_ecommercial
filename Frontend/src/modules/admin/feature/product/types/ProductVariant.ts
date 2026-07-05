export interface ProductVariantRequest {

    capacityId: number;

    bottleTypeId: number;

    sku: string;

    price: number;

    stockQuantity: number;

    status: number;

}

export interface ProductVariant {

    id?: number;

    productId?: number;

    productName?: string;

    capacityId: number;

    capacityValue?: number;

    bottleTypeId: number;

    bottleTypeName?: string;

    sku: string;

    price: number;

    stockQuantity: number;

    status: number;

}