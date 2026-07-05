<template>
  <div class="container-fluid py-4" style="background-color: #f8f9fc; min-height: 100vh;">
    
    <h3 class="fw-bold mb-4">Danh sách danh mục</h3> 
    
    <div class="card shadow-sm border-0 rounded-3">
      <div class="card-header bg-white d-flex justify-content-between align-items-center py-3 border-0">
        
        <div class="input-group" style="max-width: 350px;">
          <span class="input-group-text bg-white border-end-0 text-muted"><i class="bi bi-search"></i></span>
          <input 
            v-model="searchKeyword" 
            @keyup.enter="handleSearch"
            type="text" 
            class="form-control border-start-0 ps-0 shadow-none" 
            placeholder="Tìm kiếm danh mục..."
          >
        </div>
        
        <button v-if="canEdit" @click="openAddModal" class="btn btn-primary px-4 shadow-sm" style="background-color: #0d6efd;">
          <i class="bi bi-plus-lg me-1"></i> Thêm danh mục
        </button>
      </div>
      
      <div class="card-body p-0">
        <div v-if="categoryStore.isLoading" class="text-center py-5">
          <div class="spinner-border text-primary" role="status"></div>
        </div>

        <CategoryTable 
          v-else 
          :categories="categoryStore.categories" 
          :can-edit="canEdit"
          :can-delete="canDelete"
          @edit="openEditModal"
          @delete="handleDelete"
          @toggle-status="handleToggleStatus"
        />

        <div class="d-flex justify-content-between align-items-center p-3 border-top" v-if="categoryStore.totalPages > 1">
          <span class="text-muted small">
            Đang hiển thị trang {{ categoryStore.currentPage + 1 }} / {{ categoryStore.totalPages }}
          </span>
          <nav>
            <ul class="pagination pagination-sm mb-0">
              <li class="page-item" :class="{ disabled: categoryStore.currentPage === 0 }">
                <button class="page-link shadow-none" @click="changePage(categoryStore.currentPage - 1)">Trước</button>
              </li>
              
              <li 
                class="page-item" 
                v-for="pageIndex in displayedPages" 
                :key="pageIndex" 
                :class="{ active: categoryStore.currentPage === pageIndex }"
              >
                <button class="page-link shadow-none" @click="changePage(pageIndex)">{{ pageIndex + 1 }}</button>
              </li>
              
              <li class="page-item" :class="{ disabled: categoryStore.currentPage === categoryStore.totalPages - 1 }">
                <button class="page-link shadow-none" @click="changePage(categoryStore.currentPage + 1)">Sau</button>
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
            <h5 class="modal-title fw-bold">{{ isEdit ? 'Cập nhật danh mục' : 'Thêm danh mục mới' }}</h5>
            <button @click="showModal = false" type="button" class="btn-close shadow-none"></button>
          </div>
          <div class="modal-body">
            <div class="mb-3">
              <label class="form-label fw-medium">Tên danh mục <span class="text-danger">*</span></label>
              <input v-model="formData.name" type="text" class="form-control shadow-none" placeholder="Nhập tên danh mục...">
            </div>
          </div>
          <div class="modal-footer border-0 bg-light">
            <button @click="showModal = false" class="btn btn-light border px-4">Hủy</button>
            <button @click="handleSubmit" class="btn btn-primary px-4">Lưu lại</button>
          </div>
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

const Toast = Swal.mixin({
  toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, timerProgressBar: true
});

// --- 🔐 LOGIC PHÂN QUYỀN ---
const currentUserRole = computed(() => {
  const role = authStore.role || localStorage.getItem('role') || "";
  return role.toUpperCase().trim(); 
});

const canEdit = computed(() => ['OWNER', 'MANAGER'].includes(currentUserRole.value));
const canDelete = computed(() => currentUserRole.value === 'OWNER');

// --- 🛠️ ALGORITHM: THU GỌN SỐ TRANG TRÊN GIAO DIỆN (Hiển thị tối đa 5 nút trang kề cận) ---
const displayedPages = computed(() => {
  const total = categoryStore.totalPages;
  const current = categoryStore.currentPage;
  const maxVisible = 5; // Số lượng nút trang tối đa muốn hiển thị
  
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
  return pages; // Trả về mảng số trang dạng index: [0, 1, 2, 3, 4]
});

onMounted(() => {
  categoryStore.fetchCategories();
});

const handleSearch = () => {
  categoryStore.fetchCategories(searchKeyword.value, 0); // Tìm kiếm mặc định nhảy về trang 0
};

const changePage = (page: number) => {
  if (page >= 0 && page < categoryStore.totalPages) {
    categoryStore.fetchCategories(searchKeyword.value, page);
  }
};

const openAddModal = () => {
  isEdit.value = false;
  formData.value = { name: '', status: 1 };
  showModal.value = true;
};

const openEditModal = (category: Category) => {
  isEdit.value = true;
  currentId.value = category.id;
  formData.value = { name: category.name, status: category.status };
  showModal.value = true;
};

// --- 🔄 ĐỒNG BỘ LUỒNG DỮ LIỆU SAU KHI SUBMIT ---
const handleSubmit = async () => {
  if (!formData.value.name.trim()) {
    Toast.fire({ icon: 'warning', title: 'Vui lòng nhập tên danh mục!' });
    return;
  }

  try {
    if (isEdit.value && currentId.value) {
      await categoryStore.updateCategory(currentId.value, formData.value);
      // Khi Cập nhật thành công: Tải lại và giữ nguyên từ khóa tìm kiếm kèm trang hiện tại
      await categoryStore.fetchCategories(searchKeyword.value, categoryStore.currentPage);
      Toast.fire({ icon: 'success', title: 'Cập nhật thành công!' });
    } else {
      await categoryStore.createCategory(formData.value);
      // Khi Thêm mới thành công: Reset từ khóa tìm kiếm về rỗng và nhảy về trang đầu tiên (trang 0) để người dùng thấy ngay danh mục mới tạo
      searchKeyword.value = '';
      await categoryStore.fetchCategories('', 0);
      Toast.fire({ icon: 'success', title: 'Thêm mới thành công!' });
    }
    showModal.value = false; 
  } catch (error: any) {
    Toast.fire({ icon: 'error', title: error.message || 'Có lỗi xảy ra!' });
  }
};

const handleToggleStatus = async (category: Category) => {
  const newStatus = category.status === 1 ? 0 : 1;
  try {
    await categoryStore.updateCategory(category.id, { name: category.name, status: newStatus });
    // Đồng bộ: Giữ nguyên từ khóa tìm kiếm và trang hiện tại khi đổi trạng thái ẩn/hiện
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
    confirmButtonColor: '#dc3545',
    cancelButtonColor: '#6c757d',
    confirmButtonText: 'Vâng, xóa nó!',
    cancelButtonText: 'Hủy'
  }).then(async (result) => {
    if (result.isConfirmed) {
      try {
        const isLastItemOnPage = categoryStore.categories.length === 1;
        const isNotFirstPage = categoryStore.currentPage > 0;

        await categoryStore.deleteCategory(id);
        
        // Thao tác chuyển trang thông minh khi bản ghi duy nhất/cuối cùng của trang bị xóa
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