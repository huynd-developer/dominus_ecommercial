<template>
  <div class="user-profile-layout py-4 view-transition text-light-cream">
    
    <!-- BANNER NAVY TỐI THƯỢNG -->
    <div class="profile-banner bg-navy-card row border border-secondary border-opacity-10 rounded-4 mx-0 mb-4 align-items-center text-white p-4 p-md-5 position-relative overflow-hidden shadow">
      <div class="col-md-8 ps-md-3">
        <span class="text-uppercase tracking-widest fw-bold text-gold" style="font-size: 0.7rem;">Tài khoản thành viên</span>
        <h1 class="font-luxury display-5 my-2 fw-normal text-gold" style="letter-spacing: 1px;">Hồ sơ DOMINUS</h1>
        <p class="text-white-50 small mb-0 lh-base" style="max-width: 480px; font-weight: 300;">
          Quản lý thông tin cá nhân, địa chỉ nhận hàng và bảo mật mật khẩu trong cùng một không gian sang trọng.
        </p>
      </div>
      <div class="col-md-4 d-none d-md-flex justify-content-end pe-md-4">
        <div class="border border-gold border-opacity-20 d-flex align-items-center justify-content-center rounded-3 shadow-sm" style="width: 75px; height: 95px; background: rgba(197, 147, 70, 0.02);">
          <span class="font-luxury h1 mb-0 text-gold" style="border-bottom: 2px solid var(--dominus-gold); padding-bottom: 2px; font-weight: 300;">D</span>
        </div>
      </div>
    </div>

    <div class="row g-4">
      <!-- CỘT TRÁI: MENU ĐIỀU HƯỚNG TỐI MÀU -->
      <aside class="col-lg-3">
        <div class="card border border-secondary border-opacity-10 shadow-sm p-3 bg-navy-card rounded-4">
          <!-- Avatar & Tên thành viên -->
          <div class="d-flex align-items-center gap-3 mb-3 border-bottom border-secondary border-opacity-20 pb-3">
            <div class="avatar bg-gold text-navy-dark rounded-circle d-flex align-items-center justify-content-center fw-bold font-luxury shadow-sm" style="width: 50px; height: 50px; font-size: 1.25rem;">
              L
            </div>
            <div>
              <h6 class="mb-0 fw-bold text-gold">Linh Nguyễn</h6>
              <span class="text-white-50" style="font-size: 0.7rem; font-weight: 500;">Gold Member</span>
            </div>
          </div>

          <!-- Nhóm nút chuyển tab -->
          <div class="nav flex-column gap-1">
            <button class="nav-btn-link active">Hồ sơ</button>
            <button class="nav-btn-link text-white-50">Đổi mật khẩu</button>
            <button class="nav-btn-link text-white-50">Đơn hàng</button>
            <button class="nav-btn-link text-white-50">Ưu đãi</button>
          </div>
        </div>
      </aside>

      <!-- CỘT PHẢI: NỘI DUNG FORM CHÍNH -->
      <main class="col-lg-9">
        <div class="row g-4">
          <div class="col-md-8">
            <!-- Nơi chứa các ô input nhập liệu -->
            <UpdateProfile />
            <ChangePasswor />

            <!-- Cụm nút hành động bên dưới form -->
            <div class="d-flex justify-content-end gap-3 mt-4">
              <button class="btn btn-dominus-secondary">Hủy thay đổi</button>
              <button class="btn btn-dominus-primary shadow-sm">Lưu hồ sơ</button>
            </div>
          </div>

          <!-- KHU VỰC THẺ PHỤ TRỢ (TỔNG QUAN VÀ VIP) -->
          <div class="col-md-4">
            <!-- Thẻ tổng quan dữ liệu -->
            <div class="card border border-secondary border-opacity-10 shadow-sm p-4 bg-navy-card rounded-4 mb-4" style="font-size: 0.8rem;">
              <span class="text-uppercase fw-bold text-gold tracking-wider mb-3 d-block" style="font-size: 0.7rem;">Tổng quan</span>
              <div class="d-flex flex-column gap-3">
                <div class="d-flex align-items-center gap-3 text-white-50 font-weight-500">
                  <i class="bi bi-box-seam fs-6 text-gold"></i>
                  <span><strong class="text-white">12</strong> đơn hàng đã hoàn tất</span>
                </div>
                <div class="d-flex align-items-center gap-3 text-white-50">
                  <i class="bi bi-envelope fs-6 text-gold"></i>
                  <span>Nhận bản tin mùi hương</span>
                </div>
                <div class="d-flex align-items-center gap-3 text-white-50">
                  <i class="bi bi-geo-alt fs-6 text-gold"></i>
                  <span>Giao nhanh nội thành</span>
                </div>
              </div>
            </div>

            <!-- Thẻ đặc quyền Thăng hạng -->
            <div class="card border border-gold border-opacity-10 p-4 rounded-4 text-white shadow-lg bg-navy-card">
              <h5 class="font-luxury fw-normal text-gold mb-2" style="font-size: 1.15rem;">Gold Privilege</h5>
              <p class="text-white-50 lh-base mb-4" style="font-size: 0.725rem; font-weight: 300;">
                Bạn còn 1.250.000đ để lên hạng Platinum và nhận ưu đãi chiết 10ml độc quyền.
              </p>
              <div class="progress bg-white bg-opacity-10" style="height: 4px;">
                <div class="progress-bar" style="width: 75%; background-color: var(--dominus-gold) !important;"></div>
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
/* Đồng bộ biến màu hệ thống nội bộ component */
.bg-navy-card { background-color: #131a2e !important; }
.text-gold { color: var(--dominus-gold) !important; }
.text-navy-dark { color: #0b0f19 !important; }

/* Ép tất cả class .card có sẵn hoặc từ component con về màu nền tối hoàng gia */
.user-profile-layout :deep(.card),
.user-profile-layout .card {
  background-color: #131a2e !important;
  color: #f8fafc !important;
}

/* 
  Bẻ gãy (Deep selector) CSS của các Form con (UpdateProfile, ChangePassword).
  Giúp các ô Input chuyển sang nền xám đen mịn, chữ trắng và viền vàng khi tương tác.
*/
.user-profile-layout :deep(.form-control),
.user-profile-layout :deep(.dominus-input),
.user-profile-layout :deep(input) {
  background-color: #1e293b !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  border-radius: 8px !important;
  padding: 10px 16px !important;
  color: #ffffff !important;
  font-size: 0.875rem !important;
}

.user-profile-layout :deep(.form-control:focus),
.user-profile-layout :deep(.dominus-input:focus),
.user-profile-layout :deep(input:focus) {
  border-color: var(--dominus-gold) !important;
  box-shadow: 0 0 0 3px rgba(197, 147, 70, 0.15) !important;
  outline: none;
}

/* Đổi màu chữ hiển thị tiêu đề nhãn (Label) của form con */
.user-profile-layout :deep(.form-label),
.user-profile-layout :deep(label),
.user-profile-layout :deep(.text-muted) {
  color: #94a3b8 !important;
}

/* Điều chỉnh icon nằm trong cụm input (nếu có) */
.user-profile-layout :deep(.position-relative) i,
.user-profile-layout :deep(.position-relative) .text-muted {
  color: var(--dominus-gold) !important;
  opacity: 0.8;
}

/* Nút bấm Menu danh mục bên trái */
.nav-btn-link {
  width: 100%;
  text-align: left;
  border: none;
  background: transparent;
  padding: 11px 16px;
  border-radius: 8px;
  font-size: 0.825rem;
  font-weight: 600;
  color: #94a3b8;
  transition: all 0.2s ease;
}
.nav-btn-link:hover {
  background-color: rgba(255, 255, 255, 0.03);
  color: var(--dominus-gold);
}
.nav-btn-link.active {
  background-color: var(--dominus-gold) !important;
  color: #0b0f19 !important; /* Chữ đen huyền trên nền nút vàng */
}

/* Custom lại hệ thống nút bấm hành động cuối form */
.btn-dominus-primary {
  background-color: var(--dominus-gold) !important;
  color: #0b0f19 !important;
  border: none !important;
  border-radius: 50px !important;
  padding: 10px 28px !important;
  font-size: 0.85rem !important;
  font-weight: 700 !important;
  transition: all 0.2s ease;
}
.btn-dominus-primary:hover {
  background-color: #dfaa53 !important;
  transform: translateY(-1px);
}

.btn-dominus-secondary {
  background-color: transparent !important;
  color: #94a3b8 !important;
  border: 1px solid rgba(255, 255, 255, 0.15) !important;
  border-radius: 50px !important;
  padding: 10px 28px !important;
  font-size: 0.85rem !important;
  font-weight: 600 !important;
  transition: all 0.2s ease;
}
.btn-dominus-secondary:hover {
  background-color: rgba(255, 255, 255, 0.03) !important;
  color: #ffffff !important;
}
</style>