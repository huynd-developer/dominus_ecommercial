<template>
  <div class="card border-0 shadow-sm">
    <div class="card-header bg-white py-3">
      <h5 class="mb-0 fw-bold text-primary">Thêm Mới Voucher</h5>
    </div>
    
    <div class="card-body p-4">
      <form @submit.prevent="handleSubmit">
        <div class="row g-3">
          <div class="col-md-6">
            <label class="form-label">Mã Voucher <span class="text-danger">*</span></label>
            <input type="text" class="form-control text-uppercase" v-model="form.code" required placeholder="VD: SUMMER24" />
          </div>
          
          <div class="col-md-3">
            <label class="form-label">Kiểu giảm <span class="text-danger">*</span></label>
            <select class="form-select" v-model="form.discountType">
              <option value="PERCENT">Phần trăm (%)</option>
              <option value="FIXED">Số tiền cố định (VNĐ)</option>
            </select>
          </div>

          <div class="col-md-3">
            <label class="form-label">Mức giảm <span class="text-danger">*</span></label>
            <input type="number" class="form-control" v-model="form.discountValue" required min="0" />
          </div>

          <div class="col-md-6">
            <label class="form-label">Đơn tối thiểu (VNĐ)</label>
            <input type="number" class="form-control" v-model="form.minOrderValue" min="0" />
          </div>

          <div class="col-md-6">
            <label class="form-label">Mức giảm tối đa (VNĐ)</label>
            <input type="number" class="form-control" v-model="form.maxDiscount" min="0" :disabled="form.discountType === 'FIXED'" />
          </div>

          <div class="col-md-6">
            <label class="form-label">Giới hạn lượt dùng <span class="text-danger">*</span></label>
            <input type="number" class="form-control" v-model="form.usageLimit" required min="1" />
          </div>

          <div class="col-md-6">
            <label class="form-label">Trạng thái</label>
            <select class="form-select" v-model="form.status">
              <option :value="1">Hoạt động</option>
              <option :value="0">Tạm dừng</option>
            </select>
          </div>

          <!-- BẮT LỖI NGÀY THÁNG NHƯ YÊU CẦU -->
          <div class="col-md-6">
            <label class="form-label">Ngày bắt đầu <span class="text-danger">*</span></label>
            <input type="datetime-local" class="form-control" v-model="form.startDate" @change="validateDates" required />
          </div>

          <div class="col-md-6">
            <label class="form-label">Ngày kết thúc <span class="text-danger">*</span></label>
            <input type="datetime-local" class="form-control" :class="{'is-invalid': dateError}" v-model="form.endDate" @change="validateDates" required />
            <div v-if="dateError" class="invalid-feedback fw-bold">
              <i class="bi bi-exclamation-triangle-fill"></i> {{ dateError }}
            </div>
          </div>
        </div>

        <div class="d-flex justify-content-end mt-4">
          <RouterLink to="/admin/vouchers" class="btn btn-light me-2">Hủy</RouterLink>
          <button type="submit" class="btn btn-primary" :disabled="!!dateError || isLoading">
            <span v-if="isLoading" class="spinner-border spinner-border-sm me-2"></span>
            Lưu Voucher
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import Swal from 'sweetalert2';

const router = useRouter();
const isLoading = ref(false);
const dateError = ref('');

const form = ref({
  code: '',
  discountType: 'PERCENT',
  discountValue: 0,
  minOrderValue: 0,
  maxDiscount: 0,
  usageLimit: 1,
  startDate: '',
  endDate: '',
  status: 1
});

const validateDates = () => {
  if (form.value.startDate && form.value.endDate) {
    if (new Date(form.value.startDate) > new Date(form.value.endDate)) {
      dateError.value = 'Ngày bắt đầu không được lớn hơn ngày kết thúc!';
      return false;
    }
  }
  dateError.value = '';
  return true;
};

const handleSubmit = async () => {
  if (!validateDates()) return;

  isLoading.value = true;
  try {
    const token = localStorage.getItem('token');
    await axios.post('http://localhost:8080/api/admin/vouchers', form.value, {
      headers: { Authorization: `Bearer ${token}` }
    });
    
    Swal.fire('Thành công', 'Thêm Voucher thành công', 'success');
    router.push('/admin/vouchers');
  } catch (error: any) {
    if (error.response?.status === 400) {
      // Nhận lỗi từ Backend ném ra
      dateError.value = error.response.data || 'Lỗi kiểm tra ngày tháng!';
      Swal.fire('Lỗi', dateError.value, 'error');
    } else {
      Swal.fire('Lỗi', 'Không thể thêm Voucher!', 'error');
    }
  } finally {
    isLoading.value = false;
  }
};
</script>