<template>
  <div class="brand-page">
    <div class="page-header">
      <div>
        <h3 class="page-title">
          <i class="bi bi-star me-2"></i>
          Quản lý thương hiệu
        </h3>
      </div>
      
      <button @click="openAddModal" class="btn btn-primary px-4">
        <i class="bi bi-plus-circle me-2"></i>
        Thêm thương hiệu
      </button>
    </div>
    
    <div class="toolbar">
      <div class="search-box">
        <i class="bi bi-search"></i>
        <input 
          v-model="searchKeyword" 
          @keyup.enter="handleSearch"
          type="text" 
          placeholder="Tìm kiếm thương hiệu..."
        >
      </div>
    </div>
      
    <div v-if="brandStore.isLoading" class="loading-state">
      Đang tải dữ liệu...
    </div>

    <div v-else class="table-wrapper">
      <BrandTable 
        :brands="brandStore.brands" 
        @edit="openEditModal"
        @delete="handleDelete"
        @toggle-status="handleToggleStatus"
      />
    </div>

    <div class="footer" v-if="brandStore.totalPages > 0 && !brandStore.isLoading">
      <div class="text-muted">
        Đang hiển thị trang <b>{{ brandStore.currentPage + 1 }}</b> / <b>{{ brandStore.totalPages }}</b>
      </div>
      <div class="pagination">
        <button 
          class="btn btn-light" 
          :disabled="brandStore.currentPage === 0" 
          @click="changePage(brandStore.currentPage - 1)"
        >
          ←
        </button>
        
        <button 
          v-for="p in brandStore.totalPages" 
          :key="p"
          class="btn"
          :class="brandStore.currentPage === (p - 1) ? 'btn-primary' : 'btn-light'"
          @click="changePage(p - 1)"
        >
          {{ p }}
        </button>
        
        <button 
          class="btn btn-light" 
          :disabled="brandStore.currentPage === brandStore.totalPages - 1" 
          @click="changePage(brandStore.currentPage + 1)"
        >
          →
        </button>
      </div>
    </div>

    <!-- MODAL -->
    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="custom-modal">
        <div class="modal-header">
          <h5 class="modal-title">{{ isEdit ? 'Cập nhật thương hiệu' : 'Thêm thương hiệu mới' }}</h5>
          <button @click="showModal = false" type="button" class="btn-close-modal">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <div class="modal-body">
          
          <div class="mb-4">
            <label class="form-label">Logo thương hiệu</label>
            <div class="d-flex align-items-center gap-3">
              <img v-if="previewImageUrl" :src="getImageUrl(previewImageUrl)" class="upload-preview">
              <div v-else class="upload-placeholder">
                <i class="bi bi-image"></i>
              </div>
              
              <div>
                <input type="file" @change="handleFileChange" class="form-control form-control-sm w-auto" accept="image/jpeg, image/png, image/jpg, image/webp">
                <small class="text-muted d-block mt-1">Hỗ trợ JPG, PNG, WEBP. Tối đa 5MB.</small>
                <button v-if="previewImageUrl" @click="removeImage" type="button" class="btn btn-sm btn-outline-danger mt-2" style="font-size: 12px; padding: 4px 10px;">
                  <i class="bi bi-trash me-1"></i> Xóa ảnh
                </button>
              </div>
            </div>
          </div>

          <div class="mb-3">
            <label class="form-label">Tên thương hiệu <span class="text-danger">*</span></label>
            <input 
              v-model="formData.name" 
              type="text" 
              class="form-control" 
              :class="{ 'is-invalid': errors.name }"
              placeholder="VD: Dior, Chanel..."
              @input="validateForm"
              @keyup.enter="handleSubmit"
            >
            <small v-if="errors.name" class="text-danger mt-2 d-block fw-medium">
              <i class="bi bi-exclamation-circle me-1"></i> {{ errors.name }}
            </small>
          </div>
          
          <div class="mb-3">
            <label class="form-label">Mô tả chi tiết</label>
            <textarea 
              v-model="formData.description" 
              class="form-control" 
              :class="{ 'is-invalid': errors.description }"
              rows="3" 
              placeholder="Giới thiệu thương hiệu..."
              @input="validateForm"
            ></textarea>
            <small v-if="errors.description" class="text-danger mt-2 d-block fw-medium">
              <i class="bi bi-exclamation-circle me-1"></i> {{ errors.description }}
            </small>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="showModal = false" class="btn btn-light px-4" :disabled="isUploading">Hủy</button>
          <button @click="handleSubmit" class="btn btn-primary px-4" :disabled="isUploading">
            <span v-if="isUploading" class="spinner-border spinner-border-sm me-2"></span>
            {{ isUploading ? 'Đang lưu...' : 'Lưu lại' }}
          </button>
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

