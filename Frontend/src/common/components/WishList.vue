<template>
  <div class="wishlist-page-wrapper py-4 view-transition">
    <!-- Breadcrumb điều hướng -->
    <nav class="text-xs text-muted mb-4">
      <span class="cursor-pointer text-navy" @click="goTo('home')">Trang chủ</span> 
      <span class="mx-2">&gt;</span> 
      <span class="cursor-pointer text-navy" @click="goTo('profile')">Tài khoản</span> 
      <span class="mx-2">&gt;</span> 
      <span class="fw-bold text-navy">Sản phẩm đã lưu</span>
    </nav>

    <!-- Khung nội dung bo tròn lớn theo mẫu ảnh 2 -->
    <div class="card border-0 shadow-sm p-4 p-md-5 bg-white rounded-5">
      <div class="row g-4">
        
        <!-- Khối Banner đen huyền bí bên trái -->
        <div class="col-lg-4">
          <div class="wishlist-dark-card p-4 p-md-5 rounded-4 h-100 d-flex flex-column justify-content-between position-relative overflow-hidden text-white">
            <div>
              <span class="text-overline text-gold uppercase d-block mb-2">WISHLIST DOMINUS</span>
              <h2 class="font-luxury display-6 mb-3">Sản phẩm <br/>đã lưu</h2>
              <p class="text-muted-light text-xs lh-base">
                Đây là nơi bạn xem lại những mùi hương đã bấm "Lưu sản phẩm". Khi quay lại DOMINUS, danh sách này giúp bạn mua nhanh hơn hoặc theo dõi giá sale.
              </p>
            </div>
            <button @click="goTo('home')" class="btn btn-gold text-xs rounded-pill py-2-5 w-100 mt-4 fw-bold">
              Tiếp tục xem sản phẩm
            </button>
          </div>
        </div>

        <!-- Khối hiển thị danh sách sản phẩm bên phải -->
        <div class="col-lg-8">
          <div class="d-flex justify-content-between align-items-baseline mb-3">
            <div>
              <span class="text-overline text-gold uppercase d-block mb-1">BỘ SƯU TẬP CÁ NHÂN</span>
              <h3 class="font-luxury text-navy mb-0">Danh sách đã lưu</h3>
            </div>
            <span class="badge bg-light text-dark border rounded-pill px-3 py-2 text-xs fw-medium">
              {{ favorites.length }} sản phẩm
            </span>
          </div>

          <!-- TRẠNG THÁI TRỐNG (Y hệt ảnh mẫu image_8165e8.png) -->
          <div v-if="favorites.length === 0" class="empty-state-box rounded-4 border border-dashed p-5 text-center my-3">
            <div class="empty-heart-icon text-gold mb-3">
              <i class="bi bi-heart fs-1"></i>
            </div>
            <h5 class="fw-bold text-navy mb-2">Bạn chưa lưu sản phẩm nào</h5>
            <p class="text-muted text-xs mb-0">Bấm "Lưu sản phẩm" ở trang chi tiết để sản phẩm xuất hiện tại đây.</p>
          </div>

          <!-- TRẠNG THÁI CÓ SẢN PHẨM -->
          <div v-else class="d-flex flex-column gap-3 mt-3">
            <div v-for="item in favorites" :key="item.id" class="product-item-row p-3 rounded-4 border bg-light-cream d-flex align-items-center justify-content-between gap-3 transition-all">
              <div class="d-flex align-items-center gap-3">
                <div class="img-container bg-white border rounded-3 p-2 d-flex align-items-center justify-content-center" style="width: 65px; height: 75px;">
                  <img :src="item.image" class="object-fit-contain w-100 h-100" />
                </div>
                <div>
                  <span class="text-gold text-xxs uppercase fw-bold tracking-wider">{{ item.brand || 'DOMINUS' }}</span>
                  <h6 class="fw-bold text-navy mb-1 text-sm font-luxury">{{ item.name }}</h6>
                  <span class="text-danger fw-bold text-xs">{{ item.salePrice.toLocaleString('vi-VN') }}đ</span>
                </div>
              </div>
              <div class="d-flex gap-2">
                <button @click="toggleWishlist(item)" class="btn btn-sm btn-outline-danger rounded-pill px-3 text-xs">Bỏ lưu</button>
              </div>
            </div>
          </div>

        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { inject } from 'vue';

const favorites = inject('favorites');
const toggleWishlist = inject('toggleWishlist');
const goTo = inject('goTo');
</script>

<style scoped>
.wishlist-dark-card { background: linear-gradient(135deg, #111625 0%, #1a2238 100%); min-height: 320px; }
.empty-state-box { border-style: dashed !important; border-color: #d1cbd4 !important; background-color: #fcfbf9; padding: 4rem 2rem !important; }
.product-item-row:hover { border-color: #c59346 !important; background: #fff; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.02); }
.text-gold { color: #c59346; }
.btn-gold { background-color: #c59346; color: white; border: none; }
.btn-gold:hover { background-color: #b08139; color: white; }
.cursor-pointer { cursor: pointer; }
.text-xxs { font-size: 0.65rem; }
.text-overline { font-size: 0.65rem; letter-spacing: 0.25em; }
</style>