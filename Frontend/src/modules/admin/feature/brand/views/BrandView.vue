<template>
  <div class="container-fluid py-4" style="background-color: #f8f9fc; min-height: 100vh;">
    
    <h3 class="fw-bold mb-4">Danh sách thương hiệu</h3> 
    
    <div class="card shadow-sm border-0 rounded-3">
      <div class="card-header bg-white d-flex justify-content-between align-items-center py-3 border-0">
        
        <div class="input-group" style="max-width: 350px;">
          <span class="input-group-text bg-white border-end-0 text-muted"><i class="bi bi-search"></i></span>
          <input 
            v-model="searchKeyword" 
            @keyup.enter="handleSearch"
            type="text" 
            class="form-control border-start-0 ps-0 shadow-none" 
            placeholder="Tìm kiếm thương hiệu..."
          >
        </div>
        
        <button @click="openAddModal" class="btn btn-primary px-4 shadow-sm" style="background-color: #0d6efd;">
          <i class="bi bi-plus-lg me-1"></i> Thêm thương hiệu
        </button>
      </div>
      
      <div class="card-body p-0">
        <div v-if="brandStore.isLoading" class="text-center py-5">
          <div class="spinner-border text-primary" role="status"></div>
        </div>

        <BrandTable 
          v-else 
          :brands="brandStore.brands" 
          @edit="openEditModal"
          @delete="handleDelete"
          @toggle-status="handleToggleStatus"
        />

        <div class="d-flex justify-content-between align-items-center p-3 border-top" v-if="brandStore.totalPages > 1">
          <span class="text-muted small">
            Đang hiển thị trang {{ brandStore.currentPage + 1 }} / {{ brandStore.totalPages }}
          </span>
          <nav>
            <ul class="pagination pagination-sm mb-0">
              <li class="page-item" :class="{ disabled: brandStore.currentPage === 0 }">
                <button class="page-link shadow-none" @click="changePage(brandStore.currentPage - 1)">Trước</button>
              </li>
              
              <li class="page-item" v-for="p in brandStore.totalPages" :key="p" :class="{ active: brandStore.currentPage === (p - 1) }">
                <button class="page-link shadow-none" @click="changePage(p - 1)">{{ p }}</button>
              </li>
              
              <li class="page-item" :class="{ disabled: brandStore.currentPage === brandStore.totalPages - 1 }">
                <button class="page-link shadow-none" @click="changePage(brandStore.currentPage + 1)">Sau</button>
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
            <h5 class="modal-title fw-bold">{{ isEdit ? 'Cập nhật thương hiệu' : 'Thêm thương hiệu mới' }}</h5>
            <button @click="showModal = false" type="button" class="btn-close shadow-none"></button>
          </div>
          <div class="modal-body">
            <div class="mb-3">
              <label class="form-label fw-medium">Tên thương hiệu <span class="text-danger">*</span></label>
              <input v-model="formData.name" type="text" class="form-control shadow-none" placeholder="Nhập tên thương hiệu (VD: Nike, Adidas...)">
            </div>
            <div class="mb-3">
              <label class="form-label fw-medium">Mô tả chi tiết</label>
              <textarea v-model="formData.description" class="form-control shadow-none" rows="4" placeholder="Nhập mô tả giới thiệu thương hiệu..."></textarea>
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
import { ref, onMounted } from 'vue';
import { useBrandStore } from '../stores/brand.store';
import BrandTable from '../components/BrandTable.vue';
import type { Brand, BrandRequest } from '../types/brand.type';
import Swal from 'sweetalert2'; 

const brandStore = useBrandStore();

const searchKeyword = ref('');
const showModal = ref(false);
const isEdit = ref(false);
const currentId = ref<number | null>(null);

// Cấu trúc Form chứa cả Name và Description của Brand
const formData = ref<BrandRequest>({ name: '', description: '', status: 1 });

const Toast = Swal.mixin({
  toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, timerProgressBar: true
});

onMounted(() => {
  brandStore.fetchBrands();
});

const handleSearch = () => {
  brandStore.fetchBrands(searchKeyword.value, 0); // Tìm kiếm reset về trang đầu
};

const changePage = (page: number) => {
  if (page >= 0 && page < brandStore.totalPages) {
    brandStore.fetchBrands(searchKeyword.value, page);
  }
};

const openAddModal = () => {
  isEdit.value = false;
  formData.value = { name: '', description: '', status: 1 };
  showModal.value = true;
};

const openEditModal = (brand: Brand) => {
  isEdit.value = true;
  currentId.value = brand.id;
  formData.value = { name: brand.name, description: brand.description, status: brand.status };
  showModal.value = true;
};

const handleSubmit = async () => {
  if (!formData.value.name.trim()) {
    Toast.fire({ icon: 'warning', title: 'Vui lòng nhập tên thương hiệu!' });
    return;
  }

  try {
    if (isEdit.value && currentId.value) {
      await brandStore.updateBrand(currentId.value, formData.value);
      Toast.fire({ icon: 'success', title: 'Cập nhật thành công!' });
    } else {
      await brandStore.createBrand(formData.value);
      Toast.fire({ icon: 'success', title: 'Thêm mới thành công!' });
    }
    showModal.value = false; 
  } catch (error: any) {
    // Hiển thị thông báo bóc tách chi tiết lỗi 400 truyền ra từ store
    Toast.fire({ icon: 'error', title: error.message || 'Có lỗi xảy ra!' });
  }
};

const handleToggleStatus = async (brand: Brand) => {
  const newStatus = brand.status === 1 ? 0 : 1;
  try {
    await brandStore.updateBrand(brand.id, { 
      name: brand.name, 
      description: brand.description, 
      status: newStatus 
    });
    Toast.fire({ icon: 'success', title: 'Đã thay đổi trạng thái!' });
  } catch (error) {
    Toast.fire({ icon: 'error', title: 'Không thể đổi trạng thái!' });
    brandStore.fetchBrands(searchKeyword.value, brandStore.currentPage);
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
        await brandStore.deleteBrand(id);
        Swal.fire('Đã xóa!', 'Thương hiệu đã bị xóa.', 'success');
      } catch (error) {
        Swal.fire('Lỗi!', 'Không thể xóa thương hiệu này.', 'error');
      }
    }
  });
};
</script>