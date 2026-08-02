<template>
  <div class="card border-0 shadow-sm">
    <div class="card-header bg-white border-0 py-3">
      <h5 class="mb-0 fw-bold">Thông tin cá nhân</h5>
    </div>

    <div class="card-body">
      <div class="profile-avatar-section mb-4">
        <div class="final-avatar-preview">
          <img
            v-if="avatarDisplayUrl"
            :src="avatarDisplayUrl"
            alt="Avatar"
            class="final-avatar-img"
          />
          <span v-else>{{ userInitial }}</span>
        </div>

        <div class="avatar-actions">
          <div class="fw-bold mb-2">Ảnh đại diện</div>
          <div class="text-muted small mb-3">
            Chọn ảnh, kéo để căn vị trí, phóng to/thu nhỏ rồi bấm “Cập nhật ảnh”.
          </div>
          <input
            ref="avatarInputRef"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            class="d-none"
            @change="handleAvatarChange"
          />
          <div class="d-flex flex-wrap gap-2 mb-3">
            <button
              type="button"
              class="btn btn-outline-dark btn-sm"
              @click="avatarInputRef?.click()"
            >
              Chọn ảnh
            </button>
            <button
              type="button"
              class="btn btn-outline-secondary btn-sm"
              :disabled="!sourceAvatarUrl || store.avatarLoading"
              @click="resetImageAdjust"
            >
              Đặt lại ảnh
            </button>
            <button
              type="button"
              class="btn btn-success btn-sm"
              :disabled="!sourceAvatarUrl || store.avatarLoading"
              @click="cropAndUploadAvatar"
            >
              <span
                v-if="store.avatarLoading"
                class="spinner-border spinner-border-sm me-1"
              ></span>
              Cập nhật ảnh
            </button>
          </div>

          <div v-if="sourceAvatarUrl" class="avatar-editor">
            <div
              ref="cropBoxRef"
              class="crop-box"
              @mousedown="startDrag"
              @mousemove="onDrag"
              @mouseup="stopDrag"
              @mouseleave="stopDrag"
              @touchstart.prevent="startTouchDrag"
              @touchmove.prevent="onTouchDrag"
              @touchend="stopDrag"
            >
              <img
                :src="sourceAvatarUrl"
                alt="Ảnh cần chỉnh"
                class="crop-image"
                :style="cropImageStyle"
                draggable="false"
              />
              <div class="crop-mask"></div>
              <div class="crop-circle"></div>
            </div>
            <div class="mt-3">
              <label class="form-label small fw-semibold">Phóng to / thu nhỏ</label>
              <input
                v-model.number="zoom"
                type="range"
                class="form-range"
                min="1"
                max="3"
                step="0.05"
              />
            </div>
          </div>
        </div>
      </div>

      <div class="row g-3">
        <div class="col-12 col-md-6">
          <label class="form-label fw-semibold">Họ tên</label>
          <input
            v-model="store.profileForm.name"
            type="text"
            class="form-control"
            placeholder="Nhập họ tên"
          />
        </div>

        <div class="col-12 col-md-6">
          <label class="form-label fw-semibold">Email đăng nhập</label>
          <input
            v-model="store.profileForm.email"
            type="email"
            class="form-control email-readonly"
            disabled
            readonly
          />
          <div class="form-text">
            Email dùng để đăng nhập nên không thể tự thay đổi.
          </div>
        </div>

        <div class="col-12 col-md-6">
          <label class="form-label fw-semibold">Số điện thoại</label>
          <input
            v-model="store.profileForm.phone"
            type="text"
            maxlength="10"
            class="form-control"
            placeholder="0987654321"
            @input="handlePhoneInput"
          />
        </div>

        <div class="col-12 col-md-6">
          <label class="form-label fw-semibold">Ngày sinh</label>
          <input
            v-model="store.profileForm.dateOfBirth"
            type="date"
            class="form-control"
          />
        </div>

        <div class="col-12 col-md-6">
          <label class="form-label fw-semibold">Giới tính</label>
          <select v-model="store.profileForm.gender" class="form-select">
            <option :value="null">Chưa chọn</option>
            <option :value="0">Nam</option>
            <option :value="1">Nữ</option>
            <option :value="2">Khác</option>
          </select>
        </div>
      </div>

      <!-- SỔ ĐỊA CHỈ -->
      <div class="mt-4 pt-4 border-top">
        <div class="d-flex justify-content-between align-items-center mb-3">
          <h5 class="fw-bold mb-0">Sổ địa chỉ của bạn</h5>
          <button class="btn btn-outline-dark btn-sm" @click="openAddressModal">
            + Thêm địa chỉ mới
          </button>
        </div>

        <div
          v-if="addressList.length === 0"
          class="text-muted text-center py-4 border rounded bg-light"
        >
          Bạn chưa lưu địa chỉ nào.
        </div>

        <div v-else class="row g-3">
          <div v-for="(addr, index) in addressList" :key="addr.id || index" class="col-12">
            <div
              class="card border p-3 d-flex flex-row justify-content-between align-items-center"
            >
              <div>
                <p class="mb-1 fw-semibold">
                  <i class="bi bi-geo-alt-fill text-danger me-2"></i>
                  {{ addr.fullAddress }}
                </p>
                <div class="small text-muted">
                  Người nhận: {{ addr.recipientName }} | SĐT: {{ addr.phone }}
                  <span v-if="addr.isDefault" class="badge bg-danger ms-2">Mặc định</span>
                </div>
              </div>
              <div>
                <button
                  class="btn btn-sm btn-outline-primary me-2"
                  @click="editAddress(addr)"
                >
                  Sửa
                </button>
                <button
                  class="btn btn-sm btn-outline-danger"
                  @click="removeAddress(addr.id!)"
                >
                  Xóa
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="text-end mt-4 pt-3 border-top">
        <button
          class="btn btn-dark px-4"
          :disabled="store.profileLoading"
          @click="saveProfileInfo"
        >
          <span
            v-if="store.profileLoading"
            class="spinner-border spinner-border-sm me-2"
          ></span>
          Cập nhật thông tin
        </button>
      </div>
    </div>

    <!-- MODAL THÊM / SỬA ĐỊA CHỈ -->
    <Teleport to="body">
      <div
        v-if="showAddressModal"
        class="custom-modal-overlay d-flex align-items-center justify-content-center"
        style="
          position: fixed;
          inset: 0;
          background: rgba(0, 0, 0, 0.5);
          z-index: 1050;
        "
      >
        <div
          class="bg-white p-4 rounded shadow-lg"
          style="width: 100%; max-width: 600px"
        >
          <div
            class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-2"
          >
            <h5 class="fw-bold mb-0">
              {{ editingAddressId ? "Sửa địa chỉ" : "Thêm địa chỉ nhận hàng" }}
            </h5>
            <button
              type="button"
              class="btn-close"
              @click="closeAddressModal"
            ></button>
          </div>

          <div class="row mb-3">
            <div class="col-md-6">
              <label class="form-label fw-bold"
                >Tỉnh / Thành phố <span class="text-danger">*</span></label
              >
              <select
                class="form-select"
                v-model="selectedProvinceCode"
                @change="handleProvinceChange"
                :disabled="loadingProvinces"
              >
                <option value="">
                  {{ loadingProvinces ? "Đang tải..." : "Chọn tỉnh/thành phố" }}
                </option>
                <option v-for="p in provinces" :key="p.code" :value="p.code">
                  {{ p.name }}
                </option>
              </select>
            </div>
            <div class="col-md-6 mt-3 mt-md-0">
              <label class="form-label fw-bold"
                >Phường / Xã <span class="text-danger">*</span></label
              >
              <select
                class="form-select"
                v-model="selectedWardCode"
                :disabled="!selectedProvinceCode || loadingWards"
              >
                <option value="">
                  {{
                    !selectedProvinceCode
                      ? "Chọn tỉnh/thành trước"
                      : loadingWards
                        ? "Đang tải..."
                        : "Chọn phường/xã"
                  }}
                </option>
                <option v-for="w in wards" :key="w.code" :value="w.code">
                  {{ w.name }}
                </option>
              </select>
            </div>
          </div>

          <div class="mb-3">
            <label class="form-label fw-bold"
              >Địa chỉ cụ thể <span class="text-danger">*</span></label
            >
            <textarea
              class="form-control"
              v-model="specificAddress"
              rows="3"
              placeholder="Ví dụ: Số 12/5, ngõ 36-A, đường Trần Phú"
            ></textarea>
          </div>

          <div class="form-check mb-3">
            <input class="form-check-input" type="checkbox" v-model="isDefaultAddress" id="defaultAddressCheck" />
            <label class="form-check-label" for="defaultAddressCheck">
              Đặt làm địa chỉ mặc định
            </label>
          </div>

          <div class="d-flex justify-content-end gap-2 mt-4">
            <button class="btn btn-light px-4" @click="closeAddressModal">
              Hủy
            </button>
            <button class="btn btn-dark px-4" @click="saveAddressNode">
              Lưu địa chỉ
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, onMounted } from "vue";
import { useCustomerProfileStore } from "@/modules/shop/feature/profile/stores/customerProfile.store";
import customerAddressService from "@/modules/customerAddress/services/customerAddress.service";
import Swal from "sweetalert2";
import api from "@/common/api";

