<template>
  <Teleport to="body">
    <Transition name="fade">
      <!-- 1. Nhúng CSS trực tiếp vào đây để chống mất style, dùng @mousedown.self -->
      <div
        v-if="modelValue"
        style="
          position: fixed;
          top: 0;
          left: 0;
          width: 100vw;
          height: 100vh;
          background: rgba(0, 0, 0, 0.6);
          display: flex;
          align-items: center;
          justify-content: center;
          z-index: 1050;
        "
        @mousedown.self="closeModal"
      >
        <!-- 2. Hộp trắng chứa form -->
        <div
          style="
            background: #fff;
            width: 100%;
            max-width: 500px;
            border-radius: 12px;
            box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
            display: flex;
            flex-direction: column;
            max-height: 90vh;
          "
        >
          <div
            class="modal-header"
            style="
              padding: 16px;
              border-bottom: 1px solid #dee2e6;
              display: flex;
              justify-content: space-between;
              align-items: center;
            "
          >
            <h5 class="fw-bold mb-0">{{ modalTitle }}</h5>
            <button
              type="button"
              class="btn-close"
              @click="closeModal"
            ></button>
          </div>

          <!-- modal-body có thanh cuộn nếu nội dung dài -->
          <div
            class="modal-body"
            v-if="item"
            style="padding: 16px; overflow-y: auto"
          >
            <!-- Thông tin sản phẩm -->
            <div class="product-info-mini mb-4">
              <img
                :src="item.image || fallbackImage"
                class="mini-img"
                alt="Product"
              />
              <div class="mini-details">
                <div class="mini-name">{{ item.productName }}</div>
                <div v-if="getVariantText(item)" class="mini-variant">
                  {{ getVariantText(item) }}
                </div>
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
                  :class="
                    star <= rating
                      ? 'bi-star-fill text-warning'
                      : 'bi-star text-muted'
                  "
                  @click="rating = star"
                  @mouseover="hoverRating = star"
                  @mouseleave="hoverRating = 0"
                  :style="{
                    color:
                      star <= (hoverRating || rating)
                        ? '#bd9a5f !important'
                        : '',
                  }"
                ></i>
              </div>
              <div class="rating-text text-muted small mt-1">
                {{ getRatingText(rating) }}
              </div>
            </div>

            <!-- Nhập nội dung -->
            <div class="review-content mb-3">
              <textarea
                v-model="comment"
                class="form-control review-textarea"
                rows="4"
                maxlength="500"
                placeholder="Hãy chia sẻ nhận xét của bạn về sản phẩm này nhé (tối đa 500 ký tự)..."
              ></textarea>
              <!-- BỘ ĐẾM KÝ TỰ -->
              <div class="text-end small mt-1 text-muted">
                {{ comment.length }}/500
              </div>
            </div>

            <!-- Upload Ảnh/Video -->
            <div class="media-upload-section">
              <div
                v-if="isEditMode && existingReviewMedia.length > 0"
                class="existing-media-section mb-3"
              >
                <div class="existing-media-title">Ảnh/video hiện tại:</div>

                <div class="media-preview-list">
                  <div
                    v-for="(media, index) in existingReviewMedia"
                    :key="`existing-${media.url}-${index}`"
                    class="media-preview-item existing-media-item"
                  >
                    <img
                      v-if="!media.isVideo"
                      :src="media.url"
                      class="preview-media"
                      alt="Ảnh đánh giá hiện tại"
                    />
                    <video
                      v-else
                      :src="media.url"
                      class="preview-media"
                      muted
                      playsinline
                      preload="metadata"
                    ></video>

                    <span class="existing-media-badge">Hiện tại</span>

                    <button
                      v-if="media.mediaId"
                      type="button"
                      class="btn-remove-media existing-media-remove"
                      title="Xóa ảnh/video này khỏi đánh giá"
                      @click.stop="markExistingMediaForDelete(media)"
                    >
                      <i class="bi bi-x"></i>
                    </button>
                  </div>
                </div>
              </div>

              <div v-if="isEditMode" class="existing-media-title mb-2">
                Thêm ảnh/video mới:
              </div>

              <div class="media-preview-list">
                <div
                  v-for="(media, index) in previewUrls"
                  :key="index"
                  class="media-preview-item"
                >
                  <img
                    v-if="media.type === 'image'"
                    :src="media.url"
                    class="preview-media"
                  />
                  <video
                    v-else-if="media.type === 'video'"
                    :src="media.url"
                    class="preview-media"
                  ></video>
                  <button class="btn-remove-media" @click="removeMedia(index)">
                    <i class="bi bi-x"></i>
                  </button>
                </div>

                <div
                  v-if="selectedFiles.length < 5"
                  class="upload-btn-wrapper"
                  @click="triggerFileInput"
                >
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
              <div class="small text-muted mt-2">
                Tối đa 5 file mới. Giới hạn dung lượng: 5MB/file.
              </div>
              <div v-if="isEditMode" class="small text-muted mt-1">
                Ảnh/video cũ được giữ nguyên nếu không bấm X. Ảnh/video mới sẽ
                được thêm vào đánh giá hiện tại.
              </div>
            </div>
          </div>

          <div
            class="modal-footer"
            style="
              padding: 16px;
              border-top: 1px solid #dee2e6;
              display: flex;
              justify-content: flex-end;
              gap: 8px;
            "
          >
            <button
              type="button"
              class="btn btn-outline-secondary"
              @click="closeModal"
              :disabled="loading"
            >
              Trở lại
            </button>
            <button
              type="button"
              class="btn btn-primary btn-submit"
              @click="handleSubmit"
              :disabled="loading || !isValid"
            >
              <span
                v-if="loading"
                class="spinner-border spinner-border-sm me-1"
              ></span>
              {{ submitButtonText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch, computed } from "vue";
import Swal from "sweetalert2";

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    item: any;
    loading: boolean;
    mode?: "create" | "edit";
    existingReview?: any | null;
  }>(),
  {
    mode: "create",
    existingReview: null,
  }
);

