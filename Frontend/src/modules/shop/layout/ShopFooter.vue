<template>
  <footer class="shop-footer text-center">
    <div class="container footer-container">
      <div class="footer-logo-box mx-auto">
        <img :src="logoAura" alt="DOMINUS PERFUME" class="footer-logo-img" />
      </div>

      <div class="footer-gold-line mx-auto"></div>

      <p class="footer-slogan fst-italic">
        Tinh hoa hương thơm – Đẳng cấp vượt thời gian.
      </p>

      <div
        class="social-list d-flex justify-content-center align-items-center gap-3"
      >
        <span class="social-icon" aria-label="Facebook">
          <i class="bi bi-facebook"></i>
        </span>

        <span class="social-icon" aria-label="Instagram">
          <i class="bi bi-instagram"></i>
        </span>

        <span class="social-icon" aria-label="Youtube">
          <i class="bi bi-youtube"></i>
        </span>

        <span class="social-icon" aria-label="Tiktok">
          <i class="bi bi-tiktok"></i>
        </span>
      </div>

      <div class="footer-divider mx-auto"></div>

      <div
        class="footer-info d-flex justify-content-center align-items-center flex-wrap gap-3"
      >
        <span class="footer-info-item">Về chúng tôi</span>
        <span class="footer-separator">|</span>
        <span class="footer-info-item">Chính sách</span>
        <span class="footer-separator">|</span>
        <span class="footer-info-item">Liên hệ</span>
      </div>

      <div class="footer-contact mt-3">
        <div class="d-inline-flex align-items-center justify-content-center flex-wrap gap-2">
          <span><i class="bi bi-geo-alt-fill text-warning me-1"></i> 123 Đường ABC, Quận Hoàn Kiếm, Hà Nội</span>
          <span class="footer-dot d-none d-md-inline">•</span>
          <span><i class="bi bi-telephone-fill text-warning me-1"></i> <strong>0987.654.321</strong></span>
        </div>
      </div>

      <div class="footer-copy mt-2">
        © 2026 DOMINUS PERFUME. All rights reserved.
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
import logoAura from "@/assets/logo.png";
</script>

<style scoped>
.shop-footer {
  background: radial-gradient(
      circle at top center,
      rgba(189, 154, 95, 0.08),
      transparent 34%
    ),
    #030d1a;
  color: rgba(255, 255, 255, 0.78);
  border-top: 1px solid rgba(189, 154, 95, 0.16);
}

.footer-container {
  padding-top: 34px;
  padding-bottom: 28px;
}

.footer-logo-box {
  width: 360px;
  max-width: 90vw;
  height: 92px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: visible;
}

.footer-logo-img {
  width: 260px;
  max-width: 100%;
  height: auto;
  object-fit: contain;
  display: block;
  transform: none;
  filter: drop-shadow(0 8px 22px rgba(189, 154, 95, 0.22));
}

.footer-gold-line {
  width: 210px;
  height: 1px;
  margin-top: 18px;
  margin-bottom: 18px;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(189, 154, 95, 0.72),
    transparent
  );
}

.footer-slogan {
  margin-bottom: 20px;
  color: rgba(255, 255, 255, 0.82);
  font-size: 14px;
  letter-spacing: 0.2px;
}

.social-list {
  margin-bottom: 22px;
}

.social-icon {
  width: 36px;
  height: 36px;
  border: 1px solid rgba(189, 154, 95, 0.72);
  border-radius: 50%;
  color: #bd9a5f;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  cursor: default;
  background: rgba(255, 255, 255, 0.025);
}

.social-icon i {
  font-size: 16px;
  line-height: 1;
}

.footer-divider {
  max-width: 520px;
  margin-bottom: 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
}

.footer-info {
  color: rgba(255, 255, 255, 0.86);
  font-size: 14px;
  font-weight: 600;
}

.footer-info-item {
  cursor: default;
}

.footer-separator {
  color: rgba(255, 255, 255, 0.28);
}

.footer-contact {
  color: rgba(255, 255, 255, 0.58);
  font-size: 13px;
}

.footer-dot {
  margin: 0 8px;
  color: rgba(189, 154, 95, 0.72);
}

.footer-copy {
  color: rgba(255, 255, 255, 0.42);
  font-size: 12px;
}

@media (max-width: 575.98px) {
  .footer-container {
    padding-top: 28px;
    padding-bottom: 24px;
  }

  .footer-logo-box {
    width: 260px;
    height: 76px;
  }

  .footer-logo-img {
    width: 210px;
    transform: none;
  }

  .footer-contact {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .footer-dot {
    display: none;
  }
}
</style>
