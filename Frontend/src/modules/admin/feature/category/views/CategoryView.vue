<template>
  <div class="category-page">
    <div class="page-header">
      <div>
        <h3 class="page-title">
          <i class="bi bi-folder2-open me-2"></i>
          Quản lý danh mục
        </h3>
      </div>
      
      <button v-if="canEdit" @click="openAddModal" class="btn btn-primary px-4">
        <i class="bi bi-plus-circle me-2"></i>
        Thêm danh mục
      </button>
    </div>
    
    <div class="toolbar">
      <div class="search-box">
        <i class="bi bi-search"></i>
        <input 
          v-model="searchKeyword" 
          @keyup.enter="handleSearch"
          type="text" 
          placeholder="Tìm kiếm danh mục..."
        >
      </div>
    </div>
      
    <div v-if="categoryStore.isLoading" class="loading-state">
      Đang tải dữ liệu...
    </div>

    <div v-else class="table-wrapper">
      <CategoryTable 
        :categories="categoryStore.categories" 
        :can-edit="canEdit"
        :can-delete="canDelete"
        @edit="openEditModal"
        @delete="handleDelete"
        @toggle-status="handleToggleStatus"
      />
    </div>

    <div class="footer" v-if="categoryStore.totalPages > 1 && !categoryStore.isLoading">
      <div class="text-muted">
        Đang hiển thị trang <b>{{ categoryStore.currentPage + 1 }}</b> / <b>{{ categoryStore.totalPages }}</b>
      </div>
      <div class="pagination">
        <button 
          class="btn btn-light" 
          :disabled="categoryStore.currentPage === 0" 
          @click="changePage(categoryStore.currentPage - 1)"
        >
          ←
        </button>
        
        <button 
          v-for="pageIndex in displayedPages" 
          :key="pageIndex"
          class="btn"
          :class="categoryStore.currentPage === pageIndex ? 'btn-primary' : 'btn-light'"
          @click="changePage(pageIndex)"
        >
          {{ pageIndex + 1 }}
        </button>
        
        <button 
          class="btn btn-light" 
          :disabled="categoryStore.currentPage === categoryStore.totalPages - 1" 
          @click="changePage(categoryStore.currentPage + 1)"
        >
          →
        </button>
      </div>
    </div>

    <!-- MODAL (Giữ nguyên logic của m, chỉ sửa CSS form) -->
    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="custom-modal">
        <div class="modal-header">
          <h5 class="modal-title">{{ isEdit ? 'Cập nhật danh mục' : 'Thêm danh mục mới' }}</h5>
          <button @click="showModal = false" type="button" class="btn-close-modal">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="mb-3">
            <label class="form-label">Tên danh mục <span class="text-danger">*</span></label>
            <input 
              v-model="formData.name" 
              type="text" 
              class="form-control" 
              :class="{ 'is-invalid': errors.name }"
              placeholder="Nhập tên danh mục..."
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
          <button @click="showModal = false" class="btn btn-light px-4">Hủy</button>
          <button @click="handleSubmit" class="btn btn-primary px-4">Lưu lại</button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useCategoryStore } from '../stores/category.store';
import { useAuthStore } from '@/modules/auth/stores/authStore'; 
import CategoryTable from '../components/CategoryTable.vue';
import type { Category, CategoryRequest } from '../types/category.type';
import Swal from 'sweetalert2'; 

const categoryStore = useCategoryStore();
const authStore = useAuthStore(); 

const searchKeyword = ref('');
const showModal = ref(false);
const isEdit = ref(false);
const currentId = ref<number | null>(null);
const formData = ref<CategoryRequest>({ name: '', status: 1 });

const errors = ref({ name: '' });

const Toast = Swal.mixin({
  toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, timerProgressBar: true
});

const currentUserRole = computed(() => {
  const role = authStore.role || localStorage.getItem('role') || "";
  return role.toUpperCase().trim(); 
});

const canEdit = computed(() => ['OWNER', 'MANAGER'].includes(currentUserRole.value));
const canDelete = computed(() => currentUserRole.value === 'OWNER');

const displayedPages = computed(() => {
  const total = categoryStore.totalPages;
  const current = categoryStore.currentPage;
  const maxVisible = 5; 
  
  if (total <= maxVisible) {
    return Array.from({ length: total }, (_, i) => i);
  }
  
  let start = Math.max(0, current - Math.floor(maxVisible / 2));
  let end = start + maxVisible - 1;
  
  if (end >= total) {
    end = total - 1;
    start = Math.max(0, end - maxVisible + 1);
  }
  
  const pages = [];
  for (let i = start; i <= end; i++) {
    pages.push(i);
  }
  return pages; 
});

onMounted(() => {
  categoryStore.fetchCategories();
});

const handleSearch = () => {
  categoryStore.fetchCategories(searchKeyword.value, 0); 
};

const changePage = (page: number) => {
  if (page >= 0 && page < categoryStore.totalPages) {
    categoryStore.fetchCategories(searchKeyword.value, page);
  }
};

