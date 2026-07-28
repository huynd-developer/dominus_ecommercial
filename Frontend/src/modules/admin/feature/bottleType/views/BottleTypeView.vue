<template>
  <div class="bottle-type-page">
    <div class="page-header">
      <div>
        <h3 class="page-title">
          <i class="bi bi-inboxes me-2"></i>
          Danh mục loại chai
        </h3>
      </div>
      
      <button @click="openAddModal" class="btn btn-primary px-4">
        <i class="bi bi-plus-circle me-2"></i>
        Thêm loại chai
      </button>
    </div>
    
    <div class="toolbar">
      <div class="search-box">
        <i class="bi bi-search"></i>
        <input 
          v-model="searchKeyword" 
          @keyup.enter="handleSearch"
          type="text" 
          placeholder="Tìm kiếm loại chai..."
        >
      </div>
    </div>
      
    <div v-if="bottleTypeStore.isLoading" class="loading-state">
      Đang tải dữ liệu...
    </div>

    <div v-else class="table-wrapper">
      <BottleTypeTable 
        :bottleTypes="bottleTypeStore.bottleTypes" 
        @edit="openEditModal"
        @delete="handleDelete"
        @toggle-status="handleToggleStatus"
      />
    </div>

    <div class="footer" v-if="bottleTypeStore.totalPages > 0 && !bottleTypeStore.isLoading">
      <div class="text-muted">
        Đang hiển thị trang <b>{{ bottleTypeStore.currentPage + 1 }}</b> / <b>{{ bottleTypeStore.totalPages }}</b>
      </div>
      <div class="pagination">
        <button 
          class="btn btn-light" 
          :disabled="bottleTypeStore.currentPage === 0" 
          @click="changePage(bottleTypeStore.currentPage - 1)"
        >
          ←
        </button>
        
        <button 
          v-for="p in bottleTypeStore.totalPages" 
          :key="p"
          class="btn"
          :class="bottleTypeStore.currentPage === (p - 1) ? 'btn-primary' : 'btn-light'"
          @click="changePage(p - 1)"
        >
          {{ p }}
        </button>
        
        <button 
          class="btn btn-light" 
          :disabled="bottleTypeStore.currentPage === bottleTypeStore.totalPages - 1" 
          @click="changePage(bottleTypeStore.currentPage + 1)"
        >
          →
        </button>
      </div>
    </div>

    <!-- MODAL -->
    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="custom-modal">
        <div class="modal-header">
          <h5 class="modal-title">{{ isEdit ? 'Cập nhật loại chai' : 'Thêm loại chai mới' }}</h5>
          <button @click="showModal = false" type="button" class="btn-close-modal">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="mb-3">
            <label class="form-label">Tên loại chai <span class="text-danger">*</span></label>
            <input 
              v-model="formData.name" 
              type="text" 
              class="form-control" 
              :class="{ 'is-invalid': errors.name }"
              placeholder="VD: Chai gốc Fullbox, Ống chiết..."
              @input="validateForm"
              @keyup.enter="handleSubmit"
              autofocus
            >
            <small v-if="errors.name" class="text-danger mt-2 d-block fw-medium">
              <i class="bi bi-exclamation-circle me-1"></i> {{ errors.name }}
            </small>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="showModal = false" class="btn btn-light px-4" :disabled="isSaving">Hủy</button>
          <button @click="handleSubmit" class="btn btn-primary px-4" :disabled="isSaving">
            <span v-if="isSaving" class="spinner-border spinner-border-sm me-2"></span>
            {{ isSaving ? 'Đang lưu...' : 'Lưu lại' }}
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useBottleTypeStore } from '../stores/bottle-type.store';
import BottleTypeTable from '../components/BottleTypeTable.vue';
import type { BottleType, BottleTypeRequest } from '../types/bottle-type.type';
import Swal from 'sweetalert2'; 

const bottleTypeStore = useBottleTypeStore();

