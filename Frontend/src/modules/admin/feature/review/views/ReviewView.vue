<template>
  <div class="container-fluid py-4" style="background-color: #f8f9fc; min-height: 100vh;">
    <h3 class="fw-bold mb-4">Quản lý đánh giá khách hàng</h3>

    <!-- TABS VÀ BỘ LỌC -->
    <div class="card shadow-sm border-0 rounded-3 mb-4">
      <div class="card-header bg-white border-bottom-0 pt-3">
        <ul class="nav nav-tabs card-header-tabs">
          <li class="nav-item" v-for="tab in tabs" :key="tab.value">
            <a class="nav-link cursor-pointer fw-medium" 
               :class="{ active: currentTab === tab.value }" 
               @click="changeTab(tab.value)">
              {{ tab.label }}
            </a>
          </li>
        </ul>
      </div>
      <div class="card-body bg-white border-top">
        <div class="row g-3">
          <div class="col-md-5">
            <div class="input-group">
              <span class="input-group-text bg-white"><i class="bi bi-search"></i></span>
              <input v-model="filters.keyword" @keyup.enter="fetchReviews" type="text" class="form-control" placeholder="Tìm tên KH, email, tên SP, SKU, nội dung...">
            </div>
          </div>
          <div class="col-md-2">
            <select v-model="filters.rating" @change="fetchReviews" class="form-select">
              <option value="">Tất cả số sao</option>
              <option v-for="i in 5" :key="i" :value="i">{{ i }} Sao ⭐</option>
            </select>
          </div>
          <div class="col-md-3">
            <select v-model="filters.hasMedia" @change="fetchReviews" class="form-select">
              <option value="">Tất cả loại đánh giá</option>
              <option :value="true">Có Ảnh / Video</option>
              <option :value="false">Chỉ Text</option>
            </select>
          </div>
          <div class="col-md-2">
            <button @click="fetchReviews" class="btn btn-primary w-100">Lọc dữ liệu</button>
          </div>
        </div>
      </div>
    </div>

    <!-- BẢNG ĐÁNH GIÁ -->
    <div class="card shadow-sm border-0 rounded-3">
      <div class="card-body p-0 table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th style="width: 20%;">Sản phẩm</th>
              <th style="width: 18%;">Khách hàng</th>
              <th style="width: 25%;">Đánh giá & Bình luận</th>
              <th style="width: 12%;">Hình ảnh / Video</th>
              <th style="width: 10%;">Trạng thái</th>
              <th style="width: 15%;" class="text-center">Hành động</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in reviews" :key="item.id">
              <td>
                <div class="fw-bold text-dark">{{ item.productName || 'N/A' }}</div>
                <small class="text-muted">SKU: {{ item.productSku || 'N/A' }}</small>
              </td>
              <td>
                <div class="fw-medium">{{ item.customerName || 'N/A' }}</div>
                <small class="text-muted">{{ item.customerEmail || 'N/A' }}</small>
              </td>
              <td>
                <div class="text-warning">
                  <i v-for="i in item.rating" :key="i" class="bi bi-star-fill"></i>
                </div>
                <div class="text-dark mt-1">{{ item.comment }}</div>
                <small class="text-muted d-block mt-1">{{ new Date(item.createdAt).toLocaleString('vi-VN') }}</small>
                <small v-if="item.rejectedReason" class="text-danger d-block mt-1">
                  <b>Lý do:</b> {{ item.rejectedReason }}
                </small>
              </td>
              <td>
                <button v-if="item.media && item.media.length > 0" @click="openMediaModal(item.media)" class="btn btn-sm btn-outline-info">
                  <i class="bi bi-paperclip"></i> Xem Media ({{ item.media.length }})
                </button>
                <span v-else class="text-muted small">Không có</span>
              </td>
              <td>
                <span class="badge" :class="getStatusBadgeClass(item.approvalStatus)">
                  {{ getStatusText(item.approvalStatus) }}
                </span>
              </td>
              <td class="text-center">
                <!-- Chờ duyệt (0) -> Duyệt hoặc Từ chối -->
                <template v-if="item.approvalStatus === 0">
                  <button @click="handleApprove(item.id)" class="btn btn-sm btn-success me-1" title="Duyệt">
                    <i class="bi bi-check-lg"></i>
                  </button>
                  <button @click="openRejectModal(item.id)" class="btn btn-sm btn-outline-danger" title="Từ chối">
                    <i class="bi bi-x-lg"></i>
                  </button>
                </template>

                <!-- Đã duyệt (1) -> Ẩn -->
                <template v-if="item.approvalStatus === 1">
                  <button @click="handleHide(item.id)" class="btn btn-sm btn-outline-secondary" title="Ẩn bài">
                    <i class="bi bi-eye-slash"></i> Ẩn
                  </button>
                </template>

                <!-- Từ chối (2) hoặc Đã ẩn (3) -> Duyệt lại -->
                <template v-if="item.approvalStatus === 2 || item.approvalStatus === 3">
                  <button @click="handleApprove(item.id)" class="btn btn-sm btn-outline-success" title="Duyệt lại">
                    <i class="bi bi-arrow-counterclockwise"></i> Duyệt lại
                  </button>
                </template>
              </td>
            </tr>
            <tr v-if="reviews.length === 0">
              <td colspan="6" class="text-center py-4 text-muted">Không tìm thấy đánh giá nào</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- MODAL XEM MEDIA -->
    <div v-if="showMediaModal" class="modal d-block" style="background: rgba(0,0,0,0.7); z-index: 1050;">
      <div class="modal-dialog modal-lg modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">Hình ảnh & Video đính kèm</h5>
            <button @click="showMediaModal = false" class="btn-close"></button>
          </div>
          <div class="modal-body d-flex flex-wrap gap-3 justify-content-center">
            <template v-for="m in currentMedia" :key="m.id">
              <img v-if="m.mediaType === 'image' || m.mediaType === 'IMAGE'" :src="m.mediaUrl" class="img-thumbnail" style="max-height: 300px; object-fit: contain;">
              <video v-else :src="m.mediaUrl" controls class="img-thumbnail" style="max-height: 300px;"></video>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- MODAL TỪ CHỐI -->
    <div v-if="showRejectModal" class="modal d-block" style="background: rgba(0,0,0,0.5); z-index: 1050;">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title fw-bold">Từ chối đánh giá</h5>
            <button @click="showRejectModal = false" class="btn-close"></button>
          </div>
          <div class="modal-body">
            <label class="form-label">Lý do từ chối <span class="text-danger">*</span></label>
            <textarea v-model="rejectReason" class="form-control" rows="3" placeholder="Ví dụ: Hình ảnh/video chứa nội dung không hợp lệ..."></textarea>
          </div>
          <div class="modal-footer">
            <button @click="showRejectModal = false" class="btn btn-light">Hủy</button>
            <button @click="submitReject" class="btn btn-danger" :disabled="!rejectReason.trim()">Xác nhận từ chối</button>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';

