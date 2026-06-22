<template>
  <div class="card border-0 shadow-sm p-4 bg-white rounded-4 mb-4">
    <div class="d-flex align-items-start justify-content-between mb-4">
      <div>
        <span class="text-uppercase text-gold tracking-widest fw-bold d-block" style="font-size: 0.7rem;">Thông tin cá nhân</span>
        <h3 class="font-luxury fw-normal text-navy h2 mt-1 mb-0">Chi tiết hồ sơ</h3>
      </div>
      <div class="border rounded-circle d-flex align-items-center justify-content-center border-success-subtle bg-success-subtle bg-opacity-10" style="width: 42px; height: 42px;">
        <i class="bi bi-shield-check text-success fs-4"></i>
      </div>
    </div>

    <div class="row g-3">
      <div class="col-md-6">
        <label class="form-label text-muted fw-semibold mb-1" style="font-size: 0.75rem;">Họ và tên</label>
        <input type="text" class="form-control dominus-input" v-model="profile.name" />
      </div>
      <div class="col-md-6">
        <label class="form-label text-muted fw-semibold mb-1" style="font-size: 0.75rem;">Số điện thoại</label>
        <input type="text" class="form-control dominus-input" v-model="profile.phone" />
      </div>
      <div class="col-md-6">
        <label class="form-label text-muted fw-semibold mb-1" style="font-size: 0.75rem;">Email</label>
        <input type="email" class="form-control dominus-input text-muted" v-model="profile.email" disabled style="cursor: not-allowed;" />
      </div>
      <div class="col-md-6">
        <label class="form-label text-muted fw-semibold mb-1" style="font-size: 0.75rem;">Ngày sinh</label>
        <input type="text" class="form-control dominus-input" v-model="profile.dob" />
      </div>
      <div class="col-12">
        <label class="form-label text-muted fw-semibold mb-1" style="font-size: 0.75rem;">Địa chỉ mặc định</label>
        <input type="text" class="form-control dominus-input" v-model="profile.address" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
const profile = ref({
  name: 'Linh Nguyễn',
  phone: '090 882 1688',
  email: 'linh.nguyen@dominus.vn',
  dob: '18/08/1996',
  address: '72 Lê Thánh Tôn, Quận 1, TP. Hồ Chí Minh'
});
</script>