const searchKeyword = ref('');
const showModal = ref(false);
const isEdit = ref(false);
const currentId = ref<number | null>(null);
const isSaving = ref(false);

const formData = ref<BottleTypeRequest>({ name: '', status: 1 });
const errors = ref({ name: '' });

const Toast = Swal.mixin({
  toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, timerProgressBar: true
});

onMounted(() => {
  bottleTypeStore.fetchBottleTypes();
});

const handleSearch = () => { bottleTypeStore.fetchBottleTypes(searchKeyword.value, 0); };
const changePage = (page: number) => { if (page >= 0 && page < bottleTypeStore.totalPages) bottleTypeStore.fetchBottleTypes(searchKeyword.value, page); };

const validateForm = () => {
  errors.value.name = ''; 
  const nameValue = formData.value.name.trim();
  const nameRegex = /^[\p{L}\s()]+$/u; 

  if (!nameValue) {
    errors.value.name = 'Tên loại chai không được để trống';
    return false;
  }
  if (nameValue.length > 255) {
    errors.value.name = 'Tên loại chai không được vượt quá 255 ký tự';
    return false;
  }
  if (!nameRegex.test(nameValue)) {
    errors.value.name = 'Tên loại chai chỉ được chứa chữ cái, khoảng trắng và dấu ngoặc đơn ()';
    return false;
  }
  return true;
};

const openAddModal = () => {
  isEdit.value = false;
  formData.value = { name: '', status: 1 };
  errors.value.name = ''; 
  showModal.value = true;
};

const openEditModal = (item: BottleType) => {
  isEdit.value = true;
  currentId.value = item.id;
  formData.value = { name: item.name, status: item.status };
  errors.value.name = ''; 
  showModal.value = true;
};

const handleSubmit = async () => {
  if (!validateForm()) return;

  try {
    isSaving.value = true;

    if (isEdit.value && currentId.value) {
      await bottleTypeStore.updateBottleType(currentId.value, formData.value);
      await bottleTypeStore.fetchBottleTypes(searchKeyword.value, bottleTypeStore.currentPage);
      Toast.fire({ icon: 'success', title: 'Cập nhật thành công!' });
    } else {
      await bottleTypeStore.createBottleType(formData.value);
      searchKeyword.value = '';
      await bottleTypeStore.fetchBottleTypes('', 0);
      Toast.fire({ icon: 'success', title: 'Thêm mới thành công!' });
    }
    showModal.value = false; 
  } catch (error: any) {
    console.error("Chi tiết lỗi Axios:", error);
    if (error.response && error.response.data) {
      const responseData = error.response.data;

      if (responseData.errors && responseData.errors.name) {
        errors.value.name = responseData.errors.name;
        return; 
      }

      if (responseData.message) {
        const lowerMsg = responseData.message.toLowerCase();
        if (lowerMsg.includes('tồn tại') || lowerMsg.includes('exists') || lowerMsg.includes('duplicate')) {
          errors.value.name = 'Loại chai này đã tồn tại trong hệ thống!';
        } else {
          Toast.fire({ icon: 'error', title: responseData.message });
        }
        return;
      }
    }
    Toast.fire({ icon: 'error', title: 'Máy chủ không phản hồi!' });
  } finally {
    isSaving.value = false;
  }
};

const handleToggleStatus = async (item: BottleType) => {
  const newStatus = item.status === 1 ? 0 : 1;
  try {
    await bottleTypeStore.updateBottleType(item.id, { 
      name: item.name, 
      status: newStatus
    });
    await bottleTypeStore.fetchBottleTypes(searchKeyword.value, bottleTypeStore.currentPage);
    Toast.fire({ icon: 'success', title: 'Đã thay đổi trạng thái!' });
  } catch (error) {
    Toast.fire({ icon: 'error', title: 'Không thể đổi trạng thái!' });
  }
};

