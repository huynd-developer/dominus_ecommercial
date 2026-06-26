<template>
  <div class="shop-layout bg-light min-vh-100">
    <header class="sticky-top bg-white border-bottom shadow-sm">
      <ShopHeader />
      <ShopNavbar />
    </header>

    <main class="shop-main">
      <router-view />
    </main>

    <ShopFooter />
  </div>
</template>

<script setup lang="ts">
import ShopHeader from './ShopHeader.vue';
import ShopNavbar from './ShopNavbar.vue';
import ShopFooter from './ShopFooter.vue';
</script>

<style scoped>
.shop-layout {
  color: #0a192f;
}

.shop-main {
  min-height: 70vh;
}
</style>