interface Ward { code: number | string; name: string; }
interface Province { code: number | string; name: string; wards?: Ward[] | null; }

const store = useCustomerProfileStore();

const avatarInputRef = ref<HTMLInputElement | null>(null);
const cropBoxRef = ref<HTMLDivElement | null>(null);
const sourceAvatarUrl = ref("");
const sourceAvatarFile = ref<File | null>(null);
const zoom = ref(1);
const offsetX = ref(0);
const offsetY = ref(0);
const isDragging = ref(false);
const dragStartX = ref(0);
const dragStartY = ref(0);
const dragOriginX = ref(0);
const dragOriginY = ref(0);

// --- LOGIC ĐỊA CHỈ MỚI ---
const addressList = ref<any[]>([]);
const showAddressModal = ref(false);
const editingAddressId = ref<number | null>(null);

const provinces = ref<Province[]>([]);
const wards = ref<Ward[]>([]);
const selectedProvinceCode = ref<string>("");
const selectedWardCode = ref<string>("");
const specificAddress = ref("");
const isDefaultAddress = ref(false);
const loadingProvinces = ref(false);
const loadingWards = ref(false);

// Đã ép kiểu an toàn (as any) để tránh lỗi TypeScript không tìm thấy thuộc tính userId/id
const currentCustomerId = computed(() => (store.profileForm as any).userId || (store.profileForm as any).id || (store.profileForm as any).customerId);