const emit = defineEmits(["update:modelValue", "submit"]);

const fallbackImage =
  "data:image/svg+xml;utf8," +
  encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"><rect width="100%" height="100%" fill="#f3f4f6"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="#9ca3af" font-family="Arial" font-size="12">Không có ảnh</text></svg>`
  );

const isEditMode = computed(() => props.mode === "edit");
const modalTitle = computed(() =>
  isEditMode.value ? "Sửa đánh giá sản phẩm" : "Đánh giá sản phẩm"
);
const submitButtonText = computed(() =>
  isEditMode.value ? "Cập nhật" : "Hoàn thành"
);

// State
const rating = ref(5);
const hoverRating = ref(0);
const comment = ref("");
const selectedFiles = ref<File[]>([]);
const previewUrls = ref<{ type: string; url: string }[]>([]);
const fileInput = ref<HTMLInputElement | null>(null);
const deletedMediaIds = ref<number[]>([]);

type ReviewMediaPreview = {
  mediaId: number | null;
  url: string;
  isVideo: boolean;
};

const normalizeMediaUrl = (url: unknown) => {
  const rawUrl = String(url || "").trim();

  if (!rawUrl) {
    return "";
  }

  if (
    rawUrl.startsWith("http://") ||
    rawUrl.startsWith("https://") ||
    rawUrl.startsWith("data:") ||
    rawUrl.startsWith("blob:")
  ) {
    return rawUrl;
  }

  if (rawUrl.startsWith("/")) {
    const isLocalhost =
      window.location.hostname === "localhost" ||
      window.location.hostname === "127.0.0.1";

    if (isLocalhost) {
      return `${window.location.protocol}//${window.location.hostname}:8080${rawUrl}`;
    }

    return `${window.location.origin}${rawUrl}`;
  }

  return rawUrl;
};

const getMediaUrl = (media: any) => {
  if (typeof media === "string") {
    return normalizeMediaUrl(media);
  }

  return normalizeMediaUrl(
    media?.url ??
      media?.mediaUrl ??
      media?.MediaUrl ??
      media?.secureUrl ??
      media?.secure_url ??
      media?.fileUrl ??
      media?.FileUrl ??
      media?.path ??
      media?.Path ??
      media?.imageUrl ??
      media?.ImageUrl ??
      ""
  );
};

