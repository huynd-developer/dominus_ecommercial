<template>
  <div class="modal-backdrop fade show" style="background-color: rgba(10, 31, 53, 0.4); backdrop-filter: blur(4px); z-index: 1040;"></div>
  
  <div class="modal fade show d-block" tabindex="-1" style="z-index: 1050;" @click.self="$emit('close')">
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content border-0 shadow-lg" style="border-radius: 16px; overflow: hidden;">
        
        <div class="modal-header bg-light border-bottom-0 py-3 px-4">
          <h5 class="modal-title fw-bold text-dark mb-0 fs-5">
            {{ isEditing ? 'Cập nhật thương hiệu' : 'Thêm thương hiệu mới' }}
          </h5>
          <button type="button" class="btn-close shadow-none" @click="$emit('close')"></button>
        </div>

        <div class="modal-body p-4">
          <div class="mb-4">
            <label class="form-label text-muted fw-medium small mb-2">Tên thương hiệu <span class="text-danger">*</span></label>
            <input 
              v-model="formData.name" 
              type="text" 
              class="form-control form-control-lg custom-input fs-6" 
              placeholder="Nhập tên thương hiệu (VD: Chanel, Dior)..."
            >
          </div>
          
          <div class="mb-4">
            <label class="form-label text-muted fw-medium small mb-2">Mô tả</label>
            <textarea 
              v-model="formData.description" 
              class="form-control custom-input" 
              rows="3" 
              placeholder="Nhập mô tả ngắn gọn..."
            ></textarea>
          </div>
          
          <div class="mb-2">
            <label class="form-label text-muted fw-medium small mb-2">Trạng thái hiển thị</label>
            <select v-model="formData.status" class="form-select form-select-lg custom-input fs-6">
              <option :value="1">🟢 Đang hoạt động (Hiện)</option>
              <option :value="0">⚪ Tạm ngưng (Ẩn)</option>
            </select>
          </div>
        </div>

        <div class="modal-footer border-top-0 bg-light py-3 px-4 justify-content-end">
          <button @click="$emit('close')" class="btn btn-outline-secondary px-4 py-2 rounded-3 fw-medium">Hủy bỏ</button>
          <button @click="submit" class="btn btn-primary px-4 py-2 rounded-3 fw-medium shadow-sm">
            Lưu thay đổi
          </button>
        </div>
        
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import type { Brand } from '../types/brand.type';

const props = defineProps<{ isEditing: boolean; initialData: Brand }>();
const emit = defineEmits<{ (e: 'close'): void; (e: 'save', data: Brand): void; }>();

const formData = ref<Brand>({ ...props.initialData });

// Cập nhật lại form nếu initialData từ component cha thay đổi
watch(() => props.initialData, (val) => { 
  formData.value = { ...val }; 
}, { deep: true });

const submit = () => {
  if (!formData.value.name || formData.value.name.trim() === '') {
    return alert('Vui lòng nhập tên thương hiệu!');
  }
  emit('save', formData.value);
};
</script>

<style scoped>
/* Tùy chỉnh ô input cho mềm mại và hiện đại hơn */
.custom-input {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  transition: all 0.2s ease-in-out;
  box-shadow: none !important;
}

/* Hiệu ứng khi click vào ô nhập */
.custom-input:focus {
  border-color: #0d6efd;
  background-color: #fff;
  box-shadow: 0 0 0 4px rgba(13, 110, 253, 0.1) !important;
}

textarea.custom-input {
  resize: none;
}
</style>