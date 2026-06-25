<template>
  <div class="cart-panel bg-panel rounded-3 d-flex flex-column h-100 p-3" style="background: #161c2d;">
    <!-- Header -->
    <div class="d-flex align-items-center gap-3 border-bottom border-secondary pb-3 mb-3">
      <div class="rounded bg-dark-subtle p-2">
        <i class="bi bi-cart3 fs-5 text-warning"></i>
      </div>
      <div>
        <h5 class="mb-0 text-light">Giỏ hàng</h5>
        <span class="text-secondary font-sm">{{ posStore.cart.length }} món</span>
      </div>
    </div>

    <!-- Items -->
    <div v-if="posStore.cart.length === 0" class="flex-grow-1 d-flex flex-column justify-content-center align-items-center text-secondary">
      <i class="bi bi-cart-x fs-1 mb-2 opacity-50"></i>
      <p class="mb-1">Chưa có sản phẩm nào</p>
    </div>
    <el-scrollbar v-else class="flex-grow-1 mb-3">
      <div v-for="item in posStore.cart" :key="item.id" class="d-flex justify-content-between align-items-center text-light mb-2">
        <div>
          <div class="fw-bold">{{ item.name }}</div>
          <div class="text-secondary font-sm">x{{ item.quantity }}</div>
        </div>
        <div class="text-warning">{{ formatPrice(item.price * item.quantity) }} đ</div>
      </div>
    </el-scrollbar>

    <!-- Checkout -->
    <div class="mt-auto pt-3 border-top border-secondary">
      <div class="d-flex justify-content-between align-items-center mb-4">
        <span class="text-light fw-bold">Khách phải trả</span>
        <h3 class="text-warning mb-0 fw-bold">{{ formatPrice(posStore.cartTotal) }} đ</h3>
      </div>
      <el-button type="warning" class="w-100 fw-bold text-dark py-3" size="large" :disabled="posStore.cart.length === 0">
        THANH TOÁN
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { usePosStore } from '../stores/posStore'

const posStore = usePosStore()
const formatPrice = (value) => new Intl.NumberFormat('vi-VN').format(value)
</script>