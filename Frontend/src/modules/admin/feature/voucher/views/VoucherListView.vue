<template>
  <div class="p-4 min-vh-100" style="background-color: #f8f9fa;">
    <!-- Header -->
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h4 class="fw-bold mb-0 d-flex align-items-center gap-2">
        <i class="bi bi-ticket-perforated"></i> Quản lý Mã giảm giá (Voucher)
      </h4>
      <button @click="openCreateModal" class="btn btn-primary rounded-pill px-4 py-2 shadow-sm d-flex align-items-center gap-2">
        <i class="bi bi-plus-circle"></i> Thêm voucher
      </button>
    </div>

    <!-- Bọc toàn bộ vào Card trắng bo góc như trang Sản phẩm -->
    <div class="card border-0 shadow-sm rounded-4">
      <div class="card-body p-4">
        
        <!-- Filter & Search Bar kiểu mới -->
        <div class="d-flex justify-content-between align-items-center mb-4 gap-3 flex-wrap">
          <div class="position-relative flex-grow-1" style="max-width: 400px;">
            <i class="bi bi-search position-absolute top-50 start-0 translate-middle-y ms-3 text-muted"></i>
            <input 
              type="text" 
              class="form-control rounded-pill ps-5 bg-light border-0" 
              placeholder="Tìm theo mã voucher..." 
              v-model="searchKeyword"
              @keyup.enter="handleSearch"
            />
          </div>
          <div class="d-flex align-items-center gap-2">
            <select class="form-select rounded-pill bg-light border-0 px-4" style="width: 200px;" v-model="filterStatus" @change="handleSearch">
              <option value="">Tất cả trạng thái</option>
              <option :value="1">Đang hoạt động</option>
              <option :value="0">Tạm dừng</option>
            </select>
            <button class="btn btn-light rounded-circle shadow-sm text-muted" @click="handleSearch" title="Làm mới">
              <i class="bi bi-arrow-clockwise"></i>
            </button>
          </div>
        </div>

        <!-- Table Danh Sách -->
        <div v-if="isLoading" class="text-center py-5">
          <div class="spinner-border text-primary" role="status"></div>
        </div>

        <div class="table-responsive" v-else>
          <table class="table align-middle table-borderless table-hover custom-table">
            <thead class="text-muted border-bottom">
              <tr>
                <th class="ps-3 fw-medium">Mã Voucher</th>
                <th class="fw-medium">Mức giảm</th>
                <th class="fw-medium">Đơn tối thiểu</th>
                <th class="fw-medium">Giới hạn</th>
                <th class="fw-medium">Thời hạn</th>
                <th class="fw-medium text-center">Trạng thái</th>
                <th class="fw-medium text-end pe-3">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="vouchers.length === 0">
                <td colspan="7" class="text-center py-5 text-muted">Không tìm thấy mã giảm giá nào.</td>
              </tr>

              <template v-else>
                <tr v-for="v in vouchers" :key="v.id" class="border-bottom">
                  <td class="ps-3">
                    <span class="fw-bold text-dark">{{ v.code }}</span>
                  </td>
                  <td>
                    <span class="fw-bold text-dark">{{ formatMoney(v.discountValue) }}</span>
                    <span class="text-muted ms-1 small">{{ v.discountType === 'PERCENT' ? '%' : 'VNĐ' }}</span>
                  </td>
                  <td class="text-muted">{{ formatMoney(v.minOrderValue) }}</td>
                  <td>
                    <span class="badge bg-light text-dark border">{{ v.usedCount || 0 }} / {{ v.usageLimit }}</span>
                  </td>
                  <td class="small text-muted">
                    {{ formatDate(v.startDate) }} <br>
                    <span :class="{'text-danger fw-bold': isExpired(v.endDate)}">{{ formatDate(v.endDate) }}</span>
                  </td>
                  <td class="text-center">
                    <span v-if="isExpired(v.endDate)" class="badge bg-secondary-subtle text-secondary rounded-pill px-3 py-2">
                      Đã hết hạn
                    </span>
                    <span v-else-if="v.status === 0" class="badge bg-warning-subtle text-warning rounded-pill px-3 py-2">
                      Tạm dừng
                    </span>
                    <span v-else-if="isNotStarted(v.startDate)" class="badge bg-info-subtle text-info rounded-pill px-3 py-2">
                      Chưa bắt đầu
                    </span>
                    <span v-else class="badge bg-success-subtle text-success rounded-pill px-3 py-2">
                      Đang hoạt động
                    </span>
                  </td>
                  <td class="text-end pe-3">
                    <div class="d-flex gap-1 justify-content-end">
                      <button @click="openEditModal(v)" class="btn btn-sm btn-light text-primary rounded-circle action-btn" title="Sửa">
                        <i class="bi bi-pencil-square"></i>
                      </button>

                      <button
                        v-if="v.status === 1 && !isExpired(v.endDate)"
                        class="btn btn-sm btn-light text-warning rounded-circle action-btn"
                        @click="changeStatus(v, 0)" title="Tạm dừng"
                      >
                        <i class="bi bi-pause-circle"></i>
                      </button>

                      <button
                        v-else-if="v.status === 0 && !isExpired(v.endDate)"
                        class="btn btn-sm btn-light text-success rounded-circle action-btn"
                        @click="changeStatus(v, 1)" title="Kích hoạt"
                      >
                        <i class="bi bi-play-circle"></i>
                      </button>

                      <button @click="handleDelete(v)" class="btn btn-sm btn-light text-danger rounded-circle action-btn" title="Xóa">
                        <i class="bi bi-trash3"></i>
                      </button>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>

        <!-- Pagination kiểu mới -->
        <div class="d-flex justify-content-between align-items-center mt-4 text-muted small" v-if="totalPages > 0">
          <div>
            Hiển thị trang {{ currentPage + 1 }} / {{ totalPages }}
          </div>
          <div class="d-flex gap-2 align-items-center">
            <button class="btn btn-sm btn-light rounded-circle" :disabled="currentPage === 0" @click="changePage(currentPage - 1)">
              <i class="bi bi-arrow-left"></i>
            </button>
            <span class="mx-2">{{ currentPage + 1 }} / {{ totalPages }}</span>
            <button class="btn btn-sm btn-light rounded-circle" :disabled="currentPage === totalPages - 1" @click="changePage(currentPage + 1)">
              <i class="bi bi-arrow-right"></i>
            </button>
          </div>
        </div>

      </div>
    </div>

    <!-- ============================================== -->
    <!-- MODAL (POPUP) THÊM/SỬA VOUCHER GIỮ NGUYÊN CODE -->
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
                  <div class="input-group">
                    <input 
                      type="text" 
                      class="form-control form-control-lg text-uppercase" 
                      v-model="form.code" 
                      @input="handleCodeInput"
                      maxlength="20" 
                      placeholder="Vui lòng nhập hoặc chọn mã"
                      required
                    />
                    <button class="btn btn-outline-secondary px-4 fw-bold" type="button" @click="generateRandomCode">
                      <i class="bi bi-magic me-1"></i> Tạo ngẫu nhiên
                    </button>
                  </div>
                  <small class="text-muted">Chỉ chứa ký tự chữ cái và số, tối đa 20 ký tự.</small>
                </div>
                
                <div class="col-md-6">
                  <label class="form-label fw-bold">Kiểu giảm <span class="text-danger">*</span></label>
                  <select class="form-select form-select-lg" v-model="form.discountType" @change="resetDiscountValue">
                    <option value="PERCENT">Phần trăm (%)</option>
                    <option value="FIXED">Cố định (VNĐ)</option>
                  </select>
                </div>

                <div class="col-md-6">
                  <label class="form-label fw-bold">Mức giảm <span class="text-danger">*</span></label>
                  <div class="input-group">
                    <input 
                      v-if="form.discountType === 'FIXED'"
                      type="text" 
                      class="form-control form-control-lg" 
                      :value="formatNumber(form.discountValue)" 
                      @input="handleCurrencyInput('discountValue', $event)" 
                      required
                    />
                    <input 
                      v-else
                      type="text" 
                      class="form-control form-control-lg" 
                      :value="form.discountValue" 
                      @input="handlePercentInput" 
                      required
                    />
                    <span class="input-group-text">{{ form.discountType === 'PERCENT' ? '%' : 'VNĐ' }}</span>
                  </div>
                </div>

                <div class="col-md-6">
                  <label class="form-label fw-bold">Giá tối thiểu (VNĐ) <span class="text-danger">*</span></label>
                  <input 
                    type="text" 
                    class="form-control form-control-lg" 
                    :value="formatNumber(form.minOrderValue)" 
                    @input="handleCurrencyInput('minOrderValue', $event)" 
                    required 
                  />
                </div>

                <div class="col-md-6">
                  <label class="form-label fw-bold">Mức giảm tối đa (VNĐ) <span class="text-danger">*</span></label>
                  <input 
                    type="text" 
                    class="form-control form-control-lg" 
                    :value="formatNumber(form.maxDiscount)" 
                    @input="handleCurrencyInput('maxDiscount', $event)" 
                    :disabled="form.discountType === 'FIXED'" 
                  />
                  <small class="text-muted d-block mt-1" v-if="form.discountType === 'FIXED'">Không áp dụng cho giảm tiền mặt</small>
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
                  <input 
                    type="text" 
                    class="form-control form-control-lg" 
                    :value="form.usageLimit" 
                    @input="handleUsageLimitInput"
                    required 
                  />
                  <small class="text-muted">Tối đa 1.000 lượt.</small>
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
                <button type="submit" class="btn btn-primary btn-lg px-5 rounded-pill" :disabled="!!dateError || isSaving">
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
import { ref, onMounted, onBeforeUnmount } from 'vue';
import axios from 'axios';
import Swal from 'sweetalert2';

