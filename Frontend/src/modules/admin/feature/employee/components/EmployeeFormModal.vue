<template>
  <div v-if="show" class="custom-modal-backdrop">
    <div class="custom-modal">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <h5 class="mb-0 fw-bold">
          {{ isUpdateMode ? "Cập nhật nhân viên" : "Thêm nhân viên" }}
        </h5>

        <button type="button" class="btn-close" @click="$emit('close')"></button>
      </div>

      <form @submit.prevent="submitForm">
        <div class="row g-3">
          <div class="col-12">
            <label class="form-label fw-semibold">Ảnh đại diện</label>

            <div class="avatar-section">
              <div class="avatar-preview">
                <img v-if="form.avatarUrl" :src="form.avatarUrl" alt="Avatar" />
                <span v-else>{{ avatarText }}</span>
              </div>

              <div class="flex-grow-1">
                <input
                  type="file"
                  class="form-control"
                  accept="image/png,image/jpeg,image/webp"
                  @change="handleFileChange"
                />

                <small class="text-muted d-block mt-1">
                  Chọn ảnh JPG, PNG hoặc WEBP. Tối đa 5MB. Có thể căn khung trước khi upload.
                </small>

                <div class="d-flex flex-wrap gap-2 mt-2">
                  <button
                    v-if="rawImageUrl"
                    type="button"
                    class="btn btn-sm btn-outline-dark"
                    @click="openCropModal"
                  >
                    Căn khung ảnh
                  </button>

                  <button
                    v-if="croppedFile"
                    type="button"
                    class="btn btn-sm btn-primary"
                    :disabled="uploading"
                    @click="uploadAvatar"
                  >
                    {{ uploading ? "Đang upload..." : "Upload ảnh" }}
                  </button>

                  <button
                    v-if="form.avatarUrl"
                    type="button"
                    class="btn btn-sm btn-outline-secondary"
                    @click="removeAvatar"
                  >
                    Xóa ảnh
                  </button>
                </div>

                <div v-if="croppedFile && !uploadedAfterCrop" class="text-warning small mt-2">
                  Ảnh đã căn khung nhưng chưa upload. Vui lòng upload trước khi lưu.
                </div>
              </div>
            </div>
          </div>

          <div class="col-md-6">
            <label class="form-label">Vai trò</label>
            <select v-model="form.role" class="form-select" required>
              <option value="CASHIER">Thu ngân</option>
              <option value="MANAGER">Quản lý</option>
            </select>
          </div>

          <div class="col-md-6">
            <label class="form-label">Họ tên</label>
            <input
              v-model.trim="form.name"
              type="text"
              class="form-control"
              maxlength="255"
              required
            />
          </div>

          <div class="col-md-6">
            <label class="form-label">Email</label>
            <input
              v-model.trim="form.email"
              type="email"
              class="form-control"
              maxlength="255"
              required
            />
          </div>

          <div class="col-md-6">
            <label class="form-label">
              Mật khẩu
              <small v-if="isUpdateMode" class="text-muted">
                — để trống nếu không đổi
              </small>
            </label>

            <input
              v-model.trim="form.password"
              type="password"
              class="form-control"
              minlength="6"
              maxlength="72"
              :required="!isUpdateMode"
            />
          </div>

          <div class="col-md-6">
            <label class="form-label">Số điện thoại</label>
            <input
              v-model.trim="form.phone"
              type="text"
              class="form-control"
              maxlength="10"
              required
            />
          </div>

          <div class="col-md-6">
            <label class="form-label">CCCD</label>
            <input
              v-model.trim="form.citizenId"
              type="text"
              class="form-control"
              maxlength="12"
              required
            />
          </div>

          <div class="col-md-6">
            <label class="form-label">Chức vụ</label>
            <input
              v-model.trim="form.jobTitle"
              type="text"
              class="form-control"
              maxlength="100"
              required
            />
          </div>

          <div class="col-md-6">
            <label class="form-label">Lương</label>
            <input
              v-model.number="form.salary"
              type="number"
              class="form-control"
              min="0"
              step="1000"
              required
            />
          </div>

          <div class="col-md-6">
            <label class="form-label">Ngày vào làm</label>
            <input
              v-model="form.hireDate"
              type="date"
              class="form-control"
              :max="today"
              required
            />
          </div>

          <div class="col-md-6">
            <label class="form-label">Tỉnh/Thành phố</label>
            <select
              v-model="form.province"
              class="form-select"
              :disabled="addressLoading"
              required
              @change="onProvinceChange"
            >
              <option value="">
                {{ addressLoading ? "Đang tải dữ liệu..." : "Chọn tỉnh/thành phố" }}
              </option>

              <option
                v-for="province in provinces"
                :key="province.code"
                :value="province.name"
              >
                {{ province.name }}
              </option>
            </select>
          </div>

          <div class="col-md-6">
            <label class="form-label">Phường/Xã</label>
            <select
              v-model="form.ward"
              class="form-select"
              :disabled="!form.province || addressLoading"
              required
            >
              <option value="">Chọn phường/xã</option>

              <option
                v-for="ward in wards"
                :key="ward.code"
                :value="ward.name"
              >
                {{ ward.name }}
              </option>
            </select>
          </div>

          <div class="col-12">
            <label class="form-label">Địa chỉ cụ thể</label>
            <input
              v-model.trim="form.addressDetail"
              type="text"
              class="form-control"
              maxlength="255"
              required
              placeholder="Số nhà, tên đường, tòa nhà, tầng, phòng..."
            />

            <small class="text-muted">
              Địa chỉ lưu: {{ previewAddress || "Chưa nhập đủ địa chỉ" }}
            </small>
          </div>
        </div>

        <div v-if="formError" class="alert alert-danger mt-3 mb-0">
          {{ formError }}
        </div>

        <div class="d-flex justify-content-end gap-2 mt-4">
          <button
            type="button"
            class="btn btn-outline-secondary"
            @click="$emit('close')"
          >
            Hủy
          </button>

          <button
            type="submit"
            class="btn btn-primary"
            :disabled="loading || uploading"
          >
            {{ loading || uploading ? "Đang xử lý..." : "Lưu" }}
          </button>
        </div>
      </form>
    </div>

    <div v-if="showCropModal" class="crop-modal-backdrop">
      <div class="crop-modal">
        <div class="d-flex justify-content-between align-items-center mb-3">
          <h5 class="mb-0 fw-bold">Căn khung ảnh đại diện</h5>
          <button type="button" class="btn-close" @click="closeCropModal"></button>
        </div>

        <div class="cropper-wrapper">
          <Cropper
            ref="cropperRef"
            :src="rawImageUrl"
            :stencil-props="{ aspectRatio: 1 }"
            :resize-image="{ wheel: true }"
            image-restriction="stencil"
          />
        </div>

        <div class="d-flex justify-content-end gap-2 mt-3">
          <button type="button" class="btn btn-outline-secondary" @click="closeCropModal">
            Hủy
          </button>

          <button type="button" class="btn btn-primary" @click="applyCrop">
            Áp dụng khung ảnh
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import "vue-advanced-cropper/dist/style.css";

