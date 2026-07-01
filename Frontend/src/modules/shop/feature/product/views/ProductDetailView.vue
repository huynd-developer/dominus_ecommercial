<template>
  <div class="page-wrapper">
    <ShopHeader />

    <div class="product-layout">
      <SidebarFilter />

      <main class="product-main">
        <ProductGrid v-if="!isShowingDetail" :product-list="productList" @open-detail="handleOpenDetail" />
        <ProductDetail v-else :product="activeProduct" @back="handleBackToList" @buy-now="handleBuyNow" />
      </main>
    </div>

    <ShopFooter />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';

import ShopHeader from '@/modules/shop/layout/ShopHeader.vue';
import SidebarFilter from '@/modules/shop/feature/product/components/SidebarFilter.vue';
import ProductGrid from '@/modules/shop/feature/product/components/ProductGrid.vue';
import ProductDetail from '@/modules/shop/feature/product/components/ProductDetail.vue';
import ShopFooter from '@/modules/shop/layout/ShopFooter.vue';

import { type Product, mockProductList } from '../services/mockData';

const router = useRouter();

// Trạng thái hiển thị giao diện
const isShowingDetail = ref(false);
const activeProduct = ref<any>(null);

// Mock data danh sách sản phẩm
const productList = ref<Product[]>(mockProductList);

// Các hàm xử lý đóng mở trang chi tiết
const handleOpenDetail = (item: any) => {
  activeProduct.value = item;
  isShowingDetail.value = true;
};

const handleBackToList = () => {
  isShowingDetail.value = false;
  activeProduct.value = null;
};

const handleBuyNow = () => {
  router.push('/checkout');
};
</script>

<style scoped>
/* Giữ nguyên phần CSS Layout trang của m ở đây nếu có */
.product-layout { display: flex; gap: 30px; max-width: 1400px; margin: 40px auto; padding: 0 20px; }
.product-main { flex: 1; }
</style>