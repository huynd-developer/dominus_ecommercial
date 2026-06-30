<template>
  <div class="main-layout d-flex flex-column min-vh-100 bg-light">
    <header class="bg-white border-bottom sticky-top" style="z-index: 1030;">
      <div class="py-3">
        <div class="container d-flex align-items-center justify-content-between gap-4">
          
          <router-link to="/" class="text-decoration-none">
            <h2 class="mb-0 fw-bold text-uppercase text-dark" style="letter-spacing: 2px;">DOMINUS</h2>
          </router-link>

          <div class="flex-grow-1 mx-lg-5">
            <div class="input-group border border-secondary-subtle rounded-1 overflow-hidden transition-focus">
              <input 
                v-model="searchQuery"
                type="text" 
                class="form-control border-0 shadow-none py-2 px-3" 
                placeholder="Tìm kiếm sản phẩm, thương hiệu, nhóm hương..." 
                @keyup.enter="handleSearch"
              />
              <button class="btn bg-white border-0 px-3 text-secondary" @click="handleSearch">
                <i class="bi bi-search fs-5"></i>
              </button>
            </div>
          </div>

          <div class="d-flex align-items-center gap-4">
            <div class="d-none d-lg-flex align-items-center gap-2">
              <i class="bi bi-telephone fs-4 text-dark"></i>
              <div class="lh-sm">
                <span class="d-block small text-muted">Hotline</span>
                <strong class="d-block text-dark">1900 1234</strong>
              </div>
            </div>

            <div class="d-flex align-items-center gap-2" style="cursor: pointer;">
              <i class="bi bi-person fs-3 text-dark"></i>
              <div class="lh-sm d-none d-md-block">
                <span class="d-block small text-muted">Tài khoản</span>
                <span class="d-block text-dark fw-medium" style="font-size: 14px;">Đăng nhập</span>
              </div>
            </div>

            <div class="d-flex align-items-center gap-2 position-relative" style="cursor: pointer;">
              <div class="position-relative">
                <i class="bi bi-bag fs-3 text-dark"></i>
                <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger border border-white">
                  0
                </span>
              </div>
              <span class="d-none d-md-block text-dark fw-medium ms-1" style="font-size: 14px;">Giỏ hàng</span>
            </div>
          </div>

        </div>
      </div>

      <nav class="navbar navbar-expand-lg navbar-dark bg-black py-0">
        <div class="container">
          <button class="navbar-toggler my-2" type="button" data-bs-toggle="collapse" data-bs-target="#mainMenu">
            <span class="navbar-toggler-icon"></span>
          </button>

          <div class="collapse navbar-collapse" id="mainMenu">
            <ul class="navbar-nav me-auto w-100 justify-content-between fw-semibold text-uppercase" style="font-size: 13px; letter-spacing: 0.5px;">
              <li class="nav-item"><a class="nav-link py-3 px-2 text-white" href="#">Thương hiệu</a></li>
              <li class="nav-item"><a class="nav-link py-3 px-2 text-white" href="#">Nước hoa Nam</a></li>
              <li class="nav-item"><a class="nav-link py-3 px-2 text-white" href="#">Nước hoa Nữ</a></li>
              <li class="nav-item"><a class="nav-link py-3 px-2 text-white" href="#">Unisex</a></li>
              <li class="nav-item"><a class="nav-link py-3 px-2 text-white" href="#">Nước hoa Niche</a></li>
              <li class="nav-item"><a class="nav-link py-3 px-2 text-white" href="#">Chiết 10ml</a></li>
              <li class="nav-item">
                <a class="nav-link py-3 px-2 text-danger d-flex align-items-center gap-1 fw-bold" href="#">
                  <i class="bi bi-lightning-charge-fill"></i> Flash Sale
                </a>
              </li>
            </ul>
          </div>
        </div>
      </nav>
    </header>

    <main class="flex-grow-1">
      <router-view />
    </main>

    <footer class="bg-white border-top py-4 mt-auto">
      <div class="container text-center text-muted small">
        &copy; 2026 DOMINUS PERFUME. All rights reserved.
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const searchQuery = ref('');

const handleSearch = () => {
  if (!searchQuery.value.trim()) return;
  console.log('Từ khóa tìm kiếm sản phẩm trên Header:', searchQuery.value);
  // Sau này Huy viết xong API/Route tìm kiếm, bạn kích hoạt dòng này:
  // router.push({ path: '/products', query: { q: searchQuery.value } });
};
</script>

<style scoped>
.transition-focus:focus-within {
  border-color: #111 !important;
  box-shadow: 0 0 0 1px #111;
}
.nav-link {
  transition: opacity 0.2s ease;
}
.nav-link:hover {
  opacity: 0.8;
}
</style>