import request from '@/common/services/request'

import type {
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
    const response = await request.get(
      `/api/v1/products/admin?page=${page}&size=${size}`
    )

    return response.data ?? response
  },

  getProductById: async (
    id: number
  ): Promise<ProductResponse> => {
    const response = await request.get(
      `/api/v1/products/${id}`
    )

    return response.data ?? response
  },

  createProduct: async (
    payload: ProductRequestDTO
  ) => {
    const response = await request.post(
      '/api/v1/products/admin',
      payload
    )

    return response.data ?? response
  },

  updateProduct: async (
    id: number,
    payload: ProductRequestDTO
  ) => {
    const response = await request.put(
      `/api/v1/products/admin/${id}`,
      payload
    )

    return response.data ?? response
  },

  deleteProduct: async (
    id: number
  ) => {
    const response = await request.delete(
      `/api/v1/products/admin/${id}`
    )

    return response.data ?? response
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

    const response = await request.post(
      `/api/v1/products/admin/${productId}/images`,
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
    const response = await request.get(
      '/api/admin/brands?page=0&size=999'
    )

    return response.data ?? response
  },

  getCategories: async () => {
  const response = await request.get(
    '/api/admin/categories?page=0&size=999'
  )

  return response.data ?? response
},

  getConcentrations: async () => {
    const response = await request.get(
      '/api/admin/concentrations?page=0&size=999'
    )

    return response.data ?? response
  },

  getCapacities: async () => {
    const response = await request.get(
      '/api/admin/capacities?page=0&size=999'
    )

    return response.data ?? response
  },

  getBottleTypes: async () => {
    const response = await request.get(
      '/api/admin/bottle-types?page=0&size=999'
    )

    return response.data ?? response
  },

  getFragranceFamilies: async () => {
    const response = await request.get(
      '/api/admin/fragrance-families?page=0&size=999'
    )

    return response.data ?? response
  }
}