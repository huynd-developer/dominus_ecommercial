<template>
  <div class="d-flex flex-column h-100 pe-1 select-none">
    
    <div class="filter-search-section mb-3 shrink-0">
      <el-input
        v-model="posStore.searchQuery"
        placeholder="Tìm kiếm dòng nước hoa luxury hoặc quét mã vạch..."
        class="pos-premium-search mb-3"
        clearable
        :prefix-icon="Search"
      />
      
      <div class="category-slider d-flex gap-2 overflow-auto pb-1 no-scrollbar">
        <button
          v-for="cat in posStore.categories"
          :key="cat"
          type="button"
          :class="['luxury-pill', { 'active': posStore.activeCategory === cat }]"
          @click="posStore.activeCategory = cat"
        >
          {{ cat }}
        </button>
      </div>
    </div>

    <el-scrollbar class="flex-grow-1 product-scroll-container pe-2">
      <div v-if="posStore.isLoadingProducts" class="text-center py-5 text-muted-custom font-sm">
        Đang tải sản phẩm...
      </div>

      <div v-else-if="posStore.filteredProducts.length === 0" class="text-center py-5 text-muted-custom font-sm">
        Không tìm thấy sản phẩm phù hợp
      </div>

      <div v-else class="row g-3 row-cols-2 row-cols-md-3 row-cols-lg-3 row-cols-xl-3 row-cols-xxl-3 pb-4">
        <div class="col" v-for="product in posStore.filteredProducts" :key="product.id">
          <div 
            class="product-luxury-card rounded-3 overflow-hidden position-relative h-100 d-flex flex-column border transition-all" 
            @click="posStore.addToCart(product)"
          >
            <div class="img-wrapper position-relative w-100">
              <img :src="product.image" class="w-100 h-100 object-fit-cover item-img" alt="Product Perfume">
              
              <div class="premium-price-tag position-absolute fw-black text-dark px-2.5 py-1 rounded-2 shadow-sm font-sm">
                {{ formatPrice(product.price) }} ₫
              </div>
            </div>

            <div class="p-3 card-body-bg d-flex flex-column justify-content-between flex-grow-1">
              <div>
                <h6 class="mb-1 text-light-custom fw-bold text-truncate font-sm" :title="product.name">
                  {{ product.name }}
                </h6>
                <p class="text-muted-custom mb-0 font-xs text-truncate">
                  {{ product.subName || 'DOMINUS Luxury' }}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { usePosStore } from '../stores/posStore'

const posStore = usePosStore()
const formatPrice = (val) => new Intl.NumberFormat('vi-VN').format(val || 0)

// Gọi đây để sau này khi BE có API list sản phẩm thật,
// chỉ cần sửa logic trong posStore.fetchProducts(), component này không đổi gì
onMounted(() => {
  posStore.fetchProducts()
})
</script>

<style scoped>
/* ĐỒNG BỘ ĐỊNH DẠNG MÀU DARK LUXURY */
.text-muted-custom { color: #64748b; }
.text-light-custom { color: #f1f5f9; }
.font-sm { font-size: 0.875rem; }
.font-xs { font-size: 0.75rem; }
.fw-black { font-weight: 900; }

/* ĐÈ STYLE Ô TÌM KIẾM ELEMENT PLUS */
:deep(.pos-premium-search .el-input__wrapper) {
  background-color: #0b1120 !important;
  box-shadow: none !important;
  border: 1px solid #1d2740 !important;
  border-radius: 12px;
  padding: 10px 14px;
  transition: all 0.2s ease-in-out;
}
:deep(.pos-premium-search .el-input__wrapper.is-focus) {
  border-color: #f3c63f !important;
  box-shadow: 0 0 10px rgba(243, 198, 63, 0.12) !important;
}
:deep(.pos-premium-search .el-input__inner) {
  color: #f8fafc !important;
  font-size: 0.85rem;
}
:deep(.pos-premium-search .el-input__prefix-icon) {
  color: #64748b;
  font-size: 1.1rem;
}

/* THANH TRƯỢT CATEGORY */
.no-scrollbar::-webkit-scrollbar { display: none; }
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }

.luxury-pill {
  background-color: #131c31;
  border: 1px solid #222f4f;
  color: #9fb0d3;
  padding: 8px 20px;
  border-radius: 30px;
  font-size: 0.8rem;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition: all 0.15s ease;
}
.luxury-pill:hover {
  border-color: #384f80;
  color: #f1f5f9;
}
.luxury-pill.active {
  background-color: #f3c63f !important;
  color: #000000 !important;
  border-color: #f3c63f !important;
  font-weight: 800;
  box-shadow: 0 4px 10px rgba(243, 198, 63, 0.15);
}

/* THẺ SẢN PHẨM SIÊU TO CAO CẤP */
.product-luxury-card {
  background-color: #131c31;
  border-color: #222f4f !important;
  cursor: pointer;
}
.product-luxury-card:hover {
  border-color: rgba(243, 198, 63, 0.4) !important;
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.4);
}
.product-luxury-card:hover .item-img {
  transform: scale(1.04);
}

/* Ảnh vuông tỉ lệ 1:1, tự phóng to kịch viền */
.img-wrapper { 
  aspect-ratio: 1 / 1; 
  overflow: hidden; 
  background-color: #070c18; 
}
.item-img {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.premium-price-tag { 
  bottom: 10px; 
  right: 10px; 
  background-color: #f3c63f;
  color: #000000 !important;
  font-weight: 800;
}

.card-body-bg { 
  background-color: #131c31; 
}

/* ĐỔI LAYOUT SANG FLEX COLUMN */
.product-luxury-card {
  display: flex !important;
  flex-direction: column !important;
}

/* HIỆU ỨNG CLICK THU NHỎ */
.transition-all {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
.product-luxury-card:active {
  transform: scale(0.97);
}

/* TỐI ƯU CHIỀU CAO KHUNG CUỘN ĐỂ KHÓA CHẶT ĐÚNG 2 HÀNG SẢN PHẨM */
@media (min-width: 992px) {
  .product-scroll-container {
    /* Khống chế max-height linh hoạt theo chiều cao màn hình để lộ đúng diện tích 2 hàng */
    max-height: calc(100vh - 170px) !important;
    overflow-y: auto !important;
  }
}
</style>