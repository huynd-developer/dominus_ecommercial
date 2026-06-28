<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';

const handleGlobalBFCache = (event: PageTransitionEvent) => {
  // Nếu trang web bị lôi ra từ bộ nhớ đệm lịch sử đóng băng (ấn nút Back/Forward)
  if (event.persisted) {
    const token = localStorage.getItem('token');
    const isInsideAdmin = window.location.pathname.startsWith('/admin');
    const isAtAdminLogin = window.location.pathname.includes('/admin/login');

    // Nếu đang ở trong các trang admin quản lý nhưng kiểm tra thấy KHÔNG CÓ TOKEN (đã đăng xuất)
    if (isInsideAdmin && !token && !isAtAdminLogin) {
      // Ép trình duyệt F5 tải lại hoàn toàn để Router Guard đá bay về trang Login chuẩn chỉ
      window.location.reload();
    }
  }
};

onMounted(() => {
  // Bật radar quét nút Back/Forward của trình duyệt trên toàn hệ thống
  window.addEventListener('pageshow', handleGlobalBFCache);
});

onUnmounted(() => {
  window.removeEventListener('pageshow', handleGlobalBFCache);
});
</script>

<style>
/* Reset CSS cơ bản cho toàn bộ dự án */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}
body {
  background-color: #f8f9fa;
}
</style>