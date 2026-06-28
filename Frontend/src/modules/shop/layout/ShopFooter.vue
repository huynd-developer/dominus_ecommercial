<template>
  <footer class="shop-footer text-center">
    <div class="container py-5">
      <RouterLink to="/" class="text-decoration-none d-inline-block">
        <div class="footer-logo">AURA</div>
        <div class="footer-logo-sub">PERFUME</div>
      </RouterLink>

      <div class="footer-gold-line mx-auto my-4"></div>

      <p class="footer-slogan fst-italic mb-4">
        Tinh hoa hương thơm – Đẳng cấp vượt thời gian.
      </p>

      <div class="social-list d-flex justify-content-center align-items-center gap-3 mb-4">
        <a href="#" class="social-icon" aria-label="Facebook">
          <i class="bi bi-facebook"></i>
        </a>

        <a href="#" class="social-icon" aria-label="Instagram">
          <i class="bi bi-instagram"></i>
        </a>

        <a href="#" class="social-icon" aria-label="Youtube">
          <i class="bi bi-youtube"></i>
        </a>

        <a href="#" class="social-icon" aria-label="Tiktok">
          <i class="bi bi-tiktok"></i>
        </a>
      </div>

      <div class="footer-divider mx-auto mb-4"></div>

      <div class="footer-links d-flex justify-content-center align-items-center flex-wrap gap-3">
        <RouterLink to="/about" class="footer-link">Về chúng tôi</RouterLink>
        <span class="footer-separator">|</span>
        <RouterLink to="/policy" class="footer-link">Chính sách</RouterLink>
        <span class="footer-separator">|</span>
        <RouterLink to="/contact" class="footer-link">Liên hệ</RouterLink>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router';
</script>

<style scoped>
.shop-footer {
  background: #030d1a;
  color: rgba(255, 255, 255, 0.78);
}

.footer-logo {
  font-family: Georgia, 'Times New Roman', serif;
  color: #bd9a5f;
  font-size: 42px;
  letter-spacing: 12px;
  line-height: 1;
}

.footer-logo-sub {
  color: #bd9a5f;
  font-size: 11px;
  letter-spacing: 7px;
  margin-top: 8px;
}

.footer-gold-line {
  width: 240px;
  height: 1px;
  background: rgba(189, 154, 95, 0.45);
}

.footer-slogan {
  color: rgba(255, 255, 255, 0.78);
  font-size: 15px;
}

.social-icon {
  width: 38px;
  height: 38px;
  border: 1px solid rgba(189, 154, 95, 0.75);
  border-radius: 50%;
  color: #bd9a5f;
  text-decoration: none;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  transition: all 0.2s ease;
}

.social-icon i {
  font-size: 17px;
  line-height: 1;
}

.social-icon:hover {
  background: #bd9a5f;
  color: #030d1a;
  transform: translateY(-2px);
}

.footer-divider {
  max-width: 520px;
  border-top: 1px solid rgba(255, 255, 255, 0.14);
}

.footer-link {
  color: rgba(255, 255, 255, 0.82);
  text-decoration: none;
  font-size: 14px;
  transition: color 0.2s ease;
}

.footer-link:hover {
  color: #bd9a5f;
}

.footer-separator {
  color: rgba(255, 255, 255, 0.35);
}
</style>