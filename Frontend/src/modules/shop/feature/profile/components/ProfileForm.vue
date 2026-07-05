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

          <span v-else>
            {{ userInitial }}
          </span>
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
              <label class="form-label small fw-semibold">
                Phóng to / thu nhỏ
              </label>

              <input
                v-model.number="zoom"
                type="range"
                class="form-range"
                min="1"
                max="3"
                step="0.05"
              />
            </div>

            <div class="text-muted small">
              Giữ chuột và kéo ảnh để chọn đúng vùng ảnh mong muốn.
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

        <div class="col-12">
          <label class="form-label fw-semibold">Địa chỉ</label>
          <textarea
            v-model="store.profileForm.address"
            rows="3"
            class="form-control"
            placeholder="Nhập địa chỉ nhận hàng"
          ></textarea>
        </div>
      </div>

      <div class="text-end mt-4">
        <button
          class="btn btn-dark px-4"
          :disabled="store.profileLoading"
          @click="store.updateProfile()"
        >
          <span
            v-if="store.profileLoading"
            class="spinner-border spinner-border-sm me-2"
          ></span>
          Cập nhật thông tin
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from "vue";
import { useCustomerProfileStore } from "../stores/customerProfile.store";

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

const avatarDisplayUrl = computed(() => {
  return store.avatarPreviewUrl || store.profileForm.avatarUrl || "";
});

const userInitial = computed(() => {
  const name = store.profileForm.name.trim();

  if (!name) {
    return "U";
  }

  const parts = name.split(" ").filter(Boolean);
  const last = parts[parts.length - 1];

  return last?.charAt(0).toUpperCase() || "U";
});

const cropImageStyle = computed(() => ({
  transform: `translate(-50%, -50%) translate(${offsetX.value}px, ${offsetY.value}px) scale(${zoom.value})`,
}));

const revokeSourceUrl = () => {
  if (sourceAvatarUrl.value) {
    URL.revokeObjectURL(sourceAvatarUrl.value);
  }

  sourceAvatarUrl.value = "";
};

const handleAvatarChange = (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0] || null;

  input.value = "";

  if (!file) {
    return;
  }

  const validateMessage = store.validateAvatarFile(file);

  if (validateMessage) {
    store.setError(validateMessage);
    return;
  }

  revokeSourceUrl();

  sourceAvatarFile.value = file;
  sourceAvatarUrl.value = URL.createObjectURL(file);

  store.selectedAvatarFile = null;
  store.avatarPreviewUrl = store.profileForm.avatarUrl || "";

  resetImageAdjust();
};

const resetImageAdjust = () => {
  zoom.value = 1;
  offsetX.value = 0;
  offsetY.value = 0;
};

const startDrag = (event: MouseEvent) => {
  isDragging.value = true;
  dragStartX.value = event.clientX;
  dragStartY.value = event.clientY;
  dragOriginX.value = offsetX.value;
  dragOriginY.value = offsetY.value;
};

const onDrag = (event: MouseEvent) => {
  if (!isDragging.value) {
    return;
  }

  offsetX.value = dragOriginX.value + event.clientX - dragStartX.value;
  offsetY.value = dragOriginY.value + event.clientY - dragStartY.value;
};

const startTouchDrag = (event: TouchEvent) => {
  const touch = event.touches[0];

  if (!touch) {
    return;
  }

  isDragging.value = true;
  dragStartX.value = touch.clientX;
  dragStartY.value = touch.clientY;
  dragOriginX.value = offsetX.value;
  dragOriginY.value = offsetY.value;
};

const onTouchDrag = (event: TouchEvent) => {
  const touch = event.touches[0];

  if (!touch || !isDragging.value) {
    return;
  }

  offsetX.value = dragOriginX.value + touch.clientX - dragStartX.value;
  offsetY.value = dragOriginY.value + touch.clientY - dragStartY.value;
};

const stopDrag = () => {
  isDragging.value = false;
};

const loadImage = (src: string): Promise<HTMLImageElement> => {
  return new Promise((resolve, reject) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error("Không đọc được ảnh"));
    image.src = src;
  });
};

