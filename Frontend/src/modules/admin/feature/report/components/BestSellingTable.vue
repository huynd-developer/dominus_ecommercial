<template>
  <div class="card border-0 shadow-sm">
    <div class="card-header bg-white border-0 py-3">
      <div class="d-flex align-items-center justify-content-between gap-3">
        <div>
          <h5 class="mb-1 fw-bold">Sản phẩm bán chạy nhất</h5>
          <div class="text-muted small">
            Chỉ tính các đơn đã hoàn thành trong khoảng thời gian đã chọn
          </div>
        </div>

        <span v-if="items.length > 0" class="badge bg-dark-subtle text-dark">
          {{ items.length }} sản phẩm
        </span>
      </div>
    </div>

    <div class="card-body p-0">
      <div v-if="items.length === 0" class="empty-box">
        Chưa có sản phẩm bán chạy trong khoảng thời gian này
      </div>

      <div v-else class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th style="width: 70px">Top</th>
              <th>Sản phẩm</th>
              <th>Thương hiệu</th>
              <th class="text-end">Đã bán</th>
              <th class="text-end">
                Doanh thu
                <i 
                  class="bi bi-info-circle ms-1 text-muted" 
                  style="cursor: help; font-size: 14px;" 
                  title="Doanh thu của từng sản phẩm, chưa bao gồm các khuyến mãi giảm giá trên toàn đơn hàng.">
                </i>
              </th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="(item, index) in safeItems" :key="item.productId || index">
              <td>
                <span class="rank-badge" :class="getRankClass(index)">
                  #{{ index + 1 }}
                </span>
              </td>

              <td>
                <div class="d-flex align-items-center gap-3">
                  <div class="image-box">
                    <!-- ƯU TIÊN TUYỆT ĐỐI PRIMARY IMAGE ĐỂ ĐỒNG BỘ VỚI TRANG QUẢN LÝ -->
                    <img
                      v-if="getBestSellingImageUrl(item)"
                      :src="getBestSellingImageUrl(item)"
                      class="product-img custom-img-hover"
                      loading="lazy"
                      decoding="async"
                      alt="Ảnh sản phẩm"
                      @error="handleImageError"
                    />

                    <div v-else class="product-img placeholder-img">
                      <i class="bi bi-image"></i>
                    </div>
                  </div>

                  <div class="min-w-0">
                    <div class="fw-semibold product-name">
                      {{ item.productName || "Sản phẩm" }}
                    </div>
                    <div class="text-muted small">
                      ID: {{ item.productId || "-" }}
                    </div>
                  </div>
                </div>
              </td>

              <td>
                {{ item.brandName || "Không rõ thương hiệu" }}
              </td>

              <td class="text-end fw-semibold">
                {{ formatNumber(item.totalSold) }}
              </td>

              <td class="text-end fw-bold">
                {{ formatMoney(item.revenue) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import type { BestSellingProductResponse } from "../types/report.type";
import api from "@/common/api";

const props = defineProps<{
  items: BestSellingProductResponse[];
}>();

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";
const productDetailMap = ref<Record<number, any>>({});

const getImageUrl = (url?: string) => {
  if (!url) return "";
  if (url.startsWith("http") || url.startsWith("blob:") || url.startsWith("data:")) return url;
  return url.startsWith("/") ? `${API_URL}${url}` : `${API_URL}/${url}`;
};

const fetchMissingImages = async () => {
  if (!Array.isArray(props.items)) return;
  for (const item of props.items) {
    const pId = Number(item.productId || 0);
    if (pId > 0 && !productDetailMap.value[pId]) {
      try {
        const res = await api.get(`/v1/products/${pId}`);
        const data = res.data?.data || res.data;
        if (data) {
          productDetailMap.value[pId] = data;
        }
      } catch (e) {}
    }
  }
};

watch(() => props.items, () => {
  fetchMissingImages();
}, { immediate: true, deep: true });

// HÀM ƯU TIÊN LẤY PRIMARY IMAGE ĐỂ KHỚP VỚI BÊN QUẢN LÝ SẢN PHẨM
const getBestSellingImageUrl = (item: any) => {
  if (!item) return "";
  
  const pId = Number(item.productId || 0);
  const detail = pId > 0 ? productDetailMap.value[pId] : null;

  // 1. Kiểm tra từ chi tiết sản phẩm trước (lấy chuẩn primaryImageUrl y hệt ProductList)
  if (detail) {
    const detailUrl = detail.primaryImageUrl || detail.PrimaryImageUrl || detail.imageUrl || detail.ImageUrl || detail.mainImage;
    if (detailUrl) return getImageUrl(detailUrl);
    
    if (Array.isArray(detail.images) && detail.images.length > 0) {
      const primaryObj = detail.images.find((img: any) => Boolean(img?.isPrimary || img?.is_primary));
      const fallbackObj = primaryObj || detail.images[0];
      const imgUrl = fallbackObj?.imageUrl || fallbackObj?.url || fallbackObj;
      if (imgUrl) return getImageUrl(imgUrl);
    }
  }

  // 2. Fallback về item của bảng bán chạy nhưng đặt primaryImageUrl lên đầu tiên
  const rawUrl =
    item.primaryImageUrl ||
    item.PrimaryImageUrl ||
    item.imageUrl ||
    item.ImageUrl ||
    item.image ||
    item.Image ||
    item.thumbnailUrl ||
    item.mainImage ||
    "";
  
  return getImageUrl(rawUrl);
};

const toNumber = (value: unknown) => {
  const numberValue = Number(value ?? 0);
  return Number.isFinite(numberValue) ? numberValue : 0;
};

const safeItems = computed(() => {
  return Array.isArray(props.items) ? props.items : [];
});

const formatMoney = (value: unknown) => {
  return toNumber(value).toLocaleString("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  });
};

const formatNumber = (value: unknown) => {
  return toNumber(value).toLocaleString("vi-VN");
};

const getRankClass = (index: number) => {
  if (index === 0) return "rank-gold";
  if (index === 1) return "rank-silver";
  if (index === 2) return "rank-bronze";
  return "";
};

const FALLBACK_IMAGE = "data:image/svg+xml;charset=UTF-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%22200%22%20height%3D%22200%22%3E%3Crect%20width%3D%22100%25%22%20height%3D%22100%25%22%20fill%3D%22%23f1f5f9%22%2F%3E%3Ctext%20x%3D%2250%25%22%20y%3D%2250%25%22%20dominant-baseline%3D%22middle%22%20text-anchor%3D%22middle%22%20fill%3D%22%2394a3b8%22%20font-family%3D%22Arial%22%20font-size%3D%2214%22%3EKh%C3%B4ng%20c%C3%B3%20%E1%BA%A3nh%3C%2Ftext%3E%3C%2Fsvg%3E";

const handleImageError = (event: Event) => {
  const target = event.target as HTMLImageElement | null;
  if (target) {
    target.src = FALLBACK_IMAGE;
  }
};
</script>

<style scoped>
.image-box {
  width: 55px;
  height: 55px;
}

.product-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.custom-img-hover {
  transition: 0.25s;
}

.custom-img-hover:hover {
  transform: scale(1.08);
}

.placeholder-img {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  color: #94a3b8;
  font-size: 20px;
}

.product-name {
  max-width: 360px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 42px;
  height: 30px;
  border-radius: 999px;
  background: #111827;
  color: #fff;
  font-weight: 700;
  font-size: 13px;
}

.rank-gold {
  background: #92400e;
}

.rank-silver {
  background: #4b5563;
}

.rank-bronze {
  background: #7c2d12;
}

.empty-box {
  padding: 50px;
  text-align: center;
  color: #6b7280;
  background: #f9fafb;
}

.min-w-0 {
  min-width: 0;
}
</style>