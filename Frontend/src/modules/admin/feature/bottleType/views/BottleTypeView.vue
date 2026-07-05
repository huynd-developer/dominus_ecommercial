<template>
  <div class="container-fluid py-4" style="background-color: #f8f9fc; min-height: 100vh;">
    
    <h3 class="fw-bold mb-4">Danh mục loại chai</h3> 
    
    <div class="card shadow-sm border-0 rounded-3">
      <div class="card-header bg-white d-flex justify-content-between align-items-center py-3 border-0">
        <div class="input-group" style="max-width: 350px;">
          <span class="input-group-text bg-white border-end-0 text-muted"><i class="bi bi-search"></i></span>
          <input 
            v-model="searchKeyword" 
            @keyup.enter="handleSearch"
            type="text" 
            class="form-control border-start-0 ps-0 shadow-none" 
            placeholder="Tìm kiếm loại chai..."
          >
        </div>
        <button @click="openAddModal" class="btn btn-primary px-4 shadow-sm" style="background-color: #0d6efd;">
          <i class="bi bi-plus-lg me-1"></i> Thêm loại chai
        </button>
      </div>
      
      <div class="card-body p-0">
        <div v-if="bottleTypeStore.isLoading" class="text-center py-5">
          <div class="spinner-border text-primary" role="status"></div>
        </div>

        <BottleTypeTable 
          v-else 
          :bottleTypes="bottleTypeStore.bottleTypes" 
          @edit="openEditModal"
          @delete="handleDelete"
          @toggle-status="handleToggleStatus"
        />

        <div class="d-flex justify-content-between align-items-center p-3 border-top" v-if="bottleTypeStore.totalPages > 0">
          <span class="text-muted small">
            Đang hiển thị trang {{ bottleTypeStore.currentPage + 1 }} / {{ bottleTypeStore.totalPages }}
          </span>
          <nav>
            <ul class="pagination pagination-sm mb-0">
              <li class="page-item" :class="{ disabled: bottleTypeStore.currentPage === 0 }">
                <button class="page-link shadow-none" @click="changePage(bottleTypeStore.currentPage - 1)">Trước</button>
              </li>
              <li class="page-item" v-for="p in bottleTypeStore.totalPages" :key="p" :class="{ active: bottleTypeStore.currentPage === (p - 1) }">
                <button class="page-link shadow-none" @click="changePage(p - 1)">{{ p }}</button>
              </li>
              <li class="page-item" :class="{ disabled: bottleTypeStore.currentPage === bottleTypeStore.totalPages - 1 }">
                <button class="page-link shadow-none" @click="changePage(bottleTypeStore.currentPage + 1)">Sau</button>
              </li>
            </ul>
          </nav>
        </div>
      </div>
    </div>

    <div v-if="showModal" class="modal d-block" style="background: rgba(0,0,0,0.5); z-index: 1050;">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
          <div class="modal-header">
            <h5 class="modal-title fw-bold">{{ isEdit ? 'Cập nhật loại chai' : 'Thêm loại chai mới' }}</h5>
            <button @click="showModal = false" type="button" class="btn-close shadow-none"></button>
          </div>
          <div class="modal-body">
            <div class="mb-3">
              <label class="form-label fw-medium">Tên loại chai <span class="text-danger">*</span></label>
              <input v-model="formData.name" type="text" class="form-control shadow-none" placeholder="VD: Chai gốc Fullbox, Ống chiết...">
            </div>
          </div>
          <div class="modal-footer border-0 bg-light">
            <button @click="showModal = false" class="btn btn-light border px-4" :disabled="isSaving">Hủy</button>
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

const Toast = Swal.mixin({
  toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, timerProgressBar: true
});

onMounted(() => {
  bottleTypeStore.fetchBottleTypes();
});

const handleSearch = () => { bottleTypeStore.fetchBottleTypes(searchKeyword.value, 0); };
const changePage = (page: number) => { if (page >= 0 && page < bottleTypeStore.totalPages) bottleTypeStore.fetchBottleTypes(searchKeyword.value, page); };

// Mở modal thêm mới
const openAddModal = () => {
  isEdit.value = false;
  formData.value = { name: '', status: 1 };
  showModal.value = true;
};

// Mở modal sửa (Đổ dữ liệu)
const openEditModal = (item: BottleType) => {
  isEdit.value = true;
  currentId.value = item.id;
  formData.value = { name: item.name, status: item.status };
  showModal.value = true;
};

// Click nút Lưu
const handleSubmit = async () => {
  if (!formData.value.name.trim()) {
    Toast.fire({ icon: 'warning', title: 'Vui lòng nhập tên loại chai!' });
    return;
  }

  try {
    isSaving.value = true;

    if (isEdit.value && currentId.value) {
      await bottleTypeStore.updateBottleType(currentId.value, formData.value);
      Toast.fire({ icon: 'success', title: 'Cập nhật thành công!' });
    } else {
      await bottleTypeStore.createBottleType(formData.value);
      Toast.fire({ icon: 'success', title: 'Thêm mới thành công!' });
    }
    showModal.value = false; 
  } catch (error: any) {
    Toast.fire({ icon: 'error', title: error.message || 'Có lỗi xảy ra!' });
  } finally {
    isSaving.value = false;
  }
};

// Thay đổi trạng thái nhanh (Ẩn/Hiện)
const handleToggleStatus = async (item: BottleType) => {
  const newStatus = item.status === 1 ? 0 : 1;
  try {
    await bottleTypeStore.updateBottleType(item.id, { 
      name: item.name, 
      status: newStatus
    });
    Toast.fire({ icon: 'success', title: 'Đã thay đổi trạng thái!' });
  } catch (error) {
    Toast.fire({ icon: 'error', title: 'Không thể đổi trạng thái!' });
    bottleTypeStore.fetchBottleTypes(searchKeyword.value, bottleTypeStore.currentPage);
  }
};

// Xóa mềm / Xóa vĩnh viễn tùy API Backend
const handleDelete = (id: number) => {
  Swal.fire({
    title: 'Bạn có chắc chắn muốn xóa?',
    text: "Hành động này không thể hoàn tác!",
    icon: 'warning', showCancelButton: true, confirmButtonColor: '#dc3545', cancelButtonColor: '#6c757d',
    confirmButtonText: 'Vâng, xóa nó!', cancelButtonText: 'Hủy'
  }).then(async (result) => {
    if (result.isConfirmed) {
      try {
        await bottleTypeStore.deleteBottleType(id);
        Swal.fire('Đã xóa!', 'Loại chai đã bị xóa.', 'success');
      } catch (error) {
        Swal.fire('Lỗi!', 'Không thể xóa loại chai này.', 'error');
      }
    }
  });
};
</script>