// State bảng danh sách & phân trang
const vouchers = ref<any[]>([]);
const isLoading = ref(false);
const currentPage = ref(0);
const totalPages = ref(0);
const searchKeyword = ref('');
const filterStatus = ref<number | ''>('');
const isWindowRefreshing = ref(false);

// State của Modal
const showModal = ref(false);
const isEditing = ref(false);
const isSaving = ref(false);
const editId = ref<number | null>(null);
const dateError = ref('');

const initialForm = {
  code: '',
  discountType: 'PERCENT',
  discountValue: 0,
  minOrderValue: 0,
  maxDiscount: 0,
  usageLimit: 1,
  startDate: '',
  endDate: '',
  status: 1,
  // Revision chỉ dùng khi update để BE phát hiện stale edit.
  expectedRevision: null as string | null
};
const form = ref({ ...initialForm });

/*
 * Không tự đổi status theo endDate ở FE.
 * BE hiện quy ước status = 0 là Admin chủ động tạm dừng (hoặc scheduler auto-end),
 * status = 1 là Admin cho phép Voucher hoạt động; thời gian hiệu lực được BE kiểm tra
 * riêng bằng startDate <= now < endDate.
 * Nếu FE tự set lại status = 1 khi sửa ngày, Voucher Admin vừa pause có thể bị bật lại.
 */

