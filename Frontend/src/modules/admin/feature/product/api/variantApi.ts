import request from "@/common/services/request";

import type {
    ProductVariant,
    ProductVariantRequest
} from "../types/ProductVariant";

const API = "/api/admin/products";

/* ================= VARIANT ================= */

export const getVariants = (
    productId: number
): Promise<ProductVariant[]> => {

    return request.get(
        `${API}/${productId}/variants`
    );

};

export const getVariantById = (
    variantId: number
): Promise<ProductVariant> => {

    return request.get(
        `${API}/variants/${variantId}`
    );

};

export const createVariant = (
    productId: number,
    data: ProductVariantRequest
): Promise<ProductVariant> => {

    return request.post(
        `${API}/${productId}/variants`,
        data
    );

};

export const updateVariant = (
    variantId: number,
    data: ProductVariantRequest
): Promise<ProductVariant> => {

    return request.put(
        `${API}/variants/${variantId}`,
        data
    );

};

export const deleteVariant = (
    variantId: number
): Promise<void> => {

    return request.delete(
        `${API}/variants/${variantId}`
    );

};