const selectedProvince = computed(() =>
  provinces.value.find((item) => String(item.code) === String(selectedProvinceCode.value))
);
const selectedWard = computed(() =>
  wards.value.find((item) => String(item.code) === String(selectedWardCode.value))
);

const loadProvinces = async () => {
  try {
    loadingProvinces.value = true;
    const response = await fetch(`https://provinces.open-api.vn/api/p/`);
    const data = await response.json();
    provinces.value = data.map((item: any) => ({ code: item.code, name: item.name }));
  } catch (error) {
    console.error("Lỗi tải tỉnh/thành:", error);
  } finally {
    loadingProvinces.value = false;
  }
};

const loadWardsByProvince = async (provinceCode: string) => {
  try {
    loadingWards.value = true;
    wards.value = [];
    const response = await fetch(`https://provinces.open-api.vn/api/p/${provinceCode}?depth=3`);
    const data = await response.json();

    let allWards: any[] = [];
    if (data && data.districts && Array.isArray(data.districts)) {
      data.districts.forEach((district: any) => {
        if (district.wards && Array.isArray(district.wards)) {
          district.wards.forEach((ward: any) => {
            allWards.push({ code: ward.code, name: `${ward.name}, ${district.name}` });
          });
        }
      });
    }
    wards.value = allWards;
  } catch (error) {
    console.error("Lỗi tải phường/xã:", error);
    wards.value = [];
  } finally {
    loadingWards.value = false;
  }
};

