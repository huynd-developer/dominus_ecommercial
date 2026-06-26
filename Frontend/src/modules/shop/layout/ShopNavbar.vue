<template>
  <nav class="shop-navbar bg-white border-top">
    <div class="container">
      <ul class="nav justify-content-center align-items-center flex-nowrap overflow-auto h-100">
        <li class="nav-item">
          <RouterLink to="/brands" class="nav-link menu-link">Thương hiệu</RouterLink>
        </li>

        <li class="nav-item">
          <RouterLink to="/gender" class="nav-link menu-link">Giới tính</RouterLink>
        </li>

        <li class="nav-item">
          <RouterLink to="/niche" class="nav-link menu-link">Niche</RouterLink>
        </li>

        <li class="nav-item">
          <RouterLink to="/decant-10ml" class="nav-link menu-link">Chiết 10ml</RouterLink>
        </li>

        <li class="nav-item">
          <RouterLink to="/flash-sale" class="nav-link menu-link flash-sale-link">
            Flash Sale
          </RouterLink>
        </li>
      </ul>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router';
</script>

<style scoped>
.shop-navbar {
  height: 44px;
}

.menu-link {
  color: #0a192f;
  font-size: 15px;
  font-weight: 600;
  padding: 10px 28px;
  white-space: nowrap;
  transition: color 0.2s ease;
}

.menu-link:hover {
  color: #bd9a5f;
}

.flash-sale-link {
  color: #dc3545;
  font-weight: 800;
}

.flash-sale-link:hover {
  color: #b02a37;
}

@media (max-width: 767.98px) {
  .menu-link {
    padding-left: 18px;
    padding-right: 18px;
  }
}
</style>