// frontend/src/common/store/app.store.ts
import { defineStore } from 'pinia';
import { ref } from 'vue';
// Đã xóa import 'element-plus' vì chúng ta dùng GlobalToast Custom siêu mượt

export interface ToastMessage {
  id: number;
  message: string;
  type: 'success' | 'error' | 'warning';
}

export const useAppStore = defineStore('app', () => {
  // 1. STATE 
  const globalLoading = ref(false);
  const isDark = ref(localStorage.getItem('theme') === 'dark');
  const sidebar = ref({
    opened: localStorage.getItem('sidebar_status') !== 'closed',
    withoutAnimation: false,
  });

  // --- THÊM STATE QUẢN LÝ THÔNG BÁO (TOAST) ---
  const toasts = ref<ToastMessage[]>([]);
  let toastIdCounter = 0;

  // 2. ACTIONS 
  const startLoading = () => {
    globalLoading.value = true;
  };

  const stopLoading = () => {
    globalLoading.value = false;
  };

  // --- LOGIC HIỂN THỊ THÔNG BÁO CUSTOM ---
  const addToast = (message: string, type: 'success' | 'error' | 'warning') => {
    const id = ++toastIdCounter;
    toasts.value.push({ id, message, type });

    // Tự động ẩn thông báo sau 3.5 giây
    setTimeout(() => {
      removeToast(id);
    }, 3500);
  };

  const removeToast = (id: number) => {
    toasts.value = toasts.value.filter((t) => t.id !== id);
  };

  // Ghi đè hàm cũ, chuyển data vào mảng toasts thay vì gọi ElMessage
  const notifySuccess = (message: string) => {
    addToast(message, 'success');
  };

  const notifyError = (message: string) => {
    addToast(message, 'error');
  };
  
  const notifyWarning = (message: string) => {
    addToast(message, 'warning');
  };

  const toggleTheme = () => {
    isDark.value = !isDark.value;
    const html = document.documentElement;
    if (isDark.value) {
      html.classList.add('dark');
      localStorage.setItem('theme', 'dark');
    } else {
      html.classList.remove('dark');
      localStorage.setItem('theme', 'light');
    }
  };

  // 3. RETURN lại những gì muốn dùng ở Component
  return {
    globalLoading,
    isDark,
    sidebar,
    toasts, // Trả về mảng toasts để GlobalToast.vue vẽ ra màn hình
    startLoading,
    stopLoading,
    notifySuccess,
    notifyError,
    notifyWarning,
    removeToast,
    toggleTheme
  };
});