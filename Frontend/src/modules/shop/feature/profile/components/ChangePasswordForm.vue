<template>
  <div class="card border-0 shadow-sm">
    <div class="card-header bg-white border-0 py-3">
      <h5 class="mb-0 fw-bold">Đổi mật khẩu</h5>
    </div>

    <div class="card-body">
      <div class="alert alert-light border small mb-4">
        Mật khẩu mới phải có chữ hoa, chữ thường, số, ký tự đặc biệt, từ 8 đến
        50 ký tự và không chứa khoảng trắng.
      </div>

      <div class="row g-3">
        <div class="col-12">
          <label class="form-label fw-semibold">Mật khẩu cũ</label>
          <input
            v-model="store.passwordForm.oldPassword"
            type="password"
            class="form-control"
            placeholder="Nhập mật khẩu cũ"
          />
        </div>

        <div class="col-12">
          <label class="form-label fw-semibold">Mật khẩu mới</label>
          <input
            v-model="store.passwordForm.newPassword"
            type="password"
            class="form-control"
            placeholder="Nhập mật khẩu mới"
          />
        </div>

        <div class="col-12">
          <label class="form-label fw-semibold">Xác nhận mật khẩu</label>
          <input
            v-model="store.passwordForm.confirmPassword"
            type="password"
            class="form-control"
            placeholder="Nhập lại mật khẩu mới"
            @keyup.enter="store.changePassword()"
          />
        </div>
      </div>

      <div class="text-end mt-4">
        <button
          class="btn btn-dark px-4"
          :disabled="store.passwordLoading"
          @click="store.changePassword()"
        >
          <span
            v-if="store.passwordLoading"
            class="spinner-border spinner-border-sm me-2"
          ></span>
          Đổi mật khẩu
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useCustomerProfileStore } from "../stores/customerProfile.store";

const store = useCustomerProfileStore();
</script>