const validateForm = () => {
  errors.value.name = ''; 
  // Chuẩn hóa khoảng trắng để test local (loại bỏ trường hợp qua mặt bằng nhiều dấu cách)
  const nameValue = formData.value.name.trim().replace(/\s+/g, ' ');
  const nameRegex = /^[\p{L}\s]+$/u; 

  if (!nameValue) {
    errors.value.name = 'Tên danh mục không được để trống';
    return false;
  }
  
  if (nameValue.length > 255) {
    errors.value.name = 'Tên danh mục không được vượt quá 255 ký tự';
    return false;
  }
  
  if (!nameRegex.test(nameValue)) {
    errors.value.name = 'Tên danh mục chỉ được chứa chữ cái và khoảng trắng, không bao gồm số hay ký tự đặc biệt';
    return false;
  }

  // Check trùng trên Frontend (Chỉ hoạt động tốt trong nội bộ trang hiện tại)
  const isDuplicate = categoryStore.categories.some((category) => {
    if (isEdit.value && category.id === currentId.value) return false;
    return category.name.trim().replace(/\s+/g, ' ').toLowerCase() === nameValue.toLowerCase();
  });

  if (isDuplicate) {
    errors.value.name = 'Danh mục này đã tồn tại trong hệ thống!';
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

const openEditModal = (category: Category) => {
  isEdit.value = true;
  currentId.value = category.id;
  formData.value = { name: category.name, status: category.status };
  errors.value.name = ''; 
  showModal.value = true;
};

const handleSubmit = async () => {
  if (!validateForm()) return;

  try {
    // Ép payload gửi đi được chuẩn hóa để tránh lỗi không mong muốn
    const payload = {
        ...formData.value,
        name: formData.value.name.trim().replace(/\s+/g, ' ')
    };

    if (isEdit.value && currentId.value) {
      await categoryStore.updateCategory(currentId.value, payload);
      await categoryStore.fetchCategories(searchKeyword.value, categoryStore.currentPage);
      Toast.fire({ icon: 'success', title: 'Cập nhật thành công!' });
    } else {
      await categoryStore.createCategory(payload);
      searchKeyword.value = '';
      await categoryStore.fetchCategories('', 0);
      Toast.fire({ icon: 'success', title: 'Thêm mới thành công!' });
    }
    showModal.value = false; 
  } catch (error: any) {
    console.error("Chi tiết lỗi Axios:", error);
    
    // Tối ưu hóa bộ bắt lỗi để không bị miss Exception từ backend
    let errorMsg = '';
    
    if (error.response && error.response.data) {
      const responseData = error.response.data;
      
      // Lỗi validation @Valid từ SpringBoot
      if (responseData.errors && responseData.errors.name) {
        errors.value.name = responseData.errors.name;
        return; 
      }
      
      // Trích xuất chuỗi message thông minh hơn từ SpringBoot Default Errors
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
      // Bắt các keyword lỗi trùng lặp từ exception
      if (lowerMsg.includes('tồn tại') || lowerMsg.includes('exists') || lowerMsg.includes('duplicate') || lowerMsg.includes('sử dụng')) {
        errors.value.name = 'Danh mục này đã tồn tại trong hệ thống!';
      } else {
        Toast.fire({ icon: 'error', title: errorMsg });
      }
    } else {
      Toast.fire({ icon: 'error', title: 'Máy chủ không phản hồi hoặc đã xảy ra lỗi!' });
    }
  }
};

const handleToggleStatus = async (category: Category) => {
  const newStatus = category.status === 1 ? 0 : 1;
  try {
    await categoryStore.updateCategory(category.id, { name: category.name, status: newStatus });
    await categoryStore.fetchCategories(searchKeyword.value, categoryStore.currentPage);
    Toast.fire({ icon: 'success', title: 'Đã thay đổi trạng thái!' });
  } catch (error: any) {
    Toast.fire({ icon: 'error', title: error.message || 'Không thể đổi trạng thái!' });
  }
};

const handleDelete = (id: number) => {
  Swal.fire({
    title: 'Bạn có chắc chắn muốn xóa?',
    text: "Hành động này sẽ đưa danh mục vào thùng rác!",
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#dc2626',
    cancelButtonColor: '#94a3b8',
    confirmButtonText: 'Vâng, xóa nó!',
    cancelButtonText: 'Hủy'
  }).then(async (result) => {
    if (result.isConfirmed) {
      try {
        const isLastItemOnPage = categoryStore.categories.length === 1;
        const isNotFirstPage = categoryStore.currentPage > 0;

        await categoryStore.deleteCategory(id);
        
        if (isLastItemOnPage && isNotFirstPage) {
          await categoryStore.fetchCategories(searchKeyword.value, categoryStore.currentPage - 1);
        } else {
          await categoryStore.fetchCategories(searchKeyword.value, categoryStore.currentPage);
        }

        Swal.fire('Đã xóa!', 'Danh mục đã bị đưa vào thùng rác.', 'success');
      } catch (error: any) {
        Swal.fire('Không thể xóa!', error.message || 'Lỗi hệ thống!', 'error');
      }
    }
  });
};
</script>

<style scoped>
/* Layout Component */
.category-page {
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