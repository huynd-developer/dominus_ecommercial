<!-- <template>
  <div class="row g-0 align-items-center justify-content-between">
    
    <div class="col-md-6">
      <div class="input-group border rounded-3 w-75">
        <span class="input-group-text bg-white border-0">
          <i class="bi bi-search text-dark-blue"></i>
        </span>
        <input 
          type="text" 
          class="form-control border-0 shadow-none py-2 custom-search" 
          placeholder="Tìm kiếm thương hiệu..." 
          :value="modelValue"
          @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
        />
      </div>
    </div>
    
    <div class="col-md-auto text-end">
      <button class="btn btn-primary btn-lg rounded-3 px-4 py-2 fs-6 shadow-sm" @click="$emit('add')">
        <i class="bi bi-plus-lg me-2"></i>Thêm thương hiệu
      </button>
    </div>
    
  </div>
</template>

<script setup lang="ts">
defineProps<{ modelValue: string }>();
defineEmits(['update:modelValue', 'add']);
</script>

<style scoped>
/* Xóa viền đen thô kệch, chỉ đổi màu nhẹ khi focus vào ô nhập */
.form-control:focus {
  background-color: #fff !important;
  border-color: #86b7fe;
}
.btn-primary {
  background-color: #1f3b65;
  border: none;
  font-weight: 500;
}
.btn-primary:hover {
  background-color: #1f3b65;
}
</style> -->