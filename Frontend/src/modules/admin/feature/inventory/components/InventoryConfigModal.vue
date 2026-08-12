<script setup lang="ts">
import { ref, watch } from "vue";

const props = defineProps<{
  visible: boolean;
  currentValue: number;
  saving?: boolean;
  canEdit: boolean;
}>();

const emit = defineEmits<{
  close: [];
  save: [value: number];
}>();

/*
 * Dùng String ở FE để kiểm soát input chặt chẽ.
 * Trước khi emit mới convert sang Number.
 */
const value = ref<string>(String(props.currentValue ?? ""));

const error = ref("");

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      value.value = String(props.currentValue ?? "");

      error.value = "";
    }
  }
);

watch(
  () => props.currentValue,
  (newValue) => {
    value.value = String(newValue ?? "");
  }
);

const handleClose = () => {
  if (props.saving) {
    return;
  }

  error.value = "";

  emit("close");
};

/*
 * Chỉ cho phép chữ số 0-9.
 * Tối đa 4 ký tự.
 */
const handleInput = (event: Event) => {
  const input = event.target as HTMLInputElement;

  let raw = input.value;

  // Xóa mọi ký tự không phải số
  raw = raw.replace(/\D/g, "");

  // Tối đa 4 chữ số
  raw = raw.slice(0, 4);

  value.value = raw;

  // Đồng bộ lại giá trị trên input
  input.value = raw;

  if (error.value) {
    error.value = "";
  }
};

const submit = () => {
  error.value = "";

  if (!props.canEdit) {
    error.value = "Bạn không có quyền thay đổi cấu hình kho.";

    return;
  }

  const raw = value.value.trim();

  /*
   * Không được để trống
   */
  if (!raw) {
    error.value = "Số ngày cảnh báo không được để trống.";

    return;
  }

  /*
   * Chỉ chữ số
   */
  if (!/^\d+$/.test(raw)) {
    error.value = "Số ngày cảnh báo chỉ được chứa chữ số.";

    return;
  }

  /*
   * Tối đa 4 ký tự
   */
  if (raw.length > 4) {
    error.value = "Số ngày cảnh báo tối đa 4 chữ số.";

    return;
  }

  const numberValue = Number(raw);

  /*
   * Phải là số nguyên
   */
  if (!Number.isInteger(numberValue)) {
    error.value = "Số ngày cảnh báo phải là số nguyên.";

    return;
  }

  /*
   * Backend cũng validate 0 - 3650.
   */
  if (numberValue < 0 || numberValue > 3650) {
    error.value = "Số ngày cảnh báo phải từ 0 đến 3650 ngày.";

    return;
  }

  emit("save", numberValue);
};
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="inventory-config-backdrop">
      <div
        class="inventory-config-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="inventory-config-title"
      >
        <!-- HEADER -->
        <div class="inventory-config-header">
          <div>
            <h3 id="inventory-config-title" class="inventory-config-title">
              Thiết lập cảnh báo hạn sử dụng
            </h3>

            <p class="inventory-config-subtitle">
              Cấu hình số ngày cảnh báo trước khi lô hàng hết hạn.
            </p>
          </div>

          <button
            type="button"
            class="inventory-config-close"
            :disabled="saving"
            aria-label="Đóng"
            @click="handleClose"
          >
            <i class="bi bi-x-lg"></i>
          </button>
        </div>

        <!-- BODY -->
        <div class="inventory-config-body">
          <div class="form-group">
            <label for="expiryWarningDays" class="form-label">
              Số ngày cảnh báo
              <span class="required">*</span>
            </label>

            <div class="input-wrapper">
              <input
                id="expiryWarningDays"
                :value="value"
                type="text"
                inputmode="numeric"
                maxlength="4"
                class="config-input"
                :class="{ 'input-error': error }"
                :disabled="!canEdit || saving"
                autocomplete="off"
                placeholder="Ví dụ: 30"
                @input="handleInput"
              />

              <span class="input-suffix"> ngày </span>
            </div>

            <div class="config-hint">
              <i class="bi bi-info-circle"></i>

              <span>
                Lô hàng sẽ được đánh dấu
                <strong>sắp hết hạn</strong>
                khi số ngày còn lại nhỏ hơn hoặc bằng giá trị này.
              </span>
            </div>

            <div v-if="error" class="validation-error">
              <i class="bi bi-exclamation-circle"></i>
              <span>{{ error }}</span>
            </div>

            <div v-if="!canEdit" class="permission-warning">
              <i class="bi bi-lock"></i>

              <span>
                Bạn chỉ có quyền xem cấu hình. Chỉ OWNER hoặc MANAGER được phép
                thay đổi.
              </span>
            </div>
          </div>
        </div>

        <!-- FOOTER -->
        <div class="inventory-config-footer">
          <button
            type="button"
            class="btn-cancel"
            :disabled="saving"
            @click="handleClose"
          >
            Hủy
          </button>

          <button
            v-if="canEdit"
            type="button"
            class="btn-save"
            :disabled="saving"
            @click="submit"
          >
            <span v-if="saving" class="spinner-border spinner-border-sm"></span>

            <i v-else class="bi bi-check-lg"></i>

            {{ saving ? "Đang lưu..." : "Lưu cấu hình" }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.inventory-config-backdrop {
  position: fixed;
  inset: 0;

  z-index: 99999;

  display: flex;
  align-items: center;
  justify-content: center;

  padding: 20px;

  background: rgba(15, 23, 42, 0.48);

  backdrop-filter: blur(2px);
  -webkit-backdrop-filter: blur(2px);
}

.inventory-config-dialog {
  display: block;

  width: 100%;
  max-width: 520px;

  background: #ffffff;

  border: 1px solid #e2e8f0;
  border-radius: 16px;

  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22),
    0 8px 20px rgba(15, 23, 42, 0.08);

  overflow: hidden;

  animation: inventoryModalShow 0.18s ease-out;
}

