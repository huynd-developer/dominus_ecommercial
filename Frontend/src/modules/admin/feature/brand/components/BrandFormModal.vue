<template>
  <div class="modal-backdrop fade show" style="background-color: rgba(10, 31, 53, 0.4); backdrop-filter: blur(4px); z-index: 1040;"></div>
  
  <div class="modal fade show d-block" tabindex="-1" style="z-index: 1050;" @click.self="$emit('close')">
    <div class="modal-dialog modal-dialog-centered modal-lg"> 
      <div class="modal-content border-0 shadow-lg" style="border-radius: 16px; overflow: hidden;">
        
        <div class="modal-header bg-light border-bottom-0 py-3 px-4">
          <h5 class="modal-title fw-bold text-dark mb-0 fs-5">
            {{ isEditing ? 'Cập nhật sản phẩm' : 'Thêm sản phẩm mới' }}
          </h5>
          <button type="button" class="btn-close shadow-none" @click="$emit('close')"></button>
        </div>

        <div class="modal-body p-4">
          <div class="row g-3">
            
            <div class="col-md-12 text-center mb-2">
              <label class="form-label text-muted fw-medium small d-block mb-2 text-start">Hình ảnh sản phẩm</label>
              
              <div 
                class="d-flex justify-content-center align-items-center border rounded-3 bg-light position-relative" 
                style="width: 120px; height: 120px; margin: 0 auto; cursor: pointer; border-style: dashed !important; overflow: hidden;"
                title="Click để chọn ảnh từ máy tính"
                @click="triggerFileInput"
              >
                <input 
                  type="file" 
                  ref="fileInput" 
                  class="d-none" 
                  accept="image/*" 
                  @change="handleFileUpload"
                />

                <div v-if="!previewImageUrl && !formData.mainImageUrl" class="text-center text-muted">
                  <i class="bi bi-cloud-arrow-up fs-1 text-primary"></i>
                  <p class="mb-0 mt-1 fw-medium" style="font-size: 11px;">Tải ảnh lên</p>
                </div>

                <img 
                  v-else 
                  :src="previewImageUrl || formData.mainImageUrl" 
                  class="w-100 h-100 object-fit-cover" 
                  alt="Preview"
                >
              </div>
              <div v-if="selectedFile" class="small text-success mt-2 fw-medium">
                Đã chọn: {{ selectedFile.name }}
              </div>
            </div>

            <div class="col-md-12">
              <label class="form-label text-muted fw-medium small mb-2">Tên sản phẩm <span class="text-danger">*</span></label>
              <input v-model="formData.name" type="text" class="form-control custom-input fs-6" placeholder="Ví dụ: Bleu de Chanel Parfum...">
            </div>

            <div class="col-md-6">
              <label class="form-label text-muted fw-medium small mb-2">Thương hiệu</label>
              <select v-model="formData.brandName" class="form-select custom-input fs-6">
                <option value="">Chọn thương hiệu</option>
                <option value="Chanel">Chanel</option>
                <option value="Dior">Dior</option>
                <option value="YSL">YSL</option>
              </select>
            </div>
            
            <div class="col-md-6">
              <label class="form-label text-muted fw-medium small mb-2">Nhóm hương</label>
              <input v-model="formData.fragranceGroup" type="text" class="form-control custom-input fs-6" placeholder="Ví dụ: Gỗ - Thơm...">
            </div>

            <div class="col-md-6">
              <label class="form-label text-muted fw-medium small mb-2">Giá biến thể (đ)</label>
              <input v-model="formData.priceRange" type="text" class="form-control custom-input fs-6" placeholder="Ví dụ: 2.450.000...">
            </div>
            
            <div class="col-md-6">
              <label class="form-label text-muted fw-medium small mb-2">Số lượng tồn kho</label>
              <input v-model.number="formData.stock" type="number" class="form-control custom-input fs-6" placeholder="0">
            </div>

            <div class="col-md-12">
              <label class="form-label text-muted fw-medium small mb-2">Trạng thái hiển thị</label>
              <select v-model="formData.status" class="form-select custom-input fs-6">
                <option :value="1">🟢 Đang bán (Hiện)</option>
                <option :value="0">⚪ Tạm ngưng (Ẩn)</option>
              </select>
            </div>
          </div>
        </div>

        <div class="modal-footer border-top-0 bg-light py-3 px-4">
          <button @click="$emit('close')" class="btn btn-outline-secondary px-4 py-2 rounded-3 fw-medium">Hủy bỏ</button>
          <button @click="submit" class="btn btn-primary px-4 py-2 rounded-3 fw-medium shadow-sm">
            {{ isEditing ? 'Cập nhật' : 'Thêm sản phẩm' }}
          </button>
        </div>
        
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';

const props = defineProps<{ isEditing: boolean; initialData: any }>();
const emit = defineEmits<{ 
  (e: 'close'): void; 
  // Cập nhật sự kiện save để truyền thêm file ảnh
  (e: 'save', payload: { data: any, file: File | null }): void; 
}>();

const formData = ref({ ...props.initialData });

// XỬ LÝ ẢNH
const fileInput = ref<HTMLInputElement | null>(null);
const selectedFile = ref<File | null>(null);
const previewImageUrl = ref<string | null>(null);

// Hàm được gọi khi bấm vào khung ảnh
const triggerFileInput = () => {
  fileInput.value?.click(); // Kích hoạt thẻ input type="file" bị ẩn
};

// Hàm xử lý khi người dùng chọn file từ máy tính
const handleFileUpload = (event: Event) => {
  const target = event.target as HTMLInputElement;
  
  // Dùng .item(0) để lấy file an toàn hơn, hoặc check kĩ càng
  if (target.files && target.files.length > 0) {
    const file = target.files.item(0); // hoặc target.files[0]
    
    // Thêm dòng if (file) này để báo cho TypeScript biết "yên tâm, file có tồn tại"
    if (file) {
      selectedFile.value = file;
      
      // Tạo link ảo để hiển thị ảnh preview
      if (previewImageUrl.value) URL.revokeObjectURL(previewImageUrl.value);
      previewImageUrl.value = URL.createObjectURL(file);
    }
  }
};
watch(() => props.initialData, (newVal) => {
  formData.value = { ...newVal };
  // Reset ảnh khi form mở lại với dữ liệu mới
  selectedFile.value = null;
  previewImageUrl.value = null;
}, { deep: true });

const submit = () => {
  if (!formData.value.name || formData.value.name.trim() === '') {
    alert('Vui lòng nhập tên sản phẩm!');
    return;
  }
  
  // Gửi cả thông tin form và file ảnh thật về component cha
  emit('save', {
    data: formData.value,
    file: selectedFile.value
  });
};
</script>

<style scoped>
.custom-input {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px 15px;
  transition: all 0.2s ease;
}

.custom-input:focus {
  border-color: #0d6efd;
  box-shadow: 0 0 0 4px rgba(13, 110, 253, 0.1);
  outline: none;
}

.modal-content {
  animation: modalScale 0.2s ease-out;
}

@keyframes modalScale {
  from { transform: scale(0.95); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}
</style>