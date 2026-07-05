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
              <input v-model="formData.name" type="text" class="form-control" placeholder="VD: Floral, Woody..." @keyup.enter="handleSubmit">
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
import Swal from 'sweetalert2'; // Bắt buộc import SweetAlert2 để làm hiệu ứng

const store = useFragranceFamilyStore();
const searchKeyword = ref('');

// Các biến phục vụ cho form Modal Thêm/Sửa
const showModal = ref(false);
const isEdit = ref(false);
const currentId = ref<number | null>(null);
const isSaving = ref(false);
const formData = ref<FragranceFamilyRequest>({ name: '', status: 1 });

// Cấu hình Toast thông báo góc trên bên phải
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

// Mở modal thêm mới (thay cho prompt cũ)
const openCreateModal = () => {
  isEdit.value = false;
  formData.value = { name: '', status: 1 };
  showModal.value = true;
};

// Mở modal sửa (Đổ dữ liệu lên form thay cho prompt cũ)
const openEditModal = (item: FragranceFamily) => {
  isEdit.value = true;
  currentId.value = item.id;
  formData.value = { name: item.name, status: item.status };
  showModal.value = true;
};

// Hàm mới: Xử lý khi click "Lưu" trong Form Modal
const handleSubmit = async () => {
  if (!formData.value.name.trim()) {
    Toast.fire({ icon: 'warning', title: 'Vui lòng nhập tên nhóm hương!' });
    return;
  }

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
    Toast.fire({ icon: 'error', title: error.message || 'Có lỗi xảy ra!' });
  } finally {
    isSaving.value = false;
  }
};

// Thay đổi trạng thái nhanh bằng con mắt (Ẩn/Hiện) có Toast góc phải
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
    // Nếu lỗi thì gọi lại list để reset UI về như cũ
    store.fetchFragranceFamilies(searchKeyword.value, store.currentPage);
  }
};

// Xóa nhóm hương: Dùng SweetAlert2 ở giữa màn hình thay cho confirm()
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