const createCroppedAvatarFile = async (): Promise<{
  file: File;
  previewUrl: string;
}> => {
  if (!sourceAvatarUrl.value || !sourceAvatarFile.value) {
    throw new Error("Vui lòng chọn ảnh đại diện");
  }

  const cropBox = cropBoxRef.value;

  if (!cropBox) {
    throw new Error("Không tìm thấy vùng chỉnh ảnh");
  }

  const image = await loadImage(sourceAvatarUrl.value);

  const cropSize = cropBox.clientWidth;
  const outputSize = 500;

  const imageRatio = image.naturalWidth / image.naturalHeight;

  let baseWidth = cropSize;
  let baseHeight = cropSize;

  if (imageRatio > 1) {
    baseHeight = cropSize;
    baseWidth = cropSize * imageRatio;
  } else {
    baseWidth = cropSize;
    baseHeight = cropSize / imageRatio;
  }

  const canvas = document.createElement("canvas");
  canvas.width = outputSize;
  canvas.height = outputSize;

  const ctx = canvas.getContext("2d");

  if (!ctx) {
    throw new Error("Trình duyệt không hỗ trợ chỉnh ảnh");
  }

  const scaleToCanvas = outputSize / cropSize;

  ctx.fillStyle = "#ffffff";
  ctx.fillRect(0, 0, outputSize, outputSize);

  ctx.save();

  ctx.translate(
    outputSize / 2 + offsetX.value * scaleToCanvas,
    outputSize / 2 + offsetY.value * scaleToCanvas
  );

  ctx.scale(zoom.value * scaleToCanvas, zoom.value * scaleToCanvas);

  ctx.drawImage(
    image,
    -baseWidth / 2,
    -baseHeight / 2,
    baseWidth,
    baseHeight
  );

  ctx.restore();

  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (!blob) {
          reject(new Error("Không thể tạo ảnh sau khi chỉnh"));
          return;
        }

        const file = new File([blob], "avatar-cropped.webp", {
          type: "image/webp",
        });

        resolve({
          file,
          previewUrl: URL.createObjectURL(blob),
        });
      },
      "image/webp",
      0.92
    );
  });
};

const cropAndUploadAvatar = async () => {
  try {
    const result = await createCroppedAvatarFile();

    store.selectAvatarFile(result.file, result.previewUrl);

    await store.uploadAvatar();

    if (!store.selectedAvatarFile) {
      revokeSourceUrl();
      sourceAvatarFile.value = null;
    }
  } catch (error: any) {
    store.setError(error?.message || "Chỉnh ảnh thất bại. Vui lòng chọn ảnh khác.");
  }
};

const handlePhoneInput = () => {
  store.profileForm.phone = store.profileForm.phone.replace(/[^\d]/g, "");
};

onBeforeUnmount(() => {
  revokeSourceUrl();
});
</script>

<style scoped>
.profile-avatar-section {
  display: flex;
  gap: 22px;
  align-items: flex-start;
  padding: 18px;
  border-radius: 18px;
  background: #f9fafb;
  border: 1px solid #eef0f3;
}

.final-avatar-preview {
  width: 118px;
  height: 118px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background: #111827;
  color: #ffffff;
  font-size: 42px;
  font-weight: 900;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 4px solid #bd9a5f;
  box-shadow: 0 10px 26px rgba(189, 154, 95, 0.28);
}

.final-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-actions {
  flex: 1;
}

.avatar-editor {
  margin-top: 12px;
  padding: 14px;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
}

.crop-box {
  width: 260px;
  height: 260px;
  max-width: 100%;
  position: relative;
  overflow: hidden;
  border-radius: 18px;
  background: #111827;
  cursor: grab;
  user-select: none;
  touch-action: none;
}

.crop-box:active {
  cursor: grabbing;
}

.crop-image {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 100%;
  height: auto;
  min-height: 100%;
  object-fit: contain;
  transform-origin: center center;
  pointer-events: none;
}

.crop-mask {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    circle at center,
    transparent 0 48%,
    rgba(0, 0, 0, 0.48) 49% 100%
  );
  pointer-events: none;
}

.crop-circle {
  position: absolute;
  inset: 18px;
  border-radius: 50%;
  border: 2px solid #bd9a5f;
  box-shadow: 0 0 0 999px rgba(0, 0, 0, 0.15);
  pointer-events: none;
}

.email-readonly {
  background: #f3f4f6;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .profile-avatar-section {
    flex-direction: column;
  }

  .crop-box {
    width: 230px;
    height: 230px;
  }
}
</style>