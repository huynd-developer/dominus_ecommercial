<template>
  <header class="top-header">
    <div class="header-search">
      <i class="bi bi-search"></i>
      <input type="text" class="form-control" placeholder="Tìm kiếm toàn hệ thống...">
    </div>

    <div class="header-tools">
      <div class="icon-group">
        <i class="bi bi-bell position-relative">
          <span class="position-absolute top-0 start-100 translate-middle p-1 bg-danger border border-light rounded-circle"></span>
        </i>
        <i class="bi bi-chat-left-text"></i>
      </div>
      <div class="vertical-line"></div>
      <div class="user-profile">
        <img src="https://ui-avatars.com/api/?name=Admin+Dominus&background=0D6EFD&color=fff" alt="Avatar">
        <div class="user-info">
          <span class="name">Quản lý hệ thống</span>
          <span class="role">Admin</span>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.top-header {
  background-color: #ffffff; /* Nền trắng đồng bộ */
  height: 65px;
  border-bottom: 1px solid #e9ecef;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30px;
  width: 100%;
}

.header-search {
  position: relative;
  width: 320px;
}

.header-search input {
  border-radius: 10px;
  padding-left: 38px;
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  height: 38px;
}

.header-search input:focus {
  background-color: #fff;
  border-color: #0d6efd;
}

.header-search i {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #6c757d;
}

.header-tools {
  display: flex;
  align-items: center;
  gap: 20px;
}

.icon-group {
  display: flex;
  gap: 18px;
}

.icon-group i {
  font-size: 1.25rem;
  color: #495057;
  cursor: pointer;
}

.vertical-line {
  width: 1px;
  height: 25px;
  background-color: #dee2e6;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-profile img {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  object-fit: cover;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-info .name {
  font-size: 0.95rem;
  font-weight: 600;
  color: #212529;
}

.user-info .role {
  font-size: 0.8rem;
  color: #6c757d;
}
</style>