export interface ProductImage {

    id: number;

    imageUrl: string;

    isPrimary: boolean;

}

export interface Product {

    id: number;

    name: string;

    brandId: number;

    brandName: string;

    categoryId: number;

    categoryName: string;

    concentrationId: number;

    concentrationName: string;

    description: string;

    gender: number;

    isNiche: boolean;

    status: number;

    images: ProductImage[];

}

export interface ProductPage {

    content: Product[];

    totalElements: number;

    totalPages: number;

    number: number;

    size: number;

}

export interface ProductSearch {

    keyword: string;

    page: number;

    size: number;

}