<template>
  <section class="home-banner position-relative overflow-hidden rounded-4">
    <div class="banner-overlay"></div>

    <div class="banner-content position-relative h-100 d-flex align-items-center">
      <div class="px-4 px-md-5">
        <p class="banner-kicker mb-2">Aura Perfume</p>

        <h1 class="banner-title mb-4">
          Hương Thơm <br />
          Đẳng Cấp.
        </h1>

        <RouterLink to="/products" class="btn banner-btn px-4 py-2 fw-bold">
          MUA NGAY
          <i class="bi bi-arrow-right ms-2"></i>
        </RouterLink>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router';
</script>

<style scoped>
.home-banner {
  aspect-ratio: 21 / 9;
  background-image: url('https://i.imgur.com/E8wZ6jL.jpeg');
  background-size: cover;
  background-position: center;
  min-height: 320px;
}

.banner-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    90deg,
    rgba(3, 13, 26, 0.88) 0%,
    rgba(3, 13, 26, 0.72) 34%,
    rgba(3, 13, 26, 0.2) 68%,
    rgba(3, 13, 26, 0.08) 100%
  );
}

.banner-content {
  z-index: 2;
}

.banner-kicker {
  color: #bd9a5f;
  letter-spacing: 4px;
  text-transform: uppercase;
  font-weight: 700;
  font-size: 14px;
}

.banner-title {
  /* Thay font ở đây */
  font-family: 'Playfair Display', serif; 
  color: #ffffff;
  font-size: clamp(42px, 5vw, 76px);
  line-height: 1.08;
  font-weight: 500;
}

.banner-btn {
  background: #bd9a5f;
  color: #080808;
  border-radius: 8px;
  border: none;
  min-width: 150px;
  transition: all 0.2s ease;
}

.banner-btn:hover {
  background: #a3824d;
  color: #ffffff;
  transform: translateY(-1px);
}

@media (max-width: 767.98px) {
  .home-banner {
    aspect-ratio: auto;
    min-height: 360px;
  }
}
</style>