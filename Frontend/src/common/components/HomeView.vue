<template>
  <div class="home-view-wrapper py-2">
    <!-- Hero Banner -->
    <div class="bg-navy text-white p-5 rounded-4 mb-5 shadow-sm">
      <span class="text-gold text-xs uppercase tracking-widest d-block mb-3">✦ NEW COLLECTION 2026</span>
      <h1 class="font-luxury display-4 mb-4">Hương thơm<br/>của quyền lực</h1>
      <button @click="goTo('profile', 'update')" class="custom-gold-btn text-xs">Xem bộ sưu tập của bạn ✦</button>
    </div>

    <!-- Product Grid -->
    <div class="row g-4 row-cols-1 row-cols-md-2 row-cols-lg-4">
      <div v-for="p in products" :key="p.id" class="col">
        <div class="bg-white p-3 rounded-4 border h-100 d-flex flex-column justify-content-between position-relative">
          <button @click="toggleWishlist(p)" class="btn border-0 bg-transparent position-absolute top-0 end-0 m-2 z-1">
            <i :class="['bi fs-5', isSaved(p.id) ? 'bi-heart-fill text-gold' : 'bi-heart text-muted']"></i>
          </button>
          
          <div class="bg-light-cream rounded-3 p-4 text-center mb-3">
            <img :src="p.image" style="height: 170px; object-fit: contain;" />
          </div>

          <div>
            <h5 class="font-luxury fw-bold text-navy mb-1">{{ p.name }}</h5>
            <span class="text-danger fw-bold">{{ p.salePrice.toLocaleString('vi-VN') }}đ</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, inject } from 'vue';

const favorites = inject('favorites');
const toggleWishlist = inject('toggleWishlist');
const goTo = inject('goTo');

const products = ref([
  { id: 101, name: 'Baccarat Rouge 540', salePrice: 6850000, image: 'https://images.unsplash.com/photo-1594035910387-fea47794261f?w=500' },
  { id: 102, name: 'Santal 33 EDP', salePrice: 4890000, image: 'https://images.unsplash.com/photo-1547887537-6158d64c35b3?w=500' },
  { id: 103, name: 'Tom Ford Oud Wood', salePrice: 5550000, image: 'https://images.unsplash.com/photo-1588405748480-12d9d757b85e?w=500' },
  { id: 104, name: 'Dior Sauvage Elixir', salePrice: 3850000, image: 'https://images.unsplash.com/photo-1523293182086-7651a899d37f?w=500' }
]);

const isSaved = (id) => favorites.value.some(item => item.id === id);
</script>