import { computed, reactive, ref, watch } from "vue";
import Swal from "sweetalert2";
import { Cropper } from "vue-advanced-cropper";
import { employeeService } from "../services/employee.service";
import type {
  EmployeeCreateRequest,
  EmployeeResponse,
  EmployeeRole,
  EmployeeUpdateRequest,
} from "../types/employee.type";

interface VietnamWard {
  code: string | number;
  name: string;
}

interface VietnamProvince {
  code: string | number;
  name: string;
  wards?: VietnamWard[];
}

interface EmployeeForm {
  role: EmployeeRole;
  name: string;
  email: string;
  password: string;
  phone: string;
  addressDetail: string;
  province: string;
  ward: string;
  avatarUrl: string;
  citizenId: string;
  jobTitle: string;
  salary: number;
  hireDate: string;
}

const props = defineProps<{
  show: boolean;
  employee: EmployeeResponse | null;
  loading: boolean;
}>();

const emit = defineEmits<{
  (e: "close"): void;
  (
    e: "submit",
    value: {
      userId: number | null;
      payload: EmployeeCreateRequest | EmployeeUpdateRequest;
    }
  ): void;
}>();

const today = new Date().toISOString().slice(0, 10);
const isUpdateMode = computed(() => props.employee !== null);

const formError = ref("");
const uploading = ref(false);

const rawImageUrl = ref("");
const croppedFile = ref<File | null>(null);
const uploadedAfterCrop = ref(true);
const showCropModal = ref(false);
const cropperRef = ref<any>(null);

const provinces = ref<VietnamProvince[]>([]);
const addressLoading = ref(false);

const form = reactive<EmployeeForm>({
  role: "CASHIER",
  name: "",
  email: "",
  password: "",
  phone: "",
  addressDetail: "",
  province: "",
  ward: "",
  avatarUrl: "",
  citizenId: "",
  jobTitle: "Thu ngân",
  salary: 0,
  hireDate: today,
});

const avatarText = computed(() => {
  if (!form.name.trim()) return "NV";
  return form.name.trim().substring(0, 2).toUpperCase();
});

const wards = computed(() => {
  const province = provinces.value.find((item) => item.name === form.province);
  return province?.wards || [];
});

const previewAddress = computed(() => buildAddress());

watch(
  () => props.show,
  async (show) => {
    if (show) {
      await loadVietnamAddressData();
      fillForm();
    } else {
      clearSelectedImage();
    }
  }
);

