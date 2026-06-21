<template>
  <div class="row justify-content-center">
    <div class="col-md-6 col-lg-5">
      <div class="card shadow-sm border-0 rounded-3">
        <div class="card-body p-4">
          <h4 class="card-title fw-bold text-dark mb-1">Đổi mật khẩu</h4>
          <p class="text-muted small mb-4">Mật khẩu mới nên có ít nhất 8 ký tự bao gồm chữ và số.</p>

          <form @submit.prevent="handleChangePassword">
            <div class="mb-3">
              <label class="form-label fw-semibold text-secondary small text-uppercase">Mật khẩu hiện tại *</label>
              <div class="input-group">
                <input 
                  :type="showCurrent ? 'text' : 'password'" 
                  v-model="form.currentPassword"
                  class="form-control" 
                  placeholder="Nhập mật khẩu cũ"
                  required
                />
                <button class="btn btn-outline-secondary" type="button" @click="showCurrent = !showCurrent">
                  <i :class="showCurrent ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                </button>
              </div>
            </div>

            <div class="mb-3">
              <label class="form-label fw-semibold text-secondary small text-uppercase">Mật khẩu mới *</label>
              <div class="input-group">
                <input 
                  :type="showNew ? 'text' : 'password'" 
                  v-model="form.newPassword"
                  class="form-control" 
                  placeholder="Nhập mật khẩu mới"
                  required
                />
                <button class="btn btn-outline-secondary" type="button" @click="showNew = !showNew">
                  <i :class="showNew ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                </button>
              </div>
            </div>

            <div class="mb-4">
              <label class="form-label fw-semibold text-secondary small text-uppercase">Xác nhận mật khẩu mới *</label>
              <div class="input-group">
                <input 
                  :type="showConfirm ? 'text' : 'password'" 
                  v-model="form.confirmPassword"
                  class="form-control" 
                  placeholder="Nhập lại mật khẩu mới"
                  required
                />
                <button class="btn btn-outline-secondary" type="button" @click="showConfirm = !showConfirm">
                  <i :class="showConfirm ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                </button>
              </div>
            </div>

            <div class="d-flex justify-content-end gap-2">
              <button type="button" class="btn btn-light px-4" @click="resetForm">Hủy</button>
              <button type="submit" class="btn btn-primary px-4 shadow-sm" :disabled="loading">
                <span v-if="loading" class="spinner-border spinner-border-sm me-1"></span>
                Cập nhật mật khẩu
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';

const showCurrent = ref(false);
const showNew = ref(false);
const showConfirm = ref(false);
const loading = ref(false);

const form = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const handleChangePassword = async () => {
  if (form.newPassword !== form.confirmPassword) {
    alert('Mật khẩu nhập lại không trùng khớp!');
    return;
  }
  
  loading.value = true;
  try {
    // Tương đương với API: POST /api/v1/customer/change-password
    console.log('Gửi API Đổi mật khẩu:', form);
    
    // Giả lập độ trễ mạng của API
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    alert('Đổi mật khẩu thành công. Vui lòng đăng nhập lại!');
    resetForm();
  } catch (error) {
    alert('Có lỗi xảy ra, vui lòng thử lại.');
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  form.currentPassword = '';
  form.newPassword = '';
  form.confirmPassword = '';
};
</script>