<template>
  <div class="auth-card shadow-lg border-0 bg-white">
    <div class="text-center mb-4">
      <div class="brand-logo mb-2">
        <img src="@/assets/logo.png" alt="Aura Perfume Logo" class="logo-img" />
      </div>
      <div class="divider-line my-2"></div>
      <h2 class="form-title text-secondary fw-semibold m-0">
        {{ isAdminMode ? 'Hệ thống đăng nhập của nhân viên' : 'Đăng nhập' }}
      </h2>
    </div>

    <form @submit.prevent="handleLogin" novalidate>
      <div class="mb-3">
        <label class="form-label text-dark fw-semibold small"> Email</label>
        <div class="input-group-custom" :class="{ 'has-error': validationErrors.email }">
          <span class="input-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
              <path d="M0 4a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2zm2-1a1 1 0 0 0-1 1v.217l7 4.2 7-4.2V4a1 1 0 0 0-1-1zm13 2.383-4.708 2.825L15 11.105zm-.034 6.876-5.64-3.471L8 9.583l-1.326-.795-5.64 3.47A1 1 0 0 0 2 13h12a1 1 0 0 0 .966-.741zM1 11.105l4.708-2.897L1 5.383z"/>
            </svg>
          </span>
          <input v-model="credentials.email" type="email" class="form-control-custom" placeholder="Nhập email của bạn" required />
        </div>
        <span class="field-error-text" v-if="validationErrors.email">{{ validationErrors.email }}</span>
      </div>

      <div class="mb-3">
        <label class="form-label text-dark fw-semibold small">Mật khẩu</label>
        <div class="input-group-custom" :class="{ 'has-error': validationErrors.password }">
          <span class="input-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16">
              <path d="M8 1a2 2 0 0 1 2 2v4H6V3a2 2 0 0 1 2-2zm3 6V3a3 3 0 0 0-6 0v4a2 2 0 0 0-2 2v5a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2zM5 8h6a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z"/>
            </svg>
          </span>
          <input v-model="credentials.password" :type="showPassword ? 'text' : 'password'" class="form-control-custom pe-5" placeholder="Nhập mật khẩu của bạn" required />
          <span class="password-toggle" @click="showPassword = !showPassword">
            <svg v-if="showPassword" xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 16 16">
              <path d="M13.359 11.238C15.06 9.72 16 8 16 8s-3-5.5-8-5.5a7.028 7.028 0 0 0-2.79.588l.77.771A5.944 5.944 0 0 1 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.134 13.134 0 0 1 14.828 8c-.058.087-.122.183-.195.288l.338.338zm-.652 1.144-.739-.739A5.94 5.94 0 0 1 8 12.5c-2.12 0-3.879-1.168-5.168-2.457A13.134 13.134 0 0 1 1.172 8c.058-.087.122-.183.195-.288l-.338-.338A14.33 14.33 0 0 0 0 8s3 5.5 8 5.5a7.028 7.028 0 0 0 2.79-.588l-.77-.771z"/>
              <path d="M5.525 7.646a2.5 2.5 0 0 0 2.829 2.829l-2.83-2.829zm4.95.708-2.829-2.83a2.5 2.5 0 0 0 2.829 2.83zm-1.1 1.1a3.5 3.5 0 0 1-4.95-4.95l4.95 4.95zM0 1.146l4 .4 4-4-4-4-4 4 4 4z"/>
            </svg>
            <svg v-else xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="currentColor" viewBox="0 0 16 16">
              <path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8zM1.173 8a13.133 13.133 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.133 13.133 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.119 12.5 8 12.5c-2.12 0-3.879-1.168-5.168-2.457A13.134 13.134 0 0 1 1.172 8z"/>
              <path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5zM4.5 8a3.5 3.5 0 1 1 7 0 3.5 3.5 0 0 1-7 0z"/>
            </svg>
          </span>
        </div>
        <span class="field-error-text" v-if="validationErrors.password">{{ validationErrors.password }}</span>
      </div>

      <div v-if="errorMessage" class="alert alert-danger-custom text-center py-2 mb-3" role="alert">
        {{ errorMessage }}
      </div>

      <button type="submit" class="btn-secure-login w-100 mt-2 d-flex align-items-center justify-content-center" :disabled="loading">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" class="me-2" viewBox="0 0 16 16">
          <path d="M8 1a2 2 0 0 1 2 2v4H6V3a2 2 0 0 1 2-2zm3 6V3a3 3 0 0 0-6 0v4a2 2 0 0 0-2 2v5a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2z"/>
        </svg>
        <span>{{ loading ? 'Authenticating...' : 'Đăng nhập' }}</span>
      </button>
    </form>

    <div class="text-center mt-4 pt-2 border-top">
      <p v-if="isAdminMode" class="footer-link text-muted m-0">Forgot Password? Contact IT Dept</p>
      <p v-else class="footer-link m-0">
        New here? <router-link to="/register" class="text-dark-blue fw-semibold decoration-none">Create an Account</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useAuthStore } from '../stores/authStore';
