<template>
  <section class="product-reviews">
    <div class="review-header">
      <div>
        <h3>Đánh giá sản phẩm</h3>
        <p>Phản hồi thật từ khách hàng đã mua sản phẩm</p>
      </div>

      <button
        type="button"
        class="btn-refresh-review"
        :disabled="loading"
        @click="fetchReviews(0)"
      >
        <span
          v-if="loading"
          class="spinner-border spinner-border-sm me-1"
        ></span>
        Làm mới
      </button>
    </div>

    <div class="review-summary-card">
      <div class="summary-score">
        <div class="score-number">
          {{ formatAverage(summary?.averageRating) }}
        </div>

        <div class="score-stars">
          <i
            v-for="star in 5"
            :key="star"
            class="bi"
            :class="star <= roundedAverage ? 'bi-star-fill' : 'bi-star'"
          ></i>
        </div>

        <div class="summary-count">
          {{ summary?.reviewCount || 0 }} đánh giá
        </div>
      </div>

      <div class="summary-breakdown">
        <div
          v-for="row in ratingRows"
          :key="row.rating"
          class="rating-row"
        >
          <span>{{ row.rating }} sao</span>

          <div class="progress">
            <div
              class="progress-bar"
              :style="{ width: getPercent(row.count) + '%' }"
            ></div>
          </div>

          <strong>{{ row.count }}</strong>
        </div>
      </div>
    </div>

    <div v-if="loading" class="review-loading">
      <div class="spinner-border"></div>
      <p>Đang tải đánh giá...</p>
    </div>

    <div v-else-if="reviews.length === 0" class="empty-review">
      Chưa có đánh giá nào cho sản phẩm này.
    </div>

    <div v-else class="review-list">
      <div
        v-for="review in reviews"
        :key="review.reviewId"
        class="review-item"
      >
        <div class="review-item-top">
          <div>
            <div class="customer-name">
              {{ review.customerName || "Khách hàng" }}
            </div>

            <div class="review-date">
              {{ formatDate(review.createdAt) }}
            </div>
          </div>

          <div class="review-stars">
            <i
              v-for="star in 5"
              :key="star"
              class="bi"
              :class="star <= review.rating ? 'bi-star-fill' : 'bi-star'"
            ></i>
          </div>
        </div>

        <div v-if="review.comment" class="review-comment">
          {{ review.comment }}
        </div>

        <div v-else class="review-comment empty-comment">
          Khách hàng không để lại bình luận.
        </div>

        <!-- ĐÃ THÊM: Khu vực hiển thị Ảnh/Video đánh giá -->
        <div v-if="review.mediaFiles && review.mediaFiles.length > 0" class="review-media-list mt-3">
          <div v-for="(media, index) in review.mediaFiles" :key="index" class="media-item">
            <!-- Nếu là Video -->
            <video 
              v-if="media.type === 'video' || (media.url && media.url.match(/\.(mp4|webm|ogg)$/i)) || (typeof media === 'string' && media.match(/\.(mp4|webm|ogg)$/i))"
              :src="media.url || media"
              controls
              class="media-preview"
            ></video>
            
            <!-- Nếu là Ảnh -->
            <img 
              v-else
              :src="media.url || media" 
              class="media-preview img-thumbnail-custom" 
              alt="Ảnh đánh giá" 
              @click="openMedia(media.url || media)"
            />
          </div>
        </div>

      </div>
    </div>

    <div
      v-if="!loading && totalPages > 1"
      class="review-pagination"
    >
      <button
        type="button"
        class="review-page-btn"
        :disabled="pageNumber <= 0"
        @click="fetchReviews(pageNumber - 1)"
      >
        Trước
      </button>

      <span>
        Trang {{ pageNumber + 1 }} / {{ totalPages }}
      </span>

      <button
        type="button"
        class="review-page-btn"
        :disabled="pageNumber + 1 >= totalPages"
        @click="fetchReviews(pageNumber + 1)"
      >
        Sau
      </button>
    </div>

    <div v-if="errorMessage" class="review-error">
      {{ errorMessage }}
    </div>

    <!-- ĐÃ THÊM: Popup xem ảnh to -->
    <div v-if="showMediaModal" class="media-modal-backdrop" @click="showMediaModal = false">
      <div class="media-modal-content" @click.stop>
        <button class="btn-close-modal" @click="showMediaModal = false">×</button>
        <img :src="selectedMediaUrl" class="img-fluid" alt="Ảnh phóng to" />
      </div>
    </div>

  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { productReviewService } from "../services/productReview.service";
