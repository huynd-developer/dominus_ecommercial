<template>
  <div class="auth-page-wrapper">
    <div class="auth-card">
      <div class="brand-logo-area">
        <img
          src="@/assets/logo.png"
          alt="Logo Thương Hiệu"
          class="brand-logo-img"
        />
        <h1 class="brand-title">DOMINUS</h1>
      </div>

      <h2 class="auth-subtitle">Tạo tài khoản mới</h2>

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

      <RegisterForm :is-loading="isLoading" @submit-register="handleRegister" />

      <p class="auth-switch-text">
        Đã có tài khoản?
        <router-link to="/login" class="auth-link">Đăng nhập</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../stores/authStore";
import RegisterForm from "../components/RegisterForm.vue";

const authStore = useAuthStore();
const router = useRouter();
const isLoading = ref(false);
const errorMessage = ref("");

const handleRegister = async (payload: any) => {
  isLoading.value = true;
  errorMessage.value = ""; // Xóa lỗi cũ trước khi gửi request mới

  const result = await authStore.registerCustomer(payload);
  isLoading.value = false;

  if (result.success) {
    alert("Đăng ký thành công!");
    router.push("/login");
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
  background-color: #fcfcfd;
  font-family: "Inter", system-ui, -apple-system, sans-serif;
  padding: 20px; /* Chuẩn 20px để mobile hiển thị đẹp */
}
.auth-card {
  background: #ffffff;
  width: 100%;
  max-width: 460px;
  padding: 40px 32px;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.03), 0 1px 2px rgba(0, 0, 0, 0.02);
  border: 1px solid #f0f0f2;
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
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: 2px;
  color: #0f2c59;
  font-weight: 600;
}
.auth-subtitle {
  font-size: 24px;
  font-weight: 700;
  color: #101828;
  text-align: center;
  margin-bottom: 24px;
}

/* Banner báo lỗi tinh tế hơn */
.error-alert-banner {
  display: flex;
  align-items: flex-start; /* Đổi thành flex-start đề phòng chữ quá dài xuống dòng */
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
  margin-top: 2px; /* Căn chỉnh icon khít với dòng chữ đầu tiên */
  color: #f5222d;
}
.error-message-text {
  font-weight: 500;
}

/* Animation hiệu ứng mượt mà */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.25s ease-out;
}
.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.auth-switch-text {
  font-size: 14px;
  color: #475467;
  text-align: center;
  margin-top: 24px;
}
.auth-link {
  color: #0f2c59;
  font-weight: 600;
  text-decoration: none;
}
.auth-link:hover {
  text-decoration: underline;
}
</style>