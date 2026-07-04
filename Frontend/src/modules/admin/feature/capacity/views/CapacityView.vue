<template>
  <div class="container-fluid py-4" style="background-color: #f8f9fc; min-height: 100vh;">
    
    <h3 class="fw-bold mb-4">Danh sách dung tích</h3> 
    
    <div class="card shadow-sm border-0 rounded-3">
      <div class="card-header bg-white d-flex justify-content-between align-items-center py-3 border-0">
        <div class="input-group" style="max-width: 350px;">
          <span class="input-group-text bg-white border-end-0 text-muted"><i class="bi bi-search"></i></span>
          <input 
            v-model="searchKeyword" 
            @keyup.enter="handleSearch"
            type="number" 
            step="0.1"
            class="form-control border-start-0 ps-0 shadow-none" 
            placeholder="Tìm theo dung tích (VD: 50)..."
          >
        </div>
        <button @click="openAddModal" class="btn btn-primary px-4 shadow-sm" style="background-color: #0d6efd;">
          <i class="bi bi-plus-lg me-1"></i> Thêm dung tích
        </button>
      </div>
      
      <div class="card-body p-0">
        <div v-if="capacityStore.isLoading" class="text-center py-5">
          <div class="spinner-border text-primary" role="status"></div>
        </div>

        <CapacityTable 
          v-else 
          :capacities="capacityStore.capacities" 
          @edit="openEditModal"
          @delete="handleDelete"
          @toggle-status="handleToggleStatus"
        />

<div class="d-flex justify-content-between align-items-center p-3 border-top" v-if="capacityStore.totalPages > 0">
  <span class="text-muted small">
    Đang hiển thị trang {{ capacityStore.currentPage + 1 }} / {{ capacityStore.totalPages }}
  </span>
  <nav>
    <ul class="pagination pagination-sm mb-0">
      
      <li class="page-item" :class="{ disabled: capacityStore.currentPage === 0 }">
        <button 
          class="page-link shadow-none" 
          @click="changePage(capacityStore.currentPage - 1)"
          :disabled="capacityStore.currentPage === 0"
        >
          Trước
        </button>
      </li>

      <li class="page-item" v-for="p in capacityStore.totalPages" :key="p" :class="{ active: capacityStore.currentPage === (p - 1) }">
        <button class="page-link shadow-none" @click="changePage(p - 1)">{{ p }}</button>
      </li>

      <li class="page-item" :class="{ disabled: capacityStore.currentPage === capacityStore.totalPages - 1 }">
        <button 
          class="page-link shadow-none" 
          @click="changePage(capacityStore.currentPage + 1)"
          :disabled="capacityStore.currentPage === capacityStore.totalPages - 1"
        >
          Sau
        </button>
      </li>

    </ul>
  </nav>
</div>
      </div>
    </div>

    <div v-if="showModal" class="modal d-block" style="background: rgba(0,0,0,0.5); z-index: 1050;">
      <div class="modal-dialog modal-dialog-centered modal-sm">
        <div class="modal-content border-0 shadow">
          <div class="modal-header">
            <h5 class="modal-title fw-bold">{{ isEdit ? 'Cập nhật dung tích' : 'Thêm dung tích mới' }}</h5>
            <button @click="closeModal" type="button" class="btn-close shadow-none"></button>
          </div>
          <div class="modal-body">
            
            <div class="mb-3">
              <label class="form-label fw-medium">Giá trị dung tích (ml) <span class="text-danger">*</span></label>
              <input v-model="formData.value" type="number" step="0.1" min="0.1" class="form-control shadow-none" placeholder="VD: 50, 100...">
            </div>
            
          </div>
          <div class="modal-footer border-0 bg-light">
            <button @click="closeModal" class="btn btn-light border px-4" :disabled="isSaving">Hủy</button>
            <button @click="handleSubmit" class="btn btn-primary px-4" :disabled="isSaving">
              <span v-if="isSaving" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
              {{ isSaving ? 'Đang lưu...' : 'Lưu lại' }}
            </button>
          </div>
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
import Swal from 'sweetalert2'; // 1. Import thư viện SweetAlert2

const capacityStore = useCapacityStore();
const searchKeyword = ref('');

// State Modal
const showModal = ref(false);
const isEdit = ref(false);
const isSaving = ref(false);
const editId = ref<number | null>(null);

const formData = ref<CapacityRequest>({
  value: 0,
  status: 1
});

// 2. Định nghĩa cấu hình hiệu ứng Toast nhảy ra ở góc trên bên phải
const Toast = Swal.mixin({
  toast: true,
  position: 'top-end',
  showConfirmButton: false,
  timer: 3000,
  timerProgressBar: true
});

// Init load
onMounted(() => {
  capacityStore.fetchCapacities();
});

// Handlers
const handleSearch = () => {
  capacityStore.fetchCapacities(searchKeyword.value, 0);
};

const changePage = (pageIndex: number) => {
  if (pageIndex >= 0 && pageIndex < capacityStore.totalPages) {
    capacityStore.fetchCapacities(searchKeyword.value, pageIndex);
  }
};

const openAddModal = () => {
  isEdit.value = false;
  editId.value = null;
  formData.value = { value: 0, status: 1 };
  showModal.value = true;
};

const openEditModal = (capacity: Capacity) => {
  isEdit.value = true;
  editId.value = capacity.id;
  formData.value = { value: capacity.value, status: capacity.status };
  showModal.value = true;
};

const closeModal = () => {
  showModal.value = false;
};

const handleSubmit = async () => {
  if (!formData.value.value || formData.value.value <= 0) {
    // Thay alert thành Toast cảnh báo
    Toast.fire({ icon: 'warning', title: 'Vui lòng nhập dung tích hợp lệ (lớn hơn 0)!' });
    return;
  }

  isSaving.value = true;
  try {
    if (isEdit.value && editId.value) {
      await capacityStore.updateCapacity(editId.value, formData.value);
      // Thay alert thành Toast thành công
      Toast.fire({ icon: 'success', title: 'Cập nhật thành công!' });
    } else {
      await capacityStore.createCapacity(formData.value);
      // Thay alert thành Toast thành công
      Toast.fire({ icon: 'success', title: 'Thêm dung tích thành công!' });
    }
    closeModal();
  } catch (error: any) {
    // Thay alert thành Toast báo lỗi
    Toast.fire({ icon: 'error', title: error.message || 'Có lỗi xảy ra!' });
  } finally {
    isSaving.value = false;
  }
};

const handleDelete = (id: number) => {
  // Thay confirm thành hộp thoại Swal xịn sò ở giữa màn hình
  Swal.fire({
    title: 'Bạn có chắc chắn muốn xóa?',
    text: "Hành động này sẽ đưa dung tích này vào thùng rác!",
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#dc3545',
    cancelButtonColor: '#6c757d',
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
  
  // Bỏ hẳn hộp thoại confirm để đổi trạng thái mượt mà bằng một cú click con mắt, báo Toast liền
  try {
    await capacityStore.updateCapacity(capacity.id, {
      value: capacity.value,
      status: newStatus
    });
    Toast.fire({ icon: 'success', title: 'Đã thay đổi trạng thái!' });
  } catch (error: any) {
    Toast.fire({ icon: 'error', title: 'Không thể đổi trạng thái!' });
    // Reload lại nếu lỗi để đồng bộ UI
    capacityStore.fetchCapacities(searchKeyword.value, capacityStore.currentPage);
  }
};
</script>