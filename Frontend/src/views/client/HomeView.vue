<template>
  <div class="home-view py-5">
    <div class="container">
      
      <section class="mb-5 bg-white p-4 rounded-3 border border-danger border-2 shadow-sm position-relative">
        <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-3">
          <div class="d-flex align-items-center gap-3">
            <h3 class="fw-bold mb-0 text-danger text-uppercase d-flex align-items-center gap-1">
              <i class="bi bi-lightning-charge-fill"></i> Flash Sale
            </h3>
            <div class="d-flex gap-1 fw-bold text-white align-items-center" style="font-size: 14px;">
              <span class="bg-danger rounded px-2 py-1 shadow-sm">00</span><span class="text-danger">:</span>
              <span class="bg-danger rounded px-2 py-1 shadow-sm">00</span><span class="text-danger">:</span>
              <span class="bg-danger rounded px-2 py-1 shadow-sm">00</span>
            </div>
          </div>
          <span class="text-danger small fw-semibold" style="cursor: pointer;">Xem thêm <i class="bi bi-chevron-right"></i></span>
        </div>

        <div class="row g-3">
          <div class="col-6 col-md-3" v-for="n in 4" :key="'flash-' + n">
            <div class="card h-100 border-0 bg-light-subtle skeleton-card">
              <div class="skeleton-image d-flex align-items-center justify-content-center text-muted bg-secondary-subtle" style="height: 200px; border-radius: 6px 6px 0 0;">
                <i class="bi bi-image fs-2 text-secondary opacity-50"></i>
              </div>
              <div class="card-body bg-white p-3 border-top">
                <div class="skeleton-line w-70 mb-2"></div>
                <div class="skeleton-line w-40 mb-3"></div>
                <div class="d-flex justify-content-between align-items-center">
                  <div class="skeleton-line w-50 bg-danger"></div>
                  <div class="skeleton-line w-30"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="mb-5">
        <div class="d-flex justify-content-between align-items-end mb-4">
          <h4 class="fw-bold text-uppercase mb-0 border-start border-dark border-4 ps-2">Sản phẩm mới về</h4>
          <span class="text-muted small">Xem tất cả</span>
        </div>

        <div class="row g-3">
          <div class="col-6 col-md-3" v-for="n in 4" :key="'new-' + n">
            <div class="card h-100 border-0 shadow-sm skeleton-card">
              <div class="skeleton-image d-flex align-items-center justify-content-center text-muted bg-secondary-subtle" style="height: 220px;">
                <i class="bi bi-box-seam fs-2 text-secondary opacity-50"></i>
              </div>
              <div class="card-body bg-white p-3">
                <div class="skeleton-line w-85 mb-2"></div>
                <div class="skeleton-line w-50 mb-3"></div>
                <div class="skeleton-line w-40 bg-dark"></div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="mb-5">
        <div class="d-flex justify-content-between align-items-end mb-4">
          <h4 class="fw-bold text-uppercase mb-0 border-start border-dark border-4 ps-2">Sản phẩm nổi bật</h4>
          <span class="text-muted small">Xem tất cả</span>
        </div>

        <div class="row g-3">
          <div class="col-6 col-md-3" v-for="n in 4" :key="'hot-' + n">
            <div class="card h-100 border-0 shadow-sm skeleton-card">
              <div class="skeleton-image d-flex align-items-center justify-content-center text-muted bg-secondary-subtle" style="height: 220px;">
                <i class="bi bi-stars fs-2 text-secondary opacity-50"></i>
              </div>
              <div class="card-body bg-white p-3">
                <div class="skeleton-line w-80 mb-2"></div>
                <div class="d-flex gap-1 mb-3 text-warning">
                  <i class="bi bi-star-fill small"></i>
                  <i class="bi bi-star-fill small"></i>
                  <i class="bi bi-star-fill small"></i>
                  <i class="bi bi-star-fill small"></i>
                  <i class="bi bi-star-fill small"></i>
                </div>
                <div class="skeleton-line w-45 bg-dark"></div>
              </div>
            </div>
          </div>
        </div>
      </section>

    </div>
  </div>
</template>

<script setup lang="ts">
// Hiện tại chỉ dựng khung xương (Skeleton UI), chưa khai báo mảng sản phẩm.
// Sang các task sau khi có API, bạn chỉ cần dùng v-if/v-else thế chỗ cho các khối này.
</script>

<style scoped>
/* Hiệu ứng nhấp nháy chuyển động nhẹ cho khung xương để tạo cảm giác chuyên nghiệp */
@keyframes pulse {
  0% { opacity: 0.6; }
  50% { opacity: 1; }
  100% { opacity: 0.6; }
}

.skeleton-card {
  overflow: hidden;
  border-radius: 8px;
}

.skeleton-image {
  animation: pulse 1.5s infinite ease-in-out;
}

.skeleton-line {
  height: 14px;
  background-color: #e9ecef;
  border-radius: 4px;
  animation: pulse 1.5s infinite ease-in-out;
}

/* Các tỉ lệ chiều rộng của vạch giả lập */
.w-30 { width: 30%; }
.w-40 { width: 40%; }
.w-45 { width: 45%; }
.w-50 { width: 50%; }
.w-70 { width: 70%; }
.w-80 { width: 80%; }
.w-85 { width: 85%; }
</style>