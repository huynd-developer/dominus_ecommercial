<template>
  <div class="dominus-app min-vh-100 pb-5">
    
    <header class="bg-white border-bottom sticky-top shadow-sm">
      <div class="container-xl py-3">
        <div class="row align-items-center g-3">
          <div class="col-6 col-md-3 text-start">
            <h1 class="logo-text m-0">DOMINUS <span class="small-perfume">PERFUME</span></h1>
          </div>
          <div class="col-12 col-md-6 order-3 order-md-2">
            <div class="search-box position-relative">
              <input type="text" class="form-control rounded-pill ps-4 pe-5" placeholder="Tìm kiếm nước hoa...">
              <i class="bi bi-search position-absolute end-0 top-50 translate-middle-y me-4 text-muted"></i>
            </div>
          </div>
          <div class="col-6 col-md-3 text-end order-2 order-md-3">
            <div class="d-flex justify-content-end gap-3 text-nowrap">
              <a href="#" class="text-dark text-decoration-none small-menu"><i class="bi bi-person me-1"></i> Tài khoản</a>
              <a href="#" class="text-dark text-decoration-none small-menu"><i class="bi bi-bag me-1"></i> Giỏ hàng</a>
            </div>
          </div>
        </div>
      </div>
      
      <div class="bg-white border-top py-2 d-none d-md-block">
        <div class="container-xl">
          <ul class="nav justify-content-center gap-4 text-uppercase fw-semibold sub-nav">
            <li class="nav-item"><a class="nav-link text-secondary" href="#">Thương hiệu</a></li>
            <li class="nav-item"><a class="nav-link text-secondary" href="#">Giới tính</a></li>
            <li class="nav-item"><a class="nav-link text-secondary" href="#">Niche</a></li>
            <li class="nav-item"><a class="nav-link text-secondary" href="#">Chiết 10ml</a></li>
            <li class="nav-item"><a class="nav-link text-golden" href="#">Flash Sale</a></li>
          </ul>
        </div>
      </div>
    </header>

    <main class="container-xl mt-4">
      
      <section class="hero-banner rounded-4 mb-4 overflow-hidden shadow-sm position-relative">
        <div class="row g-0 h-100 align-items-center">
          <div class="col-md-7 p-4 p-lg-5 text-white z-2">
            <span class="text-uppercase tracking-wider text-golden small-title">Tài khoản thành viên</span>
            <h2 class="display-6 fw-serif mt-1 mb-3">Hồ Sơ DOMINUS</h2>
            <p class="text-light-muted m-0 lead-sm">
              Quản lý thông tin cá nhân, địa chỉ nhận hàng và bảo mật mật khẩu trong cùng một không gian sang trọng.
            </p>
          </div>
          <div class="col-md-5 d-none d-md-flex justify-content-center align-items-center p-4 position-relative banner-right-decor">
            <div class="gold-badge-box text-center p-4 rounded-3 shadow">
              <div class="gold-line mb-2"></div>
              <span class="d-logo">D</span>
              <div class="gold-line mt-2"></div>
            </div>
          </div>
        </div>
      </section>

      <div class="row g-4">
        
        <aside class="col-12 col-md-4 col-xl-3">
          <div class="card card-custom p-4 h-100">
            <div class="d-flex align-items-center gap-3 pb-3 border-bottom mb-4">
              <div class="avatar-circle d-flex align-items-center justify-content-center text-white fw-bold">
                L
              </div>
              <div>
                <h5 class="m-0 fw-bold text-dark">Linh Nguyễn</h5>
                <span class="badge bg-gold-light text-golden rounded-pill px-2 py-1 small-tag">Gold Member</span>
              </div>
            </div>
            
            <ul class="nav flex-column sidebar-menu gap-2">
              <li class="nav-item">
                <a class="nav-link active" href="#"><i class="bi bi-person-vcard me-2"></i> Hồ sơ</a>
              </li>
              <li class="nav-item">
                <a class="nav-link" href="#"><i class="bi bi-shield-lock me-2"></i> Đổi mật khẩu</a>
              </li>
              <li class="nav-item">
                <a class="nav-link" href="#"><i class="bi bi-box-seam me-2"></i> Đơn hàng</a>
              </li>
              <li class="nav-item">
                <a class="nav-link" href="#"><i class="bi bi-gift me-2"></i> Ưu đãi</a>
              </li>
            </ul>
          </div>
        </aside>

        <section class="col-12 col-md-8 col-xl-6">
          <div class="card card-custom p-4 p-lg-5">
            <form @submit.prevent="handleSubmit">
              
              <div class="mb-5">
                <div class="d-flex justify-content-between align-items-center mb-3">
                  <div>
                    <span class="text-uppercase text-golden small-title fw-semibold">Thông tin cá nhân</span>
                    <h3 class="fw-serif text-dark m-0">Chi tiết hồ sơ</h3>
                  </div>
                  <i class="bi bi-shield-check text-golden fs-3"></i>
                </div>
                
                <div class="row g-3">
                  <div class="col-12 col-sm-6">
                    <label class="form-label text-muted small">Họ và tên</label>
                    <input type="text" v-model="profile.fullName" class="form-control form-control-custom">
                  </div>
                  <div class="col-12 col-sm-6">
                    <label class="form-label text-muted small">Số điện thoại</label>
                    <input type="text" v-model="profile.phone" class="form-control form-control-custom">
                  </div>
                  <div class="col-12">
                    <label class="form-label text-muted small">Email</label>
                    <input type="email" v-model="profile.email" class="form-control form-control-custom">
                  </div>
                  <div class="col-12 col-sm-6">
                    <label class="form-label text-muted small">Ngày sinh</label>
                    <input type="text" v-model="profile.dob" class="form-control form-control-custom" placeholder="DD/MM/YYYY">
                  </div>
                  <div class="col-12">
                    <label class="form-label text-muted small">Địa chỉ mặc định</label>
                    <textarea v-model="profile.address" class="form-control form-control-custom" rows="2"></textarea>
                  </div>
                </div>
              </div>

              <div class="mb-4">
                <span class="text-uppercase text-golden small-title fw-semibold">Bảo mật</span>
                <h3 class="fw-serif text-dark mb-4">Đổi mật khẩu</h3>
                
                <div class="row g-3">
                  <div class="col-12">
                    <label class="form-label text-muted small">Mật khẩu hiện tại</label>
                    <div class="input-group-custom position-relative">
                      <i class="bi bi-lock position-absolute start-0 top-50 translate-middle-y ms-3 text-muted"></i>
                      <input :type="showPass.current ? 'text' : 'password'" v-model="security.currentPassword" class="form-control form-control-custom ps-5" placeholder="Nhập mật khẩu hiện tại">
                      <i @click="showPass.current = !showPass.current" :class="showPass.current ? 'bi bi-eye-slash' : 'bi bi-eye'" class="bi position-absolute end-0 top-50 translate-middle-y me-3 cursor-pointer text-muted"></i>
                    </div>
                  </div>
                  
                  <div class="col-12 col-sm-6">
                    <label class="form-label text-muted small">Mật khẩu mới</label>
                    <div class="input-group-custom position-relative">
                      <i class="bi bi-lock position-absolute start-0 top-50 translate-middle-y ms-3 text-muted"></i>
                      <input :type="showPass.new ? 'text' : 'password'" v-model="security.newPassword" class="form-control form-control-custom ps-5" placeholder="Tối thiểu 8 ký tự">
                      <i @click="showPass.new = !showPass.new" :class="showPass.new ? 'bi bi-eye-slash' : 'bi bi-eye'" class="bi position-absolute end-0 top-50 translate-middle-y me-3 cursor-pointer text-muted"></i>
                    </div>
                  </div>

                  <div class="col-12 col-sm-6">
                    <label class="form-label text-muted small">Xác nhận mật khẩu</label>
                    <div class="input-group-custom position-relative">
                      <i class="bi bi-lock position-absolute start-0 top-50 translate-middle-y ms-3 text-muted"></i>
                      <input :type="showPass.confirm ? 'text' : 'password'" v-model="security.confirmPassword" class="form-control form-control-custom ps-5" placeholder="Nhập lại mật khẩu mới">
                      <i @click="showPass.confirm = !showPass.confirm" :class="showPass.confirm ? 'bi bi-eye-slash' : 'bi bi-eye'" class="bi position-absolute end-0 top-50 translate-middle-y me-3 cursor-pointer text-muted"></i>
                    </div>
                  </div>
                </div>
              </div>

              <div class="d-flex justify-content-end gap-3 pt-3 border-top mt-4 flex-wrap">
                <button type="button" @click="handleCancel" class="btn btn-outline-custom px-4 rounded-pill py-2">Hủy thay đổi</button>
                <button type="submit" class="btn btn-gold px-5 rounded-pill py-2 text-white shadow-sm" :disabled="loading">
                  <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
                  Lưu hồ sơ
                </button>
              </div>

            </form>
          </div>
        </section>

        <aside class="col-12 col-md-12 col-xl-3">
          <div class="row g-4">
            <div class="col-12 col-md-6 col-xl-12">
              <div class="card card-custom p-4">
                <h5 class="text-uppercase text-golden small-title fw-bold mb-3">Tổng quan</h5>
                <ul class="list-unstyled d-flex flex-column gap-3 m-0 overview-list">
                  <li class="d-flex align-items-center gap-2">
                    <i class="bi bi-box2 text-dark"></i>
                    <span><strong>12</strong> đơn hàng đã hoàn tất</span>
                  </li>
                  <li class="d-flex align-items-center gap-2">
                    <i class="bi bi-envelope text-dark"></i>
                    <span>Nhận bản tin mùi hương</span>
                  </li>
                  <li class="d-flex align-items-center gap-2">
                    <i class="bi bi-geo-alt text-dark"></i>
                    <span>Giao nhanh nội thành</span>
                  </li>
                </ul>
              </div>
            </div>

            <div class="col-12 col-md-6 col-xl-12">
              <div class="card gold-privilege-card p-4 text-white position-relative overflow-hidden h-100">
                <div class="z-2 position-relative">
                  <h4 class="fw-serif text-golden-light mb-2">Gold Privilege</h4>
                  <p class="small text-light-muted mb-4">
                    Bạn còn <strong>1.250.000đ</strong> để lên hạng Platinum và nhận ưu đãi chiết 10ml độc quyền.
                  </p>
                  <div class="progress progress-custom bg-dark-muted rounded-pill" style="height: 6px;">
                    <div class="progress-bar bg-gold-gradient rounded-pill" role="progressbar" style="width: 75%"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </aside>

      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';