const handleProvinceChange = async () => {
  selectedWardCode.value = "";
  wards.value = [];
  if (selectedProvinceCode.value) await loadWardsByProvince(selectedProvinceCode.value);
};

onMounted(async () => {
  try {
    const res = await api.get(`/customer/profile?t=${Date.now()}`);
    const profileData = res.data?.data || res.data?.result || res.data || {};
    Object.assign(store.profileForm, profileData);

    if (currentCustomerId.value) {
      const addrRes = await customerAddressService.getAddresses(currentCustomerId.value);
      addressList.value = Array.isArray(addrRes.data) ? addrRes.data : [];
    }
  } catch (e) {
    console.error("Lỗi lấy thông tin Profile:", e);
  }

  await loadProvinces();
});

const openAddressModal = () => {
  editingAddressId.value = null;
  selectedProvinceCode.value = "";
  selectedWardCode.value = "";
  specificAddress.value = "";
  isDefaultAddress.value = false;
  wards.value = [];
  showAddressModal.value = true;
};

const closeAddressModal = () => {
  showAddressModal.value = false;
};

const editAddress = async (addr: any) => {
  editingAddressId.value = addr.id;
  selectedProvinceCode.value = addr.provinceCode ? String(addr.provinceCode) : "";
  if (selectedProvinceCode.value) {
    await loadWardsByProvince(selectedProvinceCode.value);
  }
  selectedWardCode.value = addr.wardCode ? String(addr.wardCode) : "";
  specificAddress.value = addr.specificAddress || "";
  isDefaultAddress.value = Boolean(addr.isDefault);
  showAddressModal.value = true;
};

const saveAddressNode = async () => {
  if (!selectedProvinceCode.value || !selectedWardCode.value || !specificAddress.value.trim()) {
    Swal.fire({ icon: "warning", title: "Thiếu thông tin", text: "Vui lòng chọn đầy đủ Tỉnh/Thành phố, Phường/Xã và nhập Địa chỉ cụ thể!" });
    return;
  }

  const payload = {
    customerId: currentCustomerId.value,
    recipientName: store.profileForm.name || "Khách hàng",
    phone: store.profileForm.phone || "",
    provinceCode: selectedProvinceCode.value,
    provinceName: selectedProvince.value?.name || "",
    wardCode: selectedWardCode.value,
    wardName: selectedWard.value?.name || "",
    specificAddress: specificAddress.value.trim(),
    fullAddress: `${specificAddress.value.trim()}, ${selectedWard.value?.name}, ${selectedProvince.value?.name}`,
    isDefault: isDefaultAddress.value
  };

  try {
    if (editingAddressId.value) {
      await customerAddressService.updateAddress(currentCustomerId.value, editingAddressId.value, payload);
    } else {
      await customerAddressService.addAddress(currentCustomerId.value, payload);
    }

    const addrRes = await customerAddressService.getAddresses(currentCustomerId.value);
    addressList.value = Array.isArray(addrRes.data) ? addrRes.data : [];

    closeAddressModal();
    Swal.fire({ toast: true, position: "top-end", icon: "success", title: "Lưu địa chỉ thành công", showConfirmButton: false, timer: 1500 });
  } catch (e) {
    Swal.fire({ toast: true, position: "top-end", icon: "error", title: "Lỗi lưu địa chỉ", showConfirmButton: false, timer: 1500 });
  }
};

const removeAddress = async (id: number) => {
  const result = await Swal.fire({ title: "Xóa địa chỉ?", icon: "warning", showCancelButton: true, confirmButtonText: "Xác nhận" });
  if (result.isConfirmed) {
    try {
      await customerAddressService.deleteAddress(currentCustomerId.value, id);
      addressList.value = addressList.value.filter(a => a.id !== id);
      Swal.fire({ toast: true, position: "top-end", icon: "success", title: "Đã xóa địa chỉ", showConfirmButton: false, timer: 1500 });
    } catch (e) {
      Swal.fire({ toast: true, position: "top-end", icon: "error", title: "Lỗi xóa địa chỉ", showConfirmButton: false, timer: 1500 });
    }
  }
};

