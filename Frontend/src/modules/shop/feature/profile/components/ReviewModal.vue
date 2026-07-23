<template>
  <Teleport to="body">
    <Transition name="fade">
      <!-- 1. Nhúng CSS trực tiếp vào đây để chống mất style, dùng @mousedown.self -->
      <div 
        v-if="modelValue" 
        style="position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background: rgba(0, 0, 0, 0.6); display: flex; align-items: center; justify-content: center; z-index: 1050;" 
        @mousedown.self="closeModal"
      >
        <!-- 2. Hộp trắng chứa form -->
        <div style="background: #fff; width: 100%; max-width: 500px; border-radius: 12px; box-shadow: 0 10px 25px rgba(0,0,0,0.15); display: flex; flex-direction: column; max-height: 90vh;">
          
          <div class="modal-header" style="padding: 16px; border-bottom: 1px solid #dee2e6; display: flex; justify-content: space-between; align-items: center;">
            <h5 class="fw-bold mb-0">Đánh giá sản phẩm</h5>
            <button type="button" class="btn-close" @click="closeModal"></button>
          </div>

          <!-- modal-body có thanh cuộn nếu nội dung dài -->
          <div class="modal-body" v-if="item" style="padding: 16px; overflow-y: auto;">
            <!-- Thông tin sản phẩm -->
            <div class="product-info-mini mb-4">
              <img :src="item.image || fallbackImage" class="mini-img" alt="Product" />
              <div class="mini-details">
                <div class="mini-name">{{ item.productName }}</div>
                <div class="mini-variant text-muted small">Phân loại: {{ item.capacity || 'Đang cập nhật' }}</div>
              </div>
            </div>

            <!-- Đánh giá sao -->
            <div class="rating-section text-center mb-4">
              <div class="mb-2 fw-bold">Chất lượng sản phẩm</div>
              <div class="stars-container">
                <i
                  v-for="star in 5"
                  :key="star"
                  class="bi star-icon"
                  :class="star <= rating ? 'bi-star-fill text-warning' : 'bi-star text-muted'"
                  @click="rating = star"
                  @mouseover="hoverRating = star"
                  @mouseleave="hoverRating = 0"
                  :style="{ color: star <= (hoverRating || rating) ? '#bd9a5f !important' : '' }"
                ></i>
              </div>
              <div class="rating-text text-muted small mt-1">{{ getRatingText(rating) }}</div>
            </div>

            <!-- Nhập nội dung -->
            <div class="review-content mb-3">
              <textarea
                v-model="comment"
                class="form-control review-textarea"
                rows="4"
                placeholder="Hãy chia sẻ nhận xét của bạn về sản phẩm này nhé (tối thiểu 10 ký tự)..."
              ></textarea>
            </div>

            <!-- Upload Ảnh/Video -->
            <div class="media-upload-section">
              <div class="media-preview-list">
                <div v-for="(media, index) in previewUrls" :key="index" class="media-preview-item">
                  <img v-if="media.type === 'image'" :src="media.url" class="preview-media" />
                  <video v-else-if="media.type === 'video'" :src="media.url" class="preview-media"></video>
                  <button class="btn-remove-media" @click="removeMedia(index)">
                    <i class="bi bi-x"></i>
                  </button>
                </div>

                <div v-if="selectedFiles.length < 5" class="upload-btn-wrapper" @click="triggerFileInput">
                  <i class="bi bi-camera fs-4 text-muted"></i>
                  <span class="small text-muted mt-1">Thêm Ảnh/Video</span>
                  <input
                    ref="fileInput"
                    type="file"
                    multiple
                    accept="image/*, video/*"
                    class="d-none"
                    @change="handleFileSelect"
                  />
                </div>
              </div>
              <div class="small text-muted mt-2">Tối đa 5 file. Giới hạn dung lượng: 5MB/file.</div>
            </div>
          </div>

          <div class="modal-footer" style="padding: 16px; border-top: 1px solid #dee2e6; display: flex; justify-content: flex-end; gap: 8px;">
            <button type="button" class="btn btn-outline-secondary" @click="closeModal" :disabled="loading">
              Trở lại
            </button>
            <button type="button" class="btn btn-primary btn-submit" @click="handleSubmit" :disabled="loading || !isValid">
              <span v-if="loading" class="spinner-border spinner-border-sm me-1"></span>
              Hoàn thành
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue';
import Swal from 'sweetalert2';

const props = defineProps<{
  modelValue: boolean;
  item: any;
  loading: boolean;
}>();

const emit = defineEmits(['update:modelValue', 'submit']);

