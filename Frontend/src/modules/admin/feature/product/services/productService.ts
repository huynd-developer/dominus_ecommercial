// src/modules/admin/feature/product/services/productService.ts

import request from '@/common/services/request'

import type {
  Product,
  ProductRequestDTO,
  ProductResponse
} from '../types/product.type'

export const productService = {
  // ==========================
  // PRODUCT
  // ==========================

  getProducts: async (
    page = 0,
    size = 10
  ) => {
    const response = await request.get<any, any>(
      `/products/admin?page=${page}&size=${size}`
    )

    return response.data ?? response
  },

  getProductById: async (
    id: number
  ): Promise<ProductResponse> => {
    const response = await request.get<any, any>(
      `/products/${id}`
    )

    return response.data ?? response
  },

  createProduct: async (
    payload: ProductRequestDTO
  ) => {
    const response = await request.post<any, any>(
      '/products/admin',
      payload
    )

    return response.data ?? response
  },

  updateProduct: async (
    id: number,
    payload: ProductRequestDTO
  ) => {
    const response = await request.put<any, any>(
      `/products/admin/${id}`,
      payload
    )

    return response.data ?? response
  },

  deleteProduct: async (
    id: number
  ) => {
    return await request.delete<any, any>(
      `/products/admin/${id}`
    )
  },

  // ==========================
  // IMAGE
  // ==========================

  uploadImage: async (
    productId: number,
    file: File
  ) => {
    const formData = new FormData()

    formData.append('file', file)

    const response = await request.post<any, any>(
      `/products/admin/${productId}/images`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      }
    )

    return response.data ?? response
  },

  // ==========================
  // MASTER DATA
  // ==========================

  getBrands: async () => {
    const response = await request.get<any, any>(
      '/brands'
    )

    return response.data ?? response
  },

  getCategories: async () => {
    const response = await request.get<any, any>(
      '/categories'
    )

    return response.data ?? response
  },

  getConcentrations: async () => {
    const response = await request.get<any, any>(
      '/concentrations'
    )

    return response.data ?? response
  },

  getCapacities: async () => {
    const response = await request.get<any, any>(
      '/capacities'
    )

    return response.data ?? response
  },

  getBottleTypes: async () => {
    const response = await request.get<any, any>(
      '/bottle-types'
    )

    return response.data ?? response
  },

  getFragranceFamilies: async () => {
    const response = await request.get<any, any>(
      '/fragrance-families'
    )

    return response.data ?? response
  }
}