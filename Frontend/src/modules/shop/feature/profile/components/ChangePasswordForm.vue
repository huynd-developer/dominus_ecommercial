<template>
  <div class="card border-0 shadow-sm">
    <div class="card-header bg-white border-0 py-3">
      <h5 class="mb-0 fw-bold">Đổi mật khẩu</h5>
    </div>

    <div class="card-body">
      <div class="alert alert-light border small mb-4">
        Mật khẩu mới phải có chữ hoa, chữ thường, số, ít nhất 1 ký tự đặc biệt
        trong <strong>@$!%*?&amp;.#</strong>, từ 8 đến 50 ký tự và không chứa khoảng trắng.
      </div>

      <div
        v-if="passwordSubmitTouched && formError"
        class="alert alert-danger small mb-3"
      >
        {{ formError }}
      </div>

      <div class="row g-3">
        <div class="col-12">
          <label class="form-label fw-semibold">Mật khẩu cũ</label>

          <div class="input-group">
            <input
              v-model="store.passwordForm.oldPassword"
              :type="showOldPassword ? 'text' : 'password'"
              class="form-control"
              :class="{ 'is-invalid': passwordSubmitTouched && oldPasswordError }"
              placeholder="Nhập mật khẩu cũ"
              maxlength="50"
              autocomplete="current-password"
              @keyup.enter="handleChangePassword"
            />

            <button
              type="button"
              class="btn btn-outline-secondary"
              tabindex="-1"
              @click="showOldPassword = !showOldPassword"
            >
              <i
                class="bi"
                :class="showOldPassword ? 'bi-eye-slash' : 'bi-eye'"
              ></i>
            </button>
          </div>

          <div
            v-if="passwordSubmitTouched && oldPasswordError"
            class="invalid-feedback d-block"
          >
            {{ oldPasswordError }}
          </div>
        </div>

        <div class="col-12">
          <label class="form-label fw-semibold">Mật khẩu mới</label>

          <div class="input-group">
            <input
              v-model="store.passwordForm.newPassword"
              :type="showNewPassword ? 'text' : 'password'"
              class="form-control"
              :class="{ 'is-invalid': passwordSubmitTouched && newPasswordError }"
              placeholder="Nhập mật khẩu mới"
              maxlength="50"
              autocomplete="new-password"
              @keyup.enter="handleChangePassword"
            />

            <button
              type="button"
              class="btn btn-outline-secondary"
              tabindex="-1"
              @click="showNewPassword = !showNewPassword"
            >
              <i
                class="bi"
                :class="showNewPassword ? 'bi-eye-slash' : 'bi-eye'"
              ></i>
            </button>
          </div>

          <div
            v-if="passwordSubmitTouched && newPasswordError"
            class="invalid-feedback d-block"
          >
            {{ newPasswordError }}
          </div>
        </div>

        <div class="col-12">
          <label class="form-label fw-semibold">Xác nhận mật khẩu</label>

          <div class="input-group">
            <input
              v-model="store.passwordForm.confirmPassword"
              :type="showConfirmPassword ? 'text' : 'password'"
              class="form-control"
              :class="{
                'is-invalid': passwordSubmitTouched && confirmPasswordError,
              }"
              placeholder="Nhập lại mật khẩu mới"
              maxlength="50"
              autocomplete="new-password"
              @keyup.enter="handleChangePassword"
            />

            <button
              type="button"
              class="btn btn-outline-secondary"
              tabindex="-1"
              @click="showConfirmPassword = !showConfirmPassword"
            >
              <i
                class="bi"
                :class="showConfirmPassword ? 'bi-eye-slash' : 'bi-eye'"
              ></i>
            </button>
          </div>

          <div
            v-if="passwordSubmitTouched && confirmPasswordError"
            class="invalid-feedback d-block"
          >
            {{ confirmPasswordError }}
          </div>
        </div>
      </div>

      <div class="text-end mt-4">
        <button
          class="btn btn-dark px-4"
          :disabled="store.passwordLoading"
          @click="handleChangePassword"
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
import { computed, ref } from "vue";
import { useCustomerProfileStore } from "../stores/customerProfile.store";

const store = useCustomerProfileStore();

const passwordSubmitTouched = ref(false);

const showOldPassword = ref(false);
const showNewPassword = ref(false);
const showConfirmPassword = ref(false);

const PASSWORD_RULE_MESSAGE =
  "Mật khẩu mới phải từ 8 đến 50 ký tự, có chữ hoa, chữ thường, số, ít nhất 1 ký tự đặc biệt trong @$!%*?&.# và không chứa khoảng trắng.";

const PASSWORD_PATTERN =
  /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&.#])[A-Za-z\d@$!%*?&.#]{8,50}$/;

const hasWhitespace = (value: string) => /\s/.test(value);

const oldPasswordError = computed(() => {
  const value = store.passwordForm.oldPassword || "";

  if (!value) {
    return "Mật khẩu cũ không được để trống.";
  }

  if (value.length < 8 || value.length > 50) {
    return "Mật khẩu cũ phải từ 8 đến 50 ký tự.";
  }

  if (hasWhitespace(value)) {
    return "Mật khẩu cũ không được chứa khoảng trắng.";
  }

  return "";
});

const newPasswordError = computed(() => {
  const value = store.passwordForm.newPassword || "";

  if (!value) {
    return "Mật khẩu mới không được để trống.";
  }

  if (value.length < 8 || value.length > 50) {
    return "Mật khẩu mới phải từ 8 đến 50 ký tự.";
  }

  if (hasWhitespace(value)) {
    return "Mật khẩu mới không được chứa khoảng trắng.";
  }

  if (!PASSWORD_PATTERN.test(value)) {
    return PASSWORD_RULE_MESSAGE;
  }

  if (
    store.passwordForm.oldPassword &&
    value === store.passwordForm.oldPassword
  ) {
    return "Mật khẩu mới không được trùng mật khẩu cũ.";
  }

  return "";
});

const confirmPasswordError = computed(() => {
  const value = store.passwordForm.confirmPassword || "";

  if (!value) {
    return "Xác nhận mật khẩu không được để trống.";
  }

  if (value.length < 8 || value.length > 50) {
    return "Xác nhận mật khẩu phải từ 8 đến 50 ký tự.";
  }

  if (hasWhitespace(value)) {
    return "Xác nhận mật khẩu không được chứa khoảng trắng.";
  }

  if (value !== store.passwordForm.newPassword) {
    return "Xác nhận mật khẩu không khớp.";
  }

  return "";
});

const formError = computed(() => {
  return (
    oldPasswordError.value ||
    newPasswordError.value ||
    confirmPasswordError.value
  );
});

const handleChangePassword = async () => {
  passwordSubmitTouched.value = true;

  if (formError.value || store.passwordLoading) {
    return;
  }

  try {
    await store.changePassword();

    passwordSubmitTouched.value = false;

    showOldPassword.value = false;
    showNewPassword.value = false;
    showConfirmPassword.value = false;
  } catch (error) {
    passwordSubmitTouched.value = true;
  }
};
</script>