const getErrorMessage = (error: any, fallback = 'Có lỗi xảy ra!') => {
  const data = error?.response?.data;

  if (!data) return fallback;
  if (typeof data === 'string') return data;
  if (data.message) return String(data.message);

  if (data.errors && typeof data.errors === 'object') {
    const firstError = Object.values(data.errors)[0];
    if (firstError) return String(firstError);
  }

  return fallback;
};

const toDateTimeLocal = (value: any) => {
  if (!value) return '';
  return String(value).substring(0, 16);
};

const buildVoucherPayload = (
  source: any,
  options?: { status?: number; expectedRevision?: string | null }
) => {
  const discountType = String(source?.discountType || 'PERCENT').trim().toUpperCase();

  const payload: any = {
    code: String(source?.code || '').trim().toUpperCase(),
    discountType,
    discountValue: Number(source?.discountValue || 0),
    minOrderValue: Number(source?.minOrderValue || 0),
    maxDiscount: discountType === 'FIXED' ? 0 : Number(source?.maxDiscount || 0),
    usageLimit: Number(source?.usageLimit || 0),
    startDate: toDateTimeLocal(source?.startDate),
    endDate: toDateTimeLocal(source?.endDate),
    status: options?.status ?? Number(source?.status ?? 1)
  };

  if (options?.expectedRevision) {
    payload.expectedRevision = options.expectedRevision;
  }

  return payload;
};

