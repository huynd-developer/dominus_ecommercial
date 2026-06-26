<template>
  <div class="d-flex flex-column h-100 pe-1">
    
    <!-- Ô NHẬP TEXT & FILTER ĐƯỢC CHUỐT LẠI SIÊU ĐẸP -->
    <div class="filter-search-section mb-3">
      <el-input
        v-model="posStore.searchQuery"
        placeholder="Tìm kiếm dòng nước hoa luxury hoặc quét mã vạch..."
        class="pos-premium-search mb-3"
        clearable
        :prefix-icon="Search"
      />
      
      <!-- Thanh cuộn ngang cho danh mục sản phẩm mượt mà -->
      <div class="category-slider d-flex gap-2 overflow-auto pb-2">
        <button
          v-for="cat in posStore.categories"
          :key="cat"
          :class="['luxury-pill', { 'active': posStore.activeCategory === cat }]"
          @click="posStore.activeCategory = cat"
        >
          {{ cat }}
        </button>
      </div>
    </div>

    <!-- GRID SẢN PHẨM AUTO-FIT KHÔNG GIAN LANDSCAPE -->
    <el-scrollbar class="flex-grow-1 pe-2">
      <div class="row g-3 row-cols-1 row-cols-sm-2 row-cols-md-2 row-cols-xl-3 row-cols-xxl-4">
        <div class="col" v-for="product in posStore.filteredProducts" :key="product.id">
          <div class="product-luxury-card rounded-3 overflow-hidden position-relative h-100 d-flex flex-column" @click="posStore.addToCart(product)">
            <div class="img-wrapper position-relative">
              <img :src="product.image" class="w-100 h-100 object-fit-cover" alt="Product">
              <div class="premium-price-tag position-absolute fw-bold text-dark bg-warning px-2 py-1 rounded font-xs">
                {{ formatPrice(product.price) }} đ
              </div>
            </div>
            <div class="p-3 card-body-bg flex-grow-1 d-flex flex-column justify-content-between">
              <div>
                <h6 class="mb-1 text-light fw-bold text-truncate font-sm">{{ product.name }}</h6>
                <p class="text-muted-custom mb-0 font-xs text-truncate">{{ product.subName }}</p>
              </div>
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
const formatPrice = (val) => new Intl.NumberFormat('vi-VN').format(val)
</script>

<style scoped>
.text-muted-custom { color: #64748b; }
.font-sm { font-size: 0.88rem; }
.font-xs { font-size: 0.75rem; }

/* Ô NHẬP TEXT PREMIUM */
:deep(.pos-premium-search .el-input__wrapper) {
  background-color: #111726 !important;
  box-shadow: none !important;
  border: 1px solid #1e293b !important;
  border-radius: 10px;
  padding: 10px 14px;
  transition: all 0.25s ease-in-out;
}
:deep(.pos-premium-search .el-input__wrapper.is-focus) {
  border-color: #facc15 !important;
  box-shadow: 0 0 8px rgba(250, 204, 21, 0.15) !important;
}
:deep(.pos-premium-search .el-input__inner) {
  color: #f8fafc !important;
  font-size: 0.85rem;
}

/* BỘ LỌC DANH MỤC SANG XỊN */
.category-slider::-webkit-scrollbar { height: 4px; }
.category-slider::-webkit-scrollbar-thumb { background: #1e293b; border-radius: 10px; }

.luxury-pill {
  background-color: #111726;
  border: 1px solid #1e293b;
  color: #94a3b8;
  padding: 8px 18px;
  border-radius: 30px;
  font-size: 0.82rem;
  font-weight: 500;
  white-space: text-nowrap;
  cursor: pointer;
  transition: all 0.2s ease;
}
.luxury-pill:hover {
  border-color: #facc15;
  color: #fff;
}
.luxury-pill.active {
  background-color: #facc15 !important;
  color: #090d1a !important;
  border-color: #facc15 !important;
  font-weight: 600;
}

/* THẺ SẢN PHẨM CAO CẤP */
.product-luxury-card {
  background-color: #111726;
  border: 1px solid #1e293b;
  cursor: pointer;
  transition: all 0.25s ease;
}
.product-luxury-card:hover {
  transform: translateY(-4px);
  border-color: #facc15;
  box-shadow: 0 8px 20px rgba(0,0,0,0.4);
}
.img-wrapper { width: 100%; height: 135px; overflow: hidden; background-color: #090d1a; }
.premium-price-tag { bottom: 8px; right: 8px; font-weight: 700 !important; }
.card-body-bg { background-color: #111726; }
</style>