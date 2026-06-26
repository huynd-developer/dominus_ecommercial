<template>
  <nav class="sidebar">
    <a href="#" class="brand-logo">
      <i class="bi bi-crown-fill text-primary"></i> <span>DOMI</span>NUS
    </a>
    <ul class="nav flex-column">
      <li class="nav-item"><a class="nav-link" href="#"><i class="bi bi-speedometer2"></i> Tổng quan</a></li>
      <li class="nav-item"><a class="nav-link" href="#"><i class="bi bi-cart3"></i> Đơn hàng</a></li>
      <li class="nav-item"><a class="nav-link" href="#"><i class="bi bi-box-seam"></i> Sản phẩm</a></li>
      <li class="nav-item"><a class="nav-link active" href="#"><i class="bi bi-tags"></i> Thương hiệu</a></li>
      <li class="nav-item"><a class="nav-link" href="#"><i class="bi bi-people"></i> Khách hàng</a></li>
      <li class="nav-item"><a class="nav-link" href="#"><i class="bi bi-person-badge"></i> Nhân viên</a></li>
      <li class="nav-item"><a class="nav-link" href="#"><i class="bi bi-bar-chart-line"></i> Thống kê</a></li>
      <hr class="sidebar-divider">
      <li class="nav-item"><a class="nav-link text-danger" href="#"><i class="bi bi-box-arrow-right"></i> Đăng xuất</a></li>
    </ul>
  </nav>
</template>

<style scoped>
.sidebar {
  width: 250px;
  height: 100vh;
  background-color: #ffffff;
  position: fixed;
  top: 0;
  left: 0;
  padding-top: 20px;
  z-index: 1000;
  border-right: 1px solid #e9ecef;
}
.sidebar .brand-logo {
  text-align: center;
  font-size: 1.5rem;
  font-weight: 800;
  letter-spacing: 1px;
  color: #212529;
  margin-bottom: 25px;
  text-decoration: none;
  display: block;
  border-bottom: 1px solid #f1f3f5;
  padding-bottom: 20px;
}
.sidebar .brand-logo span { color: #0d6efd; }
.sidebar-divider { border-color: #e9ecef; margin: 15px 0; }
.sidebar .nav-link {
  color: #495057;
  padding: 12px 20px;
  font-size: 0.95rem;
  display: flex;
  align-items: center;
  text-decoration: none;
  font-weight: 500;
  border-radius: 0 25px 25px 0;
  margin-right: 15px;
}
.sidebar .nav-link i { margin-right: 12px; font-size: 1.1rem; }
.sidebar .nav-link:hover, .sidebar .nav-link.active {
  background-color: #e7f1ff;
  color: #0d6efd;
}
</style>