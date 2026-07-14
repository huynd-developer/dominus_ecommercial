<template>
  <div class="container-fluid py-4">
    <h2 class="mb-4">Danh sách Nhóm hương</h2>

    <div class="d-flex justify-content-between mb-3">
      <input 
        v-model="searchKeyword" 
        @input="handleSearch"
        type="text" 
        class="form-control w-25" 
        placeholder="Tìm theo tên nhóm hương..." 
        />
      <button @click="openCreateModal" class="btn btn-dark">
        + Thêm nhóm hương
      </button>
    </div>

    <FragranceFamilyTable 
      :fragranceFamilies="store.fragranceFamilies"
      @edit="openEditModal"
      @delete="handleDelete"
      @toggle-status="handleToggleStatus"
    />

    <div class="d-flex justify-content-between align-items-center mt-3">
      <span class="text-muted">Đang hiển thị trang {{ store.currentPage + 1 }} / {{ store.totalPages }}</span>
      <div>
        <button @click="changePage(store.currentPage - 1)" :disabled="store.currentPage === 0" class="btn btn-sm btn-light border me-1">Trước</button>
        <button @click="changePage(store.currentPage + 1)" :disabled="store.currentPage >= store.totalPages - 1" class="btn btn-sm btn-light border">Sau</button>
      </div>
    </div>

    <div v-if="showModal" class="modal fade show d-block" tabindex="-1" style="background: rgba(0,0,0,0.5);">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header border-bottom-0">
            <h5 class="modal-title fw-bold">{{ isEdit ? 'Cập nhật nhóm hương' : 'Thêm nhóm hương mới' }}</h5>
            <button type="button" class="btn-close" @click="showModal = false"></button>
          </div>
          <div class="modal-body">
            <div class="mb-3">
              <label class="form-label fw-medium">Tên nhóm hương <span class="text-danger">*</span></label>
              <input 
                v-model="formData.name" 
                type="text" 
                class="form-control" 
                :class="{ 'is-invalid': errors.name }" 
                placeholder="VD: Floral, Woody..." 
                @input="validateForm" 
                @keyup.enter="handleSubmit"
              >
              <small v-if="errors.name" class="text-danger mt-1 d-block">{{ errors.name }}</small>
            </div>
            <div class="mb-3">
              <label class="form-label fw-medium">Trạng thái</label>
              <select v-model="formData.status" class="form-select">
                <option :value="1">Đang hoạt động</option>
                <option :value="0">Tạm ẩn</option>
              </select>
            </div>
          </div>
          <div class="modal-footer border-top-0">
            <button type="button" class="btn btn-light border" @click="showModal = false">Hủy</button>
            <button type="button" class="btn btn-primary px-4" @click="handleSubmit" :disabled="isSaving">
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

// 👇 THÊM MỚI: Biến lưu trữ lỗi của Form
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

// 👇 THÊM MỚI: Hàm kiểm tra lỗi (sẽ chạy khi gõ phím và khi bấm Lưu)
const validateForm = () => {
  errors.value.name = ''; // Reset lỗi
  const nameValue = formData.value.name.trim();
  // Trong Javascript, Regex có unicode (tiếng Việt) phải thêm cờ 'u' ở cuối
  const nameRegex = /^[\p{L}\s\(\)]+$/u;

  if (!nameValue) {
    errors.value.name = 'Tên nhóm hương không được để trống';
    return false;
  }
  if (nameValue.length > 255) {
    errors.value.name = 'Tên nhóm hương không được vượt quá 255 ký tự';
    return false;
  }
  if (!nameRegex.test(nameValue)) {
    // Sửa lại câu thông báo cho hợp lý
    errors.value.name = 'Chỉ được chứa chữ cái, khoảng trắng và dấu ngoặc đơn';
    return false;
  }
  return true;
};

const openCreateModal = () => {
  isEdit.value = false;
  formData.value = { name: '', status: 1 };
  errors.value.name = ''; // Reset lỗi khi mở form
  showModal.value = true;
};

const openEditModal = (item: FragranceFamily) => {
  isEdit.value = true;
  currentId.value = item.id;
  formData.value = { name: item.name, status: item.status };
  errors.value.name = ''; // Reset lỗi khi mở form
  showModal.value = true;
};

const handleSubmit = async () => {
  if (!validateForm()) return; 

  try {
    isSaving.value = true;

    if (isEdit.value && currentId.value) {
      await store.updateFragranceFamily(currentId.value, formData.value);
      Toast.fire({ icon: 'success', title: 'Cập nhật thành công!' });
    } else {
      await store.createFragranceFamily(formData.value);
      Toast.fire({ icon: 'success', title: 'Thêm mới thành công!' });
    }
    showModal.value = false; 

  } catch (error: any) {
    // 1. In ra console để bạn dễ debug F12
    console.error("Chi tiết lỗi API:", error);

    // 2. Bóc tách thông báo lỗi an toàn
    let errorMsg = 'Có lỗi xảy ra!';
    
    if (error.response?.data) {
      if (typeof error.response.data === 'string') {
        // Trường hợp backend trả về thẳng 1 chuỗi text
        errorMsg = error.response.data;
      } else if (error.response.data.message) {
        // Trường hợp backend trả về JSON có trường message
        errorMsg = error.response.data.message;
      } else if (error.response.data.timestamp) {
        // Trường hợp rớt vào lỗi mặc định của Spring Boot (nơi giấu message)
        errorMsg = 'Nhóm hương này đã tồn tại hoặc dữ liệu không hợp lệ!';
      }
    } else if (error.message) {
      // Lỗi đứt kết nối mạng hoặc Axios
      errorMsg = error.message;
    }

    // 3. Ép kiểu về chuỗi chữ thường để an toàn so sánh
    const lowerMsg = String(errorMsg).toLowerCase();

    // 4. Nếu là lỗi trùng lặp -> Nhét xuống chữ đỏ dưới ô input
    if (lowerMsg.includes('tồn tại') || lowerMsg.includes('exists') || lowerMsg.includes('duplicate')) {
      errors.value.name = 'Nhóm hương này đã tồn tại trong hệ thống!'; 
    } else {
      // Các lỗi hệ thống khác -> Văng Toast
      Toast.fire({ icon: 'error', title: errorMsg });
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
    confirmButtonColor: '#dc3545', 
    cancelButtonColor: '#6c757d',
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