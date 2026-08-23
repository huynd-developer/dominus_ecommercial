<template>
  <div class="fragrance-page">
    <div class="page-header">
      <div>
        <h3 class="page-title">
          <i class="bi bi-flower1 me-2"></i>
          Quản lý nhóm hương
        </h3>
      </div>
      
      <button @click="openCreateModal" class="btn btn-primary px-4">
        <i class="bi bi-plus-circle me-2"></i>
        Thêm nhóm hương
      </button>
    </div>
    
    <div class="toolbar">
      <div class="search-box">
        <i class="bi bi-search"></i>
        <input 
          v-model="searchKeyword" 
          @input="handleSearch"
          @keyup.enter="handleSearch"
          type="text" 
          placeholder="Tìm theo tên nhóm hương..."
        >
      </div>
    </div>
      
    <div v-if="store.isLoading" class="loading-state">
      Đang tải dữ liệu...
    </div>

    <div v-else class="table-wrapper">
      <FragranceFamilyTable 
        :fragranceFamilies="store.fragranceFamilies"
        @edit="openEditModal"
        @delete="handleDelete"
        @toggle-status="handleToggleStatus"
      />
    </div>

    <div class="footer" v-if="store.totalPages > 0 && !store.isLoading">
      <div class="text-muted">
        Đang hiển thị trang <b>{{ store.currentPage + 1 }}</b> / <b>{{ store.totalPages }}</b>
      </div>
      <div class="pagination">
        <button 
          class="btn btn-light" 
          :disabled="store.currentPage === 0" 
          @click="changePage(store.currentPage - 1)"
        >
          ←
        </button>
        
        <button 
          v-for="p in store.totalPages" 
          :key="p"
          class="btn"
          :class="store.currentPage === (p - 1) ? 'btn-primary' : 'btn-light'"
          @click="changePage(p - 1)"
        >
          {{ p }}
        </button>
        
        <button 
          class="btn btn-light" 
          :disabled="store.currentPage === store.totalPages - 1" 
          @click="changePage(store.currentPage + 1)"
        >
          →
        </button>
      </div>
    </div>

    <!-- MODAL -->
    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="custom-modal">
        <div class="modal-header">
          <h5 class="modal-title">{{ isEdit ? 'Cập nhật nhóm hương' : 'Thêm nhóm hương mới' }}</h5>
          <button @click="showModal = false" type="button" class="btn-close-modal">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="mb-3">
            <label class="form-label">Tên nhóm hương <span class="text-danger">*</span></label>
            <!-- ĐÃ THÊM maxlength="50" Ở ĐÂY -->
            <input 
              v-model="formData.name" 
              type="text" 
              class="form-control" 
              :class="{ 'is-invalid': errors.name }"
              placeholder="VD: Floral, Woody..." 
              maxlength="50"
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
import { useFragranceFamilyStore } from '../stores/fragrance-family.store';
import FragranceFamilyTable from '../components/FragranceFamilyTable.vue';
import type { FragranceFamily, FragranceFamilyRequest } from '../types/fragrance-family.type';
import Swal from 'sweetalert2'; 

const store = useFragranceFamilyStore();
const searchKeyword = ref('');

const showModal = ref(false);
const isEdit = ref(false);
const currentId = ref<number | null>(null);
const isSaving = ref(false);
const formData = ref<FragranceFamilyRequest>({ name: '', status: 1 });

const errors = ref({ name: '' });

const Toast = Swal.mixin({
  toast: true, 
  position: 'top-end', 
  showConfirmButton: false, 
  timer: 3000, 
  timerProgressBar: true
});

onMounted(() => {
  store.fetchFragranceFamilies('', 0);
});

const handleSearch = () => { store.fetchFragranceFamilies(searchKeyword.value, 0); };
const changePage = (page: number) => { if (page >= 0 && page < store.totalPages) store.fetchFragranceFamilies(searchKeyword.value, page); };

