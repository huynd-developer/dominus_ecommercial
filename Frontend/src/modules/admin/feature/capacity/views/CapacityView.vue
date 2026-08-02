<template>
  <div class="capacity-page">
    <div class="page-header">
      <div>
        <h3 class="page-title">
          <i class="bi bi-droplet-half me-2"></i>
          Quản lý dung tích
        </h3>
      </div>

      <button @click="openAddModal" class="btn btn-primary px-4">
        <i class="bi bi-plus-circle me-2"></i>
        Thêm dung tích
      </button>
    </div>

    <div class="toolbar">
      <div class="search-box">
        <i class="bi bi-search"></i>
        <input
          v-model="searchKeyword"
          @keyup.enter="handleSearch"
          type="number"
          step="0.1"
          placeholder="Tìm theo dung tích (VD: 50)..."
        >
      </div>
    </div>

    <div v-if="capacityStore.isLoading" class="loading-state">
      Đang tải dữ liệu...
    </div>

    <div v-else class="table-wrapper">
      <CapacityTable
        :capacities="capacityStore.capacities"
        @edit="openEditModal"
        @delete="handleDelete"
        @toggle-status="handleToggleStatus"
      />
    </div>

    <div class="footer" v-if="capacityStore.totalPages > 0 && !capacityStore.isLoading">
      <div class="text-muted">
        Đang hiển thị trang <b>{{ capacityStore.currentPage + 1 }}</b> / <b>{{ capacityStore.totalPages }}</b>
      </div>
      <div class="pagination">
        <button
          class="btn btn-light"
          :disabled="capacityStore.currentPage === 0"
          @click="changePage(capacityStore.currentPage - 1)"
        >
          ←
        </button>

        <button
          v-for="p in capacityStore.totalPages"
          :key="p"
          class="btn"
          :class="capacityStore.currentPage === (p - 1) ? 'btn-primary' : 'btn-light'"
          @click="changePage(p - 1)"
        >
          {{ p }}
        </button>

        <button
          class="btn btn-light"
          :disabled="capacityStore.currentPage === capacityStore.totalPages - 1"
          @click="changePage(capacityStore.currentPage + 1)"
        >
          →
        </button>
      </div>
    </div>

    <!-- MODAL -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="custom-modal modal-sm">
        <div class="modal-header">
          <h5 class="modal-title">{{ isEdit ? 'Cập nhật dung tích' : 'Thêm dung tích' }}</h5>
          <button @click="closeModal" type="button" class="btn-close-modal">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="mb-3">
            <label class="form-label">Giá trị (ml) <span class="text-danger">*</span></label>
            <input
              v-model="formData.value"
              type="number"
              step="0.1"
              min="0.01"
              max="5000"
              class="form-control"
              :class="{ 'is-invalid': errors.value }"
              placeholder="VD: 50, 100..."
              @input="validateForm"
              @keyup.enter="handleSubmit"
              autofocus
            >
            <small v-if="errors.value" class="text-danger mt-2 d-block fw-medium">
              <i class="bi bi-exclamation-circle me-1"></i> {{ errors.value }}
            </small>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="closeModal" class="btn btn-light px-4" :disabled="isSaving">Hủy</button>
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
import CapacityTable from '../components/CapacityTable.vue';
import { useCapacityStore } from '../stores/capacity.store';
import type { Capacity, CapacityRequest } from '../types/capacity.type';
import Swal from 'sweetalert2';

const capacityStore = useCapacityStore();
const searchKeyword = ref('');

const showModal = ref(false);
const isEdit = ref(false);
const isSaving = ref(false);
const editId = ref<number | null>(null);

const formData = ref<CapacityRequest>({
  value: 0,
  status: 1
});

const errors = ref({ value: '' });

const Toast = Swal.mixin({
  toast: true,
  position: 'top-end',
  showConfirmButton: false,
  timer: 3000,
  timerProgressBar: true
});

onMounted(() => {
  capacityStore.fetchCapacities();
});

const handleSearch = () => {
  capacityStore.fetchCapacities(searchKeyword.value, 0);
};

const changePage = (pageIndex: number) => {
  if (pageIndex >= 0 && pageIndex < capacityStore.totalPages) {
    capacityStore.fetchCapacities(searchKeyword.value, pageIndex);
  }
};

const normalizeCapacityValue = (value: unknown): number | null => {
  if (value === null || value === undefined || String(value).trim() === '') {
    return null;
  }

  const numberValue = Number(value);

  if (!Number.isFinite(numberValue)) {
    return null;
  }

  return Number(numberValue.toFixed(2));
};

