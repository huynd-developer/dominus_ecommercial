<template>
  <div class="page-wrapper">
    <ShopHeader />

    <div class="product-layout">
      <SidebarFilter v-if="!isShowingDetail" @filter-change="handleFilterChange" />

      <main class="product-main">
        <div v-if="isLoading" style="padding: 50px; text-align: center; color: #666;">
          Đang tải danh sách sản phẩm...
        </div>
        <ProductGrid v-else-if="!isShowingDetail" :product-list="filteredProductList" @open-detail="handleOpenDetail" />
        <ProductDetail v-else :product="activeProduct" @back="handleBackToList" @buy-now="handleBuyNow" />
      </main>
    </div>

    <ShopFooter />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

import ShopHeader from '@/modules/shop/layout/ShopHeader.vue';
import SidebarFilter from '@/modules/shop/feature/product/components/SidebarFilter.vue';
import ProductGrid from '@/modules/shop/feature/product/components/ProductGrid.vue';
import ProductDetail from '@/modules/shop/feature/product/components/ProductDetail.vue';
import ShopFooter from '@/modules/shop/layout/ShopFooter.vue';

const router = useRouter();

const isShowingDetail = ref(false);
const isLoading = ref(true);
const activeProduct = ref<any>(null);
const productList = ref<any[]>([]); // Khởi tạo mảng rỗng chờ API

// GỌI API THẬT TỪ BACKEND
// Trong file ProductDetailView.vue
const fetchProducts = async () => {
  try {
    isLoading.value = true;
    
    // GỌI CHAY LUÔN: Không truyền params gì để Backend tự dùng defaultValue
    const res = await axios.get('http://localhost:8080/api/products');

    const responseData = res.data.data;
    productList.value = responseData.content ? responseData.content : responseData;
  } catch (error) {
    console.error("Lỗi tải danh sách sản phẩm từ DB:", error);
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  fetchProducts();
});

// Nơi giữ các bộ lọc
const activeFilters = ref({
  genders: [] as string[],
  capacities: [] as string[],        
  concentrations: [] as string[],
  fragranceFamilies: [] as string[], 
  bottleTypes: [] as string[],       
  brands: [] as string[]
});

const handleFilterChange = (filters: any) => {
  activeFilters.value = { ...activeFilters.value, ...filters };
};

// Hàm lọc ở Frontend (Chờ BE bổ sung param lọc thì mình thay sau)
const filteredProductList = computed(() => {
  return productList.value.filter((product: any) => {
    if (activeFilters.value.genders?.length > 0 && (!product.gender || !activeFilters.value.genders.includes(product.gender))) return false;
    if (activeFilters.value.brands?.length > 0 && (!product.brand || !activeFilters.value.brands.includes(product.brand))) return false;
    if (activeFilters.value.concentrations?.length > 0 && (!product.concentration || !activeFilters.value.concentrations.includes(product.concentration))) return false;
    if (activeFilters.value.fragranceFamilies?.length > 0) {
      if (!product.scents || !Array.isArray(product.scents)) return false; 
      const hasMatchingFragrance = activeFilters.value.fragranceFamilies.some((f: string) => product.scents.includes(f));
      if (!hasMatchingFragrance) return false;
    }
    if (activeFilters.value.capacities?.length > 0) {
      if (!product.variants || !Array.isArray(product.variants)) return false; 
      const hasMatchingCapacity = product.variants.some((variant: any) => activeFilters.value.capacities.includes(variant.capacity));
      if (!hasMatchingCapacity) return false;
    }
    if (activeFilters.value.bottleTypes?.length > 0 && (!product.bottleType || !activeFilters.value.bottleTypes.includes(product.bottleType))) return false;
    
    return true; 
  });
});

const handleOpenDetail = (item: any) => {
  activeProduct.value = item;
  isShowingDetail.value = true;
  window.scrollTo({ top: 0, behavior: 'smooth' });
};
const handleBackToList = () => {
  isShowingDetail.value = false;
  activeProduct.value = null;
};
const handleBuyNow = () => router.push('/checkout');
</script>

<style scoped>
/* Giữ nguyên CSS cũ của m */
.page-wrapper { font-family: 'Inter', sans-serif; background-color: #ffffff; min-height: 100vh; }
.product-layout { display: flex; flex-direction: row; gap: 40px; max-width: 1320px; margin: 40px auto; padding: 0 20px; }
.product-main { flex: 1; min-width: 0; }
:deep(.sidebar-filter) { width: 280px; flex-shrink: 0; }
@media (max-width: 991px) { .product-layout { flex-direction: column; } :deep(.sidebar-filter) { width: 100%; } }
</style>