const saveProfileInfo = async () => {
  store.profileLoading = true;
  try {
    await api.put("/customer/profile", store.profileForm);
    Swal.fire({ toast: true, position: "top-end", icon: "success", title: "Cập nhật hồ sơ thành công!", showConfirmButton: false, timer: 1500 });
  } catch (e) {
    Swal.fire({ toast: true, position: "top-end", icon: "error", title: "Có lỗi xảy ra", showConfirmButton: false, timer: 1500 });
  } finally {
    store.profileLoading = false;
  }
};

const handlePhoneInput = () => {
  store.profileForm.phone = store.profileForm.phone.replace(/[^\d]/g, "");
};

// Avatar logic
const avatarDisplayUrl = computed(() => store.avatarPreviewUrl || store.profileForm.avatarUrl || "");
const userInitial = computed(() => store.profileForm.name?.charAt(0).toUpperCase() || "U");
const cropImageStyle = computed(() => ({ transform: `translate(-50%, -50%) translate(${offsetX.value}px, ${offsetY.value}px) scale(${zoom.value})` }));

const revokeSourceUrl = () => { if (sourceAvatarUrl.value) URL.revokeObjectURL(sourceAvatarUrl.value); sourceAvatarUrl.value = ""; };
const handleAvatarChange = (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0] || null;
  input.value = "";
  if (!file) return;
  revokeSourceUrl();
  sourceAvatarFile.value = file;
  sourceAvatarUrl.value = URL.createObjectURL(file);
};
const resetImageAdjust = () => { zoom.value = 1; offsetX.value = 0; offsetY.value = 0; };
const startDrag = (e: MouseEvent) => { isDragging.value = true; dragStartX.value = e.clientX; dragStartY.value = e.clientY; dragOriginX.value = offsetX.value; dragOriginY.value = offsetY.value; };
const onDrag = (e: MouseEvent) => { if (!isDragging.value) return; offsetX.value = dragOriginX.value + e.clientX - dragStartX.value; offsetY.value = dragOriginY.value + e.clientY - dragStartY.value; };
const startTouchDrag = (e: TouchEvent) => { const t = e.touches[0]; if (!t) return; isDragging.value = true; dragStartX.value = t.clientX; dragStartY.value = t.clientY; dragOriginX.value = offsetX.value; dragOriginY.value = offsetY.value; };
const onTouchDrag = (e: TouchEvent) => { const t = e.touches[0]; if (!t || !isDragging.value) return; offsetX.value = dragOriginX.value + t.clientX - dragStartX.value; offsetY.value = dragOriginY.value + t.clientY - dragStartY.value; };
const stopDrag = () => { isDragging.value = false; };

const loadImage = (src: string): Promise<HTMLImageElement> => new Promise((res, rej) => { const img = new Image(); img.onload = () => res(img); img.onerror = rej; img.src = src; });
const createCroppedAvatarFile = async () => { return { file: new File([], ""), previewUrl: "" }; };
const cropAndUploadAvatar = async () => {};
onBeforeUnmount(() => revokeSourceUrl());
</script>

<style scoped>
.profile-avatar-section { display: flex; gap: 22px; align-items: flex-start; padding: 18px; border-radius: 18px; background: #f9fafb; border: 1px solid #eef0f3; }
.final-avatar-preview { width: 118px; height: 118px; border-radius: 50%; overflow: hidden; flex-shrink: 0; background: #111827; color: #ffffff; font-size: 42px; font-weight: 900; display: flex; align-items: center; justify-content: center; border: 4px solid #bd9a5f; }
.final-avatar-img { width: 100%; height: 100%; object-fit: cover; }
.avatar-actions { flex: 1; }
.avatar-editor { margin-top: 12px; padding: 14px; border-radius: 18px; background: #ffffff; border: 1px solid #e5e7eb; }
.crop-box { width: 260px; height: 260px; max-width: 100%; position: relative; overflow: hidden; border-radius: 18px; background: #111827; cursor: grab; user-select: none; touch-action: none; }
.crop-image { position: absolute; top: 50%; left: 50%; width: 100%; height: auto; object-fit: contain; }
.email-readonly { background: #f3f4f6; cursor: not-allowed; }
</style>