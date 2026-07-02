<template>
  <div class="pos-view-wrapper d-flex flex-column vh-100 p-3 overflow-hidden">
    <PosHeader />

    <div class="row flex-grow-1 g-3 mt-1 overflow-hidden">
      <div class="col-12 col-lg-8 h-100 overflow-y-auto pb-3">
        <ProductGrid />
      </div>
      
      <div class="col-12 col-lg-4 h-100 overflow-y-auto pb-3">
        <CartSideBar />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { usePosStore } from '../stores/posStore'
import PosHeader from '../components/PosHeader.vue'
import ProductGrid from '../components/ProductGrid.vue'
import CartSideBar from '../components/CartSideBar.vue'

const posStore = usePosStore()

// Tự động tải danh sách sản phẩm thật từ BE lên màn hình ngay khi mở app
onMounted(() => {
  posStore.fetchProducts()
})
</script>

<style scoped>
.pos-view-wrapper {
  background-color: #070c18; /* Màu nền tối sang trọng chuẩn gaming/hiện đại của mày */
}

/* Tùy biến thanh cuộn cho mượt và đẹp hơn */
::-webkit-scrollbar {
  width: 6px;
}
::-webkit-scrollbar-track {
  background: #070c18;
}
::-webkit-scrollbar-thumb {
  background: #1a233a;
  border-radius: 4px;
}
::-webkit-scrollbar-thumb:hover {
  background: #2d3d63;
}
</style>