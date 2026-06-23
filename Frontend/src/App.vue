<template>
  <div class="dominus-master-app min-vh-100 pb-5 bg-navy-dark">
    
    <header class="dominus-navbar border-bottom border-secondary border-opacity-20 py-3 px-3 px-md-5 sticky-top bg-navy-card">
      <div class="container-xl d-flex justify-content-between align-items-center">
        <a href="#" @click.prevent="goTo('home')" class="font-luxury fs-3 text-gold text-decoration-none fw-bold tracking-wider">
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
            <span v-if="favorites.length > 0" class="badge-count-gold">{{ favorites.length }}</span>
          </a>
        </div>
      </div>
    </header>

    <main class="container-xl px-3 px-md-4 mt-4">
      <HomeView v-if="currentView === 'home'" />
      <UserProfile v-else-if="currentView === 'profile'" />
      <WishlistView v-else-if="currentView === 'wishlist'" />
    </main>

  </div>
</template>

<script setup>
import { ref, provide } from 'vue';

// Giữ nguyên các đường dẫn Import của bạn
import HomeView from './common/components/HomeView.vue'; 
import UserProfile from './common/components/UserProfile.vue';
import WishlistView from './common/components/WishList.vue';

const currentView = ref('home');

const favorites = ref([
  { 
    id: 101, 
    brand: 'Maison Francis', 
    name: 'Baccarat Rouge 540', 
    salePrice: 6850000, 
    image: 'https://images.unsplash.com/photo-1594035910387-fea47794261f?w=500' 
  }
]);

const goTo = (viewName) => {
  currentView.value = viewName;
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

const toggleWishlist = (product) => {
  const index = favorites.value.findIndex(item => item.id === product.id);
  if (index > -1) {
    favorites.value.splice(index, 1);
  } else {
    favorites.value.push(product);
  }
};

provide('goTo', goTo);
provide('favorites', favorites);
provide('toggleWishlist', toggleWishlist);
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Cinzel:wght@400;500;600;700&family=Plus+Jakarta+Sans:wght@300;400;500;600;700&display=swap');

:root {
  --dominus-navy-dark: #0b0f19;  /* Màu nền tổng thể sâu thẳm */
  --dominus-navy-card: #131a2e;  /* Màu nền cho các khối card, header */
  --dominus-gold: #c59346;       /* Sắc vàng Gold hoàng gia ánh kim */
  --dominus-gold-light: #f7f3eb;
  --dominus-text-light: #e2e8f0;  /* Màu chữ phụ trắng xám dịu mắt */
}

body {
  font-family: 'Plus Jakarta Sans', sans-serif;
  background-color: var(--dominus-navy-dark) !important;
  color: var(--dominus-text-light);
  margin: 0;
}

/* Biến màu nền động áp dụng cho toàn app */
.bg-navy-dark { background-color: var(--dominus-navy-dark) !important; }
.bg-navy-card { background-color: var(--dominus-navy-card) !important; }
.text-gold { color: var(--dominus-gold) !important; }

/* Font chữ Serif dành riêng cho thương hiệu */
.font-luxury {
  font-family: 'Playfair Display' Georgia, serif;
  letter-spacing: 0.05em;
}

.text-xs { font-size: 0.75rem; }
.tracking-wider { letter-spacing: 0.1em; }

/* Thiết kế lại thanh Navbar theo khối tối màu */
.dominus-navbar {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

/* Các tab liên kết khi ở trạng thái đợi (Màu trắng mờ) */
.nav-tab-link {
  text-decoration: none;
  color: #94a3b8;
  padding: 8px 4px;
  transition: all 0.2s ease;
  border-bottom: 2px solid transparent;
}

.nav-tab-link:hover {
  color: var(--dominus-gold);
}

/* Khi tab được CHỌN: Chữ đổi hẳn sang màu Vàng Gold rực rỡ và gạch chân Gold */
.active-tab {
  color: var(--dominus-gold) !important;
  font-weight: 700;
  border-bottom: 2px solid var(--dominus-gold);
}

/* Badge đếm số lượng được đổi từ đỏ sang nền Gold chữ Đen nổi bật trên nền tối */
.badge-count-gold {
  position: absolute;
  top: -2px;
  right: -14px;
  background: var(--dominus-gold);
  color: var(--dominus-navy-dark);
  font-size: 0.6rem;
  padding: 1px 5px;
  border-radius: 50px;
  font-weight: bold;
}

/* Hiệu ứng chuyển cảnh mượt */
.view-transition {
  animation: dominusFadeIn 0.35s ease-out;
}
@keyframes dominusFadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>