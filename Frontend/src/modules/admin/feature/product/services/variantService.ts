import * as api from "../api/variantApi";

import type {
    ProductVariant,
    ProductVariantRequest
} from "../types/ProductVariant";

class VariantService {

    async getByProduct(
        productId: number
    ): Promise<ProductVariant[]> {

        return await api.getVariants(
            productId
        );

    }

    async getById(
        id: number
    ): Promise<ProductVariant> {

        return await api.getVariantById(id);

    }

    async create(
        productId: number,
        request: ProductVariantRequest
    ): Promise<ProductVariant> {

        return await api.createVariant(
            productId,
            request
        );

    }

    async update(
        variantId: number,
        request: ProductVariantRequest
    ): Promise<ProductVariant> {

        return await api.updateVariant(
            variantId,
            request
        );

    }

    async delete(
        variantId: number
    ) {

        return await api.deleteVariant(
            variantId
        );

    }

}

export default new VariantService();