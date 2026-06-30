<template>
  <div class="pos-view-wrapper d-flex flex-column height: 100% p-3">
    <PosHeader />

    <div class="row flex-grow-1 g-3 mt-1 overflow-hidden">
      <div class="col-12 col-lg-8 h-100">
        <ProductGrid />
      </div>
      <div class="col-12 col-lg-4 h-100">
        <CartSideBar />
      </div>
    </div>
  </div>
</template>

<script setup>
import PosHeader from '../components/PosHeader.vue'
import ProductGrid from '../components/ProductGrid.vue'
import CartSideBar from '../components/CartSideBar.vue'
</script>

<style scoped>
.pos-view-wrapper {
  background-color: #070c18;
}
</style>