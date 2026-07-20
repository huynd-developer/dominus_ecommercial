<template>
  <div class="card border-0 shadow-sm">
    <div class="card-header bg-white border-0 py-3">
      <h5 class="mb-0 fw-bold">Thông tin cá nhân</h5>
    </div>

    <div class="card-body">
      <div class="profile-avatar-section mb-4">
        <div class="final-avatar-preview">
          <img v-if="avatarDisplayUrl" :src="avatarDisplayUrl" alt="Avatar" class="final-avatar-img" />
          <span v-else>{{ userInitial }}</span>
        </div>

        <div class="avatar-actions">
          <div class="fw-bold mb-2">Ảnh đại diện</div>
          <div class="text-muted small mb-3">Chọn ảnh, kéo để căn vị trí, phóng to/thu nhỏ rồi bấm “Cập nhật ảnh”.</div>
          <input ref="avatarInputRef" type="file" accept="image/jpeg,image/png,image/webp" class="d-none" @change="handleAvatarChange" />
          <div class="d-flex flex-wrap gap-2 mb-3">
            <button type="button" class="btn btn-outline-dark btn-sm" @click="avatarInputRef?.click()">Chọn ảnh</button>
            <button type="button" class="btn btn-outline-secondary btn-sm" :disabled="!sourceAvatarUrl || store.avatarLoading" @click="resetImageAdjust">Đặt lại ảnh</button>
            <button type="button" class="btn btn-success btn-sm" :disabled="!sourceAvatarUrl || store.avatarLoading" @click="cropAndUploadAvatar">
              <span v-if="store.avatarLoading" class="spinner-border spinner-border-sm me-1"></span> Cập nhật ảnh
            </button>
          </div>

          <div v-if="sourceAvatarUrl" class="avatar-editor">
            <div ref="cropBoxRef" class="crop-box" @mousedown="startDrag" @mousemove="onDrag" @mouseup="stopDrag" @mouseleave="stopDrag" @touchstart.prevent="startTouchDrag" @touchmove.prevent="onTouchDrag" @touchend="stopDrag">
              <img :src="sourceAvatarUrl" alt="Ảnh cần chỉnh" class="crop-image" :style="cropImageStyle" draggable="false" />
              <div class="crop-mask"></div>
              <div class="crop-circle"></div>
            </div>
            <div class="mt-3">
              <label class="form-label small fw-semibold">Phóng to / thu nhỏ</label>
              <input v-model.number="zoom" type="range" class="form-range" min="1" max="3" step="0.05" />
            </div>
          </div>
        </div>
      </div>

      <div class="row g-3">
        <div class="col-12 col-md-6">
          <label class="form-label fw-semibold">Họ tên</label>
          <input v-model="store.profileForm.name" type="text" class="form-control" placeholder="Nhập họ tên" />
        </div>

        <div class="col-12 col-md-6">
          <label class="form-label fw-semibold">Email đăng nhập</label>
          <input v-model="store.profileForm.email" type="email" class="form-control email-readonly" disabled readonly />
          <div class="form-text">Email dùng để đăng nhập nên không thể tự thay đổi.</div>
        </div>

        <div class="col-12 col-md-6">
          <label class="form-label fw-semibold">Số điện thoại</label>
          <input v-model="store.profileForm.phone" type="text" maxlength="10" class="form-control" placeholder="0987654321" @input="handlePhoneInput" />
        </div>

        <div class="col-12 col-md-6">
          <label class="form-label fw-semibold">Ngày sinh</label>
          <input v-model="store.profileForm.dateOfBirth" type="date" class="form-control" />
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

        <div v-if="addressList.length === 0" class="text-muted text-center py-4 border rounded bg-light">
          Bạn chưa lưu địa chỉ nào.
        </div>
        
        <div v-else class="row g-3">
          <div v-for="(addr, index) in addressList" :key="index" class="col-12">
            <div class="card border p-3 d-flex flex-row justify-content-between align-items-center">
              <p class="mb-0 fw-semibold"><i class="bi bi-geo-alt-fill text-danger me-2"></i> {{ addr.fullAddress }}</p>
              <div>
                <button class="btn btn-sm btn-outline-primary me-2" @click="editAddress(index)">Sửa</button>
                <button class="btn btn-sm btn-outline-danger" @click="removeAddress(index)">Xóa</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="text-end mt-4 pt-3 border-top">
        <button class="btn btn-dark px-4" :disabled="store.profileLoading" @click="saveProfile">
          <span v-if="store.profileLoading" class="spinner-border spinner-border-sm me-2"></span>
          Cập nhật thông tin
        </button>
      </div>
    </div>

    <!-- MODAL THÊM / SỬA ĐỊA CHỈ -->
    <Teleport to="body">
      <div v-if="showAddressModal" class="custom-modal-overlay d-flex align-items-center justify-content-center" style="position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 1050;">
        <div class="bg-white p-4 rounded shadow-lg" style="width: 100%; max-width: 600px;">
          <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-2">
            <h5 class="fw-bold mb-0">{{ editingIndex > -1 ? 'Sửa địa chỉ' : 'Thêm địa chỉ nhận hàng' }}</h5>
            <button type="button" class="btn-close" @click="closeAddressModal"></button>
          </div>
          
          <div class="row mb-3">
            <div class="col-md-6">
              <label class="form-label fw-bold">Tỉnh / Thành phố <span class="text-danger">*</span></label>
              <select class="form-select" v-model="selectedProvinceCode" @change="handleProvinceChange" :disabled="loadingProvinces">
                <option value="">{{ loadingProvinces ? "Đang tải..." : "Chọn tỉnh/thành phố" }}</option>
                <option v-for="p in provinces" :key="p.code" :value="p.code">{{ p.name }}</option>
              </select>
            </div>
            <div class="col-md-6 mt-3 mt-md-0">
              <label class="form-label fw-bold">Phường / Xã / Đặc khu <span class="text-danger">*</span></label>
              <select class="form-select" v-model="selectedWardCode" :disabled="!selectedProvinceCode || loadingWards">
                <option value="">{{ !selectedProvinceCode ? "Chọn tỉnh/thành trước" : loadingWards ? "Đang tải..." : "Chọn phường/xã" }}</option>
                <option v-for="w in wards" :key="w.code" :value="w.code">{{ w.name }}</option>
              </select>
            </div>
          </div>

          <div class="mb-3">
            <label class="form-label fw-bold">Địa chỉ cụ thể <span class="text-danger">*</span></label>
            <textarea class="form-control" v-model="specificAddress" rows="3" placeholder="Ví dụ: Số 12/5, ngõ 36-A, đường Trần Phú"></textarea>
            <small class="form-text text-muted">Nhập số nhà, ngõ, đường, tòa nhà. Không nhập ký tự lạ như @ $ %.</small>
          </div>

          <div class="d-flex justify-content-end gap-2 mt-4">
            <button class="btn btn-light px-4" @click="closeAddressModal">Hủy</button>
            <button class="btn btn-dark px-4" @click="saveAddressNode">Lưu địa chỉ</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, onMounted } from "vue";