const handleCodeInput = (event: Event) => {
  const target = event.target as HTMLInputElement;
  let rawVal = target.value.replace(/[^a-zA-Z0-9]/g, '').toUpperCase().slice(0, 20);
  form.value.code = rawVal;
  target.value = rawVal;
};

const formatNumber = (value: any) => {
  if (value === null || value === undefined || value === '') return '';
  return String(value).replace(/\D/g, '').replace(/\B(?=(\d{3})+(?!\d))/g, '.');
};

const resetDiscountValue = () => {
  form.value.discountValue = 0;
  if (form.value.discountType === 'FIXED') {
    form.value.maxDiscount = 0;
  }
};

const handleCurrencyInput = (field: string, event: Event) => {
  const target = event.target as HTMLInputElement;
  let rawValue = target.value.replace(/\D/g, '');
  let numValue = Number(rawValue);

  if (numValue > 100000000) {
    Swal.fire({
      toast: true, position: 'top-end', icon: 'warning',
      title: 'Số tiền tối đa là 100.000.000 VNĐ',
      showConfirmButton: false, timer: 2000
    });
    numValue = 100000000;
  }

  (form.value as any)[field] = numValue;
  target.value = formatNumber(numValue);
};

const handlePercentInput = (event: Event) => {
  const target = event.target as HTMLInputElement;
  let rawValue = target.value.replace(/\D/g, '');
  let numValue = Number(rawValue);

  if (numValue > 100) {
    Swal.fire({
      toast: true, position: 'top-end', icon: 'warning',
      title: 'Mức giảm phần trăm không vượt quá 100%',
      showConfirmButton: false, timer: 2000
    });
    numValue = 100;
  }

  form.value.discountValue = numValue;
  target.value = String(numValue);
};

const handleUsageLimitInput = (event: Event) => {
  const target = event.target as HTMLInputElement;
  let rawValue = target.value.replace(/\D/g, '');
  let numValue = Number(rawValue);

  if (numValue > 1000) {
    Swal.fire({
      toast: true, position: 'top-end', icon: 'warning',
      title: 'Giới hạn tối đa là 1.000 lượt',
      showConfirmButton: false, timer: 20000
    });
    numValue = 1000;
  }

  form.value.usageLimit = numValue;
  target.value = String(numValue);
};

const generateRandomCode = () => {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  let code = 'SALE';
  for (let i = 0; i < 6; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  form.value.code = code;
};

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

    vouchers.value = res.data.content || [];
    totalPages.value = res.data.totalPages || 0;
  } catch (error) {
    console.error('Lỗi lấy danh sách Voucher:', error);
  } finally {
    isLoading.value = false;
  }
};

const fetchVoucherDetail = async (id: number) => {
  const token = localStorage.getItem('token');
  const res = await axios.get(`http://localhost:8080/api/admin/vouchers/${id}`, {
    headers: { Authorization: `Bearer ${token}` }
  });
  return res.data;
};

