<template>
  <div class="admin-layout">
    <Sidebar />

    <div class="main-wrapper">
      <Header />

      <main class="main-content">
        <slot></slot>
      </main>
    </div>
  </div>
</template>

<script setup>
import Sidebar from '../components/Sidebar.vue'
import Header from '../components/Header.vue'
</script>

<style scoped>
.admin-layout {
  display: flex;
  background-color: #f4f6f9;
  min-height: 100vh;
  width: 100vw;
}
.main-wrapper {
  margin-left: 250px; /* Đẩy nội dung sang phải, không bị Sidebar đè lên */
  width: calc(100% - 250px);
  display: flex;
  flex-direction: column;
}
.main-content {
  padding: 30px;
  flex-grow: 1;
}
</style>