import type {
  PageResponse,
  ProductReviewSummaryResponse,
  PublicProductReviewResponse,
} from "../types/product-review.type";

const props = defineProps<{
  productId: number;
}>();

const emit = defineEmits<{
  (e: "summary-loaded", summary: ProductReviewSummaryResponse): void;
}>();

const reviews = ref<PublicProductReviewResponse[]>([]);
const summary = ref<ProductReviewSummaryResponse | null>(null);

const loading = ref(false);
const errorMessage = ref("");

const pageNumber = ref(0);
const pageSize = ref(5);
const totalPages = ref(0);
const totalElements = ref(0);

// ĐÃ THÊM: Biến quản lý trạng thái hiển thị ảnh to
const selectedMediaUrl = ref<string | undefined>("");
const showMediaModal = ref(false);

// ĐÃ THÊM: Hàm mở ảnh to
const openMedia = (url: string) => {
  selectedMediaUrl.value = url;
  showMediaModal.value = true;
};

const extractContent = <T,>(data: PageResponse<T> | T[]): T[] => {
  if (Array.isArray(data)) {
    totalPages.value = 1;
    totalElements.value = data.length;
    return data;
  }

  pageNumber.value = data.page?.number ?? data.number ?? 0;
  totalPages.value = data.page?.totalPages ?? data.totalPages ?? 0;
  totalElements.value = data.page?.totalElements ?? data.totalElements ?? 0;

  return data.content || [];
};

const roundedAverage = computed(() => {
  return Math.round(summary.value?.averageRating || 0);
});

const ratingRows = computed(() => {
  return [
    {
      rating: 5,
      count: summary.value?.fiveStarCount || 0,
    },
    {
      rating: 4,
      count: summary.value?.fourStarCount || 0,
    },
    {
      rating: 3,
      count: summary.value?.threeStarCount || 0,
    },
    {
      rating: 2,
      count: summary.value?.twoStarCount || 0,
    },
    {
      rating: 1,
      count: summary.value?.oneStarCount || 0,
    },
  ];
});

const fetchReviews = async (page = pageNumber.value) => {
  if (!props.productId || props.productId <= 0) {
    reviews.value = [];
    summary.value = null;
    return;
  }

  try {
    loading.value = true;
    errorMessage.value = "";

    const [summaryRes, reviewsRes] = await Promise.all([
      productReviewService.getSummary(props.productId),
      productReviewService.getReviews(props.productId, {
        page,
        size: pageSize.value,
      }),
    ]);

    summary.value = summaryRes.data;
    // Chú ý: Dữ liệu review.mediaFiles phải được Backend trả về sẵn ở đây
    reviews.value = extractContent<PublicProductReviewResponse>(reviewsRes.data);

    emit("summary-loaded", summaryRes.data);
  } catch (error: any) {
    reviews.value = [];

    errorMessage.value =
      error?.response?.data?.message ||
      error?.message ||
      "Không tải được đánh giá sản phẩm";
  } finally {
    loading.value = false;
  }
};

const getPercent = (count: number) => {
  const total = summary.value?.reviewCount || 0;

  if (total <= 0) {
    return 0;
  }

  return Math.round((count / total) * 100);
};

const formatAverage = (value: number | null | undefined) => {
  return Number(value || 0).toFixed(1);
};

const formatDate = (value: string | null | undefined) => {
  if (!value) {
    return "-";
  }

  return new Date(value).toLocaleDateString("vi-VN");
};

onMounted(() => {
  fetchReviews(0);
});

watch(
  () => props.productId,
  () => {
    pageNumber.value = 0;
    fetchReviews(0);
  }
);
</script>

<style scoped>
.product-reviews {
  margin-top: 40px;
  background: #ffffff;
  border-radius: 18px;
  padding: 28px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
}

