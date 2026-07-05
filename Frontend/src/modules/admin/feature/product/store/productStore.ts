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

        currentProduct: null as Product | null,

        loading: false,

        keyword: "",

        currentPage: 0,

        pageSize: 10

    }),

    getters: {

        totalPages: (state) => state.page.totalPages ?? 0,

        totalElements: (state) => state.page.totalElements ?? 0

    },

    actions: {

        /* ==========================
           Danh sách
        ========================== */

        async fetchProducts() {

            this.loading = true;

            try {

                const page = await productService.getAll(

                    this.keyword,

                    this.currentPage,

                    this.pageSize

                );

                this.page = page;

                this.products = page.content ?? [];

            }

            finally {

                this.loading = false;

            }

        },

        /* ==========================
           Detail
        ========================== */

        async getById(id: number) {

            this.loading = true;

            try {

                const product = await productService.getById(id);

                this.currentProduct = product;

                return product;

            }

            finally {

                this.loading = false;

            }

        },

        /* ==========================
           Search
        ========================== */

        async search(keyword: string) {

            this.keyword = keyword;

            this.currentPage = 0;

            await this.fetchProducts();

        },

        /* ==========================
           Paging
        ========================== */

        async changePage(page: number) {

            this.currentPage = page;

            await this.fetchProducts();

        },

        async changePageSize(size: number) {

            this.pageSize = size;

            this.currentPage = 0;

            await this.fetchProducts();

        },

        /* ==========================
           Create
        ========================== */

        async create(formData: FormData) {

            const response = await productService.create(formData);

            await this.fetchProducts();

            return response;

        },

        /* ==========================
           Update
        ========================== */

        async update(

            id: number,

            formData: FormData

        ) {

            const response = await productService.update(

                id,

                formData

            );

            await this.fetchProducts();

            return response;

        },

        /* ==========================
           Delete
        ========================== */

        async delete(id: number) {

            await productService.delete(id);

            await this.fetchProducts();

        }

    }

});