// Trạng thái Loading của Nút Lưu
const loading = ref(false);

// Kiểm soát Trạng thái Ẩn/Hiện mật khẩu
const showPass = reactive({
  current: false,
  new: false,
  confirm: false
});

// Dữ liệu Form Thông tin cá nhân khớp hình ảnh
const profile = reactive({
  fullName: 'Linh Nguyễn',
  phone: '090 882 1688',
  email: 'linh.nguyen@dominus.vn',
  dob: '18/08/1996',
  address: ''
});

// Dữ liệu Form Mật khẩu bảo mật
const security = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
});

// Xử lý gửi dữ liệu lên API đồng thời cả 2 chức năng
const handleSubmit = async () => {
  // Validate kiểm tra trùng khớp mật khẩu mới
  if (security.newPassword && security.newPassword !== security.confirmPassword) {
    alert('Mật khẩu xác nhận mới không trùng khớp!');
    return;
  }

  loading.value = true;
  try {
    console.log('--- CALLING API PROCESSES ---');
    console.log('1. PUT /api/v1/customer/profile -> Data:', profile);
    if (security.currentPassword && security.newPassword) {
      console.log('2. POST /api/v1/customer/change-password -> Data:', security);
    }
    
    // Giả lập xử lý API trì hoãn mạng
    await new Promise(resolve => setTimeout(resolve, 1500));
    alert('Hệ thống Dominus đã lưu thông tin và mật khẩu mới thành công!');
    
    // Reset form bảo mật
    security.currentPassword = '';
    security.newPassword = '';
    security.confirmPassword = '';
  } catch (error) {
    alert('Có lỗi hệ thống xảy ra, vui lòng tải lại trang.');
  } finally {
    loading.value = false;
  }
};

