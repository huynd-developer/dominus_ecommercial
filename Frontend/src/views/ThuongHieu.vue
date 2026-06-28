<template>
  <AdminLayout>
    <div class="container-fluid p-0">
      <h4 class="mb-4 fw-bold text-dark text-uppercase tracking-wider">Quản lý Thương hiệu</h4>

      <div class="card p-4 custom-card">
        <div class="row g-3 mb-4 align-items-end">
          <div class="col-md-3">
            <label class="form-label fw-semibold text-secondary small">Trạng thái</label>
            <select class="form-select border-light-subtle">
              <option value="">Tất cả</option>
              <option value="1">Đang hoạt động</option>
              <option value="0">Ngừng hoạt động</option>
            </select>
          </div>
          <div class="col-md-4">
            <label class="form-label fw-semibold text-secondary small">Tìm kiếm nhanh</label>
            <input type="text" class="form-control border-light-subtle" placeholder="Nhập tên thương hiệu...">
          </div>
          <div class="col-md-5 text-end">
            <button class="btn btn-light border me-2 px-3"><i class="bi bi-arrow-clockwise"></i> Làm mới</button>
            <button class="btn btn-primary px-3"><i class="bi bi-plus-lg"></i> Thêm Mới</button>
          </div>
        </div>

        <div class="table-responsive">
          <table class="table table-hover align-middle custom-table">
            <thead>
              <tr class="text-center text-secondary text-uppercase small fw-bold">
                <th width="8%">STT</th>
                <th width="22%" class="text-start">Tên Thương Hiệu</th>
                <th width="40%" class="text-start">Mô Tả Chi Tiết</th>
                <th width="15%">Trạng Thái</th>
                <th width="15%">Thao Tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in listThuongHieu" :key="item.id" class="text-center">
                <td class="fw-bold text-secondary">{{ index + 1 }}</td>
                <td class="text-start fw-bold text-dark">{{ item.tenThuongHieu }}</td>
                <td class="text-start text-muted">{{ item.moTa }}</td>
                <td>
                  <span :class="item.trangThai === 1 ? 'badge-active' : 'badge-inactive'">
                    {{ item.trangThai === 1 ? 'Đang hoạt động' : 'Ngừng hoạt động' }}
                  </span>
                </td>
                <td>
                  <button class="btn btn-sm btn-action text-warning me-1" title="Sửa"><i class="bi bi-pencil-square"></i></button>
                  <button class="btn btn-sm btn-action text-danger" title="Xóa"><i class="bi bi-trash3"></i></button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <nav class="mt-4">
          <ul class="pagination pagination-sm justify-content-end mb-0">
            <li class="page-item disabled"><a class="page-link" href="#">Trước</a></li>
            <li class="page-item active"><a class="page-link" href="#">1</a></li>
            <li class="page-item"><a class="page-link" href="#">2</a></li>
            <li class="page-item"><a class="page-link" href="#">Sau</a></li>
          </ul>
        </nav>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref } from 'vue'
import AdminLayout from '../layouts/AdminLayout.vue' // QUAN TRỌNG: Import layout ở đây

// Data mẫu đổ vào bảng
const listThuongHieu = ref([
  { id: 1, tenThuongHieu: 'Gucci', moTa: 'Thương hiệu thời trang cao cấp xa xỉ biểu tượng toàn cầu từ Ý.', trangThai: 1 },
  { id: 2, tenThuongHieu: 'Chanel', moTa: 'Hãng thời trang, nước hoa cao cấp, quý phái bậc nhất nước Pháp.', trangThai: 1 },
  { id: 3, tenThuongHieu: 'Hermès', moTa: 'Nổi tiếng thế giới với đồ da thủ công tinh xảo và túi xách Birkin giá trị.', trangThai: 1 },
  { id: 4, tenThuongHieu: 'Nike', moTa: 'Thương hiệu giày thể thao, trang phục năng động dẫn đầu thị trường.', trangThai: 1 },
  { id: 5, tenThuongHieu: 'Adidas', moTa: 'Đại kình địch của Nike với phong cách thiết kế ba sọc kinh điển.', trangThai: 0 },
  { id: 6, tenThuongHieu: 'Louis Vuitton', moTa: 'Họa tiết Monogram huyền thoại chuyên trị các dòng vali, túi xách thượng lưu.', trangThai: 1 }
])
</script>

<style scoped>
.custom-card { border: none; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.03); background: #fff; }
.custom-table thead th { background-color: #f8f9fa; padding: 14px; border-bottom: 1px solid #edf2f7; font-size: 0.8rem;}
.custom-table tbody td { padding: 14px; border-bottom: 1px solid #f1f3f5; font-size: 0.9rem; }
.badge-active { background-color: #e6f4ea; color: #137333; padding: 5px 12px; border-radius: 20px; font-size: 0.8rem; font-weight: 600; }
.badge-inactive { background-color: #fce8e6; color: #c5221f; padding: 5px 12px; border-radius: 20px; font-size: 0.8rem; font-weight: 600; }
.btn-action { background-color: #f8f9fa; border: 1px solid #e9ecef; padding: 4px 8px; }
.btn-action:hover { background-color: #e9ecef; }
</style>