const validateForm = () => {
  errors.value.name = ''; 
  // Bổ sung chuẩn hóa khoảng trắng để test
  const nameValue = formData.value.name.trim().replace(/\s+/g, ' ');
  const nameRegex = /^[\p{L}\s\(\)]+$/u;

  if (!nameValue) {
    errors.value.name = 'Tên nhóm hương không được để trống';
    return false;
  }
  // ĐÃ SỬA CHỖ NÀY THÀNH 50
  if (nameValue.length > 50) {
    errors.value.name = 'Tên nhóm hương không được vượt quá 50 ký tự';
    return false;
  }
  if (!nameRegex.test(nameValue)) {
    errors.value.name = 'Chỉ được chứa chữ cái, khoảng trắng và dấu ngoặc đơn';
    return false;
  }

  // BỔ SUNG: Kiểm tra trùng lặp trên Local Frontend
  const isDuplicate = store.fragranceFamilies.some((item) => {
    if (isEdit.value && item.id === currentId.value) return false;
    return item.name.trim().replace(/\s+/g, ' ').toLowerCase() === nameValue.toLowerCase();
  });

  if (isDuplicate) {
    errors.value.name = 'Nhóm hương này đã tồn tại trong hệ thống!';
    return false;
  }

  return true;
};

const openCreateModal = () => {
  isEdit.value = false;
  formData.value = { name: '', status: 1 };
  errors.value.name = ''; 
  showModal.value = true;
};

const openEditModal = (item: FragranceFamily) => {
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
    
    // Ép chuẩn hóa dữ liệu gửi lên Backend tránh qua mặt bằng 2 dấu cách
    const payload = {
        ...formData.value,
        name: formData.value.name.trim().replace(/\s+/g, ' ')
    };

    if (isEdit.value && currentId.value) {
      await store.updateFragranceFamily(currentId.value, payload);
      Toast.fire({ icon: 'success', title: 'Cập nhật thành công!' });
    } else {
      await store.createFragranceFamily(payload);
      Toast.fire({ icon: 'success', title: 'Thêm mới thành công!' });
    }
    showModal.value = false; 
    store.fetchFragranceFamilies(searchKeyword.value, store.currentPage); // Load lại data cho chắc

  } catch (error: any) {
    console.error("Chi tiết lỗi API:", error);

    let errorMsg = '';
    
    // Nâng cấp bộ bắt lỗi đồng bộ với các module khác
    if (error.response && error.response.data) {
      const responseData = error.response.data;
      if (responseData.errors && responseData.errors.name) {
        errors.value.name = responseData.errors.name;
        return;
      }
      
      if (typeof responseData === 'string') {
        errorMsg = responseData;
      } else if (responseData.message) {
        errorMsg = responseData.message;
      } else if (responseData.error) {
        errorMsg = responseData.error;
      }
    }

    if (errorMsg) {
      const lowerMsg = errorMsg.toLowerCase();
      if (lowerMsg.includes('tồn tại') || lowerMsg.includes('exists') || lowerMsg.includes('duplicate') || lowerMsg.includes('sử dụng')) {
        errors.value.name = 'Nhóm hương này đã tồn tại trong hệ thống!'; 
      } else {
        Toast.fire({ icon: 'error', title: errorMsg });
      }
    } else {
      Toast.fire({ icon: 'error', title: 'Máy chủ không phản hồi!' });
    }
  } finally {
    isSaving.value = false;
  }
};

const handleToggleStatus = async (item: FragranceFamily) => {
  const newStatus = item.status === 1 ? 0 : 1;
  try {
    await store.updateFragranceFamily(item.id, { 
      name: item.name, 
      status: newStatus 
    });
    Toast.fire({ icon: 'success', title: 'Đã thay đổi trạng thái!' });
  } catch (error) {
    Toast.fire({ icon: 'error', title: 'Không thể đổi trạng thái!' });
    store.fetchFragranceFamilies(searchKeyword.value, store.currentPage);
  }
};

const handleDelete = (id: number) => {
  Swal.fire({
    title: 'Bạn có chắc chắn muốn xóa?',
    text: "Hành động này không thể hoàn tác!",
    icon: 'warning', 
    showCancelButton: true, 
    confirmButtonColor: '#dc2626', 
    cancelButtonColor: '#94a3b8',
    confirmButtonText: 'Vâng, xóa nó!', 
    cancelButtonText: 'Hủy'
  }).then(async (result) => {
    if (result.isConfirmed) {
      try {
        await store.deleteFragranceFamily(id);
        Swal.fire('Đã xóa!', 'Nhóm hương đã bị xóa.', 'success');
      } catch (error) {
        Swal.fire('Lỗi!', 'Không thể xóa nhóm hương này.', 'error');
      }
    }
  });
};
</script>

<style scoped>
/* Layout Component */
.fragrance-page {
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