watch(
  () => props.employee,
  async () => {
    if (props.show) {
      await loadVietnamAddressData();
      fillForm();
    }
  }
);

async function loadVietnamAddressData() {
  if (provinces.value.length > 0) return;

  try {
    addressLoading.value = true;

    const response = await fetch("https://provinces.open-api.vn/api/v2/?depth=2");

    if (!response.ok) {
      throw new Error("Không thể tải dữ liệu địa chỉ");
    }

    const data = await response.json();
    provinces.value = Array.isArray(data) ? data : data?.data || [];

    if (!Array.isArray(provinces.value)) {
      provinces.value = [];
    }
  } catch {
    formError.value =
      "Không thể tải dữ liệu tỉnh/phường hiện nay. Vui lòng thử lại hoặc kiểm tra mạng.";
  } finally {
    addressLoading.value = false;
  }
}

function fillForm() {
  formError.value = "";
  clearSelectedImage();

  if (props.employee) {
    form.role = normalizeEmployeeRole(props.employee.roleName);
    form.name = props.employee.name || "";
    form.email = props.employee.email || "";
    form.password = "";
    form.phone = props.employee.phone || "";
    form.avatarUrl = props.employee.avatarUrl || "";
    form.citizenId = props.employee.citizenId || "";
    form.jobTitle = props.employee.jobTitle || "";
    form.salary = Number(props.employee.salary || 0);
    form.hireDate = props.employee.hireDate || today;

    parseAddress(props.employee.address || "");
    return;
  }

  form.role = "CASHIER";
  form.name = "";
  form.email = "";
  form.password = "";
  form.phone = "";
  form.addressDetail = "";
  form.province = "";
  form.ward = "";
  form.avatarUrl = "";
  form.citizenId = "";
  form.jobTitle = "Thu ngân";
  form.salary = 0;
  form.hireDate = today;
}

function normalizeEmployeeRole(role: string): EmployeeRole {
  const cleanRole = (role || "").toUpperCase().replace("ROLE_", "").trim();

  if (cleanRole === "MANAGER") return "MANAGER";
  return "CASHIER";
}

function parseAddress(address?: string | null) {
  form.addressDetail = "";
  form.province = "";
  form.ward = "";

  if (!address) return;

  const parts = address
    .split(",")
    .map((item) => item.trim())
    .filter((item) => item.length > 0);

  if (parts.length >= 3) {
    form.province = parts[parts.length - 1] ?? "";
    form.ward = parts[parts.length - 2] ?? "";
    form.addressDetail = parts.slice(0, parts.length - 2).join(", ");
    return;
  }

  form.addressDetail = address;
}

function onProvinceChange() {
  form.ward = "";
}

function buildAddress() {
  const parts = [
    form.addressDetail.trim(),
    form.ward.trim(),
    form.province.trim(),
  ].filter((item) => item.length > 0);

  return parts.join(", ");
}

function validateAddress() {
  if (!form.province.trim()) {
    formError.value = "Vui lòng chọn Tỉnh/Thành phố.";
    return false;
  }

  if (!form.ward.trim()) {
    formError.value = "Vui lòng chọn Phường/Xã.";
    return false;
  }

  if (!form.addressDetail.trim()) {
    formError.value = "Vui lòng nhập địa chỉ cụ thể.";
    return false;
  }

  return true;
}

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];

  clearSelectedImage();

  if (!file) return;

  if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
    formError.value = "Chỉ hỗ trợ ảnh JPG, PNG hoặc WEBP.";
    input.value = "";
    return;
  }

  if (file.size > 5 * 1024 * 1024) {
    formError.value = "Ảnh không được vượt quá 5MB.";
    input.value = "";
    return;
  }

  rawImageUrl.value = URL.createObjectURL(file);
  croppedFile.value = null;
  uploadedAfterCrop.value = false;
  formError.value = "";
  showCropModal.value = true;
}

function openCropModal() {
  if (!rawImageUrl.value) return;
  showCropModal.value = true;
}

function closeCropModal() {
  showCropModal.value = false;
}

function applyCrop() {
  const result = cropperRef.value?.getResult();

  if (!result?.canvas) {
    formError.value = "Không thể lấy khung ảnh. Vui lòng chọn lại ảnh.";
    return;
  }

  result.canvas.toBlob(
    (blob: Blob | null) => {
      if (!blob) {
        formError.value = "Không thể xử lý ảnh đã cắt.";
        return;
      }

      const file = new File([blob], `employee-avatar-${Date.now()}.webp`, {
        type: "image/webp",
      });

      croppedFile.value = file;

      if (form.avatarUrl && form.avatarUrl.startsWith("blob:")) {
        URL.revokeObjectURL(form.avatarUrl);
      }

      form.avatarUrl = URL.createObjectURL(file);
      uploadedAfterCrop.value = false;
      showCropModal.value = false;
      formError.value = "";
    },
    "image/webp",
    0.9
  );
}

