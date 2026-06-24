<!-- frontend/src/common/components/GlobalToast.vue -->
<template>
  <!-- Vùng chứa cố định ở góc trên bên phải, z-index cực cao để đè mọi Modal/Offcanvas -->
  <div class="toast-wrapper">
    <TransitionGroup name="toast-slide" tag="div" class="d-flex flex-column gap-2">
      <div 
        v-for="toast in appStore.toasts" 
        :key="toast.id"
        class="custom-toast shadow-lg d-flex align-items-center p-3"
        :class="getToastClass(toast.type)"
      >
        <!-- Icon theo trạng thái -->
        <div class="toast-icon me-3">
          <i v-if="toast.type === 'success'" class="bi bi-check-circle-fill text-success fs-4"></i>
          <i v-else-if="toast.type === 'error'" class="bi bi-exclamation-triangle-fill text-danger fs-4"></i>
          <i v-else class="bi bi-info-circle-fill text-warning fs-4"></i>
        </div>
        
        <!-- Nội dung -->
        <div class="flex-grow-1">
          <h6 class="mb-0 fw-bold text-dark">{{ getToastTitle(toast.type) }}</h6>
          <span class="small text-muted fw-medium">{{ toast.message }}</span>
        </div>

        <!-- Nút Đóng -->
        <button 
          @click="appStore.removeToast(toast.id)" 
          class="btn btn-sm btn-link text-muted p-0 ms-3 shadow-none border-0"
        >
          <i class="bi bi-x-lg fs-5"></i>
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup lang="ts">
import { useAppStore } from '@/common/store/app.store';

const appStore = useAppStore();

const getToastTitle = (type: string) => {
  if (type === 'success') return 'Thành công';
  if (type === 'error') return 'Đã xảy ra lỗi';
  return 'Thông báo';
};

const getToastClass = (type: string) => {
  if (type === 'success') return 'border-start border-4 border-success';
  if (type === 'error') return 'border-start border-4 border-danger';
  return 'border-start border-4 border-warning';
};
</script>

<style scoped>
/* Container cố định góc phải màn hình */
.toast-wrapper {
  position: fixed;
  top: 24px;
  right: 24px;
  z-index: 99999; /* Đảm bảo luôn nằm trên cùng */
  pointer-events: none; /* Xuyên click ở khoảng trống */
  width: 320px;
  max-width: calc(100vw - 48px);
}

/* Thẻ thông báo */
.custom-toast {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px); /* Hiệu ứng kính mờ */
  border-radius: 12px;
  pointer-events: auto; /* Cho phép click vào nút Đóng */
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

/* Animation khi thông báo trượt ra/vào */
.toast-slide-enter-active,
.toast-slide-leave-active {
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.toast-slide-enter-from {
  opacity: 0;
  transform: translateX(100%) scale(0.9);
}

.toast-slide-leave-to {
  opacity: 0;
  transform: translateX(100%);
}
</style>