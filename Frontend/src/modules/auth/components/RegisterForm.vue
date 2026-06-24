<template>
  <div class="auth-card shadow-lg border-0 bg-white">
    <div class="text-center mb-4">
      <div class="brand-logo mb-2">
        <img src="@/assets/logo.png" alt="Aura Perfume Logo" class="logo-img" />
      </div>
      <div class="divider-line my-2"></div>
      <h4 class="form-title text-secondary fw-semibold m-0">Tạo tài khoản</h4>
    </div>

    <form @submit.prevent="handleRegister">
      <div class="mb-3">
        <label class="form-label text-dark fw-semibold small">Họ và tên</label>
        <div class="input-group-custom" :class="{ 'has-error': validationErrors.name }">
          <span class="input-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16"><path d="M8 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6zm2-3a2 2 0 1 1-4 0 2 2 0 0 1 4 0zm4 8c0 1-1 1-1 1H3s-1 0-1-1 1-4 6-4 6 3 6 4zm-1-.004c-.001-.246-.154-.986-.832-1.664C11.516 10.68 10.289 10 8 10c-2.29 0-3.516.68-4.168 1.332-.678.678-.83 1.418-.832 1.664h10z"/></svg>
          </span>
          <input v-model="form.name" type="text" class="form-control-custom" placeholder="Nhập họ và tên của bạn" />
        </div>
        <span class="field-error-text" v-if="validationErrors.name">{{ validationErrors.name }}</span>
      </div>

      <div class="mb-3">
        <label class="form-label text-dark fw-semibold small">Email</label>
        <div class="input-group-custom" :class="{ 'has-error': validationErrors.email }">
          <span class="input-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16"><path d="M0 4a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2zm2-1a1 1 0 0 0-1 1v.217l7 4.2 7-4.2V4a1 1 0 0 0-1-1zm13 2.383-4.708 2.825L15 11.105zm-.034 6.876-5.64-3.471L8 9.583l-1.326-.795-5.64 3.47A1 1 0 0 0 2 13h12a1 1 0 0 0 .966-.741zM1 11.105l4.708-2.897L1 5.383z"/></svg>
          </span>
          <input v-model="form.email" type="email" class="form-control-custom" placeholder="name@example.com" />
        </div>
        <span class="field-error-text" v-if="validationErrors.email">{{ validationErrors.email }}</span>
      </div>

      <div class="mb-3">
        <label class="form-label text-dark fw-semibold small">Số điện thoại</label>
        <div class="input-group-custom" :class="{ 'has-error': validationErrors.phone }">
          <span class="input-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16"><path d="M3.654 1.328a.678.678 0 0 0-.58-.506L1.51.714A.678.678 0 0 0 .79 1.166l-.718 3.123a.678.678 0 0 0 .413.75l3.123 1.25a.678.678 0 0 0 .75-.413l1.25-3.123a.678.678 0 0 0-.413-.75l-3.122-1.25zM1.884.511a1.745 1.745 0 0 1 1.492 1.3l.534 2.2a1.745 1.745 0 0 1-.346 1.557l-1.03 1.256A11.234 11.234 0 0 0 6.64 10.93l1.256-1.03a1.745 1.745 0 0 1 1.557-.346l2.2.534a1.745 1.745 0 0 1 1.3 1.492l.51 2.053a1.745 1.745 0 0 1-1.165 2.046l-2.053.51a1.745 1.745 0 0 1-2.046-1.165l-.51-2.053a1.745 1.745 0 0 1 1.165-2.046l2.053-.51z"/></svg>
          </span>
          <input v-model="form.phone" type="text" class="form-control-custom" placeholder="Nhập số điện thoại" />
        </div>
        <span class="field-error-text" v-if="validationErrors.phone">{{ validationErrors.phone }}</span>
      </div>

      <div class="mb-3">
        <label class="form-label text-dark fw-semibold small">Mật khẩu</label>
        <div class="input-group-custom" :class="{ 'has-error': validationErrors.password }">
          <span class="input-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor" viewBox="0 0 16 16"><path d="M8 1a2 2 0 0 1 2 2v4H6V3a2 2 0 0 1 2-2zm3 6V3a3 3 0 0 0-6 0v4a2 2 0 0 0-2 2v5a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2zM5 8h6a1 1 0 0 1 1 1v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z"/></svg>
          </span>
          <input v-model="form.password" type="password" class="form-control-custom" placeholder="Ít nhất 6 ký tự" />
        </div>
        <span class="field-error-text" v-if="validationErrors.password">{{ validationErrors.password }}</span>
      </div>

      <div v-if="generalError" class="alert alert-danger-custom text-center py-2 mb-3">{{ generalError }}</div>
      <div v-if="successMessage" class="alert alert-success-custom text-center py-2 mb-3">{{ successMessage }}</div>

      <button type="submit" class="btn-secure-register w-100 mt-2" :disabled="loading">
        {{ loading ? 'Processing...' : 'Create Account' }}
      </button>
    </form>

    <div class="text-center mt-4 pt-2 border-top">
      <p class="footer-link m-0">
        Already have an account? <router-link to="/login" class="text-dark-blue fw-semibold decoration-none">Login Securely</router-link>
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useAuthStore } from '../stores/authStore';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();

