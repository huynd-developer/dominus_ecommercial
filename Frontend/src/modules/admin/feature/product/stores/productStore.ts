import { defineStore } from 'pinia'
import { ref } from 'vue'
import { productService } from '../services/productService'
import { useAppStore } from '@/common/store/app.store'
import type { Product } from '../types/product.type'

export const useProductStore = defineStore(
  'productStore',
  () => {
    const appStore = useAppStore()

    const products = ref<Product[]>([])

    const brandList = ref<any[]>([])
    const categoryList = ref<any[]>([])
    const concentrationList = ref<any[]>([])
    const fragranceFamilyList = ref<any[]>([])
    const capacityList = ref<any[]>([])
    const bottleTypeList = ref<any[]>([])

    const isLoading = ref(false)

    // =========================
    // PRODUCT
    // =========================

    const fetchProducts = async () => {
      isLoading.value = true

      try {
        const response =
          await productService.getProducts()

        if (response.content) {
          products.value = response.content
        } else {
          products.value = response
        }
      } catch (err: any) {
        appStore.notifyError(
          err?.response?.data?.message ||
            'Không thể tải danh sách sản phẩm'
        )
      } finally {
        isLoading.value = false
      }
    }

    // =========================
    // DROPDOWN DATA
    // =========================

    const fetchDropdowns = async () => {
      try {
        const [
          brands,
          categories,
          concentrations,
          fragranceFamilies,
          capacities,
          bottleTypes
        ] = await Promise.all([
          productService.getBrands(),
          productService.getCategories(),
          productService.getConcentrations(),
          productService.getFragranceFamilies(),
          productService.getCapacities(),
          productService.getBottleTypes()
        ])

        brandList.value = brands
        categoryList.value = categories
        concentrationList.value = concentrations
        fragranceFamilyList.value =
          fragranceFamilies
        capacityList.value = capacities
        bottleTypeList.value = bottleTypes
      } catch (err) {
        console.error(
          'Lỗi tải dữ liệu dropdown',
          err
        )
      }
    }

    return {
      products,

      brandList,
      categoryList,
      concentrationList,
      fragranceFamilyList,
      capacityList,
      bottleTypeList,

      isLoading,

      fetchProducts,
      fetchDropdowns
    }
  }
)