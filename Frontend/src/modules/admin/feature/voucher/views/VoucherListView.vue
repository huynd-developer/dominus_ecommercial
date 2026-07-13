<template>
  <div class="admin-voucher-page">
    <!-- Header -->
    <div class="d-flex justify-content-between align-items-center mb-4">
      <div>
        <h3 class="fw-bold mb-1 text-dark">Quản lý Mã giảm giá (Voucher)</h3>
        <p class="text-muted small mb-0">CRUD mã giảm giá, cấu hình phần trăm giảm giá và thời hạn.</p>
      </div>
      <button @click="openCreateModal" class="btn btn-dark fw-bold px-4 py-2 custom-btn-add">
        + Tạo voucher
      </button>
    </div>

    <!-- Filter & Search Bar -->
    <div class="card border-0 shadow-sm mb-4">
      <div class="card-body p-3">
        <div class="row g-3">
          <div class="col-md-5">
            <input 
              type="text" 
              class="form-control form-control-lg bg-light" 
              placeholder="Tìm theo mã voucher..." 
              v-model="searchKeyword"
              @keyup.enter="handleSearch"
            />
          </div>
          <div class="col-md-4">
            <select class="form-select form-select-lg bg-light" v-model="filterStatus" @change="handleSearch">
              <option value="">Tất cả trạng thái</option>
              <option :value="1">Đang bật</option>
              <option :value="0">Tạm dừng</option>
            </select>
          </div>
          <div class="col-md-3">
            <button @click="handleSearch" class="btn btn-outline-dark btn-lg w-100">
              Tìm kiếm
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Table Danh Sách -->
    <div class="card border-0 shadow-sm">
      <div class="card-body p-0">
        <div v-if="isLoading" class="text-center py-5">
          <div class="spinner-border text-primary" role="status"></div>
        </div>

        <div class="table-responsive" v-else>
          <table class="table table-hover align-middle mb-0">
            <thead class="table-light">
              <tr>
                <th class="ps-4">Mã Voucher</th>
                <th>Loại - Mức giảm</th>
                <th>Đơn tối thiểu</th>
                <th>Đã dùng / Giới hạn</th>
                <th>Thời hạn</th>
                <th>Trạng thái</th>
                <th class="text-center">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="v in vouchers" :key="v.id">
                <td class="ps-4 fw-bold text-success">{{ v.code }}</td>
                <td>
                  <span class="fw-bold">{{ formatMoney(v.discountValue) }}</span>
                  <span class="badge bg-secondary ms-1">{{ v.discountType === 'PERCENT' ? '%' : 'VNĐ' }}</span>
                </td>
                <td>{{ formatMoney(v.minOrderValue) }}</td>
                <td>{{ v.usedCount }} / {{ v.usageLimit }}</td>
                <td class="small text-muted">
                  Bắt đầu: {{ formatDate(v.startDate) }}<br>
                  Kết thúc: <span :class="{'text-danger fw-bold': isExpired(v.endDate)}">{{ formatDate(v.endDate) }}</span>
                </td>
                <td>
                  <span class="badge" :class="v.status === 1 ? 'bg-success' : 'bg-secondary'">
                    {{ v.status === 1 ? 'Hoạt động' : 'Tạm dừng' }}
                  </span>
                </td>
                <td class="text-center">
                  <div class="d-flex gap-2 justify-content-center">
                    <button @click="openEditModal(v)" class="btn btn-sm btn-outline-secondary">Xem/Sửa</button>
                    <button @click="handleDelete(v.id)" class="btn btn-sm btn-outline-danger">Xóa</button>
                  </div>
                </td>
              </tr>
              <tr v-if="vouchers.length === 0">
                <td colspan="7" class="text-center py-5 text-muted">Không tìm thấy mã giảm giá nào.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Pagination -->
      <div class="card-footer bg-white border-0 py-3" v-if="totalPages > 1">
        <ul class="pagination justify-content-center mb-0">
          <li class="page-item" :class="{ disabled: currentPage === 0 }">
            <button class="page-link" @click="changePage(currentPage - 1)">Trước</button>
          </li>
          <li class="page-item" v-for="page in totalPages" :key="page" :class="{ active: currentPage === page - 1 }">
            <button class="page-link" @click="changePage(page - 1)">{{ page }}</button>
          </li>
          <li class="page-item" :class="{ disabled: currentPage === totalPages - 1 }">
            <button class="page-link" @click="changePage(currentPage + 1)">Sau</button>
          </li>
        </ul>
      </div>
    </div>

    <!-- ============================================== -->
    <!-- MODAL (POPUP) THÊM/SỬA VOUCHER                 -->
    <!-- ============================================== -->
    <Teleport to="body">
      <div v-if="showModal" class="custom-modal-overlay" @click.self="closeModal">
        <div class="custom-modal-content">
          <div class="modal-header">
            <h4 class="fw-bold mb-0">{{ isEditing ? 'Cập nhật Voucher' : 'Tạo Voucher' }}</h4>
            <button type="button" class="btn-close" @click="closeModal"></button>
          </div>
          
          <div class="modal-body p-4">
            <p class="text-muted mb-4 small">Cấu hình phần trăm giảm giá, đơn tối thiểu và giới hạn sử dụng cho mã giảm giá.</p>
            
            <form @submit.prevent="handleSubmit">
              <div class="row g-4">
                <div class="col-md-12">
                  <label class="form-label fw-bold">Mã Voucher <span class="text-danger">*</span></label>
                  <input type="text" class="form-control form-control-lg text-uppercase" v-model="form.code" required placeholder="VD: SUMMER24" />
                </div>
                
                <div class="col-md-6">
                  <label class="form-label fw-bold">Kiểu giảm <span class="text-danger">*</span></label>
                  <select class="form-select form-select-lg" v-model="form.discountType">
                    <option value="PERCENT">Phần trăm (%)</option>
                    <option value="FIXED">Cố định (VNĐ)</option>
                  </select>
                </div>
                <div class="col-md-6">
                  <label class="form-label fw-bold">Mức giảm <span class="text-danger">*</span></label>
                  <input type="number" class="form-control form-control-lg" v-model="form.discountValue" required min="0" />
                </div>

                <div class="col-md-6">
                  <label class="form-label fw-bold">Đơn tối thiểu (VNĐ)</label>
                  <input type="number" class="form-control form-control-lg" v-model="form.minOrderValue" min="0" />
                </div>
                <div class="col-md-6">
                  <label class="form-label fw-bold">Mức giảm tối đa (VNĐ)</label>
                  <input type="number" class="form-control form-control-lg" v-model="form.maxDiscount" min="0" :disabled="form.discountType === 'FIXED'" />
                </div>

                <div class="col-md-6">
                  <label class="form-label fw-bold">Ngày bắt đầu <span class="text-danger">*</span></label>
                  <input type="datetime-local" class="form-control form-control-lg" v-model="form.startDate" @change="validateDates" required />
                </div>
                <div class="col-md-6">
                  <label class="form-label fw-bold">Ngày kết thúc <span class="text-danger">*</span></label>
                  <input type="datetime-local" class="form-control form-control-lg" :class="{'is-invalid': dateError}" v-model="form.endDate" @change="validateDates" required />
                  <div v-if="dateError" class="invalid-feedback fw-bold">{{ dateError }}</div>
                </div>

                <div class="col-md-6">
                  <label class="form-label fw-bold">Giới hạn số lượt dùng <span class="text-danger">*</span></label>
                  <input type="number" class="form-control form-control-lg" v-model="form.usageLimit" required min="1" />
                </div>
                <div class="col-md-6">
                  <label class="form-label fw-bold">Trạng thái</label>
                  <select class="form-select form-select-lg" v-model="form.status">
                    <option :value="1">Hoạt động</option>
                    <option :value="0">Tạm dừng</option>
                  </select>
                </div>
              </div>

              <div class="modal-footer mt-4 pt-4 border-top">
                <button type="button" class="btn btn-light btn-lg px-4" @click="closeModal">Hủy</button>
                <button type="submit" class="btn btn-dark btn-lg px-5" :disabled="!!dateError || isSaving">
                  <span v-if="isSaving" class="spinner-border spinner-border-sm me-2"></span>
                  {{ isEditing ? 'Lưu thay đổi' : 'Tạo Voucher' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from 'axios';
import Swal from 'sweetalert2';

// State bảng danh sách & phân trang
const vouchers = ref<any[]>([]);
const isLoading = ref(false);
const currentPage = ref(0);
const totalPages = ref(0);
const searchKeyword = ref('');
const filterStatus = ref<number | ''>('');

// State của Modal
const showModal = ref(false);
const isEditing = ref(false);
const isSaving = ref(false);
const editId = ref<number | null>(null);
const dateError = ref('');

const initialForm = {
  code: '', discountType: 'PERCENT', discountValue: 0, minOrderValue: 0,
  maxDiscount: 0, usageLimit: 1, startDate: '', endDate: '', status: 1
};
const form = ref({ ...initialForm });

// Lấy danh sách Voucher (có Phân trang & Filter)
const fetchVouchers = async () => {
  isLoading.value = true;
  try {
    const token = localStorage.getItem('token');
    const res = await axios.get('http://localhost:8080/api/admin/vouchers', {
      params: { 
        page: currentPage.value, 
        size: 10,
        keyword: searchKeyword.value || null,
        status: filterStatus.value === '' ? null : filterStatus.value
      },
      headers: { Authorization: `Bearer ${token}` }
    });
    
    // Vì backend trả về Page object nên dữ liệu thực nằm trong content
    vouchers.value = res.data.content || [];
    totalPages.value = res.data.totalPages || 0;
  } catch (error) {
    console.error("Lỗi lấy danh sách Voucher:", error);
  } finally {
    isLoading.value = false;
  }
};

// Xử lý Phân trang & Tìm kiếm
const changePage = (page: number) => {
  if (page >= 0 && page < totalPages.value) {
    currentPage.value = page;
    fetchVouchers();
  }
};
const handleSearch = () => {
  currentPage.value = 0; // Reset về trang 1 khi search
  fetchVouchers();
};

// ================= CÁC HÀM XỬ LÝ MODAL =================
const openCreateModal = () => {
  isEditing.value = false;
  editId.value = null;
  form.value = { ...initialForm };
  dateError.value = '';
  showModal.value = true;
};

const openEditModal = (voucher: any) => {
  isEditing.value = true;
  editId.value = voucher.id;
  
  // Format lại ngày để nhét vừa thẻ input datetime-local
  const sDate = voucher.startDate ? voucher.startDate.substring(0, 16) : '';
  const eDate = voucher.endDate ? voucher.endDate.substring(0, 16) : '';
  
  form.value = { ...voucher, startDate: sDate, endDate: eDate };
  dateError.value = '';
  showModal.value = true;
};

const closeModal = () => {
  showModal.value = false;
};

const validateDates = () => {
  if (form.value.startDate && form.value.endDate) {
    if (new Date(form.value.startDate) > new Date(form.value.endDate)) {
      dateError.value = 'Ngày bắt đầu không được lớn hơn ngày kết thúc!';
      return false;
    }
  }
  dateError.value = ''; return true;
};

const handleSubmit = async () => {
  if (!validateDates()) return;
  isSaving.value = true;
  try {
    const token = localStorage.getItem('token');
    if (isEditing.value) {
      await axios.put(`http://localhost:8080/api/admin/vouchers/${editId.value}`, form.value, {
        headers: { Authorization: `Bearer ${token}` }
      });
      Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Cập nhật thành công', showConfirmButton: false, timer: 1500 });
    } else {
      await axios.post('http://localhost:8080/api/admin/vouchers', form.value, {
        headers: { Authorization: `Bearer ${token}` }
      });
      Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Thêm mới thành công', showConfirmButton: false, timer: 1500 });
    }
    closeModal();
    fetchVouchers(); // Refresh bảng
  } catch (error: any) {
    Swal.fire('Lỗi', error.response?.data || 'Có lỗi xảy ra!', 'error');
  } finally {
    isSaving.value = false;
  }
};

// Xóa Voucher
const handleDelete = async (id: number) => {
  const result = await Swal.fire({
    title: 'Xác nhận xóa?',
    text: "Voucher này sẽ bị ẩn khỏi hệ thống!",
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#dc3545',
    cancelButtonColor: '#6c757d',
    confirmButtonText: 'Xóa ngay!',
    cancelButtonText: 'Hủy'
  });

  if (result.isConfirmed) {
    try {
      const token = localStorage.getItem('token');
      await axios.delete(`http://localhost:8080/api/admin/vouchers/${id}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Đã xóa voucher', showConfirmButton: false, timer: 1500 });
      fetchVouchers();
    } catch (error) {
      Swal.fire('Lỗi', 'Không thể xóa voucher này', 'error');
    }
  }
};

const formatMoney = (val: number) => new Intl.NumberFormat('vi-VN').format(val || 0);
const formatDate = (dateStr: string) => {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' });
};
const isExpired = (endDate: string) => new Date(endDate) < new Date();

onMounted(fetchVouchers);
</script>

<style scoped>
.admin-voucher-page {
  padding: 24px;
  background-color: #f8f9fa;
  min-height: 100vh;
}

.custom-btn-add {
  background-color: #1a1a1a;
  border-radius: 8px;
}

/* Modal CSS */
.custom-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1050;
  backdrop-filter: blur(4px);
}

.custom-modal-content {
  background-color: white;
  width: 90%;
  max-width: 800px;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.2);
  display: flex;
  flex-direction: column;
  max-height: 90vh;
}

.modal-header {
  padding: 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-body {
  overflow-y: auto;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>