import { useCustomerProfileStore } from "../stores/customerProfile.store";

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

// --- LOGIC ĐỊA CHỈ & TỈNH THÀNH ---
const addressList = ref<any[]>([]);
const showAddressModal = ref(false);
const editingIndex = ref(-1);

const provinces = ref<Province[]>([]);
const wards = ref<Ward[]>([]);
const selectedProvinceCode = ref<string>("");
const selectedWardCode = ref<string>("");
const specificAddress = ref("");
const loadingProvinces = ref(false);
const loadingWards = ref(false);

const selectedProvince = computed(() => provinces.value.find((item) => String(item.code) === String(selectedProvinceCode.value)));
const selectedWard = computed(() => wards.value.find((item) => String(item.code) === String(selectedWardCode.value)));

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
            allWards.push({
              code: ward.code,
              name: `${ward.name}, ${district.name}`
            });
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
    const rawAddress = store.profileForm.address;
    if (rawAddress && rawAddress.startsWith('[')) {
      addressList.value = JSON.parse(rawAddress);
    } else if (rawAddress) {
      addressList.value = [{ fullAddress: rawAddress }];
    }
  } catch (e) {
    console.error("Lỗi parse địa chỉ:", e);
  }
  
  await loadProvinces();
});

const openAddressModal = () => {
  editingIndex.value = -1;
  selectedProvinceCode.value = "";
  selectedWardCode.value = "";
  specificAddress.value = "";
  wards.value = [];
  showAddressModal.value = true;
};

const closeAddressModal = () => {
  showAddressModal.value = false;
};

