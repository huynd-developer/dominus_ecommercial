<template>
  <div class="page-wrapper">
    <ShopHeader />

    <div class="product-layout">
      <SidebarFilter v-if="!isShowingDetail" @filter-change="handleFilterChange" />

      <main class="product-main">
        <ProductGrid v-if="!isShowingDetail" :product-list="filteredProductList" @open-detail="handleOpenDetail" />
        <ProductDetail v-else :product="activeProduct" @back="handleBackToList" @buy-now="handleBuyNow" />
      </main>
    </div>

    <ShopFooter />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';

import ShopHeader from '@/modules/shop/layout/ShopHeader.vue';
import SidebarFilter from '@/modules/shop/feature/product/components/SidebarFilter.vue';
import ProductGrid from '@/modules/shop/feature/product/components/ProductGrid.vue';
import ProductDetail from '@/modules/shop/feature/product/components/ProductDetail.vue';
import ShopFooter from '@/modules/shop/layout/ShopFooter.vue';

import { type Product, mockProductList } from '../services/mockData';

const router = useRouter();

const isShowingDetail = ref(false);
const activeProduct = ref<any>(null);
const productList = ref<Product[]>(mockProductList);

// Nơi giữ các bộ lọc
const activeFilters = ref({
  genders: [] as string[],
  volumes: [] as string[],
  concentrations: [] as string[],
  scents: [] as string[],
  brands: [] as string[]
});

const handleFilterChange = (filters: any) => {
  activeFilters.value = filters;
};

// Hàm tính toán danh sách
const filteredProductList = computed(() => {
  return productList.value.filter((product: any) => {
    
    // 1. Giới tính
    if (activeFilters.value.genders.length > 0) {
      if (!product.gender || !activeFilters.value.genders.includes(product.gender)) return false;
    }

    // 2. Thương hiệu
    if (activeFilters.value.brands.length > 0) {
      if (!product.brand || !activeFilters.value.brands.includes(product.brand)) return false;
    }

    // 3. Nồng độ
    if (activeFilters.value.concentrations.length > 0) {
      if (!product.concentration || !activeFilters.value.concentrations.includes(product.concentration)) return false;
    }

    // 4. Mùi hương (Kiểm tra xem product.scents có tồn tại không rồi mới lọc)
    if (activeFilters.value.scents.length > 0) {
      if (!product.scents || !Array.isArray(product.scents)) return false; // Tránh lỗi undefined sập web
      const hasMatchingScent = activeFilters.value.scents.some((s: string) => product.scents.includes(s));
      if (!hasMatchingScent) return false;
    }

    // 5. Dung tích (Kiểm tra xem product.variants có tồn tại không)
    if (activeFilters.value.volumes.length > 0) {
      if (!product.variants || !Array.isArray(product.variants)) return false; // Tránh lỗi undefined sập web
      const hasMatchingVolume = product.variants.some((variant: any) => 
        activeFilters.value.volumes.includes(variant.capacity)
      );
      if (!hasMatchingVolume) return false;
    }

    return true; 
  });
});

const handleOpenDetail = (item: any) => {
  activeProduct.value = item;
  isShowingDetail.value = true;
};
const handleBackToList = () => {
  isShowingDetail.value = false;
  activeProduct.value = null;
};
const handleBuyNow = () => router.push('/checkout');
</script>

<style scoped>
.page-wrapper {
  font-family: 'Inter', sans-serif;
  background-color: #ffffff;
  min-height: 100vh;
}
.product-layout {
  display: flex;
  flex-direction: row;
  gap: 40px;
  max-width: 1320px;
  margin: 40px auto;
  padding: 0 20px;
}
.product-main {
  flex: 1;
  min-width: 0;
}
:deep(.sidebar-filter) {
  width: 280px;
  flex-shrink: 0;
}
@media (max-width: 991px) {
  .product-layout {
    flex-direction: column;
  }
  :deep(.sidebar-filter) {
    width: 100%;
  }
}
</style>