// 1. SỬA LỖI 2 & 3: Sử dụng alias '@' để trỏ chính xác vị trí file
import { reviewService } from '@/modules/admin/feature/review/services/review.service';
import type { Review, ReviewMedia } from '@/modules/admin/feature/review/types/review.type';
import Swal from 'sweetalert2';

const reviews = ref<Review[]>([]);

// 2. SỬA LỖI 1: Nới lỏng kiểu dữ liệu thành (number | string)
const currentTab = ref<number | string>(''); 
const filters = ref({ keyword: '', rating: '', hasMedia: '' });

const showMediaModal = ref(false);
const currentMedia = ref<ReviewMedia[]>([]);

const showRejectModal = ref(false);
const selectedReviewId = ref<number | null>(null);
const rejectReason = ref('');

const tabs = [
  { label: 'Tất cả', value: '' },
  { label: 'Chờ duyệt', value: 0 },
  { label: 'Đã duyệt', value: 1 },
  { label: 'Từ chối', value: 2 },
  { label: 'Đã ẩn', value: 3 }
];

const Toast = Swal.mixin({ 
  toast: true, 
  position: 'top-end', 
  showConfirmButton: false, 
  timer: 3000,
  timerProgressBar: true
});

onMounted(() => {
  fetchReviews();
});

// 2. SỬA LỖI 1: Đổi tham số val thành (number | string)
const changeTab = (val: number | string) => {
  currentTab.value = val;
  fetchReviews();
};

const fetchReviews = async () => {
  try {
    const params = {
      status: currentTab.value !== '' ? currentTab.value : undefined,
      keyword: filters.value.keyword || undefined,
      rating: filters.value.rating || undefined,
      hasMedia: filters.value.hasMedia !== '' ? filters.value.hasMedia : undefined,
      page: 0,
      size: 50
    };
    const res = await reviewService.getReviews(params);
    reviews.value = res.data.content;
  } catch (error) {
    Toast.fire({ icon: 'error', title: 'Lỗi tải danh sách đánh giá!' });
  }
};

const getStatusText = (status: number) => {
  switch (status) {
    case 0: return 'Chờ duyệt';
    case 1: return 'Đã duyệt';
    case 2: return 'Từ chối';
    case 3: return 'Đã ẩn';
    default: return 'Khác';
  }
};

const getStatusBadgeClass = (status: number) => {
  switch (status) {
    case 0: return 'bg-warning text-dark';
    case 1: return 'bg-success';
    case 2: return 'bg-danger';
    case 3: return 'bg-secondary';
    default: return 'bg-light text-dark';
  }
};

const openMediaModal = (media: ReviewMedia[]) => {
  currentMedia.value = media;
  showMediaModal.value = true;
};

const openRejectModal = (id: number) => {
  selectedReviewId.value = id;
  rejectReason.value = '';
  showRejectModal.value = true;
};

const handleApprove = async (id: number) => {
  try {
    await reviewService.approveReview(id);
    Toast.fire({ icon: 'success', title: 'Đã duyệt thành công!' });
    fetchReviews();
  } catch (error) {
    Toast.fire({ icon: 'error', title: 'Không thể duyệt!' });
  }
};

const submitReject = async () => {
  if (!selectedReviewId.value || !rejectReason.value.trim()) return;
  try {
    await reviewService.rejectReview(selectedReviewId.value, rejectReason.value);
    showRejectModal.value = false;
    Toast.fire({ icon: 'success', title: 'Đã từ chối đánh giá!' });
    fetchReviews();
  } catch (error) {
    Toast.fire({ icon: 'error', title: 'Lỗi khi từ chối!' });
  }
};

const handleHide = async (id: number) => {
  try {
    await reviewService.hideReview(id);
    Toast.fire({ icon: 'success', title: 'Đã ẩn đánh giá!' });
    fetchReviews();
  } catch (error) {
    Toast.fire({ icon: 'error', title: 'Không thể ẩn!' });
  }
};
</script>

<style scoped>
.cursor-pointer { cursor: pointer; }
.nav-tabs .nav-link { color: #495057; border-radius: 0; }
.nav-tabs .nav-link.active { color: #0d6efd; font-weight: 600; border-bottom: 2px solid #0d6efd; background: transparent;}
</style>