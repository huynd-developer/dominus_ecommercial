<template>
  <div class="card border-0 shadow-sm">
    <div class="card-header bg-white py-3">
      <h5 class="mb-0 fw-bold text-primary">Cập Nhật Voucher</h5>
    </div>
    
    <div class="card-body p-4">
      <div v-if="isFetching" class="text-center py-5">
        <div class="spinner-border text-primary"></div>
      </div>
      <form v-else @submit.prevent="handleSubmit">
        <!-- COPY NGUYÊN XI CÁC Ô INPUT TỪ TRANG CREATE SANG ĐÂY -->
        <div class="row g-3">
          <div class="col-md-6">
            <label class="form-label">Mã Voucher <span class="text-danger">*</span></label>
            <input type="text" class="form-control text-uppercase" v-model="form.code" required />
          </div>
          <div class="col-md-3">
            <label class="form-label">Kiểu giảm <span class="text-danger">*</span></label>
            <select class="form-select" v-model="form.discountType">
              <option value="PERCENT">Phần trăm (%)</option>
              <option value="FIXED">Cố định (VNĐ)</option>
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
            <label class="form-label">Giới hạn lượt dùng</label>
            <input type="number" class="form-control" v-model="form.usageLimit" required min="1" />
          </div>
          <div class="col-md-6">
            <label class="form-label">Trạng thái</label>
            <select class="form-select" v-model="form.status">
              <option :value="1">Hoạt động</option>
              <option :value="0">Tạm dừng</option>
            </select>
          </div>
          <div class="col-md-6">
            <label class="form-label">Ngày bắt đầu</label>
            <input type="datetime-local" class="form-control" v-model="form.startDate" @change="validateDates" required />
          </div>
          <div class="col-md-6">
            <label class="form-label">Ngày kết thúc</label>
            <input type="datetime-local" class="form-control" :class="{'is-invalid': dateError}" v-model="form.endDate" @change="validateDates" required />
            <div v-if="dateError" class="invalid-feedback fw-bold"><i class="bi bi-exclamation-triangle-fill"></i> {{ dateError }}</div>
          </div>
        </div>

        <div class="d-flex justify-content-end mt-4">
          <RouterLink to="/admin/vouchers" class="btn btn-light me-2">Hủy</RouterLink>
          <button type="submit" class="btn btn-primary" :disabled="!!dateError || isLoading">
            Cập nhật Voucher
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import axios from 'axios';
import Swal from 'sweetalert2';

const router = useRouter();
const route = useRoute();
const isLoading = ref(false);
const isFetching = ref(true);
const dateError = ref('');
const voucherId = route.params.id;

const form = ref({
  code: '', discountType: 'PERCENT', discountValue: 0, minOrderValue: 0,
  maxDiscount: 0, usageLimit: 1, startDate: '', endDate: '', status: 1
});

// Lấy dữ liệu cũ đổ vào form
onMounted(async () => {
  try {
    const token = localStorage.getItem('token');
    const res = await axios.get(`http://localhost:8080/api/admin/vouchers/${voucherId}`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    
    // Format lại ngày tháng cho input datetime-local (cắt bỏ phần giây dư thừa)
    const data = res.data;
    data.startDate = data.startDate ? data.startDate.substring(0, 16) : '';
    data.endDate = data.endDate ? data.endDate.substring(0, 16) : '';
    
    form.value = data;
  } catch (error) {
    Swal.fire('Lỗi', 'Không lấy được thông tin Voucher', 'error');
    router.push('/admin/vouchers');
  } finally {
    isFetching.value = false;
  }
});

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
  isLoading.value = true;
  try {
    const token = localStorage.getItem('token');
    await axios.put(`http://localhost:8080/api/admin/vouchers/${voucherId}`, form.value, {
      headers: { Authorization: `Bearer ${token}` }
    });
    Swal.fire('Thành công', 'Cập nhật thành công', 'success');
    router.push('/admin/vouchers');
  } catch (error: any) {
    Swal.fire('Lỗi', error.response?.data || 'Không thể cập nhật!', 'error');
  } finally {
    isLoading.value = false;
  }
};
</script>