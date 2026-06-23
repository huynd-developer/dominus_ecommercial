<template>
  <div class="auth-page-wrapper admin-bg">
    <div class="auth-card">
      <div class="brand-logo-area">
        <img
          src="@/assets/logo.png"
          alt="Admin Logo"
          class="brand-logo-img"
        />
        <h1 class="brand-title">DOMINUS INTERNAL</h1>
      </div>

      <h2 class="auth-subtitle">Cổng đăng nhập Nội bộ</h2>

      <transition name="fade-slide">
        <div v-if="errorMessage" class="error-alert-banner">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
          <span class="error-message-text">{{ errorMessage }}</span>
        </div>
      </transition>

      <LoginForm :is-loading="isLoading" @submit-login="handleEmployeeLogin" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../stores/authStore";
import LoginForm from "../components/LoginForm.vue";

const authStore = useAuthStore();
const router = useRouter();
const isLoading = ref(false);
const errorMessage = ref("");

const handleEmployeeLogin = async (payload: any) => {
  isLoading.value = true;
  errorMessage.value = ""; // Xóa thông báo lỗi cũ

  const result = await authStore.loginEmployee(payload);
  isLoading.value = false;

  if (result.success) {
    router.push("/admin/dashboard");
  } else {
    errorMessage.value = result.message;
  }
};
</script>

<style scoped>
.auth-page-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  font-family: "Inter", system-ui, -apple-system, sans-serif;
  padding: 20px; /* ĐÃ SỬA: từ 200px về 20px để tránh vỡ layout */
}
.admin-bg {
  background-color: #f0f2f5; /* Nền xám nhạt đặc trưng cho trang quản trị */
}
.auth-card {
  background: #ffffff;
  width: 100%;
  max-width: 460px;
  padding: 40px 32px;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.02);
  border: 1px solid #e4e7ec;
}
.brand-logo-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 24px;
}
.brand-logo-img {
  width: 120px;
  height: auto;
  object-fit: contain;
  margin-bottom: 12px;
  mix-blend-mode: multiply;
}
.brand-title {
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 2px;
  color: #475467;
  font-weight: 600;
}
.auth-subtitle {
  font-size: 24px;
  font-weight: 700;
  color: #101828;
  text-align: center;
  margin-bottom: 24px;
}

/* Banner báo lỗi dành cho cổng Admin */
.error-alert-banner {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  background-color: #fff1f0;
  border: 1px solid #ffa39e;
  border-left: 4px solid #f5222d;
  color: #cf1322;
  padding: 12px 14px;
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 20px;
  text-align: left;
  line-height: 1.4;
  box-shadow: 0 2px 8px rgba(207, 19, 34, 0.05);
}
.error-alert-banner svg {
  flex-shrink: 0;
  margin-top: 2px;
  color: #f5222d;
}
.error-message-text {
  font-weight: 500;
}

/* Animation mượt mà */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.25s ease-out;
}
.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>