const editAddress = async (index: number) => {
  editingIndex.value = index;
  const addr = addressList.value[index];
  
  if (addr.provinceCode && addr.wardCode) {
    selectedProvinceCode.value = String(addr.provinceCode);
    await loadWardsByProvince(selectedProvinceCode.value);
    selectedWardCode.value = String(addr.wardCode);
    specificAddress.value = addr.specificAddress || "";
  } else {
    // Dành cho data cũ chưa có cấu trúc
    selectedProvinceCode.value = "";
    selectedWardCode.value = "";
    specificAddress.value = addr.fullAddress || "";
  }
  showAddressModal.value = true;
};

const saveAddressNode = () => {
  if (!selectedProvinceCode.value || !selectedWardCode.value || !specificAddress.value.trim()) {
    alert("Vui lòng chọn đầy đủ Tỉnh/Thành phố, Phường/Xã và nhập Địa chỉ cụ thể!");
    return;
  }

  const pName = selectedProvince.value?.name || "";
  const wName = selectedWard.value?.name || "";
  const addressDetail = specificAddress.value.trim();

  const fullAddr = [addressDetail, wName, pName].filter(Boolean).join(", ");

  const addrObj = {
    provinceCode: selectedProvinceCode.value,
    wardCode: selectedWardCode.value,
    specificAddress: addressDetail,
    fullAddress: fullAddr
  };

  if (editingIndex.value > -1) {
    addressList.value[editingIndex.value] = addrObj;
  } else {
    addressList.value.push(addrObj);
  }
  
  closeAddressModal();
};

const removeAddress = (index: number) => {
  if(confirm("Bạn có chắc chắn muốn xóa địa chỉ này?")) {
    addressList.value.splice(index, 1);
  }
};

const saveProfile = () => {
  if (addressList.value.length > 0) {
    store.profileForm.address = JSON.stringify(addressList.value);
  } else {
    store.profileForm.address = "";
  }
  store.updateProfile();
};

const handlePhoneInput = () => {
  store.profileForm.phone = store.profileForm.phone.replace(/[^\d]/g, "");
};

// --- LOGIC AVATAR GIỮ NGUYÊN ---
const avatarDisplayUrl = computed(() => store.avatarPreviewUrl || store.profileForm.avatarUrl || "");
const userInitial = computed(() => { const name = store.profileForm.name.trim(); if (!name) return "U"; const parts = name.split(" ").filter(Boolean); const last = parts[parts.length - 1]; return last?.charAt(0).toUpperCase() || "U"; });
const cropImageStyle = computed(() => ({ transform: `translate(-50%, -50%) translate(${offsetX.value}px, ${offsetY.value}px) scale(${zoom.value})` }));

const revokeSourceUrl = () => { if (sourceAvatarUrl.value) URL.revokeObjectURL(sourceAvatarUrl.value); sourceAvatarUrl.value = ""; };
const handleAvatarChange = (event: Event) => { const input = event.target as HTMLInputElement; const file = input.files?.[0] || null; input.value = ""; if (!file) return; const validateMessage = store.validateAvatarFile(file); if (validateMessage) { store.setError(validateMessage); return; } revokeSourceUrl(); sourceAvatarFile.value = file; sourceAvatarUrl.value = URL.createObjectURL(file); store.selectedAvatarFile = null; store.avatarPreviewUrl = store.profileForm.avatarUrl || ""; resetImageAdjust(); };
const resetImageAdjust = () => { zoom.value = 1; offsetX.value = 0; offsetY.value = 0; };
const startDrag = (event: MouseEvent) => { isDragging.value = true; dragStartX.value = event.clientX; dragStartY.value = event.clientY; dragOriginX.value = offsetX.value; dragOriginY.value = offsetY.value; };
const onDrag = (event: MouseEvent) => { if (!isDragging.value) return; offsetX.value = dragOriginX.value + event.clientX - dragStartX.value; offsetY.value = dragOriginY.value + event.clientY - dragStartY.value; };
const startTouchDrag = (event: TouchEvent) => { const touch = event.touches[0]; if (!touch) return; isDragging.value = true; dragStartX.value = touch.clientX; dragStartY.value = touch.clientY; dragOriginX.value = offsetX.value; dragOriginY.value = offsetY.value; };
const onTouchDrag = (event: TouchEvent) => { const touch = event.touches[0]; if (!touch || !isDragging.value) return; offsetX.value = dragOriginX.value + touch.clientX - dragStartX.value; offsetY.value = dragOriginY.value + touch.clientY - dragStartY.value; };
const stopDrag = () => { isDragging.value = false; };

