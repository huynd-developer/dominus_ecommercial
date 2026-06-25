<template>
  <div class="dominus-pos-wrapper container-fluid p-3 vh-100 d-flex flex-column" style="background-color: #0f1423; color: #f8fafc;">
    <!-- Nạp Component Header -->
    <PosHeader />

    <!-- Nạp Layout chính chứa Grid và Cart -->
    <div class="row flex-grow-1 overflow-hidden m-0">
      <div class="col-lg-8 col-xl-9">
        <ProductGrid />
      </div>
      <div class="col-lg-4 col-xl-3 ps-0 h-100">
        <CartSidebar />
      </div>
    </div>
  </div>
</template>

<script setup>
import PosHeader from '../components/PosHeader.vue'
import ProductGrid from '../components/ProductGrid.vue'
import CartSidebar from '../components/CartSidebar.vue'
</script>

<style>
/* Đặt các CSS global riêng cho module POS ở đây (Element Plus overrides) */
.dominus-pos-wrapper .el-input__wrapper {
  background-color: #161c2d;
  box-shadow: none !important;
  border: 1px solid #2e3b52;
}
.dominus-pos-wrapper .el-input__inner { color: white; }
</style>