import { useRouter } from 'vue-router';

const props = defineProps({
  isAdminMode: { type: Boolean, default: false }
});

const authStore = useAuthStore();
const router = useRouter();

const credentials = reactive({ email: '', password: '' });
const loading = ref(false);
const errorMessage = ref('');
const showPassword = ref(false); 

// Bổ sung: Biến lưu mảng lỗi từng dòng
const validationErrors = ref<Record<string, string>>({});

const handleLogin = async () => {
  loading.value = true;
  errorMessage.value = '';
  validationErrors.value = {}; // Bổ sung: Reset lỗi khi ấn login lại
  
  const result = props.isAdminMode 
    ? await authStore.loginEmployee(credentials)
    : await authStore.loginCustomer(credentials);

  if (result.success) {
    if (props.isAdminMode) {
      router.replace('/admin/dashboard');
    } else {
      router.replace('/');
    }
  } else {
    // Bổ sung: Tách luồng lỗi. Lỗi Validation thì gán vào validationErrors, lỗi chung thì gán vào errorMessage
    if (result.validationErrors) {
      validationErrors.value = result.validationErrors;
    } else {
      errorMessage.value = result.message;
    }
  }
  loading.value = false;
};
</script>

<style scoped>
/* Giữ nguyên 100% CSS cũ của ông */
.auth-card { width: 100%; max-width: 440px; padding: 40px 35px; border-radius: 16px !important; }
.text-dark-blue { color: #0a1931; }
.brand-title { color: #0a1931; font-size: 22px; letter-spacing: 2px; }
.divider-line { height: 1px; width: 40px; background-color: #d1d5db; margin: 0 auto; }
.form-title { font-size: 18px; color: #1f3c4d; }
.input-group-custom { position: relative; display: flex; align-items: center; }
.input-icon { position: absolute; left: 14px; color: #9ca3af; display: flex; align-items: center; }
.form-control-custom { width: 100%; padding: 12px 16px 12px 42px; border: 1px solid #e5e7eb; border-radius: 8px; font-size: 15px; color: #1f2937; background-color: #fcfcfc; transition: all 0.2s ease; }
.form-control-custom:focus { outline: none; border-color: #0a1931; background-color: #ffffff; box-shadow: 0 0 0 3px rgba(10, 25, 49, 0.08); }
.password-toggle { position: absolute; right: 14px; color: #9ca3af; cursor: pointer; display: flex; align-items: center; user-select: none; }
.password-toggle:hover { color: #4b5563; }
.alert-danger-custom { background-color: #fef2f2; border: 1px solid #fee2e2; color: #dc2626; border-radius: 8px; font-size: 14px; }
.btn-secure-login { background-color: #0a1931; color: #ffffff; border: none; padding: 13px; border-radius: 8px; font-weight: 600; font-size: 15px; transition: all 0.25s ease; }
.btn-secure-login:hover { background-color: #15305b; transform: translateY(-1px); }
.btn-secure-login:disabled { background-color: #9ca3af; transform: none; cursor: not-allowed; }
.footer-link { font-size: 13.5px; }
.decoration-none { text-decoration: none; }
.decoration-none:hover { text-decoration: underline; }
.logo-img { width: 80px; height: 80px; object-fit: contain; border-radius: 8px; transition: transform 0.3s ease; }

/* Chỉ BỔ SUNG CSS báo lỗi đỏ */
.has-error .form-control-custom {
  border-color: #dc2626;
  background-color: #fef2f2;
}
.field-error-text {
  color: #dc2626;
  font-size: 12px;
  display: block;
  margin-top: 4px;
  text-align: left;
  padding-left: 2px;
}
</style>