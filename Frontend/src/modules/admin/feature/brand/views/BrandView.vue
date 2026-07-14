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
<div class="d-flex justify-content-between align-items-center p-3 border-top" v-if="brandStore.totalPages > 0">
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
            
            <div class="mb-4">
              <label class="form-label fw-medium">Logo thương hiệu</label>
              <div class="d-flex align-items-center gap-3">
                <img v-if="previewImageUrl" :src="previewImageUrl" class="rounded border" style="width: 80px; height: 80px; object-fit: cover;">
                <div v-else class="rounded bg-light d-flex align-items-center justify-content-center text-muted border" style="width: 80px; height: 80px;">
                  <i class="bi bi-image fs-3"></i>
                </div>
                
                <div>
                  <input type="file" @change="handleFileChange" class="form-control form-control-sm shadow-none w-auto" accept="image/jpeg, image/png, image/jpg, image/webp">
                  <small class="text-muted d-block mt-1">Hỗ trợ JPG, PNG, WEBP. Tối đa 5MB.</small>
                  <button v-if="previewImageUrl" @click="removeImage" type="button" class="btn btn-sm btn-outline-danger mt-2">
                    <i class="bi bi-trash"></i> Xóa ảnh
                  </button>
                </div>
              </div>
            </div>

            <div class="mb-3">
              <label class="form-label fw-medium">Tên thương hiệu <span class="text-danger">*</span></label>
              <input 
                v-model="formData.name" 
                type="text" 
                class="form-control shadow-none" 
                :class="{ 'is-invalid': errors.name }"
                placeholder="VD: Nike, Adidas, H&M..."
                @input="validateForm"
                @keyup.enter="handleSubmit"
              >
              <small v-if="errors.name" class="text-danger mt-1 d-block">{{ errors.name }}</small>
            </div>
            <div class="mb-3">
              <label class="form-label fw-medium">Mô tả chi tiết</label>
              <textarea 
                v-model="formData.description" 
                class="form-control shadow-none" 
                :class="{ 'is-invalid': errors.description }"
                rows="3" 
                placeholder="Giới thiệu thương hiệu..."
                @input="validateForm"
              ></textarea>
              <small v-if="errors.description" class="text-danger mt-1 d-block">{{ errors.description }}</small>
            </div>
          </div>
          <div class="modal-footer border-0 bg-light">
            <button @click="showModal = false" class="btn btn-light border px-4" :disabled="isUploading">Hủy</button>
            <button @click="handleSubmit" class="btn btn-primary px-4" :disabled="isUploading">
              <span v-if="isUploading" class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
              {{ isUploading ? 'Đang lưu...' : 'Lưu lại' }}
            </button>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useBrandStore } from '../stores/brand.store';
import { brandService } from '../services/brand.service'; 
import BrandTable from '../components/BrandTable.vue';
import type { Brand, BrandRequest } from '../types/brand.type';
import Swal from 'sweetalert2'; 

const brandStore = useBrandStore();

const searchKeyword = ref('');
const showModal = ref(false);
const isEdit = ref(false);
const currentId = ref<number | null>(null);

// Trạng thái Upload ảnh
const selectedFile = ref<File | null>(null);
const previewImageUrl = ref<string | null>(null);
const isUploading = ref(false);

const formData = ref<BrandRequest>({ name: '', description: '', status: 1, logoUrl: null });

// Quản lý lỗi hiển thị (chữ đỏ)
const errors = ref({ name: '', description: '' });

const Toast = Swal.mixin({
  toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, timerProgressBar: true
});

onMounted(() => {
  brandStore.fetchBrands();
});

const handleSearch = () => { brandStore.fetchBrands(searchKeyword.value, 0); };
const changePage = (page: number) => { if (page >= 0 && page < brandStore.totalPages) brandStore.fetchBrands(searchKeyword.value, page); };

// Xử lý chọn File Ảnh
const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return; 

  if (file.size > 5 * 1024 * 1024) {
    Toast.fire({ icon: 'error', title: 'Dung lượng ảnh quá lớn (> 5MB)' });
    target.value = ''; 
    return;
  }
  
  selectedFile.value = file;
  previewImageUrl.value = URL.createObjectURL(file); 
};

const removeImage = () => {
  selectedFile.value = null;
  previewImageUrl.value = null;
  formData.value.logoUrl = null; 
};

