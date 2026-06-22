<template>
  <div class="card border-0 shadow-sm p-4 bg-white rounded-4">
    <div class="mb-4">
      <span class="text-uppercase text-gold tracking-widest fw-bold d-block" style="font-size: 0.7rem;">Bảo mật</span>
      <h3 class="font-luxury fw-normal text-navy h2 mt-1 mb-0">Đổi mật khẩu</h3>
    </div>

    <div class="row g-3">
      <div class="col-12">
        <label class="form-label text-muted fw-semibold mb-1" style="font-size: 0.75rem;">Mật khẩu hiện tại</label>
        <div class="position-relative">
          <span class="position-absolute top-50 start-0 translate-middle-y ms-3 text-muted opacity-50"><i class="bi bi-lock"></i></span>
          <input type="password" class="form-control dominus-input ps-5" placeholder="Nhập mật khẩu hiện tại" />
        </div>
      </div>
      <div class="col-md-6">
        <label class="form-label text-muted fw-semibold mb-1" style="font-size: 0.75rem;">Mật khẩu mới</label>
        <div class="position-relative">
          <span class="position-absolute top-50 start-0 translate-middle-y ms-3 text-muted opacity-50"><i class="bi bi-lock"></i></span>
          <input type="password" class="form-control dominus-input ps-5" placeholder="Tối thiểu 8 ký tự" />
        </div>
      </div>
      <div class="col-md-6">
        <label class="form-label text-muted fw-semibold mb-1" style="font-size: 0.75rem;">Xác nhận mật khẩu</label>
        <div class="position-relative">
          <span class="position-absolute top-50 start-0 translate-middle-y ms-3 text-muted opacity-50"><i class="bi bi-lock"></i></span>
          <input type="password" class="form-control dominus-input ps-5" placeholder="Nhập lại mật khẩu mới" />
        </div>
      </div>
    </div>
  </div>
</template>