const getMediaId = (media: any) => {
  if (typeof media === "string") {
    return null;
  }

  const rawId =
    media?.mediaId ??
    media?.id ??
    media?.reviewMediaId ??
    media?.reviewMediaFileId ??
    null;

  const mediaId = Number(rawId);

  return Number.isFinite(mediaId) && mediaId > 0 ? mediaId : null;
};

const isVideoMedia = (media: any, url: string) => {
  if (typeof media !== "string") {
    if (media?.isVideo === true) {
      return true;
    }

    const mediaType = String(
      media?.mediaType ??
        media?.type ??
        media?.contentType ??
        media?.resourceType ??
        ""
    )
      .trim()
      .toLowerCase();

    if (mediaType.includes("video")) {
      return true;
    }
  }

  return (
    /\.(mp4|webm|ogg|mov|m4v)(\?|#|$)/i.test(url) ||
    url.includes("/video/upload/")
  );
};

const getExistingReviewRawMedia = (review: any) => {
  if (!review) {
    return [];
  }

  const rawMedia =
    review?.mediaFiles ??
    review?.mediaUrls ??
    review?.mediaUrl ??
    review?.reviewMedias ??
    review?.reviewMediaUrls ??
    review?.reviewMediaFiles ??
    review?.reviewMedia ??
    review?.medias ??
    review?.media ??
    review?.images ??
    review?.files ??
    [];

  return Array.isArray(rawMedia) ? rawMedia : [rawMedia];
};

const existingReviewMedia = computed<ReviewMediaPreview[]>(() => {
  if (!isEditMode.value) {
    return [];
  }

  const usedUrls = new Set<string>();

  return getExistingReviewRawMedia(props.existingReview)
    .map((media: any) => {
      const url = getMediaUrl(media);
      const mediaId = getMediaId(media);

      if (!url || usedUrls.has(url)) {
        return null;
      }

      if (mediaId && deletedMediaIds.value.includes(mediaId)) {
        return null;
      }

      usedUrls.add(url);

      return {
        mediaId,
        url,
        isVideo: isVideoMedia(media, url),
      };
    })
    .filter((media: ReviewMediaPreview | null): media is ReviewMediaPreview =>
      Boolean(media)
    );
});

const resetSelectedMedia = () => {
  selectedFiles.value = [];
  deletedMediaIds.value = [];
  previewUrls.value.forEach((preview) => URL.revokeObjectURL(preview.url));
  previewUrls.value = [];
};

// Reset modal khi mở
watch(
  () => [props.modelValue, props.mode, props.existingReview] as const,
  ([newVal]) => {
    if (newVal) {
      rating.value = Number(props.existingReview?.rating || 5);
      comment.value = String(props.existingReview?.comment || "");
      resetSelectedMedia();
    }
  }
);

const getRatingText = (val: number) => {
  const texts = [
    "Tệ",
    "Không hài lòng",
    "Bình thường",
    "Hài lòng",
    "Tuyệt vời",
  ];
  return texts[val - 1] || "";
};

const normalizeTextValue = (value: unknown) => {
  if (value === null || value === undefined) {
    return "";
  }

  return String(value).trim();
};

const getCapacityText = (reviewItem: any) => {
  const raw =
    reviewItem?.capacityLabel ??
    reviewItem?.capacityName ??
    reviewItem?.capacityText ??
    reviewItem?.capacity ??
    reviewItem?.capacityValue ??
    reviewItem?.volume ??
    reviewItem?.volumeValue ??
    null;

  const text = normalizeTextValue(raw);

  if (text) {
    return text.toLowerCase().includes("ml") ? text : `${text}ml`;
  }

  const sku = normalizeTextValue(reviewItem?.sku);
  const match = sku.match(/-(\d+(?:\.\d+)?)-/);

  if (match?.[1]) {
    return `${match[1]}ml`;
  }

  return "";
};

const getVariantText = (reviewItem: any) => {
  const capacityText = getCapacityText(reviewItem);

  return capacityText ? `Dung tích: ${capacityText}` : "";
};

const isValid = computed(() => {
  return rating.value > 0 && comment.value.length <= 500;
});

const closeModal = () => {
  if (props.loading) return;
  emit("update:modelValue", false);
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
  let hasInvalidTypeFile = false;

  Array.from(files).forEach((file) => {
    // Chặn quá 5 file
    if (selectedFiles.value.length >= 5) return;

    // CHECK ĐỊNH DẠNG: Chỉ nhận hình ảnh hoặc video
    const isImage = file.type.startsWith("image/");
    const isVideo = file.type.startsWith("video/");

    if (!isImage && !isVideo) {
      hasInvalidTypeFile = true;
      return; // Bỏ qua file sai định dạng
    }

    // Chặn file > 5MB
    if (file.size > MAX_SIZE) {
      hasOversizedFile = true;
      return;
    }

    selectedFiles.value.push(file);
    previewUrls.value.push({
      type: isVideo ? "video" : "image",
      url: URL.createObjectURL(file),
    });
  });

  // Cảnh báo nếu có file sai định dạng
  if (hasInvalidTypeFile) {
    Swal.fire({
      icon: "error",
      title: "Định dạng không hợp lệ",
      text: "Hệ thống chỉ cho phép tải lên tệp tin hình ảnh hoặc video.",
      confirmButtonColor: "#bd9a5f",
    });
  }
  // Cảnh báo dung lượng
  else if (hasOversizedFile) {
    Swal.fire({
      icon: "warning",
      title: "File quá lớn",
      text: "Một số file đã bị loại bỏ vì vượt quá dung lượng 5MB.",
      confirmButtonColor: "#bd9a5f",
    });
  }

  // Reset input để có thể chọn lại đúng file đó nếu lỡ xóa
  input.value = "";
};

const markExistingMediaForDelete = (media: ReviewMediaPreview) => {
  if (!media.mediaId) {
    return;
  }

  if (!deletedMediaIds.value.includes(media.mediaId)) {
    deletedMediaIds.value.push(media.mediaId);
  }
};

const removeMedia = (index: number) => {
  if (previewUrls.value[index]) {
    URL.revokeObjectURL(previewUrls.value[index].url);
  }
  previewUrls.value.splice(index, 1);
  selectedFiles.value.splice(index, 1);
};

const handleSubmit = async () => {
  if (isEditMode.value) {
    const result = await Swal.fire({
      icon: "warning",
      title: "Xác nhận sửa đánh giá?",
      text: "Đây là lần sửa duy nhất. Sau khi hoàn tất, bạn sẽ không thể sửa đánh giá này nữa.",
      showCancelButton: true,
      confirmButtonText: "Đồng ý sửa",
      cancelButtonText: "Quay lại",
      confirmButtonColor: "#bd9a5f",
      cancelButtonColor: "#6b7280",
    });

    if (!result.isConfirmed) {
      return;
    }
  }

  // Gửi data kèm file về cho OrderHistory.vue xử lý
  emit("submit", {
    rating: rating.value,
    comment: comment.value,
    files: selectedFiles.value,
    deletedMediaIds: [...deletedMediaIds.value],
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
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
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

.mini-variant {
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.35;
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

.edit-review-note {
  border: 1px solid #fed7aa;
  background: #fff7ed;
  color: #9a3412;
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.45;
}

.existing-media-section {
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  border-radius: 10px;
  padding: 10px;
}

.existing-media-title {
  color: #475569;
  font-size: 13px;
  font-weight: 600;
}

.existing-media-section .existing-media-title {
  margin-bottom: 8px;
}

.existing-media-item {
  background: #fff;
}

.existing-media-badge {
  position: absolute;
  left: 4px;
  bottom: 4px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.72);
  color: #fff;
  padding: 1px 6px;
  font-size: 10px;
  line-height: 16px;
}

.existing-media-remove {
  top: -6px;
  right: -6px;
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

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
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
  z-index: 99999;
}
</style>
