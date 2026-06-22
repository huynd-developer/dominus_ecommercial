<template>
  <div class="dominus-master-app min-vh-100 pb-5">
    
    <header class="dominus-navbar border-bottom py-3 px-3 px-md-5 sticky-top bg-white">
      <div class="container-xl d-flex justify-content-between align-items-center">
        <a href="#" @click.prevent="goTo('home')" class="font-luxury fs-3 text-navy text-decoration-none fw-bold tracking-wider">
          DOMINUS
        </a>

        <div class="d-flex gap-4 align-items-center text-xs fw-semibold">
          <a href="#" @click.prevent="goTo('home')" :class="['nav-tab-link', currentView === 'home' ? 'active-tab' : '']">
            Trang chủ
          </a>
          
          <a href="#" @click.prevent="goTo('profile')" :class="['nav-tab-link', currentView === 'profile' ? 'active-tab' : '']">
            <i class="bi bi-person me-1 fs-6"></i> Tài khoản
          </a>

          <a href="#" @click.prevent="goTo('wishlist')" :class="['nav-tab-link position-relative', currentView === 'wishlist' ? 'active-tab' : '']">
            <i class="bi bi-heart me-1 fs-6"></i> Đã lưu
            <span v-if="favorites.length > 0" class="badge-count-red">{{ favorites.length }}</span>
          </a>
        </div>
      </div>
    </header>

    <main class="container-xl px-3 px-md-4">
      <HomeView v-if="currentView === 'home'" />
      <UserProfile v-else-if="currentView === 'profile'" />
      <WishlistView v-else-if="currentView === 'wishlist'" />
    </main>

  </div>
</template>

<script setup>
import { ref, provide, onMounted } from 'vue';

// Import trực tiếp từ thư mục đi xuống để loại bỏ hoàn toàn lỗi "Failed to resolve import"
import HomeView from './common/components/HomeView.vue'; 
import UserProfile from './common/components/UserProfile.vue';
import WishlistView from './common/components/WishList.vue';

// Biến điều khiển bật tắt Tab: 'home' | 'profile' | 'wishlist'
const currentView = ref('home');

// State lưu danh sách sản phẩm yêu thích (Dùng chung cho cả Navbar và trang WishlistView)
const favorites = ref([
  { 
    id: 101, 
    brand: 'Maison Francis', 
    name: 'Baccarat Rouge 540', 
    salePrice: 6850000, 
    image: 'https://images.unsplash.com/photo-1594035910387-fea47794261f?w=500' 
  }
]);

// Hàm chuyển Tab mượt mà cuộn lên đầu trang
const goTo = (viewName) => {
  currentView.value = viewName;
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

// Hàm xử lý Thêm / Xóa nhanh một chai nước hoa khỏi Wishlist
const toggleWishlist = (product) => {
  const index = favorites.value.findIndex(item => item.id === product.id);
  if (index > -1) {
    favorites.value.splice(index, 1); // Đã lưu rồi thì bấm lại sẽ Xóa
  } else {
    favorites.value.push(product);    // Chưa lưu thì Thêm vào mảng
  }
};

// Bắn (Provide) các hàm và biến này xuống sâu các component con cháu đều dùng chung được
provide('goTo', goTo);
provide('favorites', favorites);
provide('toggleWishlist', toggleWishlist);
</script>

<style>
/* ĐỒNG BỘ FONT CHỮ VÀ BẢNG MÀU CAO CẤP TOÀN HỆ THỐNG */
@import url('https://fonts.googleapis.com/css2?family=Cinzel:wght@400;500;600;700&family=Plus+Jakarta+Sans:wght@300;400;500;600;700&display=swap');

:root {
  --dominus-navy-dark: #0b0f19;
  --dominus-navy-text: #111625;
  --dominus-gold: #c59346;
  --dominus-gold-light: #f7f3eb;
}

body {
  font-family: 'Plus Jakarta Sans', sans-serif;
  background-color: var(--dominus-gold-light);
  color: var(--dominus-navy-text);
  margin: 0;
}

/* Font chữ Serif dành riêng cho tiêu đề sang chảnh */
.font-luxury {
  font-family: 'Cinzel', Georgia, serif;
  letter-spacing: 0.05em;
}

/* Định hình độ mảnh/kích thước chữ phụ trợ */
.text-xs { font-size: 0.75rem; }
.tracking-wider { letter-spacing: 0.1em; }

/* Custom Thanh điều hướng Navbar */
.dominus-navbar {
  box-shadow: 0 2px 15px rgba(0, 0, 0, 0.02);
}

.nav-tab-link {
  text-decoration: none;
  color: #6c757d;
  padding: 8px 4px;
  transition: all 0.2s ease;
  border-bottom: 2px solid transparent;
}

.nav-tab-link:hover {
  color: var(--dominus-gold);
}

/* Tab được kích hoạt: Chuyển chữ đậm đen và có thanh gạch chân quý phái */
.active-tab {
  color: var(--dominus-navy-text) !important;
  font-weight: 700;
  border-bottom: 2px solid var(--dominus-navy-text);
}

/* Badge số lượng màu đỏ nằm góc trên chữ Đã lưu */
.badge-count-red {
  position: absolute;
  top: -2px;
  right: -14px;
  background: #dc3545;
  color: white;
  font-size: 0.6rem;
  padding: 1px 5px;
  border-radius: 50px;
  font-weight: bold;
}

/* Hiệu ứng chuyển trang Fade-in nhẹ nhàng mượt mắt */
.view-transition {
  animation: dominusFadeIn 0.35s ease-out;
}
@keyframes dominusFadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>