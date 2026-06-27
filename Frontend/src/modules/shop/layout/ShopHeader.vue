<template>
  <div class="shop-header">
    <div class="container-fluid px-4 px-lg-5 h-100">
      <div class="row align-items-center h-100">
        <!-- Logo -->
        <div class="col-4 col-lg-3">
          <RouterLink to="/" class="brand-logo-link text-decoration-none d-inline-flex align-items-center">
            <img
              v-if="!logoLoadFailed"
              :src="logoAura"
              alt="AURA PERFUME"
              class="brand-logo-img"
              @error="logoLoadFailed = true"
            />

            <div v-else class="brand-logo-text">
              <div class="brand-name">AURA</div>
              <div class="brand-subtitle">PERFUME</div>
            </div>
          </RouterLink>
        </div>

        <!-- Search -->
        <div class="col-12 col-lg-6 order-3 order-lg-2 mt-3 mt-lg-0">
          <div class="search-wrapper position-relative mx-auto">
            <input
              type="text"
              class="form-control rounded-pill search-input"
              placeholder="Tìm kiếm nước hoa..."
            />

            <i class="bi bi-search search-icon"></i>
          </div>
        </div>

        <!-- Account / Cart -->
        <div class="col-8 col-lg-3 order-2 order-lg-3">
          <div class="d-flex align-items-center justify-content-end gap-3 gap-lg-4">
            <RouterLink
              to="/login"
              class="header-action d-flex align-items-center gap-2 text-decoration-none"
            >
              <span class="action-icon-circle">
                <i class="bi bi-person"></i>
              </span>
              <span class="d-none d-md-inline">Tài khoản</span>
            </RouterLink>

            <RouterLink
              to="/cart"
              class="header-action cart-action d-flex align-items-center gap-2 text-decoration-none position-relative"
            >
              <span class="action-icon-circle">
                <i class="bi bi-bag"></i>
              </span>
              <span class="d-none d-md-inline">Giỏ hàng</span>
              <span class="cart-badge">0</span>
            </RouterLink>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { RouterLink } from 'vue-router';
import logoAura from '@/assets/logo.png';

const logoLoadFailed = ref(false);
</script>

<style scoped>
.shop-header {
  min-height: 92px;
  background:
    radial-gradient(circle at top left, rgba(189, 154, 95, 0.16), transparent 32%),
    linear-gradient(135deg, #030d1a 0%, #07182f 52%, #0a192f 100%);
  border-bottom: 1px solid rgba(189, 154, 95, 0.28);
}

.brand-logo-link {
  min-height: 92px;
}

.brand-logo-img {
  height: 76px;
  width: auto;
  max-width: 260px;
  object-fit: contain;
  display: block;
}

.brand-logo-text {
  text-align: center;
  line-height: 1;
}

.brand-name {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 46px;
  letter-spacing: 10px;
  font-weight: 500;
  color: #bd9a5f;
}

.brand-subtitle {
  margin-top: 7px;
  font-size: 12px;
  letter-spacing: 8px;
  color: #bd9a5f;
  font-weight: 600;
}

.search-wrapper {
  max-width: 680px;
}

.search-input {
  height: 48px;
  padding-left: 26px;
  padding-right: 56px;
  border: 1px solid rgba(189, 154, 95, 0.45);
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
  font-size: 15px;
  backdrop-filter: blur(8px);
}

.search-input::placeholder {
  color: rgba(255, 255, 255, 0.72);
}

.search-input:focus {
  background: rgba(255, 255, 255, 0.12);
  color: #ffffff;
  border-color: #bd9a5f;
  box-shadow: 0 0 0 0.2rem rgba(189, 154, 95, 0.18);
}

.search-icon {
  position: absolute;
  top: 50%;
  right: 22px;
  transform: translateY(-50%);
  font-size: 18px;
  color: #bd9a5f;
}

.header-action {
  color: #ffffff;
  font-weight: 600;
  font-size: 15px;
  transition: color 0.2s ease;
}

.header-action:hover {
  color: #bd9a5f;
}

.action-icon-circle {
  width: 38px;
  height: 38px;
  border: 1px solid rgba(189, 154, 95, 0.65);
  border-radius: 50%;
  color: #bd9a5f;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.05);
  transition: all 0.2s ease;
}

.action-icon-circle i {
  font-size: 19px;
  line-height: 1;
}

.header-action:hover .action-icon-circle {
  background: #bd9a5f;
  color: #030d1a;
  border-color: #bd9a5f;
}

.cart-badge {
  position: absolute;
  top: -9px;
  right: -11px;
  min-width: 19px;
  height: 19px;
  padding: 0 5px;
  border-radius: 999px;
  background: #ffffff;
  color: #030d1a;
  border: 1px solid #bd9a5f;
  font-size: 11px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 991.98px) {
  .shop-header {
    min-height: auto;
    padding: 16px 0;
  }

  .brand-logo-link {
    min-height: auto;
  }

  .brand-logo-img {
    height: 58px;
    max-width: 180px;
  }

  .brand-name {
    font-size: 34px;
    letter-spacing: 7px;
  }

  .brand-subtitle {
    letter-spacing: 5px;
  }
}

@media (max-width: 575.98px) {
  .brand-logo-img {
    height: 46px;
    max-width: 140px;
  }

  .action-icon-circle {
    width: 34px;
    height: 34px;
  }
}
</style>