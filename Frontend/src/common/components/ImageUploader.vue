<template>
  <div class="image-uploader-wrapper w-100">
    <input 
      type="file" 
      ref="fileInput" 
      @change="handleFileChange" 
      accept="image/*" 
      class="d-none" 
    />
    
    <div 
      class="upload-area border rounded-4 d-flex align-items-center justify-content-center bg-light position-relative overflow-hidden"
      :style="{ aspectRatio: ratio, cursor: 'pointer' }"
      @click="triggerInput"
    >
      <img v-if="preview" :src="preview" class="w-100 h-100 object-fit-cover position-absolute top-0 start-0" alt="Preview"/>
      <div v-else class="text-muted d-flex flex-column align-items-center opacity-75">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
          <circle cx="8.5" cy="8.5" r="1.5"></circle>
          <polyline points="21 15 16 10 5 21"></polyline>
        </svg>
        <span class="small mt-2 fw-medium">Chọn ảnh</span>
      </div>
    </div>

    <Teleport to="body">
      <div 
        class="modal fade" 
        id="cropperModal" 
        tabindex="-1" 
        ref="cropperModalRef" 
        aria-hidden="true" 
        style="z-index: 1060;"
      >
        <div class="modal-dialog modal-dialog-centered">
          <div class="modal-content border-0 shadow-lg">
            <div class="modal-header border-bottom-0 pb-0">
              <h6 class="modal-title fw-semibold">Điều chỉnh hình ảnh</h6>
              <button type="button" class="btn-close" @click="closeModal"></button>
            </div>
            
            <div class="modal-body">
              <cropper
                v-if="rawImageUrl"
                class="cropper-container bg-dark rounded"
                :src="rawImageUrl"
                :stencil-props="{ aspectRatio: calculatedRatio }"
                ref="cropperRef"
                style="height: 300px; width: 100%;"
              />
            </div>
            
            <div class="modal-footer border-top-0 pt-0">
              <button type="button" class="btn btn-light" @click="closeModal">Hủy</button>
              <button type="button" class="btn btn-dark px-4" @click="cropImage">Áp dụng</button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, onMounted, onBeforeUnmount } from 'vue';
import { Cropper } from 'vue-advanced-cropper';
import 'vue-advanced-cropper/dist/style.css';
import * as bootstrap from 'bootstrap';

const props = defineProps<{ initialUrl?: string | null; ratio?: string; }>();
const emit = defineEmits<{ (e: 'update:file', file: File | null): void; }>();

const fileInput = ref<HTMLInputElement | null>(null);
const preview = ref<string | null>(props.initialUrl || null);
const rawImageUrl = ref<string | null>(null); // Ảnh gốc trước khi cắt
const cropperRef = ref<any>(null);
const cropperModalRef = ref<HTMLElement | null>(null);

// Sử dụng kiểu any để tránh lỗi TS nếu chưa kịp cài @types/bootstrap
const bsModal = ref<any>(null);

const calculatedRatio = computed(() => {
  if (!props.ratio) return 1;
  const parts = props.ratio.split('/');
  return parts.length === 2 ? Number(parts[0]) / Number(parts[1]) : 1;
});

watch(() => props.initialUrl, (newVal) => { 
  preview.value = newVal || null; 
});

onMounted(() => {
  if (cropperModalRef.value) {
    bsModal.value = new bootstrap.Modal(cropperModalRef.value, { 
      backdrop: 'static',
      keyboard: false
    });
  }
});

// Dọn dẹp an toàn khi component bị đóng (tránh kẹt màn hình đen)
onBeforeUnmount(() => {
  if (bsModal.value) {
    bsModal.value.hide();
    bsModal.value.dispose();
  }
  if (rawImageUrl.value) URL.revokeObjectURL(rawImageUrl.value);
  if (preview.value && preview.value.startsWith('blob:')) URL.revokeObjectURL(preview.value);
});

const triggerInput = () => {
  fileInput.value?.click();
};

const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0]; 
  if (!file) return;

  if (rawImageUrl.value) URL.revokeObjectURL(rawImageUrl.value);
  rawImageUrl.value = URL.createObjectURL(file);
  
  bsModal.value?.show(); // Mở modal
  target.value = ''; // Reset input
};

const closeModal = () => {
  bsModal.value?.hide();
};

const cropImage = () => {
  const { canvas } = cropperRef.value.getResult();
  if (canvas) {
    canvas.toBlob((blob: Blob | null) => {
      if (blob) {
        // Khởi tạo file ảnh sau khi cắt
        const croppedFile = new File([blob], "cropped_image.jpg", { type: "image/jpeg" });
        
        if (preview.value && preview.value.startsWith('blob:')) {
          URL.revokeObjectURL(preview.value);
        }
        
        // Hiển thị ra màn hình form
        preview.value = URL.createObjectURL(croppedFile); 
        
        // Bắn dữ liệu ra ngoài cho Offcanvas
        emit('update:file', croppedFile); 
        closeModal();
      }
    }, 'image/jpeg', 0.9);
  }
};
</script>

<style scoped>
.upload-area { 
  border-style: dashed !important; 
  border-width: 1.5px !important; 
  transition: all 0.2s ease; 
}
.upload-area:hover { 
  background-color: #f8f9fa !important; 
  border-color: #adb5bd !important; 
}
/* Tuỳ chỉnh viền cắt của cropper cho đẹp hơn */
:deep(.vue-advanced-cropper__background) {
  background-color: #212529;
}
</style>