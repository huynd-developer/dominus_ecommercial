import * as api from "../api/productApi";

import type {
    Product,
    ProductPage
} from "../types/Product";

class ProductService {

    async getAll(
        keyword = "",
        page = 0,
        size = 10
    ): Promise<ProductPage> {

        return await api.getProducts({
            keyword,
            page,
            size
        });

    }

    async getById(
        id: number
    ): Promise<Product> {

        return await api.getProductById(id);

    }

    async create(
        formData: FormData
    ) {

        return await api.createProduct(formData);

    }

    async update(
        id: number,
        formData: FormData
    ) {

        return await api.updateProduct(
            id,
            formData
        );

    }

    async delete(
        id: number
    ) {

        return await api.deleteProduct(id);

    }

}

export default new ProductService();