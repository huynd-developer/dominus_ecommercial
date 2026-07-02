import { defineStore } from "pinia";
import productService from "../services/productService";

import type {
    Product,
    ProductPage
} from "../types/Product";

export const useProductStore = defineStore("product", {

    state: () => ({

        products: [] as Product[],

        page: {} as ProductPage,

        loading: false,

        keyword: "",

        currentPage: 0,

        pageSize: 10

    }),

    actions: {

        async fetchProducts() {

            this.loading = true;

            try {

                const response = await productService.getAll(
                    this.keyword,
                    this.currentPage,
                    this.pageSize
                );

                console.log("Response Product:", response);

                // request.ts đã return response.data
                this.page = response as ProductPage;

                this.products = response.content ?? [];

            } catch (error) {

                console.error("Lỗi lấy danh sách sản phẩm:", error);

                this.page = {} as ProductPage;
                this.products = [];

            } finally {

                this.loading = false;

            }

        },

        async getById(id:number){

    const response = await productService.getById(id);

    return response;

},

        async create(formData: FormData) {

            const response = await productService.create(formData);

            await this.fetchProducts();

            return response;

        },

        async update(id: number, formData: FormData) {

            const response = await productService.update(
                id,
                formData
            );

            await this.fetchProducts();

            return response;

        },

        async delete(id: number) {

            await productService.delete(id);

            await this.fetchProducts();

        }

    }

});