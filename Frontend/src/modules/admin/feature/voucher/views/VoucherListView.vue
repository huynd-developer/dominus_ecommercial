<template>
  <div class="card border-0 shadow-sm">
    <div class="card-header bg-white d-flex justify-content-between align-items-center py-3">
      <h5 class="mb-0 fw-bold text-primary">Quản Lý Voucher</h5>
      <RouterLink to="/admin/vouchers/create" class="btn btn-primary">
        <i class="bi bi-plus-lg me-1"></i> Thêm Mới
      </RouterLink>
    </div>
    
    <div class="card-body">
      <div v-if="isLoading" class="text-center py-5">
        <div class="spinner-border text-primary" role="status"></div>
        <p class="mt-2 text-muted">Đang tải dữ liệu...</p>
      </div>

      <div class="table-responsive" v-else>
        <table class="table table-hover align-middle">
          <thead class="table-light">
            <tr>
              <th>Mã Voucher</th>
              <th>Loại - Mức giảm</th>
              <th>Đơn tối thiểu</th>
              <th>Đã dùng / Giới hạn</th>
              <th>Thời hạn</th>
              <th>Trạng thái</th>
              <th>Thao tác</th> <!-- CỘT MỚI -->
            </tr>
          </thead>
          <tbody>
            <tr v-for="v in vouchers" :key="v.id">
              <td class="fw-bold text-success">{{ v.code }}</td>
              <td>
                <span class="badge bg-info text-dark me-1">{{ v.discountType === 'PERCENT' ? '%' : 'VNĐ' }}</span>
                {{ formatMoney(v.discountValue) }}
              </td>
              <td>{{ formatMoney(v.minOrderValue) }}</td>
              <td>{{ v.usedCount }} / {{ v.usageLimit }}</td>
              <td class="small">
                Bắt đầu: {{ formatDate(v.startDate) }}<br>
                Kết thúc: <span :class="{'text-danger fw-bold': isExpired(v.endDate)}">{{ formatDate(v.endDate) }}</span>
              </td>
              <td>
                <span class="badge" :class="v.status === 1 ? 'bg-success' : 'bg-secondary'">
                  {{ v.status === 1 ? 'Hoạt động' : 'Tạm dừng' }}
                </span>
              </td>
              <!-- NÚT THAO TÁC SỬA/XÓA -->
              <td>
                <div class="d-flex gap-2">
                  <RouterLink :to="`/admin/vouchers/edit/${v.id}`" class="btn btn-sm btn-outline-primary" title="Sửa">
                    <i class="bi bi-pencil-square"></i>
                  </RouterLink>
                  <button @click="handleDelete(v.id)" class="btn btn-sm btn-outline-danger" title="Xóa">
                    <i class="bi bi-trash"></i>
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="vouchers.length === 0">
              <td colspan="7" class="text-center py-4 text-muted">Chưa có mã giảm giá nào.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from 'axios';
import Swal from 'sweetalert2';

const vouchers = ref<any[]>([]);
const isLoading = ref(true);

const fetchVouchers = async () => {
  try {
    const token = localStorage.getItem('token');
    const res = await axios.get('http://localhost:8080/api/admin/vouchers', {
      headers: { Authorization: `Bearer ${token}` }
    });
    vouchers.value = res.data;
  } catch (error) {
    console.error("Lỗi lấy danh sách Voucher:", error);
  } finally {
    isLoading.value = false;
  }
};

const handleDelete = async (id: number) => {
  const result = await Swal.fire({
    title: 'Xác nhận xóa?',
    text: "Voucher này sẽ bị ẩn khỏi hệ thống!",
    icon: 'warning',
    showCancelButton: true,
    confirmButtonColor: '#d33',
    cancelButtonColor: '#3085d6',
    confirmButtonText: 'Xóa ngay!',
    cancelButtonText: 'Hủy'
  });

  if (result.isConfirmed) {
    try {
      const token = localStorage.getItem('token');
      await axios.delete(`http://localhost:8080/api/admin/vouchers/${id}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      Swal.fire('Đã xóa!', 'Voucher đã được xóa.', 'success');
      fetchVouchers();
    } catch (error) {
      Swal.fire('Lỗi', 'Không thể xóa voucher này', 'error');
    }
  }
};

const formatMoney = (val: number) => new Intl.NumberFormat('vi-VN').format(val || 0);
const formatDate = (dateStr: string) => {
  if (!dateStr) return '';
  return new Date(dateStr).toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' });
};
const isExpired = (endDate: string) => new Date(endDate) < new Date();

onMounted(fetchVouchers);
</script>