const fillEditForm = (voucher: any) => {
  form.value = {
    code: String(voucher?.code || '').trim().toUpperCase(),
    discountType: String(voucher?.discountType || 'PERCENT').trim().toUpperCase(),
    discountValue: Number(voucher?.discountValue || 0),
    minOrderValue: Number(voucher?.minOrderValue || 0),
    maxDiscount: Number(voucher?.maxDiscount || 0),
    usageLimit: Number(voucher?.usageLimit || 1),
    startDate: toDateTimeLocal(voucher?.startDate),
    endDate: toDateTimeLocal(voucher?.endDate),
    status: Number(voucher?.status ?? 1),
    expectedRevision: voucher?.revision ? String(voucher.revision) : null
  };
};

const resolveExpectedRevision = (voucher: any) => {
  const rowRevision = voucher?.revision ? String(voucher.revision).trim() : '';
  return rowRevision || null;
};

const changePage = (page: number) => {
  if (page >= 0 && page < totalPages.value) {
    currentPage.value = page;
    fetchVouchers();
  }
};

const handleSearch = () => {
  currentPage.value = 0;
  fetchVouchers();
};

const openCreateModal = () => {
  isEditing.value = false;
  editId.value = null;
  form.value = { ...initialForm };
  dateError.value = '';
  showModal.value = true;
};

const openEditModal = async (voucher: any) => {
  if (!voucher?.id) return;

  try {
    // Luôn GET detail mới nhất khi mở form để lấy đúng revision hiện tại từ BE.
    const latest = await fetchVoucherDetail(voucher.id);

    isEditing.value = true;
    editId.value = voucher.id;
    fillEditForm(latest);
    dateError.value = '';
    showModal.value = true;
  } catch (error: any) {
    await Swal.fire('Lỗi', getErrorMessage(error, 'Không thể tải chi tiết Voucher.'), 'error');
    await fetchVouchers();
  }
};

const closeModal = () => {
  showModal.value = false;
};

const validateDates = () => {
  if (form.value.startDate && form.value.endDate) {
    if (new Date(form.value.startDate) >= new Date(form.value.endDate)) {
      dateError.value = 'Ngày kết thúc phải sau ngày bắt đầu!';
      return false;
    }
  }
  dateError.value = '';
  return true;
};

const reloadEditingVoucherAfterConflict = async () => {
  if (!editId.value) return;

  const latest = await fetchVoucherDetail(editId.value);
  fillEditForm(latest);
  validateDates();
};

const handleSubmit = async () => {
  if (
    !String(form.value.code || '').trim()
    || !form.value.discountType
    || !form.value.startDate
    || !form.value.endDate
    || !form.value.usageLimit
  ) {
    Swal.fire('Lỗi', 'Vui lòng nhập đầy đủ các trường bắt buộc có dấu (*)!', 'error');
    return;
  }

  if (form.value.discountValue <= 0) {
    Swal.fire('Lỗi', 'Mức giảm giá phải lớn hơn 0!', 'error');
    return;
  }

  if (!validateDates()) return;

  isSaving.value = true;
  try {
    const token = localStorage.getItem('token');

    if (isEditing.value) {
      if (!editId.value || !form.value.expectedRevision) {
        await fetchVouchers();
        await Swal.fire(
          'Lỗi',
          'Không lấy được phiên bản Voucher hiện tại. Danh sách đã được tải lại, vui lòng đóng form và mở lại.',
          'error'
        );
        return;
      }

      const payload = buildVoucherPayload(form.value, {
        expectedRevision: form.value.expectedRevision
      });

      await axios.put(
        `http://localhost:8080/api/admin/vouchers/${editId.value}`,
        payload,
        { headers: { Authorization: `Bearer ${token}` } }
      );

      Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Cập nhật thành công', showConfirmButton: false, timer: 1500 });
    } else {
      const payload = buildVoucherPayload(form.value);

      await axios.post('http://localhost:8080/api/admin/vouchers', payload, {
        headers: { Authorization: `Bearer ${token}` }
      });

      Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Thêm mới thành công', showConfirmButton: false, timer: 1500 });
    }

    closeModal();
    await fetchVouchers();
  } catch (error: any) {
    if (error?.response?.status === 409 && isEditing.value && editId.value) {
      try {
        // Không retry mutation. Nạp dữ liệu BE mới nhất vào modal để Admin xem lại.
        await reloadEditingVoucherAfterConflict();
        await fetchVouchers();
      } catch (reloadError) {
        console.error('Không thể tải lại Voucher sau conflict:', reloadError);
      }

      await Swal.fire({
        icon: 'warning',
        title: 'Voucher đã thay đổi',
        text: getErrorMessage(error, 'Voucher đã thay đổi bởi thao tác khác. Dữ liệu mới nhất đã được tải lại, vui lòng kiểm tra rồi lưu lại.'),
        confirmButtonText: 'Đã hiểu'
      });
      return;
    }

    Swal.fire('Lỗi', getErrorMessage(error), 'error');
  } finally {
    isSaving.value = false;
  }
};

