<template>
  <div class="shop-header bg-white">
    <div class="container-fluid px-4 px-lg-5 h-100">
      <div class="row align-items-center h-100">
        <!-- Logo -->
        <div class="col-4 col-lg-3">
          <RouterLink to="/" class="text-decoration-none d-inline-block">
            <div class="brand-logo text-center">
              <!-- Logo ảnh -->
              <img
                v-if="!logoLoadFailed"
                :src="logoAura"
                alt="AURA PERFUME"
                class="brand-logo-img"
                @error="logoLoadFailed = true"
              />

              <!-- Logo chữ dự phòng nếu ảnh lỗi -->
              <template v-else>
                <div class="brand-name">AURA</div>
                <div class="brand-subtitle">PERFUME</div>
              </template>
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
  min-height: 80px;
}

.brand-logo {
  line-height: 1;
  color: #0a192f;
}

.brand-logo-img {
  height: 58px;
  width: auto;
  max-width: 180px;
  object-fit: contain;
  display: block;
}

.brand-name {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 38px;
  letter-spacing: 9px;
  font-weight: 500;
  color: #0a192f;
}

.brand-subtitle {
  margin-top: 6px;
  font-size: 11px;
  letter-spacing: 7px;
  color: #bd9a5f;
  font-weight: 600;
}

.search-wrapper {
  max-width: 620px;
}

.search-input {
  height: 44px;
  padding-left: 24px;
  padding-right: 54px;
  border-color: #dee2e6;
  color: #0a192f;
  font-size: 15px;
}

.search-input:focus {
  border-color: #bd9a5f;
  box-shadow: 0 0 0 0.2rem rgba(189, 154, 95, 0.18);
}

.search-icon {
  position: absolute;
  top: 50%;
  right: 20px;
  transform: translateY(-50%);
  font-size: 17px;
  color: #0a192f;
  opacity: 0.75;
}

.header-action {
  color: #0a192f;
  font-weight: 600;
  font-size: 15px;
  transition: color 0.2s ease;
}

.header-action:hover {
  color: #bd9a5f;
}

.action-icon-circle {
  width: 34px;
  height: 34px;
  border: 1px solid rgba(10, 25, 47, 0.16);
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  transition: all 0.2s ease;
}

.action-icon-circle i {
  font-size: 18px;
  line-height: 1;
}

.header-action:hover .action-icon-circle {
  border-color: #bd9a5f;
  background: rgba(189, 154, 95, 0.08);
}

.cart-badge {
  position: absolute;
  top: -8px;
  right: -10px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: #050505;
  color: #ffffff;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 991.98px) {
  .shop-header {
    min-height: auto;
    padding: 16px 0;
  }

  .brand-logo-img {
    height: 46px;
    max-width: 140px;
  }

  .brand-name {
    font-size: 30px;
    letter-spacing: 6px;
  }

  .brand-subtitle {
    letter-spacing: 5px;
  }
}
</style>