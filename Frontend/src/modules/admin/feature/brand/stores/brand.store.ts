import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { Brand } from '../types/brand.type';
import { brandApi } from '../services/brand.api';

export const useBrandStore = defineStore('brand-store', () => {
  const brands = ref<Brand[]>([]);
  const loading = ref(false);

  const fetchBrands = async () => {
    loading.value = true;
    try {
      brands.value = await brandApi.getAll();
    } catch (error) {
      console.error('Lỗi tải dữ liệu', error);
    } finally {
      loading.value = false;
    }
  };

  return { brands, loading, fetchBrands };
});