const handleDelete = async (voucher: any) => {
  if (!voucher?.id) return;

  const result = await Swal.fire({
    title: 'Xác nhận xóa?',
    text: 'Voucher này sẽ bị ẩn khỏi hệ thống!',
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#dc3545',
    cancelButtonColor: '#6c757d',
    confirmButtonText: 'Xóa ngay!',
    cancelButtonText: 'Hủy'
  });

  if (!result.isConfirmed) return;

  try {
    const token = localStorage.getItem('token');
    const expectedRevision = resolveExpectedRevision(voucher);

    if (!expectedRevision) {
      await fetchVouchers();
      await Swal.fire(
        'Lỗi',
        'Không lấy được phiên bản Voucher hiện tại. Danh sách đã được tải lại, vui lòng thử lại.',
        'error'
      );
      return;
    }

    await axios.delete(`http://localhost:8080/api/admin/vouchers/${voucher.id}`, {
      params: { expectedRevision },
      headers: { Authorization: `Bearer ${token}` }
    });

    Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Đã xóa voucher', showConfirmButton: false, timer: 1500 });
    await fetchVouchers();
  } catch (error: any) {
    if (error?.response?.status === 409) {
      await fetchVouchers();
      await Swal.fire({
        icon: 'warning',
        title: 'Voucher đã thay đổi',
        text: getErrorMessage(error, 'Voucher đã thay đổi bởi thao tác khác. Danh sách mới nhất đã được tải lại, vui lòng kiểm tra rồi xóa lại.'),
        confirmButtonText: 'Đã hiểu'
      });
      return;
    }

    Swal.fire('Lỗi', getErrorMessage(error, 'Không thể xóa voucher này'), 'error');
  }
};

const formatMoney = (val: number) => new Intl.NumberFormat('vi-VN').format(val || 0);
const formatDate = (dateStr: string) => {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' });
};
const isExpired = (endDate: string) => new Date(endDate).getTime() <= Date.now();
const isNotStarted = (startDate: string) => new Date(startDate).getTime() > Date.now();