const loadImage = (src: string): Promise<HTMLImageElement> => { return new Promise((resolve, reject) => { const image = new Image(); image.onload = () => resolve(image); image.onerror = () => reject(new Error("Không đọc được ảnh")); image.src = src; }); };
const createCroppedAvatarFile = async (): Promise<{ file: File; previewUrl: string; }> => { if (!sourceAvatarUrl.value || !sourceAvatarFile.value) throw new Error("Vui lòng chọn ảnh đại diện"); const cropBox = cropBoxRef.value; if (!cropBox) throw new Error("Không tìm thấy vùng chỉnh ảnh"); const image = await loadImage(sourceAvatarUrl.value); const cropSize = cropBox.clientWidth; const outputSize = 500; const imageRatio = image.naturalWidth / image.naturalHeight; let baseWidth = cropSize; let baseHeight = cropSize; if (imageRatio > 1) { baseWidth = cropSize * imageRatio; } else { baseHeight = cropSize / imageRatio; } const canvas = document.createElement("canvas"); canvas.width = outputSize; canvas.height = outputSize; const ctx = canvas.getContext("2d"); if (!ctx) throw new Error("Trình duyệt không hỗ trợ chỉnh ảnh"); const scaleToCanvas = outputSize / cropSize; ctx.fillStyle = "#ffffff"; ctx.fillRect(0, 0, outputSize, outputSize); ctx.save(); ctx.translate(outputSize / 2 + offsetX.value * scaleToCanvas, outputSize / 2 + offsetY.value * scaleToCanvas); ctx.scale(zoom.value * scaleToCanvas, zoom.value * scaleToCanvas); ctx.drawImage(image, -baseWidth / 2, -baseHeight / 2, baseWidth, baseHeight); ctx.restore(); return new Promise((resolve, reject) => { canvas.toBlob((blob) => { if (!blob) { reject(new Error("Không thể tạo ảnh sau khi chỉnh")); return; } const file = new File([blob], "avatar-cropped.webp", { type: "image/webp" }); resolve({ file, previewUrl: URL.createObjectURL(blob) }); }, "image/webp", 0.92); }); };
const cropAndUploadAvatar = async () => { try { const result = await createCroppedAvatarFile(); store.selectAvatarFile(result.file, result.previewUrl); await store.uploadAvatar(); if (!store.selectedAvatarFile) { revokeSourceUrl(); sourceAvatarFile.value = null; } } catch (error: any) { store.setError(error?.message || "Chỉnh ảnh thất bại."); } };
onBeforeUnmount(() => { revokeSourceUrl(); });
</script>

<style scoped>
.profile-avatar-section { display: flex; gap: 22px; align-items: flex-start; padding: 18px; border-radius: 18px; background: #f9fafb; border: 1px solid #eef0f3; }
.final-avatar-preview { width: 118px; height: 118px; border-radius: 50%; overflow: hidden; flex-shrink: 0; background: #111827; color: #ffffff; font-size: 42px; font-weight: 900; display: flex; align-items: center; justify-content: center; border: 4px solid #bd9a5f; box-shadow: 0 10px 26px rgba(189, 154, 95, 0.28); }
.final-avatar-img { width: 100%; height: 100%; object-fit: cover; }
.avatar-actions { flex: 1; }
.avatar-editor { margin-top: 12px; padding: 14px; border-radius: 18px; background: #ffffff; border: 1px solid #e5e7eb; }
.crop-box { width: 260px; height: 260px; max-width: 100%; position: relative; overflow: hidden; border-radius: 18px; background: #111827; cursor: grab; user-select: none; touch-action: none; }
.crop-box:active { cursor: grabbing; }
.crop-image { position: absolute; top: 50%; left: 50%; width: 100%; height: auto; min-height: 100%; object-fit: contain; transform-origin: center center; pointer-events: none; }
.crop-mask { position: absolute; inset: 0; background: radial-gradient(circle at center, transparent 0 48%, rgba(0, 0, 0, 0.48) 49% 100%); pointer-events: none; }
.crop-circle { position: absolute; inset: 18px; border-radius: 50%; border: 2px solid #bd9a5f; box-shadow: 0 0 0 999px rgba(0, 0, 0, 0.15); pointer-events: none; }
.email-readonly { background: #f3f4f6; cursor: not-allowed; }
@media (max-width: 768px) { .profile-avatar-section { flex-direction: column; } .crop-box { width: 230px; height: 230px; } }
</style>