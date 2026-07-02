import { defineStore } from "pinia";

import variantService from "../services/variantService";

import type {

    ProductVariant,

    ProductVariantRequest

} from "../types/ProductVariant";

export const useVariantStore = defineStore(

    "variant",

    {

        state: () => ({

            variants: [] as ProductVariant[],

            loading: false

        }),

        actions: {

            /* ================= */

            async fetchByProduct(

                productId: number

            ) {

                this.loading = true;

                try {

                    this.variants =

                        await variantService.getByProduct(

                            productId

                        );

                }

                finally {

                    this.loading = false;

                }

            },

            /* ================= */

            async create(

                productId: number,

                request: ProductVariantRequest

            ) {

                const variant =

                    await variantService.create(

                        productId,

                        request

                    );

                this.variants.push(

                    variant

                );

                return variant;

            },

            /* ================= */

            async update(

                variantId: number,

                request: ProductVariantRequest

            ) {

                const variant =

                    await variantService.update(

                        variantId,

                        request

                    );

                const index =

                    this.variants.findIndex(

                        x => x.id === variantId

                    );

                if (index >= 0) {

                    this.variants[index] = variant;

                }

                return variant;

            },

            /* ================= */

            async delete(

                variantId: number

            ) {

                await variantService.delete(

                    variantId

                );

                this.variants =

                    this.variants.filter(

                        x => x.id !== variantId

                    );

            }

        }

    }

);