// Xử lý nút Hủy
const handleCancel = () => {
  if (confirm('Bạn có chắc chắn muốn hủy các thay đổi vừa nhập?')) {
    security.currentPassword = '';
    security.newPassword = '';
    security.confirmPassword = '';
  }
};
</script>

<style scoped>
/* IMPORT BỘ FONT HỖ TRỢ TIẾNG VIỆT 100% - TỐI ƯU SANG TRỌNG */
@import url('https://fonts.googleapis.com/css2?family=Cormorant+Garamond:ital,wght@0,400;0,600;0,700;1,400&family=Montserrat:wght@300;400;500;600;700&display=swap');
@import url('https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css');

/* CẤU HÌNH BIẾN MÀU THEO BRAND ĐÁP ỨNG ZOOM HOÀN HẢO */
.dominus-app {
  font-family: 'Montserrat', sans-serif;
  background-color: #faf9f6; /* Nền trắng ngà siêu nhẹ như ảnh gốc */
  font-size: 0.95rem;
  /* Tăng nhẹ line-height để các dấu tiếng Việt (huyền, sắc, hỏi, ngã, nặng) có không gian hiển thị, không bị dính chữ */
  line-height: 1.7; 
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* TYPOGRAPHY CHUYÊN BIỆT CHO BRAND NƯỚC HOA */
.fw-serif {
  font-family: 'Cormorant Garamond', serif;
  font-weight: 600;
}
.logo-text {
  font-family: 'Cormorant Garamond', serif;
  font-weight: 700;
  letter-spacing: 3px;
  color: #111625;
}
.small-perfume {
  font-size: 0.55rem;
  font-family: 'Montserrat', sans-serif;
  letter-spacing: 5px;
  display: block;
  color: #bfa361;
  font-weight: 500;
  margin-top: -2px;
}
.text-golden {
  color: #bfa361 !important;
}
.text-golden-light {
  color: #dfcaa0 !important;
}
.bg-gold-light {
  background-color: #f7f3e8;
}
.small-title {
  font-size: 0.75rem;
  letter-spacing: 2px;
  font-weight: 600;
}
.tracking-wider {
  letter-spacing: 2.5px;
}

/* HEADER SUB-NAV */
.sub-nav .nav-link {
  font-size: 0.8rem;
  padding: 0.25rem 0.5rem;
  letter-spacing: 1px;
}
.small-menu {
  font-size: 0.85rem;
  letter-spacing: 0.5px;
}

/* HERO BANNER - SỬ DỤNG MIN-HEIGHT ĐỂ TRÁNH VỠ KHI ZOOM */
.hero-banner {
  background: linear-gradient(135deg, #111625 0%, #1c2336 100%);
  min-height: 220px;
}
.banner-right-decor::before {
  content: '';
  position: absolute;
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, rgba(191,163,97,0.2) 0%, transparent 70%);
  right: 10%;
  top: 50%;
  transform: translateY(-50%);
}
.gold-badge-box {
  border: 1px solid rgba(191,163,97,0.4);
  background-color: rgba(17, 22, 37, 0.6);
  backdrop-filter: blur(5px);
  width: 90px;
}
.gold-line {
  height: 1px;
  background-color: #bfa361;
  width: 30px;
  margin: 0 auto;
}
.d-logo {
  color: #bfa361;
  font-family: 'Cormorant Garamond', serif;
  font-size: 2.2rem;
  font-weight: bold;
  line-height: 1;
}

/* CARDS CHUNG */
.card-custom {
  border: none;
  background-color: #ffffff;
  border-radius: 1rem !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.015) !important;
  transition: all 0.3s ease;
}

