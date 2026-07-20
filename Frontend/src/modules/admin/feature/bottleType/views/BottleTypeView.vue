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
              <input 
                v-model="formData.name" 
                type="text" 
                class="form-control shadow-none" 
                :class="{ 'is-invalid': errors.name }"
                placeholder="VD: Chai gốc Fullbox, Ống chiết..."
                @input="validateForm"
                @keyup.enter="handleSubmit"
              >
              <small v-if="errors.name" class="text-danger mt-1 d-block">{{ errors.name }}</small>
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

// 👇 THÊM MỚI: Biến state chứa thông báo lỗi cho ô input
const errors = ref({ name: '' });

const Toast = Swal.mixin({
  toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, timerProgressBar: true
});

onMounted(() => {
  bottleTypeStore.fetchBottleTypes();
});

const handleSearch = () => { bottleTypeStore.fetchBottleTypes(searchKeyword.value, 0); };
const changePage = (page: number) => { if (page >= 0 && page < bottleTypeStore.totalPages) bottleTypeStore.fetchBottleTypes(searchKeyword.value, page); };

// 👇 THÊM MỚI: Hàm kiểm tra lỗi Form Frontend
const validateForm = () => {
  errors.value.name = ''; 
  const nameValue = formData.value.name.trim();
  
  // Regex: Cho phép chữ cái tiếng Việt, khoảng trắng và dấu ngoặc đơn ()
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

// Mở modal thêm mới
const openAddModal = () => {
  isEdit.value = false;
  formData.value = { name: '', status: 1 };
  errors.value.name = ''; // Reset lỗi
  showModal.value = true;
};

// Mở modal sửa (Đổ dữ liệu)
const openEditModal = (item: BottleType) => {
  isEdit.value = true;
  currentId.value = item.id;
  formData.value = { name: item.name, status: item.status };
  errors.value.name = ''; // Reset lỗi
  showModal.value = true;
};

// Click nút Lưu
const handleSubmit = async () => {
  // Chặn lại nếu Form lỗi
  if (!validateForm()) return;

  try {
    isSaving.value = true;

    if (isEdit.value && currentId.value) {
      await bottleTypeStore.updateBottleType(currentId.value, formData.value);
      await bottleTypeStore.fetchBottleTypes(searchKeyword.value, bottleTypeStore.currentPage); // Tải lại trang hiện tại
      Toast.fire({ icon: 'success', title: 'Cập nhật thành công!' });
    } else {
      await bottleTypeStore.createBottleType(formData.value);
      searchKeyword.value = '';
      await bottleTypeStore.fetchBottleTypes('', 0); // Về trang đầu tiên
      Toast.fire({ icon: 'success', title: 'Thêm mới thành công!' });
    }
    showModal.value = false; 
  } catch (error: any) {
    console.error("Chi tiết lỗi Axios:", error);

    // 👇 BÓC TÁCH LỖI TỪ BACKEND TRẢ VỀ
    if (error.response && error.response.data) {
      const responseData = error.response.data;

      // 1. Lỗi Validation từ Spring Boot
      if (responseData.errors && responseData.errors.name) {
        errors.value.name = responseData.errors.name;
        return; 
      }

      // 2. Lỗi trùng lặp từ Service (IllegalArgumentException)
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

// Thay đổi trạng thái nhanh (Ẩn/Hiện)
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

// Xóa mềm / Xóa vĩnh viễn tùy API Backend
const handleDelete = (id: number) => {
  Swal.fire({
    title: 'Bạn có chắc chắn muốn xóa?',
    text: "Hành động này sẽ đưa loại chai vào thùng rác!",
    icon: 'warning', showCancelButton: true, confirmButtonColor: '#dc3545', cancelButtonColor: '#6c757d',
    confirmButtonText: 'Vâng, xóa nó!', cancelButtonText: 'Hủy'
  }).then(async (result) => {
    if (result.isConfirmed) {
      try {
        const isLastItemOnPage = bottleTypeStore.bottleTypes && bottleTypeStore.bottleTypes.length === 1;
        const isNotFirstPage = bottleTypeStore.currentPage > 0;

        await bottleTypeStore.deleteBottleType(id);
        
        // Cập nhật lại danh sách thông minh
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