const formatCapacity = (value: unknown): string => {
  const numberValue = normalizeCapacityValue(value);

  if (numberValue === null) {
    return '';
  }

  return Number.isInteger(numberValue)
    ? String(numberValue)
    : String(numberValue);
};

const isDuplicateCapacity = (value: unknown, currentId: number | null = null): boolean => {
  const numberValue = normalizeCapacityValue(value);

  if (numberValue === null) {
    return false;
  }

  return capacityStore.capacities.some((capacity: Capacity) => {
    const capacityValue = normalizeCapacityValue(capacity.value);

    if (capacityValue === null) {
      return false;
    }

    return capacityValue === numberValue && capacity.id !== currentId;
  });
};

const validateForm = () => {
  errors.value.value = '';

  const numberValue = normalizeCapacityValue(formData.value.value);

  if (numberValue === null) {
    errors.value.value = 'Vui lòng nhập dung tích';
    return false;
  }

  if (numberValue <= 0) {
    errors.value.value = 'Dung tích phải lớn hơn 0';
    return false;
  }

  if (numberValue > 5000) {
    errors.value.value = 'Dung tích không hợp lệ, không được vượt quá 5000 ml';
    return false;
  }

  formData.value.value = numberValue as any;
  return true;
};

const openAddModal = () => {
  isEdit.value = false;
  editId.value = null;
  formData.value = { value: '' as any, status: 1 };
  errors.value.value = '';
  showModal.value = true;
};

const openEditModal = (capacity: Capacity) => {
  isEdit.value = true;
  editId.value = capacity.id;
  formData.value = { value: capacity.value, status: capacity.status };
  errors.value.value = '';
  showModal.value = true;
};

const closeModal = () => {
  showModal.value = false;
};

const handleSubmit = async () => {
  if (!validateForm()) return;

  const normalizedValue = normalizeCapacityValue(formData.value.value);

  if (normalizedValue === null) {
    errors.value.value = 'Vui lòng nhập dung tích';
    return;
  }

  const currentId = isEdit.value ? editId.value : null;

  if (isDuplicateCapacity(normalizedValue, currentId)) {
    errors.value.value = `Dung tích '${formatCapacity(normalizedValue)} ml' đã tồn tại và đang hoạt động!`;
    return;
  }

  formData.value.value = normalizedValue as any;
  isSaving.value = true;

  try {
    if (isEdit.value && editId.value) {
      await capacityStore.updateCapacity(editId.value, formData.value);
      Toast.fire({ icon: 'success', title: 'Cập nhật thành công!' });
    } else {
      await capacityStore.createCapacity(formData.value);
      Toast.fire({ icon: 'success', title: 'Thêm dung tích thành công!' });
    }

    closeModal();
  } catch (error: any) {
    console.error('Chi tiết lỗi Axios:', error);

    if (error.response && error.response.data) {
      const responseData = error.response.data;

      if (responseData.errors && responseData.errors.value) {
        errors.value.value = responseData.errors.value;
        return;
      }

      if (responseData.message) {
        const lowerMsg = responseData.message.toLowerCase();

        if (lowerMsg.includes('tồn tại') || lowerMsg.includes('exists') || lowerMsg.includes('duplicate')) {
          errors.value.value = responseData.message;
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

const handleDelete = (id: number) => {
  Swal.fire({
    title: 'Bạn có chắc chắn muốn xóa?',
    text: 'Hành động này sẽ đưa dung tích này vào thùng rác!',
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#dc2626',
    cancelButtonColor: '#94a3b8',
    confirmButtonText: 'Vâng, xóa nó!',
    cancelButtonText: 'Hủy'
  }).then(async (result) => {
    if (result.isConfirmed) {
      try {
        await capacityStore.deleteCapacity(id);
        Swal.fire('Đã xóa!', 'Dung tích đã được đưa vào thùng rác.', 'success');
      } catch (error: any) {
        Swal.fire('Lỗi!', error.message || 'Không thể xóa dung tích này.', 'error');
      }
    }
  });
};

const handleToggleStatus = async (capacity: Capacity) => {
  const newStatus = capacity.status === 1 ? 0 : 1;

  try {
    await capacityStore.updateCapacity(capacity.id, {
      value: capacity.value,
      status: newStatus
    });
    Toast.fire({ icon: 'success', title: 'Đã thay đổi trạng thái!' });
  } catch (error: any) {
    Toast.fire({ icon: 'error', title: 'Không thể đổi trạng thái!' });
    capacityStore.fetchCapacities(searchKeyword.value, capacityStore.currentPage);
  }
};
</script>

<style scoped>
/* Layout Component */
.capacity-page {
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

.custom-modal.modal-sm {
  max-width: 400px;
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