/* SIDEBAR LEFT */
.avatar-circle {
  width: 50px;
  height: 50px;
  background-color: #111625;
  border-radius: 50%;
  font-size: 1.25rem;
  font-family: 'Cormorant Garamond', serif;
}
.sidebar-menu .nav-link {
  color: #4a5568;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  font-weight: 500;
  font-size: 0.9rem;
  transition: all 0.2s ease;
}
.sidebar-menu .nav-link:hover {
  background-color: #f7f6f2;
  color: #111625;
}
.sidebar-menu .nav-link.active {
  background-color: #111625;
  color: #ffffff !important;
}

/* FORM CONTROLS CUSTOM ĐÚNG KHUÔN HÌNH - AN TOÀN CHO TIẾNG VIỆT */
.form-control-custom {
  border: 1px solid #e2e8f0;
  /* Tăng padding top/bottom một chút để các nguyên âm tiếng Việt có dấu mũ (như ô, â, ê) không bị chạm viền input */
  padding: 0.75rem 1.1rem;
  border-radius: 0.65rem;
  font-size: 0.9rem;
  background-color: #fff;
  color: #2d3748;
  font-family: 'Montserrat', sans-serif;
  transition: all 0.2s ease;
}
.form-control-custom:focus {
  border-color: #bfa361;
  box-shadow: 0 0 0 3px rgba(191, 163, 97, 0.12);
  outline: none;
}
textarea.form-control-custom {
  resize: none;
}

