<!-- <template>
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
          <div class="row g-4">
            
            <div class="col-md-12">
              <label class="form-label text-muted fw-bold small mb-2">Tên thương hiệu <span class="text-danger">*</span></label>
              <input 
                v-model="formData.name" 
                type="text" 
                class="form-control custom-input fs-6 py-2" 
                placeholder="Ví dụ: Dior, Chanel, Tom Ford..."
              >
            </div>

            <div class="col-md-12">
              <label class="form-label text-muted fw-bold small mb-2">Mô tả chi tiết</label>
              <textarea 
                v-model="formData.description" 
                class="form-control custom-input fs-6 py-2" 
                rows="4" 
                placeholder="Nhập thông tin giới thiệu về thương hiệu..."
              ></textarea>
            </div>

            <div class="col-md-12">
              <label class="form-label text-muted fw-bold small mb-2">Trạng thái hiển thị</label>
              <select v-model="formData.status" class="form-select custom-input fs-6 py-2">
                <option :value="1">🟢 Hoạt động (Hiện)</option>
                <option :value="0">⚪ Tạm ngưng (Ẩn)</option>
              </select>
            </div>

          </div>
        </div>

        <div class="modal-footer border-top-0 bg-light py-3 px-4">
          <button @click="$emit('close')" class="btn btn-outline-secondary px-4 py-2 rounded-3 fw-medium">Hủy bỏ</button>
          <button @click="submit" class="btn btn-primary px-4 py-2 rounded-3 fw-medium shadow-sm">
            {{ isEditing ? 'Cập nhật' : 'Thêm thương hiệu' }}
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
  // Sửa lại: Chỉ truyền đúng formData (không truyền file nữa)
  (e: 'save', payload: any): void; 
}>();

const formData = ref({ ...props.initialData });

// Cập nhật lại form khi có dữ liệu truyền vào (ví dụ lúc bấm nút Sửa)
watch(() => props.initialData, (newVal) => {
  formData.value = { ...newVal };
}, { deep: true });

const submit = () => {
  // Validate dữ liệu
  if (!formData.value.name || formData.value.name.trim() === '') {
    alert('Vui lòng nhập tên thương hiệu!');
    return;
  }
  
  // CHỈ GỬI ĐÚNG FORM DATA VỀ CHO COMPONENT CHA (BrandView)
  emit('save', formData.value);
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
</style> -->