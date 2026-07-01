<template>
  <div class="page-wrapper">
    <ShopHeader />

    <div class="product-layout">
      <SidebarFilter v-if="!isShowingDetail" />

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
.page-wrapper {
  font-family: 'Inter', sans-serif;
  background-color: #ffffff;
  min-height: 100vh;
}

/* ĐÂY LÀ CSS CHIA 2 CỘT QUAN TRỌNG CỦA M (TUYỆT ĐỐI KHÔNG XÓA) */
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
  min-width: 0; /* Tránh vỡ layout khi co màn hình */
}

/* Đảm bảo sidebar có kích thước chuẩn */
:deep(.sidebar-filter) {
  width: 280px;
  flex-shrink: 0;
}

/* Responsive cho màn hình nhỏ */
@media (max-width: 991px) {
  .product-layout {
    flex-direction: column;
  }
  :deep(.sidebar-filter) {
    width: 100%;
  }
}
</style>