/* INPUT WITH ICONS */
.input-group-custom i {
  z-index: 5;
}

/* BUTTONS */
.btn-gold {
  background-color: #bfa361;
  border: 1px solid #bfa361;
  font-weight: 500;
  font-size: 0.9rem;
  letter-spacing: 0.5px;
  transition: all 0.2s ease;
}
.btn-gold:hover {
  background-color: #ab9052;
  border-color: #ab9052;
  transform: translateY(-1px);
}
.btn-outline-custom {
  border: 1px solid #e2e8f0;
  color: #4a5568;
  background-color: transparent;
  font-weight: 500;
  font-size: 0.9rem;
}
.btn-outline-custom:hover {
  background-color: #f7f6f2;
  color: #111625;
}

/* TỔNG QUAN LIST */
.overview-list li {
  font-size: 0.9rem;
}
.overview-list li i {
  font-size: 1.15rem;
  color: #bfa361 !important;
}

/* THẺ ĐẶC QUYỀN GOLD PRIVILEGE */
.gold-privilege-card {
  background: linear-gradient(135deg, #0e121f 0%, #181f33 100%);
  border-radius: 1rem !important;
  border: none;
}
.gold-privilege-card::after {
  content: '';
  position: absolute;
  width: 150px;
  height: 150px;
  background: radial-gradient(circle, rgba(191,163,97,0.15) 0%, transparent 70%);
  bottom: -50px;
  right: -50px;
}
.bg-dark-muted {
  background-color: rgba(255, 255, 255, 0.1) !important;
}
.bg-gold-gradient {
  background: linear-gradient(90deg, #bfa361 0%, #dfcaa0 100%);
}

/* TIỆN ÍCH CHUNG */
.cursor-pointer {
  cursor: pointer;
}
.text-light-muted {
  color: #cbd5e1;
}

/* TỐI ƯU LANDSCAPE / CO GIÃN ZOOM TO NHỎ TỰ ĐỘNG PHÙ HỢP */
@media (max-width: 767.98px) {
  .hero-banner {
    min-height: auto;
  }
}
</style>