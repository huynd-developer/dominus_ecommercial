<template>
  <div class="wishlist-page-wrapper py-4 view-transition">
    <nav class="text-xs text-white-50 mb-4">
      <span class="cursor-pointer text-gold-hover" @click="goTo('home')">Trang chủ</span> 
      <span class="mx-2 text-secondary">&gt;</span> 
      <span class="cursor-pointer text-gold-hover" @click="goTo('profile')">Tài khoản</span> 
      <span class="mx-2 text-secondary">&gt;</span> 
      <span class="fw-bold text-gold">Sản phẩm đã lưu</span>
    </nav>

    <div class="card border border-secondary border-opacity-10 shadow-lg p-4 p-md-5 bg-navy-card rounded-5">
      <div class="row g-4">
        
        <div class="col-lg-4">
          <div class="wishlist-dark-card p-4 p-md-5 rounded-4 h-100 d-flex flex-column justify-content-between position-relative overflow-hidden text-white">
            <div>
              <span class="text-overline text-gold uppercase d-block mb-2 fw-semibold">WISHLIST DOMINUS</span>
              <h2 class="font-luxury display-6 mb-3 text-gold">Sản phẩm <br/>đã lưu</h2>
              <p class="text-white-50 text-xs lh-base" style="font-weight: 300;">
                Đây là nơi bạn xem lại những mùi hương đã bấm "Lưu sản phẩm". Khi quay lại DOMINUS, danh sách này giúp bạn mua nhanh hơn hoặc theo dõi giá sale.
              </p>
            </div>
            <button @click="goTo('home')" class="btn btn-gold text-xs rounded-pill py-2-5 w-100 mt-4 fw-bold">
              Tiếp tục xem sản phẩm
            </button>
          </div>
        </div>

        <div class="col-lg-8 ps-lg-4">
          <div class="d-flex justify-content-between align-items-baseline mb-4">
            <div>
              <span class="text-overline text-gold uppercase d-block mb-1 fw-semibold">BỘ SƯU TẬP CÁ NHÂN</span>
              <h3 class="font-luxury text-white mb-0">Danh sách đã lưu</h3>
            </div>
            <span class="badge border border-gold border-opacity-30 text-gold rounded-pill px-3 py-2 text-xs fw-medium bg-navy-dark">
              {{ favorites.length }} sản phẩm
            </span>
          </div>

          <div v-if="favorites.length === 0" class="empty-state-box rounded-4 border border-dashed p-5 text-center my-3">
            <div class="empty-heart-icon text-gold mb-3">
              <i class="bi bi-heart fs-1"></i>
            </div>
            <h5 class="fw-bold text-gold mb-2 font-luxury">Bạn chưa lưu sản phẩm nào</h5>
            <p class="text-white-50 text-xs mb-0">Bấm "Lưu sản phẩm" ở trang chi tiết để sản phẩm xuất hiện tại đây.</p>
          </div>

          <div v-else class="d-flex flex-column gap-3 mt-3">
            <div v-for="item in favorites" :key="item.id" class="product-item-row p-3 rounded-4 border border-secondary border-opacity-10 bg-navy-dark d-flex align-items-center justify-content-between gap-3 transition-all">
              <div class="d-flex align-items-center gap-3">
                <div class="img-container bg-input-dark border border-secondary border-opacity-10 rounded-3 p-2 d-flex align-items-center justify-content-center" style="width: 65px; height: 75px;">
                  <img :src="item.image" class="object-fit-contain w-100 h-100" />
                </div>
                <div>
                  <span class="text-gold text-xxs uppercase fw-bold tracking-wider">{{ item.brand || 'DOMINUS' }}</span>
                  <h6 class="fw-bold text-white mb-1 text-sm font-luxury">{{ item.name }}</h6>
                  <span class="text-gold fw-bold text-xs">{{ item.salePrice.toLocaleString('vi-VN') }}đ</span>
                </div>
              </div>
              <div class="d-flex gap-2">
                <button @click="toggleWishlist(item)" class="btn btn-sm btn-outline-danger-custom rounded-pill px-3 text-xs">Bỏ lưu</button>
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
/* KHAI BÁO MÀU SẮC ĐỒNG BỘ ĐÁP ỨNG DARK MODE */
.bg-navy-card { background-color: #131a2e !important; }
.bg-navy-dark { background-color: #0b0f19 !important; }
.bg-input-dark { background-color: #1e293b !important; }
.text-gold { color: var(--dominus-gold, #c59346) !important; }

/* Banner trái (Độ dốc màu mượt từ Navy sâu sang Đen tối thượng) */
.wishlist-dark-card { 
  background: linear-gradient(135deg, #0f1422 0%, #070a12 100%); 
  border: 1px solid rgba(197, 147, 70, 0.15);
  min-height: 320px; 
}

/* Khung trạng thái trống khi không có sản phẩm */
.empty-state-box { 
  border-style: dashed !important; 
  border-color: rgba(197, 147, 70, 0.25) !important; 
  background-color: #0e1526; 
  padding: 4rem 2rem !important; 
}

/* Tương tác khi Hover qua từng dòng sản phẩm nước hoa */
.product-item-row:hover { 
  border-color: var(--dominus-gold, #c59346) !important; 
  background: #192239 !important; /* Sáng nhẹ lên một chút tạo hiệu ứng chiều sâu */
  transform: translateY(-2px); 
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3); 
}

/* Nút bấm Vàng Gold chữ Đen quý phái */
.btn-gold { 
  background-color: var(--dominus-gold, #c59346); 
  color: #0b0f19; 
  border: none; 
}
.btn-gold:hover { 
  background-color: #dfaa53; 
  color: #0b0f19; 
}

/* Nút hủy lưu thiết kế thanh mảnh, không bị đỏ rực phá vỡ không gian tối */
.btn-outline-danger-custom {
  color: #ef4444;
  border: 1px solid rgba(239, 68, 68, 0.3);
  background: transparent;
  transition: all 0.2s ease;
}
.btn-outline-danger-custom:hover {
  color: #ffffff;
  background-color: #ef4444;
  border-color: #ef4444;
}

/* Trợ năng chuyển đổi màu khi rê chuột ở breadcrumb */
.text-gold-hover:hover {
  color: var(--dominus-gold, #c59346) !important;
  transition: color 0.2s ease;
}

.cursor-pointer { cursor: pointer; }
.text-xxs { font-size: 0.65rem; }
.text-overline { font-size: 0.65rem; letter-spacing: 0.25em; }
</style>