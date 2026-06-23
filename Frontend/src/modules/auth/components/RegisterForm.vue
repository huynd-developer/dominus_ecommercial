<template>
  <form @submit.prevent="submitForm" class="modern-form">
    <div class="form-group">
      <label class="form-label">Tên đăng nhập (Username)</label>
      <div class="input-icon-wrapper">
        <span class="prefix-icon">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="#a0a0a0"
            stroke-width="2"
          >
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
            <circle cx="12" cy="7" r="4"></circle>
          </svg>
        </span>
        <input
          type="text"
          v-model="formData.username"
          placeholder="Ví dụ: hoaile2k"
          required
          class="form-control-input"
        />
      </div>
    </div>

    <div class="form-group">
      <label class="form-label">Họ và tên</label>
      <div class="input-icon-wrapper">
        <span class="prefix-icon">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="#a0a0a0"
            stroke-width="2"
          >
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
          </svg>
        </span>
        <input
          type="text"
          v-model="formData.fullName"
          placeholder="Nhập đầy đủ họ tên..."
          required
          class="form-control-input"
        />
      </div>
    </div>

    <div class="form-group">
      <label class="form-label">Email Address</label>
      <div class="input-icon-wrapper">
        <span class="prefix-icon">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="#a0a0a0"
            stroke-width="2"
          >
            <path
              d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"
            ></path>
            <polyline points="22,6 12,13 2,6"></polyline>
          </svg>
        </span>
        <input
          type="email"
          v-model="formData.email"
          placeholder="example@gmail.com"
          required
          class="form-control-input"
        />
      </div>
    </div>

    <div class="form-group">
      <label class="form-label">Phone Number</label>
      <div class="input-icon-wrapper">
        <div class="phone-country-code">
          <span class="flag-icon">🇻🇳</span>
          <span>+84</span>
        </div>
        <input
          type="text"
          v-model="formData.phone"
          placeholder="Nhập số điện thoại..."
          required
          class="form-control-input phone-input-spacing"
        />
      </div>
    </div>

    <div class="form-group">
      <label class="form-label">Password</label>
      <div class="input-icon-wrapper">
        <span class="prefix-icon">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="#a0a0a0"
            stroke-width="2"
          >
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
            <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
          </svg>
        </span>
        <input
          :type="showPassword ? 'text' : 'password'"
          v-model="formData.password"
          placeholder="Tối thiểu 6 ký tự..."
          required
          class="form-control-input"
        />
        <button
          type="button"
          class="suffix-toggle-btn"
          @click="showPassword = !showPassword"
        >
          <svg
            v-if="showPassword"
            xmlns="http://www.w3.org/2000/svg"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="#666"
            stroke-width="2"
          >
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
            <circle cx="12" cy="12" r="3"></circle>
          </svg>
          <svg
            v-else
            xmlns="http://www.w3.org/2000/svg"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="#a0a0a0"
            stroke-width="2"
          >
            <path
              d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"
            ></path>
            <line x1="1" y1="1" x2="23" y2="23"></line>
          </svg>
        </button>
      </div>
    </div>

    <button type="submit" class="btn-submit-primary" :disabled="isLoading">
      <span v-if="isLoading" class="spinner-loading"></span>
      <span v-else>Sign Up</span>
    </button>
  </form>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";

defineProps({ isLoading: { type: Boolean, default: false } });
const emit = defineEmits(["submit-register"]);
const showPassword = ref(false);

const formData = reactive({
  username: "",
  fullName: "",
  email: "",
  phone: "",
  password: "",
});

const submitForm = () => {
  emit("submit-register", { ...formData });
};
</script>

<style scoped>
.modern-form .form-group {
  margin-bottom: 20px;
}
.form-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #344054;
  margin-bottom: 6px;
  text-align: left;
}
.input-icon-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}
.prefix-icon {
  position: absolute;
  left: 14px;
  display: flex;
  align-items: center;
}
.form-control-input {
  width: 100%;
  height: 46px;
  padding: 10px 14px 10px 42px;
  border: 1px solid #d0d5dd;
  border-radius: 8px;
  font-size: 14px;
  color: #101828;
  background-color: #fff;
  transition: all 0.2s ease;
}
.form-control-input:focus {
  outline: none;
  border-color: #0f2c59;
  box-shadow: 0 0 0 4px rgba(15, 44, 89, 0.08);
}
.phone-country-code {
  position: absolute;
  left: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  font-weight: 500;
  color: #344054;
  border-right: 1px solid #d0d5dd;
  padding-right: 8px;
  height: 24px;
}
.phone-input-spacing {
  padding-left: 76px !important;
}
.suffix-toggle-btn {
  position: absolute;
  right: 14px;
  background: none;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  padding: 0;
}
.btn-submit-primary {
  width: 100%;
  height: 46px;
  background-color: #0f2c59;
  color: #ffffff;
  border: 1px solid #0f2c59;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.btn-submit-primary:hover {
  background-color: #0a1f3f;
}
.btn-submit-primary:disabled {
  background-color: #98a2b3;
  border-color: #98a2b3;
  cursor: not-allowed;
}
.spinner-loading {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: #fff;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
