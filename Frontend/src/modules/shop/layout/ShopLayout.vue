<template>
  <div class="shop-layout min-vh-100">
    <header class="sticky-top shop-sticky-header">
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

<style>
:root {
  --aura-gold: #bd9a5f;
  --aura-gold-hover: #a3824d;
  --aura-navy: #051024;
  --aura-black: #1a1a1a;
  --aura-cream: #fcfcfc;
  --aura-muted: #777777;
  --aura-border: rgba(26, 26, 26, 0.08);

  --aura-sans: "Segoe UI", Arial, sans-serif;
  --aura-serif: Georgia, "Times New Roman", serif;
}

* {
  box-sizing: border-box;
}

html,
body {
  margin: 0;
  font-family: var(--aura-sans);
  background: var(--aura-cream);
  color: var(--aura-black);
  -webkit-font-smoothing: antialiased;
  text-rendering: optimizeLegibility;
}

.shop-layout {
  background: var(--aura-cream);
}

.shop-sticky-header {
  z-index: 1040;
  box-shadow: 0 8px 24px rgba(5, 16, 36, 0.08);
}

.shop-main {
  min-height: 70vh;
}
</style>