const changeStatus = async (voucher: any, newStatus: number) => {
  if (isExpired(voucher.endDate)) {
    Swal.fire({
      icon: 'warning',
      title: 'Không thể đổi trạng thái',
      text: 'Voucher này đã hết hạn sử dụng.',
      confirmButtonColor: '#bd9a5f'
    });
    return;
  }

  const confirm = await Swal.fire({
    icon: 'question',
    title: newStatus === 1 ? 'Kích hoạt Voucher?' : 'Tạm dừng Voucher?',
    text: newStatus === 1 ? 'Khách hàng sẽ có thể áp dụng mã giảm giá này.' : 'Khách hàng sẽ không thể áp dụng mã này để thanh toán nữa.',
    showCancelButton: true,
    confirmButtonText: newStatus === 1 ? 'Kích hoạt' : 'Tạm dừng',
    cancelButtonText: 'Hủy',
    confirmButtonColor: '#0d6efd'
  });

  if (!confirm.isConfirmed) return;

  try {
    const token = localStorage.getItem('token');
    const expectedRevision = resolveExpectedRevision(voucher);

    if (!expectedRevision) {
      await fetchVouchers();
      await Swal.fire(
        'Lỗi',
        'Không lấy được phiên bản Voucher hiện tại. Danh sách đã được tải lại, vui lòng thử lại.',
        'error'
      );
      return;
    }

    const payload = buildVoucherPayload(voucher, {
      status: newStatus,
      expectedRevision
    });

    await axios.put(`http://localhost:8080/api/admin/vouchers/${voucher.id}`, payload, {
      headers: { Authorization: `Bearer ${token}` }
    });

    Swal.fire({ toast: true, position: 'top-end', icon: 'success', title: 'Đã cập nhật trạng thái', showConfirmButton: false, timer: 1500 });
    await fetchVouchers();
  } catch (error: any) {
    if (error?.response?.status === 409) {
      await fetchVouchers();
      await Swal.fire({
        icon: 'warning',
        title: 'Voucher đã thay đổi',
        text: getErrorMessage(error, 'Voucher đã thay đổi bởi thao tác khác. Danh sách mới nhất đã được tải lại, vui lòng kiểm tra rồi thao tác lại.'),
        confirmButtonText: 'Đã hiểu'
      });
      return;
    }

    Swal.fire('Lỗi', getErrorMessage(error, 'Không thể thay đổi trạng thái!'), 'error');
  }
};

/*
 * Không-F5:
 * - Khi quay lại tab/window, chỉ refresh LIST từ BE.
 * - Nếu modal edit đang mở thì tuyệt đối không ghi đè draft trong form.
 * - Nếu dữ liệu đã đổi ở nơi khác, expectedRevision cũ sẽ làm PUT trả 409.
 */
const refreshVoucherListOnWindowReturn = async () => {
  if (document.visibilityState === 'hidden' || isWindowRefreshing.value || isSaving.value) {
    return;
  }

  isWindowRefreshing.value = true;
  try {
    await fetchVouchers();
  } finally {
    isWindowRefreshing.value = false;
  }
};

const handleWindowFocus = () => {
  void refreshVoucherListOnWindowReturn();
};

const handleVisibilityChange = () => {
  if (document.visibilityState === 'visible') {
    void refreshVoucherListOnWindowReturn();
  }
};

onMounted(() => {
  void fetchVouchers();
  window.addEventListener('focus', handleWindowFocus);
  document.addEventListener('visibilitychange', handleVisibilityChange);
});

onBeforeUnmount(() => {
  window.removeEventListener('focus', handleWindowFocus);
  document.removeEventListener('visibilitychange', handleVisibilityChange);
});
</script>

<style scoped>
/* Table styles đồng bộ */
.custom-table th {
  font-size: 0.85rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 1rem 0.5rem;
}
.custom-table td {
  padding: 1rem 0.5rem;
  vertical-align: middle;
}
.action-btn {
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.action-btn:hover {
  transform: translateY(-2px);
  background-color: #e9ecef !important;
}

/* Các badge màu pastel */
.bg-success-subtle {
  background-color: #d1e7dd !important;
}
.text-success {
  color: #198754 !important;
}
.bg-secondary-subtle {
  background-color: #e2e3e5 !important;
}
.bg-warning-subtle {
  background-color: #fff3cd !important;
}
.text-warning {
  color: #ffc107 !important;
}
.bg-info-subtle {
  background-color: #cff4fc !important;
}
.text-info {
  color: #087990 !important;
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
  border-radius: 16px;
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