const form = reactive({ name: '', email: '', phone: '', password: '' });
const loading = ref(false);
const successMessage = ref('');
const generalError = ref('');
const validationErrors = ref<Record<string, string>>({});

const handleRegister = async () => {
  loading.value = true;
  generalError.value = '';
  successMessage.value = '';
  validationErrors.value = {};

  const result = await authStore.registerCustomer(form);

  if (result.success) {
    successMessage.value = result.message;
    setTimeout(() => {
      router.push('/login');
    }, 2000);
  } else {
    if (result.validationErrors) {
      validationErrors.value = result.validationErrors;
    } else {
      generalError.value = result.message;
    }
  }
  loading.value = false;
};
</script>

<style scoped>
.auth-card {
  width: 100%;
  max-width: 440px;
  padding: 35px 35px;
  border-radius: 16px !important;
}
.text-dark-blue { color: #0a1931; }
.brand-title { color: #0a1931; font-size: 22px; letter-spacing: 2px;}
.divider-line { height: 1px; width: 40px; background-color: #d1d5db; margin: 0 auto; }
.form-title { font-size: 18px; color: #1f3c4d; }

.input-group-custom { position: relative; display: flex; align-items: center; }
.input-icon { position: absolute; left: 14px; color: #9ca3af; display: flex; align-items: center; }
.form-control-custom {
  width: 100%; padding: 11px 16px 11px 42px; border: 1px solid #e5e7eb;
  border-radius: 8px; font-size: 14.5px; color: #1f2937; background-color: #fcfcfc; transition: all 0.2s;
}
.form-control-custom:focus {
  outline: none; border-color: #0a1931; background-color: #fff;
  box-shadow: 0 0 0 3px rgba(10, 25, 49, 0.08);
}

/* Đổi màu viền sang đỏ nếu input dính validation error */
.has-error .form-control-custom {
  border-color: #dc2626;
  background-color: #fef2f2;
}
.field-error-text { color: #dc2626; font-size: 12px; display: block; margin-top: 3px; text-align: left; padding-left: 2px; }

.alert-danger-custom { background-color: #fef2f2; border: 1px solid #fee2e2; color: #dc2626; border-radius: 8px; font-size: 13.5px; }
.alert-success-custom { background-color: #f0fdf4; border: 1px solid #dcfce7; color: #16a34a; border-radius: 8px; font-size: 13.5px; }

.btn-secure-register {
  background-color: #0a1931; color: #fff; border: none; padding: 12px;
  border-radius: 8px; font-weight: 600; font-size: 15px; transition: all 0.25s;
}
.btn-secure-register:hover { background-color: #15305b; transform: translateY(-1px); }
.btn-secure-register:disabled { background-color: #9ca3af; transform: none; cursor: not-allowed; }

.footer-link { font-size: 13.5px; }
.decoration-none { text-decoration: none; }
.decoration-none:hover { text-decoration: underline; }
.logo-img {
  width: 80px;          /* Chiều rộng của logo (ông tùy chỉnh theo ý muốn) */
  height: 80px;         /* Chiều cao của logo */
  object-fit: contain;  /* Giữ nguyên tỉ lệ ảnh, không lo bị méo góc */
  border-radius: 8px;   /* Bo góc nhẹ cho logo (nếu logo hình tròn thì đổi thành 50%) */
  transition: transform 0.3s ease;
}
</style>