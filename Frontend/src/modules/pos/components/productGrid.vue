<template>
  <div class="d-flex flex-column h-100 pe-1 select-none">
    
    <div class="filter-search-section mb-3 shrink-0">
      <div class="category-slider d-flex gap-2 overflow-auto pb-2 no-scrollbar">
        <button
          v-for="cat in posStore.categories"
          :key="cat"
          type="button"
          :class="['luxury-pill', { 'active': posStore.selectedCategory === cat }]"
          @click="posStore.selectedCategory = cat"
        >
          {{ cat }}
        </button>
      </div>
    </div>

    <div class="flex-grow-1 overflow-y-auto pe-2 product-scroll-container">
      <div v-if="posStore.isLoading" class="text-center py-5 text-muted-custom font-sm">
        <i class="bi bi-arrow-repeat spin fs-3"></i>
        <p class="mt-2">Đang tải sản phẩm từ máy chủ...</p>
      </div>

      <div v-else-if="posStore.filteredProducts.length === 0" class="text-center py-5 text-muted-custom font-sm">
        Không tìm thấy sản phẩm phù hợp.
      </div>

      <div v-else class="row g-3 row-cols-2 row-cols-md-3 row-cols-lg-3 row-cols-xl-3 row-cols-xxl-4 pb-4">
        <div class="col" v-for="product in posStore.filteredProducts" :key="product.sku">
          <div 
            class="product-luxury-card rounded-3 overflow-hidden position-relative h-100 d-flex flex-column border transition-all" 
            @click="posStore.addToCart(product)"
            :class="{'opacity-50': product.stockQuantity <= 0}"
          >
            <div v-if="product.stockQuantity <= 0" class="position-absolute top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" style="background: rgba(0,0,0,0.6); z-index: 10;">
              <span class="text-white fw-bold bg-danger px-3 py-1 rounded">HẾT HÀNG</span>
            </div>

            <div class="img-wrapper position-relative w-100">
              <img :src="product.image || 'https://via.placeholder.com/150'" class="w-100 h-100 object-fit-cover item-img" :alt="product.name">
              <div class="premium-price-tag position-absolute fw-black text-dark px-2.5 py-1 rounded-2 shadow-sm font-sm">
                {{ formatPrice(product.price) }} ₫
              </div>
            </div>

            <div class="p-3 card-body-bg d-flex flex-column justify-content-between flex-grow-1">
              <div>
                <h6 class="mb-1 text-light-custom fw-bold text-truncate font-sm" :title="product.name">
                  {{ product.name }}
                </h6>
                <div class="d-flex justify-content-between align-items-center">
                  <p class="text-muted-custom mb-0 font-xs text-truncate">Kho: {{ product.stockQuantity }}</p>
                  <p class="text-muted-custom mb-0 font-xs">{{ product.sku }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { usePosStore } from '../stores/posStore'

const posStore = usePosStore()
const formatPrice = (val) => new Intl.NumberFormat('vi-VN').format(val || 0)
</script>

<style scoped>
.text-muted-custom { color: #64748b; }
.text-light-custom { color: #f1f5f9; }
.font-sm { font-size: 0.875rem; }
.font-xs { font-size: 0.75rem; }
.fw-black { font-weight: 900; }

.no-scrollbar::-webkit-scrollbar { display: none; }
.no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }

.luxury-pill { background-color: #131c31; border: 1px solid #222f4f; color: #9fb0d3; padding: 8px 20px; border-radius: 30px; font-size: 0.8rem; font-weight: 600; white-space: nowrap; cursor: pointer; transition: all 0.15s ease; }
.luxury-pill:hover { border-color: #384f80; color: #f1f5f9; }
.luxury-pill.active { background-color: #f3c63f !important; color: #000000 !important; border-color: #f3c63f !important; font-weight: 800; box-shadow: 0 4px 10px rgba(243, 198, 63, 0.15); }

.product-luxury-card { background-color: #131c31; border-color: #222f4f !important; cursor: pointer; }
.product-luxury-card:hover:not(.opacity-50) { border-color: rgba(243, 198, 63, 0.4) !important; box-shadow: 0 12px 24px rgba(0, 0, 0, 0.4); }
.product-luxury-card:hover:not(.opacity-50) .item-img { transform: scale(1.04); }
.img-wrapper { aspect-ratio: 1 / 1; overflow: hidden; background-color: #070c18; }
.item-img { transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.premium-price-tag { bottom: 10px; right: 10px; background-color: #f3c63f; color: #000000 !important; font-weight: 800; }
.card-body-bg { background-color: #131c31; }
.transition-all { transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1); }
.product-luxury-card:active:not(.opacity-50) { transform: scale(0.97); }

/* Tùy chỉnh thanh cuộn grid */
.product-scroll-container::-webkit-scrollbar { width: 6px; }
.product-scroll-container::-webkit-scrollbar-track { background: transparent; }
.product-scroll-container::-webkit-scrollbar-thumb { background: #1d2740; border-radius: 4px; }
.product-scroll-container::-webkit-scrollbar-thumb:hover { background: #384f80; }

.spin { animation: spin 1s linear infinite; display: inline-block; }
@keyframes spin { 100% { transform: rotate(360deg); } }
</style>