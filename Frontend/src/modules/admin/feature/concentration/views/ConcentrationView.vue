<template>
  <div class="container-fluid py-4">
    <h2 class="mb-4">Danh sách Nồng độ nước hoa</h2>

    <div class="d-flex justify-content-between mb-3">
      <div class="input-group w-25">
        <input 
          v-model="searchKeyword" 
          @input="handleSearch"
          @keyup.enter="handleSearch"
          type="text" 
          class="form-control" 
          placeholder="Tìm theo tên nồng độ..." 
        />
        <button @click="handleSearch" class="btn btn-outline-secondary" type="button">
          Tìm
        </button>
      </div>
      <button @click="openAddModal" class="btn btn-dark">
        + Thêm nồng độ
      </button>
    </div>

    <ConcentrationTable 
      :concentrations="store.concentrations"
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
            <h5 class="modal-title fw-bold">{{ isEdit ? 'Cập nhật nồng độ' : 'Thêm nồng độ mới' }}</h5>
            <button type="button" class="btn-close" @click="showModal = false"></button>
          </div>
          <div class="modal-body">
            <div class="mb-3">
              <label class="form-label fw-medium">Tên nồng độ <span class="text-danger">*</span></label>
              <input v-model="formData.name" type="text" class="form-control" placeholder="VD: EDP, EDT, Parfum..." @keyup.enter="handleSubmit">
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
import { useConcentrationStore } from '../stores/concentration.store';
import ConcentrationTable from '../components/ConcentrationTable.vue';
import type { Concentration, ConcentrationRequest } from '../types/concentration.type';
import Swal from 'sweetalert2';

const store = useConcentrationStore();
const searchKeyword = ref('');

// State Modal
const showModal = ref(false);
const isEdit = ref(false);
const isSaving = ref(false);
const editId = ref<number | null>(null);

const formData = ref<ConcentrationRequest>({
  name: '',
  status: 1
});

// Cấu hình SweetAlert2 Toast
const Toast = Swal.mixin({
  toast: true,
  position: 'top-end',
  showConfirmButton: false,
  timer: 3000,
  timerProgressBar: true
});

onMounted(() => {
  store.fetchConcentrations('', 0);
});

const handleSearch = () => {
  store.fetchConcentrations(searchKeyword.value, 0);
};

const changePage = (pageIndex: number) => {
  if (pageIndex >= 0 && pageIndex < store.totalPages) {
    store.fetchConcentrations(searchKeyword.value, pageIndex);
  }
};

const openAddModal = () => {
  isEdit.value = false;
  editId.value = null;
  formData.value = { name: '', status: 1 };
  showModal.value = true;
};

const openEditModal = (item: Concentration) => {
  isEdit.value = true;
  editId.value = item.id;
  formData.value = { name: item.name, status: item.status };
  showModal.value = true;
};

const handleSubmit = async () => {
  if (!formData.value.name.trim()) {
    Toast.fire({ icon: 'warning', title: 'Vui lòng nhập tên nồng độ!' });
    return;
  }

  isSaving.value = true;
  try {
    if (isEdit.value && editId.value) {
      await store.updateConcentration(editId.value, formData.value);
      Toast.fire({ icon: 'success', title: 'Cập nhật thành công!' });
    } else {
      await store.createConcentration(formData.value);
      Toast.fire({ icon: 'success', title: 'Thêm nồng độ thành công!' });
    }
    showModal.value = false;
  } catch (error: any) {
    Toast.fire({ icon: 'error', title: error.message || 'Có lỗi xảy ra!' });
  } finally {
    isSaving.value = false;
  }
};

const handleDelete = (id: number) => {
  Swal.fire({
    title: 'Bạn có chắc chắn muốn xóa?',
    text: "Hành động này sẽ đưa nồng độ này vào thùng rác!",
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#dc3545',
    cancelButtonColor: '#6c757d',
    confirmButtonText: 'Vâng, xóa nó!',
    cancelButtonText: 'Hủy'
  }).then(async (result) => {
    if (result.isConfirmed) {
      try {
        await store.deleteConcentration(id);
        Swal.fire('Đã xóa!', 'Nồng độ đã được đưa vào thùng rác.', 'success');
      } catch (error: any) {
        Swal.fire('Lỗi!', error.message || 'Không thể xóa nồng độ này.', 'error');
      }
    }
  });
};

const handleToggleStatus = async (item: Concentration) => {
  const newStatus = item.status === 1 ? 0 : 1;
  try {
    await store.updateConcentration(item.id, {
      name: item.name,
      status: newStatus
    });
    Toast.fire({ icon: 'success', title: 'Đã thay đổi trạng thái!' });
  } catch (error: any) {
    Toast.fire({ icon: 'error', title: 'Không thể đổi trạng thái!' });
    store.fetchConcentrations(searchKeyword.value, store.currentPage);
  }
};
</script>