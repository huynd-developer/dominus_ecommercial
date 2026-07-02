import request from "@/common/services/request";

import type {
    Product,
    ProductPage
} from "../types/Product";

const API = "/api/admin/products";

export const getProducts = (params: any): Promise<ProductPage> => {
    return request.get(API, { params });
};

export const getProductById = (id: number): Promise<Product> => {
    return request.get(`${API}/${id}`);
};

export const createProduct = (formData: FormData): Promise<Product> => {
    return request.post(API, formData, {
        headers: {
            "Content-Type": "multipart/form-data"
        }
    });
};

export const updateProduct = (
    id: number,
    formData: FormData
): Promise<Product> => {
    return request.put(`${API}/${id}`, formData, {
        headers: {
            "Content-Type": "multipart/form-data"
        }
    });
};

export const deleteProduct = (id: number): Promise<void> => {
    return request.delete(`${API}/${id}`);
};