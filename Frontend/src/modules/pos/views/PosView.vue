<template>
  <div class="pos-layout-wrapper w-100 h-100 d-flex flex-column p-3 overflow-hidden">
    <posHeader />

    <div class="row flex-grow-1 flex-nowrap overflow-hidden m-0 pt-2 gy-0 gx-3 alignment-stretch">
      
      <div class="col-left h-100 d-flex flex-column ps-0 overflow-hidden">
        <productGrid />
      </div>
      
      <div class="col-5 col-xl-4 col-xxl-3-5 h-100 pe-0 overflow-hidden">
        <cartSideBar />
      </div>
      
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import posHeader from '../components/PosHeader.vue'
import productGrid from '../components/ProductGrid.vue'
import cartSideBar from '../components/CartSideBar.vue'

onMounted(() => {
  // Kích hoạt class môi trường tối toàn cục
  document.body.classList.add('pos-global-dark-env')

  // Tránh can thiệp ngược vào DOM cha (có thể làm mất/ẩn TopHeader của Admin)
  // POS chỉ cần style cho chính wrapper của nó.
  const wrapper = document.querySelector('.pos-layout-wrapper')
  if (wrapper && wrapper instanceof HTMLElement) {
    wrapper.style.height = '100%'
    wrapper.style.overflow = 'hidden'
  }
})

onUnmounted(() => {
  document.body.classList.remove('pos-global-dark-env')
  const wrapper = document.querySelector('.pos-layout-wrapper')
  if (wrapper && wrapper instanceof HTMLElement) {
    wrapper.style.overflow = ''
    wrapper.style.height = ''
  }
})
</script>

<style scoped>
.pos-layout-wrapper {
  background-color: #090d1a !important;
  color: #f8fafc;
  height: 100% !important;      /* Điền đầy 100% chiều cao của khung chứa Admin bên phải */
  width: 100% !important;       /* Điền đầy 100% chiều rộng của khung chứa Admin bên phải */
  display: flex !important;
  flex-direction: column !important; 
  box-sizing: border-box;
}

.alignment-stretch {
  align-items: stretch !important;
}

/* KHỐI TRÁI SẢN PHẨM: Tự động lấp đầy phần không gian còn lại một cách linh hoạt */
.col-left {
  flex: 1 !important;
  min-width: 0 !important;
}

/* KHỐI PHẢI GIỎ HÀNG: Định hình chiều rộng cố định theo từng độ phân giải */
@media (min-width: 1200px) and (max-width: 1499px) {
  .col-5 { width: 35% !important; flex: 0 0 35% !important; }
}

@media (min-width: 1500px) {
  .col-xxl-3-5 { width: 26% !important; flex: 0 0 26% !important; }
}
</style>

<!-- Tránh style toàn cục có thể làm vỡ layout Admin (TopHeader) -->
<style>
/* Chỉ style nền cho vùng POS wrapper nếu cần */
.pos-global-dark-env {
  background-color: #090d1a !important;
}
</style>
