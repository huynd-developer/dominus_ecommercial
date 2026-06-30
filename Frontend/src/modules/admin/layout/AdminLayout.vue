<template>
  <div class="admin-layout">
    <TopHeader />

    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
// 🌟 Nhớ trỏ đúng đường dẫn tới file TopHeader.vue của ông nhé
import TopHeader from './TopHeader.vue';
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background-color: #f8fafc; /* Màu nền nhẹ nhàng cho trang Admin */
}

.main-content {
  /* Để chừa khoảng trống cho cái TopHeader sticky nếu cần */
  padding: 1.5rem; 
}
</style>