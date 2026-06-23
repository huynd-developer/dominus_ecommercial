<template>
  <div class="app-dark-layout min-vh-100 bg-navy-dark text-white font-sans">
    
    <header class="border-bottom border-secondary border-opacity-10 py-3 mb-2">
      <div class="container d-flex flex-column flex-md-row align-items-center justify-content-between gap-3">
        <div class="d-flex align-items-baseline gap-2 cursor-pointer" @click="goTo('home')">
          <span class="font-luxury h2 text-gold mb-0 tracking-wider">DOMINUS</span>
          <span class="text-gold-opacity uppercase tracking-widest" style="font-size: 0.6rem;">PERFUME</span>
        </div>

        <div class="search-bar-wrapper position-relative" style="width: 100%; max-width: 400px;">
          <i class="bi bi-search text-white-50 position-absolute top-50 start-3 translate-middle-y small"></i>
          <input 
            type="text" 
            class="form-control search-input text-xs rounded-pill ps-5 py-2 text-white" 
            placeholder="Tìm nước hoa hoặc đơn hàng..."
          />
        </div>

        <div class="d-flex align-items-center gap-4 text-xs font-weight-500">
          <button 
            @click="goTo('orders')" 
            :class="['btn rounded-pill px-3 py-1-5 fw-bold btn-nav-tab', currentView === 'orders' || currentView === 'review' ? 'btn-gold' : 'btn-outline-gold-light']"
          >
            Đơn hàng
          </button>
          
          <div class="d-flex align-items-center gap-3 action-links">
            <span class="cursor-pointer text-gold-hover d-flex align-items-center gap-1" :class="{'text-gold': currentView === 'wishlist'}" @click="goTo('wishlist')">
              <i class="bi bi-heart"></i> Đã lưu
            </span>
            <span class="cursor-pointer text-gold-hover d-flex align-items-center gap-1" :class="{'text-gold': currentView === 'profile'}" @click="goTo('profile')">
              <i class="bi bi-person"></i> Tài khoản
            </span>
            <span class="cursor-pointer text-gold-hover d-flex align-items-center gap-1">
              <i class="bi bi-bag"></i> Giỏ hàng
            </span>
          </div>
        </div>
      </div>
    </header>

    <main class="container py-3">
      <Transition name="fade" mode="out-in">
        <KeepAlive :include="['MyOrders', 'WishlistView']">
          <component 
            :is="renderView" 
            @goToReview="handleOpenReview"
          />
        </KeepAlive>
      </Transition>
    </main>

  </div>
</template>

<script setup>
import { ref, computed, provide } from 'vue';

// IMPORT TẤT CẢ CÁC COMPONENT GIAO DIỆN TRONG HỆ THỐNG
import HomeView from './common/components/HomeView.vue';               
import UserProfile from './common/components/UserProfile.vue';         
import WishlistView from './common/components/WishList.vue';       
import MyOrders from './common/components/MyOrders.vue';               
import ProductReview from './common/components/ProductReview.vue';     

// Quản lý trạng thái màn hình hiện tại
const currentView = ref('orders'); 

// Biến lưu trữ thông tin đơn hàng được chọn để mang đi đánh giá
const selectedOrderToReview = ref(null);

// Hàm điều hướng chính xuyên suốt các component
const goTo = (viewName) => {
  currentView.value = viewName;
};

// Hàm đặc biệt xử lý khi click "Đánh giá" từ trang đơn hàng
const handleOpenReview = (order) => {
  selectedOrderToReview.value = order;
  currentView.value = 'review'; 
};

// ĐÓNG GÓI VÀ ĐỒNG BỘ ĐIỀU HƯỚNG XUỐNG CÁC TRANG CON
provide('goTo', goTo);
provide('currentView', currentView);
provide('selectedOrderToReview', selectedOrderToReview);

// Computed ánh xạ đổi trả component tương ứng theo biến điều hướng currentView
const renderView = computed(() => {
  switch (currentView.value) {
    case 'home': return HomeView;
    case 'profile': return UserProfile;
    case 'wishlist': return WishlistView;
    case 'orders': return MyOrders;
    case 'review': return ProductReview;
    default: return MyOrders;
  }
});
</script>

<style>
/* =========================================
  HỆ THỐNG PHÔNG CHỮ & STYLE TOÀN CỤC (GLOBAL)
  =========================================
*/
@import url('https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,400;0,500;0,600;0,700;1,400&family=Plus+Jakarta+Sans:wght@300;400;500;600;700&display=swap');

:root {
  --dominus-gold: #c59346;
  --dominus-navy-dark: #0b0f19;
  --dominus-navy-card: #131a2e;
}

body {
  background-color: #0b0f19 !important;
  font-family: 'Plus Jakarta Sans', sans-serif !important;
}

.font-luxury {
  font-family: 'Playfair Display', Georgia, serif !important;
  letter-spacing: 0.02em !important;
}

.bg-navy-dark { background-color: #0b0f19 !important; }
.text-gold { color: #c59346 !important; }
.text-gold-opacity { color: rgba(197, 147, 70, 0.6); }

.search-input {
  background-color: rgba(255, 255, 255, 0.04) !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
}
.search-input:focus {
  border-color: #c59346 !important;
  box-shadow: 0 0 0 3px rgba(197, 147, 70, 0.15) !important;
}

.text-gold-hover {
  color: #94a3b8;
  transition: all 0.2s ease;
}
.text-gold-hover:hover {
  color: #c59346 !important;
}
.cursor-pointer { cursor: pointer; }

.btn-gold {
  background-color: #c59346 !important;
  color: #0b0f19 !important;
  border: none !important;
}
.btn-gold:hover {
  background-color: #dfaa53 !important;
}
.btn-outline-gold-light {
  background: rgba(255, 255, 255, 0.02) !important;
  color: #e2e8f0 !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
}
.btn-outline-gold-light:hover {
  border-color: #c59346 !important;
  color: #c59346 !important;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.text-xs { font-size: 0.8rem !important; }
.text-xxs { font-size: 0.65rem !important; }
.py-2-5 { padding-top: 0.65rem !important; padding-bottom: 0.65rem !important; }
</style>