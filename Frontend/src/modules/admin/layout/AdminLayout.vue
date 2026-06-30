<template>
  <div class="admin-layout">
    <TopHeader @collapse-change="isSidebarCollapsed = $event" />

    <main 
      class="main-content" 
      :class="[
        { 'sidebar-collapsed': isSidebarCollapsed },
        isPosPage ? 'pos-no-padding' : 'normal-padding'
      ]"
    >
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRoute } from 'vue-router'; // Import thêm useRoute
import TopHeader from './TopHeader.vue';

const route = useRoute();

// Lấy trạng thái ban đầu từ localStorage
const isSidebarCollapsed = ref(localStorage.getItem("sidebarCollapsed") === "1");

// Kiểm tra xem trang hiện tại có phải là POS không
const isPosPage = computed(() => route.path.includes('/pos'));
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background-color: #f8fafc;
  /* Ép layout thành cột để tự chia tỷ lệ */
  display: flex;
  flex-direction: column;
}

.main-content {
  /* Chìa khóa đây: Tự động điền đầy khoảng trống còn lại, bỏ qua mọi tính toán pixel */
  flex: 1; 
  margin-left: 260px;
  transition: margin-left 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow-y: auto;
}

.main-content.sidebar-collapsed {
  margin-left: 0;
}

.normal-padding {
  padding: 1.5rem;
}

.pos-no-padding {
  padding: 0;
  /* Xóa dòng height đi, vì flex: 1 ở trên đã lo việc ép khung rồi */
  overflow: hidden; /* Cấm cuộn hoàn toàn ở trang POS */
}
</style>