async function uploadAvatar() {
  if (!croppedFile.value) return;

  try {
    uploading.value = true;

    const res = await employeeService.uploadAvatar(croppedFile.value);

    if (form.avatarUrl.startsWith("blob:")) {
      URL.revokeObjectURL(form.avatarUrl);
    }

    form.avatarUrl = res.data.url;
    croppedFile.value = null;
    uploadedAfterCrop.value = true;

    Swal.fire({
      icon: "success",
      title: "Upload ảnh thành công",
      timer: 1300,
      showConfirmButton: false,
    });
  } catch (error: any) {
    const message =
      error?.response?.data?.message || "Upload ảnh thất bại. Vui lòng thử lại.";

    formError.value = message;

    Swal.fire({
      icon: "error",
      title: "Upload ảnh thất bại",
      text: message,
      confirmButtonText: "Đóng",
    });
  } finally {
    uploading.value = false;
  }
}

function removeAvatar() {
  if (form.avatarUrl.startsWith("blob:")) {
    URL.revokeObjectURL(form.avatarUrl);
  }

  form.avatarUrl = "";
  croppedFile.value = null;
  uploadedAfterCrop.value = true;
}

function clearSelectedImage() {
  if (rawImageUrl.value.startsWith("blob:")) {
    URL.revokeObjectURL(rawImageUrl.value);
  }

  rawImageUrl.value = "";
  croppedFile.value = null;
  uploadedAfterCrop.value = true;
  showCropModal.value = false;
}

function validateForm() {
  if (!["MANAGER", "CASHIER"].includes(form.role)) {
    formError.value = "Nhân viên chỉ được chọn vai trò Quản lý hoặc Thu ngân.";
    return false;
  }

  if (!form.name.trim()) {
    formError.value = "Tên nhân viên không được để trống.";
    return false;
  }

  if (!/^\S+@\S+\.\S+$/.test(form.email)) {
    formError.value = "Email không đúng định dạng.";
    return false;
  }

  if (!isUpdateMode.value && !form.password.trim()) {
    formError.value = "Mật khẩu không được để trống.";
    return false;
  }

  if (form.password && !/^\S{6,72}$/.test(form.password)) {
    formError.value = "Mật khẩu phải từ 6 đến 72 ký tự và không chứa khoảng trắng.";
    return false;
  }

  if (!/^0\d{9}$/.test(form.phone)) {
    formError.value = "Số điện thoại phải có 10 số và bắt đầu bằng 0.";
    return false;
  }

  if (!/^\d{12}$/.test(form.citizenId)) {
    formError.value = "CCCD phải gồm đúng 12 số.";
    return false;
  }

  if (!form.jobTitle.trim()) {
    formError.value = "Chức vụ không được để trống.";
    return false;
  }

  if (Number(form.salary) < 0) {
    formError.value = "Lương không được âm.";
    return false;
  }

  if (!form.hireDate || form.hireDate > today) {
    formError.value = "Ngày vào làm không được lớn hơn ngày hiện tại.";
    return false;
  }

  if (!validateAddress()) {
    return false;
  }

  if (croppedFile.value || !uploadedAfterCrop.value) {
    formError.value =
      "Bạn đã chọn ảnh mới nhưng chưa upload. Vui lòng upload ảnh trước khi lưu.";
    return false;
  }

  formError.value = "";
  return true;
}

function submitForm() {
  if (!validateForm()) return;

  const payload = {
    role: form.role,
    name: form.name.trim(),
    email: form.email.trim().toLowerCase(),
    password: form.password.trim(),
    phone: form.phone.trim(),
    address: buildAddress(),
    avatarUrl: form.avatarUrl || null,
    citizenId: form.citizenId.trim(),
    jobTitle: form.jobTitle.trim(),
    salary: Number(form.salary),
    hireDate: form.hireDate,
  };

  emit("submit", {
    userId: props.employee?.userId ?? null,
    payload,
  });
}
</script>

<style scoped>
.custom-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1050;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.custom-modal {
  width: min(980px, 100%);
  max-height: 90vh;
  overflow-y: auto;
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px;
  border: 1px dashed #cbd5e1;
  border-radius: 16px;
  background: #f8fafc;
}

.avatar-preview {
  width: 92px;
  height: 92px;
  border-radius: 20px;
  overflow: hidden;
  background: #111827;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 1.3rem;
  flex-shrink: 0;
}

.avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.crop-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1200;
  background: rgba(15, 23, 42, 0.72);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.crop-modal {
  width: min(760px, 100%);
  background: #ffffff;
  border-radius: 18px;
  padding: 20px;
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.35);
}

.cropper-wrapper {
  height: 460px;
  background: #020617;
  border-radius: 14px;
  overflow: hidden;
}
</style>