const API_URL = import.meta.env.VITE_API_URL || "";

const getImageUrl = (url?: string) => {
  if (!url) return "";
  if (url.startsWith("http") || url.startsWith("blob:")) return url;
  return `${API_URL}${url}`;
};

const brandStore = useBrandStore();

const searchKeyword = ref('');
const showModal = ref(false);
const isEdit = ref(false);
const currentId = ref<number | null>(null);

const selectedFile = ref<File | null>(null);
const previewImageUrl = ref<string | null>(null);
const isUploading = ref(false);

const formData = ref<BrandRequest>({ name: '', description: '', status: 1, logoUrl: null });
const errors = ref({ name: '', description: '' });

const Toast = Swal.mixin({
  toast: true, position: 'top-end', showConfirmButton: false, timer: 3000, timerProgressBar: true
});

onMounted(() => {
  brandStore.fetchBrands();
});

const handleSearch = () => { brandStore.fetchBrands(searchKeyword.value, 0); };
const changePage = (page: number) => { if (page >= 0 && page < brandStore.totalPages) brandStore.fetchBrands(searchKeyword.value, page); };

const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  
  if (!file) return; 

  const allowedTypes = ['image/jpeg', 'image/png', 'image/jpg', 'image/webp'];
  if (!allowedTypes.includes(file.type)) {
    Toast.fire({ icon: 'error', title: 'Chỉ hỗ trợ tải lên file ảnh (JPG, PNG, WEBP)!' });
    target.value = ''; 
    return;
  }

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

const validateForm = () => {
  errors.value = { name: '', description: '' };
  let isValid = true;
  
  // Chuẩn hóa khoảng trắng 
  const nameValue = formData.value.name.trim().replace(/\s+/g, ' ');
  const descValue = formData.value.description?.trim() || '';
  
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

  // Thêm kiểm tra trùng lặp trên Frontend (Local validation)
  if (isValid) {
    const isDuplicate = brandStore.brands.some((brand) => {
      if (isEdit.value && brand.id === currentId.value) return false;
      return brand.name.trim().replace(/\s+/g, ' ').toLowerCase() === nameValue.toLowerCase();
    });

    if (isDuplicate) {
      errors.value.name = 'Thương hiệu này đã tồn tại trong hệ thống!';
      isValid = false;
    }
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

    if (selectedFile.value) {
      const uploadRes = await brandService.uploadLogo(selectedFile.value);
      formData.value.logoUrl = uploadRes.data.url; 
    }

    // Ép chuẩn hóa dữ liệu gửi lên Backend
    const payload = {
        ...formData.value,
        name: formData.value.name.trim().replace(/\s+/g, ' ')
    };

    if (isEdit.value && currentId.value) {
      await brandStore.updateBrand(currentId.value, payload);
      await brandStore.fetchBrands(searchKeyword.value, brandStore.currentPage); 
      Toast.fire({ icon: 'success', title: 'Cập nhật thành công!' });
    } else {
      await brandStore.createBrand(payload);
      searchKeyword.value = '';
      await brandStore.fetchBrands('', 0); 
      Toast.fire({ icon: 'success', title: 'Thêm mới thành công!' });
    }
    showModal.value = false; 
  } catch (error: any) {
    console.error("Lỗi từ backend:", error);
    
    // Tối ưu bộ bắt lỗi tương tự Category
    let errorMsg = '';
    
    if (error.response && error.response.data) {
      const responseData = error.response.data;

      if (responseData.errors) {
        if (responseData.errors.name) errors.value.name = responseData.errors.name;
        if (responseData.errors.description) errors.value.description = responseData.errors.description;
        return; 
      }

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
      if (lowerMsg.includes('tồn tại') || lowerMsg.includes('exists') || lowerMsg.includes('duplicate') || lowerMsg.includes('trùng')) {
        errors.value.name = 'Thương hiệu này đã tồn tại trong hệ thống!';
      } else {
        Toast.fire({ icon: 'error', title: errorMsg });
      }
    } else {
      Toast.fire({ icon: 'error', title: 'Máy chủ không phản hồi!' });
    }
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
    icon: 'warning', showCancelButton: true, confirmButtonColor: '#dc2626', cancelButtonColor: '#94a3b8',
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

<style scoped>
/* Layout Component */
.brand-page {
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
  max-width: 550px;
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

textarea.form-control {
  min-height: 100px;
  resize: none;
}

.modal-footer {
  padding: 16px 24px;
  background: #f8fafc;
  border-top: 1px solid #eef2f7;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* Upload Image Area */
.upload-preview {
  width: 75px;
  height: 75px;
  border-radius: 14px;
  object-fit: contain;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
}

.upload-placeholder {
  width: 75px;
  height: 75px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  color: #94a3b8;
  font-size: 24px;
  border: 1px solid #e2e8f0;
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