@keyframes inventoryModalShow {
  from {
    opacity: 0;
    transform: translateY(8px) scale(0.98);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* =========================
   HEADER
   ========================= */

.inventory-config-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;

  padding: 20px 22px;

  border-bottom: 1px solid #e2e8f0;
}

.inventory-config-title {
  margin: 0;

  color: #0f172a;

  font-size: 18px;
  font-weight: 700;
}

.inventory-config-subtitle {
  margin: 5px 0 0;

  color: #64748b;

  font-size: 13px;
  line-height: 1.5;
}

.inventory-config-close {
  width: 34px;
  height: 34px;

  display: inline-flex;
  align-items: center;
  justify-content: center;

  flex-shrink: 0;

  border: none;
  border-radius: 8px;

  background: transparent;
  color: #64748b;

  cursor: pointer;

  transition: 0.15s ease;
}

.inventory-config-close:hover:not(:disabled) {
  background: #f1f5f9;
  color: #0f172a;
}

.inventory-config-close:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* =========================
   BODY
   ========================= */

.inventory-config-body {
  padding: 24px 22px;
}

.form-group {
  width: 100%;
}

.form-label {
  display: block;

  margin-bottom: 8px;

  color: #334155;

  font-size: 14px;
  font-weight: 600;
}

.required {
  color: #dc2626;
}

.input-wrapper {
  position: relative;
}

.config-input {
  box-sizing: border-box;

  width: 100%;
  height: 44px;

  padding: 0 65px 0 13px;

  border: 1px solid #cbd5e1;
  border-radius: 9px;

  background: #ffffff;

  color: #0f172a;

  font-size: 14px;

  outline: none;

  transition: border-color 0.15s, box-shadow 0.15s;
}

.config-input:focus {
  border-color: #111827;

  box-shadow: 0 0 0 3px rgba(17, 24, 39, 0.08);
}

.config-input:disabled {
  background: #f8fafc;
  color: #64748b;

  cursor: not-allowed;
}

.config-input.input-error {
  border-color: #dc2626;
}

.input-suffix {
  position: absolute;

  top: 50%;
  right: 13px;

  transform: translateY(-50%);

  color: #64748b;

  font-size: 13px;

  pointer-events: none;
}

.config-hint {
  display: flex;
  align-items: flex-start;
  gap: 7px;

  margin-top: 10px;

  color: #64748b;

  font-size: 12px;
  line-height: 1.5;
}

.config-hint i {
  margin-top: 2px;
}

/* =========================
   VALIDATION
   ========================= */

.validation-error {
  display: flex;
  align-items: center;
  gap: 7px;

  margin-top: 10px;

  color: #dc2626;

  font-size: 12px;
}

.permission-warning {
  display: flex;
  align-items: flex-start;
  gap: 8px;

  margin-top: 14px;
  padding: 11px 12px;

  border: 1px solid #fde68a;
  border-radius: 8px;

  background: #fffbeb;

  color: #92400e;

  font-size: 12px;
  line-height: 1.5;
}

/* =========================
   FOOTER
   ========================= */

.inventory-config-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;

  padding: 15px 22px;

  border-top: 1px solid #e2e8f0;

  background: #f8fafc;
}

.btn-cancel,
.btn-save {
  min-width: 90px;
  height: 39px;

  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;

  padding: 0 16px;

  border-radius: 8px;

  font-size: 13px;
  font-weight: 600;

  cursor: pointer;

  transition: 0.15s ease;
}

.btn-cancel {
  border: 1px solid #cbd5e1;

  background: #ffffff;
  color: #475569;
}

.btn-cancel:hover:not(:disabled) {
  background: #f1f5f9;
}

.btn-save {
  border: 1px solid #111827;

  background: #111827;
  color: #ffffff;
}

.btn-save:hover:not(:disabled) {
  background: #000000;
}

.btn-cancel:disabled,
.btn-save:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

@media (max-width: 576px) {
  .inventory-config-backdrop {
    align-items: flex-end;
    padding: 0;
  }

  .inventory-config-dialog {
    max-width: none;

    border-radius: 16px 16px 0 0;
  }

  .inventory-config-footer {
    flex-direction: column-reverse;
  }

  .btn-cancel,
  .btn-save {
    width: 100%;
  }
}
</style>