.review-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 22px;
}

.review-header h3 {
  margin: 0;
  color: #0a142f;
  font-size: 22px;
  font-weight: 700;
}

.review-header p {
  margin: 4px 0 0;
  color: #718096;
  font-size: 14px;
}

.btn-refresh-review {
  border: 1px solid #0a142f;
  background: #ffffff;
  color: #0a142f;
  border-radius: 999px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 600;
}

.btn-refresh-review:hover:not(:disabled) {
  background: #0a142f;
  color: #ffffff;
}

.review-summary-card {
  display: grid;
  grid-template-columns: 230px 1fr;
  gap: 24px;
  background: #faf8f4;
  border-radius: 16px;
  padding: 22px;
  margin-bottom: 22px;
}

.summary-score {
  text-align: center;
  border-right: 1px solid #e5e7eb;
  padding-right: 22px;
}

.score-number {
  font-size: 46px;
  font-weight: 800;
  color: #0a142f;
}

.score-stars,
.review-stars {
  color: #bd9a5f;
  white-space: nowrap;
}

.summary-count {
  color: #718096;
  font-size: 13px;
  margin-top: 6px;
}

.summary-breakdown {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 9px;
}

.rating-row {
  display: grid;
  grid-template-columns: 60px 1fr 40px;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #4a5568;
}

.progress {
  height: 8px;
  background: #e5e7eb;
  border-radius: 999px;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: #bd9a5f;
}

.review-loading {
  text-align: center;
  padding: 46px 20px;
  color: #718096;
}

.empty-review {
  text-align: center;
  padding: 46px 20px;
  background: #f9fafb;
  border-radius: 14px;
  color: #718096;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.review-item {
  border: 1px solid #eeeeee;
  border-radius: 16px;
  padding: 16px;
}

.review-item-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 10px;
}

.customer-name {
  font-weight: 700;
  color: #0a142f;
}

.review-date {
  color: #718096;
  font-size: 13px;
  margin-top: 2px;
}

.review-comment {
  color: #374151;
  line-height: 1.6;
  font-size: 14px;
}

.empty-comment {
  color: #9ca3af;
  font-style: italic;
}

.review-pagination {
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #718096;
  font-size: 13px;
}

.review-page-btn {
  border: 1px solid #e5e7eb;
  background: #ffffff;
  border-radius: 999px;
  padding: 7px 14px;
  font-weight: 700;
  color: #0a142f;
}

.review-page-btn:hover:not(:disabled) {
  border-color: #bd9a5f;
  color: #bd9a5f;
}

.review-page-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.review-error {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #fff1f2;
  color: #b91c1c;
  border: 1px solid #fecdd3;
  font-size: 14px;
}

@media (max-width: 768px) {
  .review-header,
  .review-item-top {
    flex-direction: column;
    align-items: stretch;
  }

  .review-summary-card {
    grid-template-columns: 1fr;
  }

  .summary-score {
    border-right: none;
    border-bottom: 1px solid #e5e7eb;
    padding-right: 0;
    padding-bottom: 18px;
  }
}

/* =========================================
   ĐÃ THÊM: CSS cho phần hiển thị Ảnh/Video 
   ========================================= */
.review-media-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 14px;
}

.media-item {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease;
  position: relative;
}

.media-item:hover {
  transform: scale(1.05);
  border-color: #bd9a5f;
  z-index: 10;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.media-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* CSS cho Popup xem ảnh to */
.media-modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999999; 
  padding: 20px;
  backdrop-filter: blur(4px);
}

.media-modal-content {
  position: relative;
  max-width: 900px;
  max-height: 90vh;
  display: flex;
  justify-content: center;
  align-items: center;
}

.media-modal-content img {
  max-width: 100%;
  max-height: 90vh;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 10px 40px rgba(0,0,0,0.3);
}

.btn-close-modal {
  position: absolute;
  top: -45px;
  right: -10px;
  background: rgba(255,255,255,0.2);
  color: white;
  border: none;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  font-size: 24px;
  font-weight: bold;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.btn-close-modal:hover {
  background: #dc2626; 
}
</style>