<template>
  <div class="d-flex flex-column h-100 pe-3">
    <!-- Search & Filter -->
    <div class="mb-3">
      <el-input
        v-model="posStore.searchQuery"
        placeholder="Tìm kiếm mặt hàng hoặc quét mã vạch..."
        class="pos-search-input mb-3"
        clearable
        :prefix-icon="Search"
      />
      <div class="category-filters d-flex gap-2 overflow-auto pb-1">
        <el-button
          v-for="cat in posStore.categories"
          :key="cat"
          :class="['cat-btn', { 'active': posStore.activeCategory === cat }]"
          @click="posStore.activeCategory = cat"
          round
        >
          {{ cat }}
        </el-button>
      </div>
    </div>

    <!-- Grid -->
    <el-scrollbar class="flex-grow-1 pe-2 custom-scrollbar">
      <div class="row g-3">
        <div class="col-md-4 col-lg-3" v-for="product in posStore.filteredProducts" :key="product.id">
          <div class="product-card rounded-3 overflow-hidden position-relative cursor-pointer transition-all" @click="posStore.addToCart(product)">
            <div class="img-wrapper bg-gradient-dark">
              <img :src="product.image" class="img-fluid w-100 object-fit-cover" alt="Product" style="height: 160px;">
            </div>
            <div class="price-badge position-absolute rounded-pill px-3 py-1 fw-bold text-dark bg-warning" style="bottom: 60px; right: 10px; z-index: 10;">
              {{ formatPrice(product.price) }} đ
            </div>
            <div class="p-3 bg-panel" style="background: #161c2d;">
              <h6 class="mb-1 text-light text-truncate font-md">{{ product.name }}</h6>
              <p class="text-secondary mb-0 font-sm">{{ product.subName }}</p>
            </div>
          </div>
        </div>
      </div>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { Search } from '@element-plus/icons-vue'
import { usePosStore } from '../stores/posStore'

const posStore = usePosStore()
const formatPrice = (value) => new Intl.NumberFormat('vi-VN').format(value)
</script>

<style scoped>
.cat-btn { background-color: #161c2d; color: #94a3b8; border-color: transparent; }
.cat-btn.active { background-color: #facc15; color: #000; font-weight: bold; }
.product-card { border: 1px solid transparent; background-color: #161c2d; }
.product-card:hover { transform: translateY(-4px); border-color: rgba(250, 204, 21, 0.4); box-shadow: 0 10px 20px rgba(0,0,0,0.3); }
</style>