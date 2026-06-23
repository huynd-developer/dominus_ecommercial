<template>
  <div class="user-profile-layout py-4 view-transition">
    
    <div class="profile-banner bg-navy-dark row rounded-4 mx-0 mb-4 align-items-center text-white p-4 p-md-5 position-relative overflow-hidden shadow">
      <div class="col-md-8 ps-md-3">
        <span class="text-uppercase tracking-widest fw-bold text-gold" style="font-size: 0.7rem;">Tài khoản thành viên</span>
        <h1 class="font-luxury display-5 my-2 fw-normal" style="letter-spacing: 1px;">Hồ sơ DOMINUS</h1>
        <p class="text-white-50 small mb-0 lh-base" style="max-width: 480px; font-weight: 300;">
          Quản lý thông tin cá nhân, địa chỉ nhận hàng và bảo mật mật khẩu trong cùng một không gian sang trọng.
        </p>
      </div>
      <div class="col-md-4 d-none d-md-flex justify-content-end pe-md-4">
        <div class="border border-secondary border-opacity-20 d-flex align-items-center justify-content-center rounded-3 shadow-sm" style="width: 75px; height: 95px; background: rgba(255,255,255,0.02);">
          <span class="font-luxury h1 mb-0 text-gold" style="border-bottom: 2px solid var(--dominus-gold); padding-bottom: 2px; font-weight: 300;">D</span>
        </div>
      </div>
    </div>

    <div class="row g-4">
      <aside class="col-lg-3">
        <div class="card border-0 shadow-sm p-3 bg-white rounded-4">
          <div class="d-flex align-items-center gap-3 mb-3 border-bottom pb-3">
            <div class="avatar bg-navy text-white rounded-circle d-flex align-items-center justify-content-center fw-bold font-luxury shadow-xs" style="width: 50px; height: 50px; font-size: 1.25rem;">
              L
            </div>
            <div>
              <h6 class="mb-0 fw-bold text-navy">Linh Nguyễn</h6>
              <span class="text-muted" style="font-size: 0.7rem; font-weight: 500;">Gold Member</span>
            </div>
          </div>

          <div class="nav flex-column gap-1">
            <button class="nav-btn-link active">Hồ sơ</button>
            <button class="nav-btn-link text-muted">Đổi mật khẩu</button>
            <button class="nav-btn-link text-muted">Đơn hàng</button>
            <button class="nav-btn-link text-muted">Ưu đãi</button>
          </div>
        </div>
      </aside>

      <main class="col-lg-9">
        <div class="row g-4">
          <div class="col-md-8">
            <UpdateProfile />
            <ChangePasswor />

            <div class="d-flex justify-content-end gap-3 mt-4">
              <button class="btn btn-dominus-secondary">Hủy thay đổi</button>
              <button class="btn btn-dominus-primary shadow-sm">Lưu hồ sơ</button>
            </div>
          </div>

          <div class="col-md-4">
            <div class="card border-0 shadow-sm p-4 bg-white rounded-4 mb-4" style="font-size: 0.8rem;">
              <span class="text-uppercase fw-bold text-gold tracking-wider mb-3 d-block" style="font-size: 0.7rem;">Tổng quan</span>
              <div class="d-flex flex-column gap-3">
                <div class="d-flex align-items-center gap-3 text-secondary font-weight-500">
                  <i class="bi bi-box-seam fs-6 text-navy"></i>
                  <span><strong>12</strong> đơn hàng đã hoàn tất</span>
                </div>
                <div class="d-flex align-items-center gap-3 text-secondary">
                  <i class="bi bi-envelope fs-6 text-navy"></i>
                  <span>Nhận bản tin mùi hương</span>
                </div>
                <div class="d-flex align-items-center gap-3 text-secondary">
                  <i class="bi bi-geo-alt fs-6 text-navy"></i>
                  <span>Giao nhanh nội thành</span>
                </div>
              </div>
            </div>

            <div class="card border-0 p-4 rounded-4 text-white shadow-lg bg-navy-dark">
              <h5 class="font-luxury fw-normal text-gold mb-2" style="font-size: 1.15rem;">Gold Privilege</h5>
              <p class="text-white-50 lh-base mb-4" style="font-size: 0.725rem; font-weight: 300;">
                Bạn còn 1.250.000đ để lên hạng Platinum và nhận ưu đãi chiết 10ml độc quyền.
              </p>
              <div class="progress bg-white bg-opacity-10" style="height: 4px;">
                <div class="progress-bar bg-gold" style="width: 75%; background-color: var(--dominus-gold) !important;"></div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import UpdateProfile from './UpdateProfile.vue';
import ChangePasswor from './ChangePasswor.vue';
</script>

<style scoped>
/* Nút bấm Menu bên trái */
.nav-btn-link {
  width: 100%;
  text-align: left;
  border: none;
  background: transparent;
  padding: 10px 14px;
  border-radius: 6px;
  font-size: 0.825rem;
  font-weight: 600;
  color: var(--dominus-navy-text);
  transition: all 0.2s ease;
}
.nav-btn-link:hover {
  background-color: var(--dominus-gray-bg);
  color: var(--dominus-navy-text);
}
.nav-btn-link.active {
  background-color: var(--dominus-navy-text) !important;
  color: #ffffff !important;
}
</style>