// 👇 ĐÃ CẬP NHẬT: Validate Frontend đồng bộ 100% với DTO Backend
const validateForm = () => {
  errors.value = { name: '', description: '' };
  let isValid = true;
  
  const nameValue = formData.value.name.trim();
  const descValue = formData.value.description?.trim() || '';
  
  // Khớp với @Pattern: "^[\\p{L}\\d\\s&'\\.\\-]+$" của Backend (Cho phép L'Oréal, Dr.Brandt...)
  const nameRegex = /^[\p{L}\d\s&'.\-]+$/u; 

  if (!nameValue) {
    errors.value.name = 'Tên thương hiệu không được để trống!';
    isValid = false;
  } else if (nameValue.length > 255) {
    errors.value.name = 'Tên thương hiệu không được vượt quá 255 ký tự!';
    isValid = false;
  } else if (!nameRegex.test(nameValue)) {
    errors.value.name = "Tên thương hiệu chỉ được chứa chữ cái, số, khoảng trắng và các ký tự: &, -, ., '";
    isValid = false;
  }

  if (descValue.length > 1000) {
    errors.value.description = 'Mô tả không được vượt quá 1000 ký tự!';
    isValid = false;
  }

  return isValid;
};

const openAddModal = () => {
  isEdit.value = false;
  formData.value = { name: '', description: '', status: 1, logoUrl: null };
  selectedFile.value = null;
  previewImageUrl.value = null;
  errors.value = { name: '', description: '' }; 
  showModal.value = true;
};

const openEditModal = (brand: Brand) => {
  isEdit.value = true;
  currentId.value = brand.id;
  formData.value = { name: brand.name, description: brand.description, status: brand.status, logoUrl: brand.logoUrl };
  
  selectedFile.value = null; 
  previewImageUrl.value = brand.logoUrl; 
  errors.value = { name: '', description: '' }; 
  showModal.value = true;
};

const handleSubmit = async () => {
  if (!validateForm()) return; 

  try {
    isUploading.value = true;

    // 1. Upload ảnh
    if (selectedFile.value) {
      const uploadRes = await brandService.uploadLogo(selectedFile.value);
      formData.value.logoUrl = uploadRes.data.url; 
    }

    // 2. Gửi dữ liệu đi
    if (isEdit.value && currentId.value) {
      await brandStore.updateBrand(currentId.value, formData.value);
      await brandStore.fetchBrands(searchKeyword.value, brandStore.currentPage); 
      Toast.fire({ icon: 'success', title: 'Cập nhật thành công!' });
    } else {
      await brandStore.createBrand(formData.value);
      searchKeyword.value = '';
      await brandStore.fetchBrands('', 0); 
      Toast.fire({ icon: 'success', title: 'Thêm mới thành công!' });
    }
    showModal.value = false; 
  } catch (error: any) {
    console.error("Lỗi từ backend:", error);
    
    if (error.response && error.response.data) {
      const responseData = error.response.data;

      // Hứng chính xác lỗi validation từ DTO
      if (responseData.errors) {
        if (responseData.errors.name) errors.value.name = responseData.errors.name;
        if (responseData.errors.description) errors.value.description = responseData.errors.description;
        return; 
      }

      // Hứng lỗi trùng tên từ Service
      if (responseData.message) {
        const lowerMsg = responseData.message.toLowerCase();
        if (lowerMsg.includes('tồn tại') || lowerMsg.includes('exists') || lowerMsg.includes('duplicate')) {
          errors.value.name = 'Thương hiệu này đã tồn tại trong hệ thống!';
        } else {
          Toast.fire({ icon: 'error', title: responseData.message });
        }
        return;
      }
    }
    Toast.fire({ icon: 'error', title: 'Máy chủ không phản hồi!' });
  } finally {
    isUploading.value = false;
  }
};

const handleToggleStatus = async (brand: Brand) => {
  const newStatus = brand.status === 1 ? 0 : 1;
  try {
    await brandStore.updateBrand(brand.id, { 
      name: brand.name, 
      description: brand.description, 
      status: newStatus,
      logoUrl: brand.logoUrl 
    });
    await brandStore.fetchBrands(searchKeyword.value, brandStore.currentPage);
    Toast.fire({ icon: 'success', title: 'Đã thay đổi trạng thái!' });
  } catch (error) {
    Toast.fire({ icon: 'error', title: 'Không thể đổi trạng thái!' });
  }
};

const handleDelete = (id: number) => {
  Swal.fire({
    title: 'Bạn có chắc chắn muốn xóa?',
    text: "Thương hiệu sẽ bị chuyển vào thùng rác!",
    icon: 'warning', showCancelButton: true, confirmButtonColor: '#dc3545', cancelButtonColor: '#6c757d',
    confirmButtonText: 'Vâng, xóa nó!', cancelButtonText: 'Hủy'
  }).then(async (result) => {
    if (result.isConfirmed) {
      try {
        const isLastItemOnPage = brandStore.brands && brandStore.brands.length === 1;
        const isNotFirstPage = brandStore.currentPage > 0;

        await brandStore.deleteBrand(id);
        
        if (isLastItemOnPage && isNotFirstPage) {
          await brandStore.fetchBrands(searchKeyword.value, brandStore.currentPage - 1);
        } else {
          await brandStore.fetchBrands(searchKeyword.value, brandStore.currentPage);
        }

        Swal.fire('Đã xóa!', 'Thương hiệu đã bị xóa.', 'success');
      } catch (error: any) {
        Swal.fire('Lỗi!', error.message || 'Không thể xóa thương hiệu này.', 'error');
      }
    }
  });
};
</script>