const handleDelete = (id: number) => {
  Swal.fire({
    title: 'Bạn có chắc chắn muốn xóa?',
    text: "Hành động này sẽ đưa loại chai vào thùng rác!",
    icon: 'warning', showCancelButton: true, confirmButtonColor: '#dc2626', cancelButtonColor: '#94a3b8',
    confirmButtonText: 'Vâng, xóa nó!', cancelButtonText: 'Hủy'
  }).then(async (result) => {
    if (result.isConfirmed) {
      try {
        const isLastItemOnPage = bottleTypeStore.bottleTypes && bottleTypeStore.bottleTypes.length === 1;
        const isNotFirstPage = bottleTypeStore.currentPage > 0;

        await bottleTypeStore.deleteBottleType(id);
        
        if (isLastItemOnPage && isNotFirstPage) {
          await bottleTypeStore.fetchBottleTypes(searchKeyword.value, bottleTypeStore.currentPage - 1);
        } else {
          await bottleTypeStore.fetchBottleTypes(searchKeyword.value, bottleTypeStore.currentPage);
        }

        Swal.fire('Đã xóa!', 'Loại chai đã bị xóa.', 'success');
      } catch (error: any) {
        Swal.fire('Lỗi!', error.message || 'Không thể xóa loại chai này.', 'error');
      }
    }
  });
};
</script>

<style scoped>
/* Layout Component */
.bottle-type-page {
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(0,0,0,.05);
  overflow: hidden;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 30px;
  border-bottom: 1px solid #eef2f7;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 30px;
  background: #fafafa;
}

.search-box {
  width: 350px;
  position: relative;
}

.search-box i {
  position: absolute;
  top: 50%;
  left: 16px;
  transform: translateY(-50%);
  color: #94a3b8;
}

.search-box input {
  width: 100%;
  padding: 12px 18px 12px 45px;
  border-radius: 999px;
  border: 1px solid #e2e8f0;
  transition: 0.25s;
  font-size: 14px;
}

.search-box input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 4px rgba(59,130,246,.15);
}

.table-wrapper {
  padding: 20px 24px;
}

.loading-state {
  padding: 80px;
  text-align: center;
  color: #64748b;
  font-size: 15px;
}

/* Footer & Pagination */
.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 28px;
  border-top: 1px solid #eee;
  background: #fafafa;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Buttons */
.btn {
  border-radius: 12px;
  font-weight: 600;
  transition: 0.25s;
}

.btn-primary {
  background: #2563eb;
  border: none;
  color: white;
}

.btn-primary:hover {
  background: #1d4ed8;
  transform: translateY(-1px);
}

.btn-light {
  background: #fff;
  border: 1px solid #dbe4ee;
  color: #475569;
}

.btn-light:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #94a3b8;
}

/* Modal Custom */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15,23,42,.45);
  backdrop-filter: blur(3px);
  z-index: 1050;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.2s ease;
}

.custom-modal {
  background: #fff;
  width: 100%;
  max-width: 500px;
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0,0,0,.1);
  overflow: hidden;
  animation: slideUp 0.3s ease;
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #eef2f7;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.btn-close-modal {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 10px;
  background: #f1f5f9;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: 0.2s;
}

.btn-close-modal:hover {
  background: #ef4444;
  color: white;
}

.modal-body {
  padding: 24px;
}

.form-label {
  font-weight: 600;
  color: #334155;
  margin-bottom: 8px;
  display: block;
}

.form-control {
  min-height: 48px;
  border-radius: 12px;
  border: 1px solid #cbd5e1;
  padding: 10px 16px;
  font-size: 14px;
  transition: 0.2s;
}

.form-control:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 4px rgba(59,130,246,.15);
}

.form-control.is-invalid {
  border-color: #ef4444;
}

.form-control.is-invalid:focus {
  box-shadow: 0 0 0 4px rgba(239,68,68,.15);
}

.modal-footer {
  padding: 16px 24px;
  background: #f8fafc;
  border-top: 1px solid #eef2f7;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
</style>