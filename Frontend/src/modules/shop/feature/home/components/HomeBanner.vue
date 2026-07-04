<template>
  <section
    class="home-banner position-relative overflow-hidden"
    :style="{ backgroundImage: `url(${bannerImage})` }"
  >
    <div class="banner-overlay"></div>

    <div class="banner-content position-relative d-flex align-items-center">
      <div class="banner-text px-4 px-md-5">
        <h1 class="banner-title mb-3">
          <span>Hương Thơm</span>
          <span>Đẳng Cấp.</span>
        </h1>

        <p class="banner-subtitle mb-4">
          Khám Phá Ngay.
        </p>

        <RouterLink to="/products" class="btn banner-btn">
          MUA NGAY
          <i class="bi bi-arrow-right-short ms-2"></i>
        </RouterLink>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router';
import bannerImage from '@/assets/images/banner-aura.png';
</script>

<style scoped>
.home-banner {
  min-height: 390px;
  border-radius: 18px;
  background-size: cover;
  background-position: center right;
  background-repeat: no-repeat;
  box-shadow: 0 20px 48px rgba(5, 16, 36, 0.14);
}

/* Lớp phủ tối bên trái để chữ nổi rõ hơn */
.banner-overlay {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at left bottom, rgba(189, 154, 95, 0.18), transparent 34%),
    linear-gradient(
      90deg,
      rgba(3, 13, 26, 0.92) 0%,
      rgba(3, 13, 26, 0.76) 34%,
      rgba(3, 13, 26, 0.36) 62%,
      rgba(3, 13, 26, 0.06) 100%
    );
}

/* Quan trọng: không dùng h-100 nữa, set min-height trực tiếp */
.banner-content {
  z-index: 3;
  min-height: 390px;
}

.banner-text {
  max-width: 620px;
  transform: translateY(4px);
}

.banner-title {
  margin: 0;
  font-family: var(--aura-serif);
  color: var(--aura-gold);
  font-size: clamp(42px, 4vw, 64px);
  line-height: 1.08;
  font-weight: 700;
  letter-spacing: -0.8px;
}

.banner-title span {
  display: block;
  white-space: nowrap;
}

.banner-subtitle {
  font-family: var(--aura-sans);
  color: rgba(255, 255, 255, 0.95);
  font-size: clamp(24px, 2.2vw, 32px);
  line-height: 1.2;
  font-weight: 400;
  letter-spacing: -0.2px;
}

.banner-btn {
  background: linear-gradient(135deg, #d1b06e, var(--aura-gold));
  color: #ffffff;
  border-radius: 6px;
  border: none;
  min-width: 150px;
  padding: 11px 24px;
  font-family: var(--aura-sans);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.6px;
  box-shadow: 0 12px 28px rgba(189, 154, 95, 0.22);
  transition: all 0.24s ease;
}

.banner-btn:hover {
  background: var(--aura-gold-hover);
  color: #ffffff;
  transform: translateY(-2px);
}

@media (max-width: 991.98px) {
  .home-banner {
    min-height: 360px;
    background-position: center right;
  }

  .banner-content {
    min-height: 360px;
  }
}

@media (max-width: 767.98px) {
  .home-banner {
    min-height: 400px;
    border-radius: 14px;
    background-position: center;
  }

  .banner-content {
    min-height: 400px;
  }

  .banner-title {
    font-size: 42px;
  }

  .banner-title span {
    white-space: normal;
  }

  .banner-subtitle {
    font-size: 24px;
  }
}
</style>