const fallbackImage = "data:image/svg+xml;utf8," + encodeURIComponent(`<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"><rect width="100%" height="100%" fill="#f3f4f6"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="#9ca3af" font-family="Arial" font-size="12">Không có ảnh</text></svg>`);

// State
const rating = ref(5);
const hoverRating = ref(0);
const comment = ref('');
const selectedFiles = ref<File[]>([]);
const previewUrls = ref<{ type: string; url: string }[]>([]);
const fileInput = ref<HTMLInputElement | null>(null);

// Reset modal khi mở
watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    rating.value = 5;
    comment.value = '';
    selectedFiles.value = [];
    previewUrls.value.forEach(p => URL.revokeObjectURL(p.url)); // Xóa bộ nhớ
    previewUrls.value = [];
  }
});

const getRatingText = (val: number) => {
  const texts = ['Tệ', 'Không hài lòng', 'Bình thường', 'Hài lòng', 'Tuyệt vời'];
  return texts[val - 1] || '';
};

const isValid = computed(() => {
  return rating.value > 0;
});

const closeModal = () => {
  if (props.loading) return;
  emit('update:modelValue', false);
};

const triggerFileInput = () => {
  fileInput.value?.click();
};

const handleFileSelect = (event: Event) => {
  const input = event.target as HTMLInputElement;
  const files = input.files;
  if (!files) return;

  const MAX_SIZE = 5 * 1024 * 1024; // 5MB
  let hasOversizedFile = false;

  Array.from(files).forEach((file) => {
    // Chặn quá 5 file
    if (selectedFiles.value.length >= 5) return;

    // Chặn file > 5MB
    if (file.size > MAX_SIZE) {
      hasOversizedFile = true;
      return;
    }

    selectedFiles.value.push(file);
    previewUrls.value.push({
      type: file.type.startsWith('video/') ? 'video' : 'image',
      url: URL.createObjectURL(file)
    });
  });

  if (hasOversizedFile) {
    Swal.fire({
      icon: 'warning',
      title: 'File quá lớn',
      text: 'Một số file đã bị loại bỏ vì vượt quá dung lượng 5MB.',
      confirmButtonColor: '#bd9a5f'
    });
  }

  // Reset input để có thể chọn lại đúng file đó nếu lỡ xóa
  input.value = '';
};

const removeMedia = (index: number) => {
  if (previewUrls.value[index]) {
    URL.revokeObjectURL(previewUrls.value[index].url);
  }
  previewUrls.value.splice(index, 1);
  selectedFiles.value.splice(index, 1);
};

const handleSubmit = () => {
  // Gửi data kèm file về cho OrderHistory.vue xử lý
  emit('submit', {
    rating: rating.value,
    comment: comment.value,
    files: selectedFiles.value
  });
};
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
}

.modal-dialog {
  background: #fff;
  width: 100%;
  max-width: 500px;
  border-radius: 12px;
  box-shadow: 0 10px 25px rgba(0,0,0,0.1);
  overflow: hidden;
}

.modal-header {
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-body {
  padding: 20px;
  max-height: 70vh;
  overflow-y: auto;
}

.product-info-mini {
  display: flex;
  gap: 12px;
  align-items: center;
  background: #f8fafc;
  padding: 10px;
  border-radius: 8px;
}

.mini-img {
  width: 50px;
  height: 50px;
  object-fit: cover;
  border-radius: 6px;
}

.mini-name {
  font-weight: 600;
  font-size: 14px;
}

.star-icon {
  font-size: 32px;
  cursor: pointer;
  margin: 0 4px;
  transition: transform 0.1s;
}

.star-icon:hover {
  transform: scale(1.1);
}

.review-textarea {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  resize: none;
}

.review-textarea:focus {
  border-color: #bd9a5f;
  box-shadow: 0 0 0 0.25rem rgba(189, 154, 95, 0.25);
}

.media-preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.media-preview-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.preview-media {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
}

.btn-remove-media {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #ef4444;
  color: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  cursor: pointer;
  z-index: 2;
}

.upload-btn-wrapper {
  width: 80px;
  height: 80px;
  border: 1px dashed #cbd5e0;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: #f8fafc;
  transition: 0.2s;
}

.upload-btn-wrapper:hover {
  border-color: #bd9a5f;
  color: #bd9a5f;
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.btn-submit {
  background-color: #bd9a5f;
  border-color: #bd9a5f;
}

.btn-submit:hover:not(:disabled) {
  background-color: #a8864d;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99999; /* Tăng hẳn lên nóc nhà */
}
</style>