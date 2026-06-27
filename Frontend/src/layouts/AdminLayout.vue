<template>
  <div class="admin-layout">
    <Sidebar />

    <div class="main-wrapper">
      <Header />

      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import Sidebar from '../components/Sidebar.vue';

// ĐÃ SỬA: Xóa đuôi /index.js đi để import đúng file Vue
import Header from '../components/Header.vue'; 
</script>

<style scoped>
.admin-layout {
  display: flex;
  background-color: #f4f6f9;
  min-height: 100vh;
  width: 100vw;
  overflow-x: hidden; /* Tránh xuất hiện thanh cuộn ngang rác */
}

.main-wrapper {
  margin-left: 250px; /* Đẩy nội dung né đúng bằng chiều rộng Sidebar */
  width: calc(100% - 250px);
  display: flex;
  flex-direction: column;
}

.main-content {
  padding: 30px;
  flex-grow: 1;
}
</style>