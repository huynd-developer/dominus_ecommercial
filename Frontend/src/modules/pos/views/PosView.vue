<template>
  <div class="pos-view-wrapper d-flex flex-column vh-100 p-3 overflow-hidden">
    <PosHeader />

    <div class="row flex-grow-1 g-3 mt-1 overflow-hidden min-h-0">
      <div class="col-12 col-xl-8 h-100 overflow-hidden">
        <ProductGrid />
      </div>

      <div class="col-12 col-xl-4 h-100 overflow-hidden">
        <CartSideBar />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from "vue";
import { usePosStore } from "../stores/posStore";
import PosHeader from "../components/PosHeader.vue";
import ProductGrid from "../components/productGrid.vue";
import CartSideBar from "../components/cartSideBar.vue";

const posStore = usePosStore();

onMounted(async () => {
  sessionStorage.removeItem("pos_pending_checkout_draft");

  await posStore.fetchProducts();
  await posStore.fetchHeldOrders();
});
</script>

<style scoped>
.pos-view-wrapper {
  background-color: #070c18;
}

.min-h-0 {
  min-height: 0;
}

::-webkit-scrollbar {
  width: 6px;
  height: 6px;
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