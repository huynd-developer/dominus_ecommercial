<template>
  <div class="toast-container position-fixed bottom-0 end-0 p-3 z-3">
    <transition-group name="toast-fade">
      <div 
        v-for="toast in toasts" 
        :key="toast.id" 
        :class="['custom-toast d-flex align-items-center gap-3 p-3 rounded-3 shadow-lg mb-2', toast.type]"
      >
        <i :class="['bi', toast.type === 'success' ? 'bi-check-circle-fill text-success' : 'bi-exclamation-triangle-fill text-warning']"></i>
        <span class="text-xs fw-medium">{{ toast.message }}</span>
        <button @click="removeToast(toast.id)" class="btn-close btn-close-white ms-auto text-xs" style="scale: 0.8;"></button>
      </div>
    </transition-group>
  </div>
</template>

<script setup>
import { ref } from 'vue';

const toasts = ref([]);
let counter = 0;

// Hàm kích hoạt Toast (Sẽ được gọi từ bên ngoài)
const show = (message, type = 'success') => {
  const id = counter++;
  toasts.value.push({ id, message, type });

  // Tự động xóa sau 3 giây
  setTimeout(() => {
    removeToast(id);
  }, 3000);
};

const removeToast = (id) => {
  toasts.value = toasts.value.filter(t => t.id !== id);
};

// Xuất hàm này ra ngoài để component cha (App.vue) có thể tóm được nó
defineExpose({ show });
</script>

<style scoped>
.toast-container { max-width: 350px; }
.custom-toast { background: #111625; color: white; border: 1px solid rgba(255, 255, 255, 0.1); min-width: 280px; }
.custom-toast.warning { border-left: 4px solid #c59346; }
.custom-toast.success { border-left: 4px solid #198754; }

/* Hiệu ứng mượt mà */
.toast-fade-enter-active, .toast-fade-leave-active { transition: all 0.3s ease; }
.toast-fade-enter-from { opacity: 0; transform: translateY(30px) scale(0.9); }
.toast-fade-leave-to { opacity: 0; transform: translateX(50px); }
</style>