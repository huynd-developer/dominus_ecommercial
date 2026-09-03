<template>
  <div class="card border-0 shadow-sm">
    <div class="card-header bg-white border-0 py-3">
      <div class="d-flex align-items-center justify-content-between gap-3">
        <div>
          <h5 class="mb-1 fw-bold">Sản phẩm bán chạy nhất</h5>

          <div class="text-muted small">
            Tính theo các giao dịch bán đã hoàn thành trong khoảng thời gian đã
            chọn
          </div>
        </div>

        <span
          v-if="safeItems.length > 0"
          class="badge bg-dark-subtle text-dark"
        >
          {{ safeItems.length }} sản phẩm
        </span>
      </div>
    </div>

    <div class="card-body p-0">
      <div v-if="safeItems.length === 0" class="empty-box">
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
                Doanh thu sản phẩm

                <i
                  class="bi bi-info-circle ms-1 text-muted"
                  style="cursor: help; font-size: 14px"
                  title="Doanh thu bán của sản phẩm đã tính giảm giá trên sản phẩm và phần voucher toàn đơn được phân bổ theo tỷ lệ. Không trừ các khoản hoàn tiền phát sinh sau bán."
                ></i>
              </th>
            </tr>
          </thead>

          <tbody>
            <tr
              v-for="(item, index) in safeItems"
              :key="item.productId ?? `item-${index}`"
            >
              <td>
                <span class="rank-badge" :class="getRankClass(index)">
                  #{{ index + 1 }}
                </span>
              </td>

              <td>
                <div class="d-flex align-items-center gap-3">
                  <div class="image-box">
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
                    <!-- Tên sản phẩm đã được làm sạch, không còn chữ "Sản phẩm đã xóa" thô kệch -->

                    <div class="fw-semibold product-name">
                      {{ getProductName(item) }}
                    </div>

                    <div
                      v-if="getCapacityText(item) || getBottleTypeText(item)"
                      class="text-muted small mt-1"
                    >
                      <span v-if="getCapacityText(item)">
                        <i class="bi bi-droplet-half me-1"></i>

                        {{ getCapacityText(item) }}
                      </span>

                      <span
                        v-if="getBottleTypeText(item)"
                        :class="{ 'ms-2': getCapacityText(item) }"
                      >
                        <i class="bi bi-box-seam me-1"></i>

                        {{ getBottleTypeText(item) }}
                      </span>
                    </div>
                  </div>
                </div>
              </td>

              <!-- Thương hiệu tự động khôi phục thông minh -->

              <td>
                {{ getBrandName(item) }}
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
import { computed } from "vue";

import type { BestSellingProductResponse } from "../types/report.type";

const props = defineProps<{
  items: BestSellingProductResponse[];
}>();

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";

const getImageUrl = (url?: string) => {
  if (!url) return "";

  if (
    url.startsWith("http") ||
    url.startsWith("blob:") ||
    url.startsWith("data:")
  )
    return url;

  return url.startsWith("/") ? `${API_URL}${url}` : `${API_URL}/${url}`;
};

const safeItems = computed(() => {
  const items = Array.isArray(props.items) ? props.items : [];

  /*

   * Backend Owner Report đã chịu trách nhiệm GROUP BY Product.

   *

   * FE tuyệt đối không group lại theo dung tích / loại chai,

   * vì màn hình này là "Sản phẩm bán chạy nhất", không phải

   * "SKU/biến thể bán chạy nhất".

   *

   * Copy array để không mutate props.

   */

  return [...items].sort((a: any, b: any) => {
    const soldDiff = Number(b?.totalSold || 0) - Number(a?.totalSold || 0);

    if (soldDiff !== 0) {
      return soldDiff;
    }

    return Number(b?.revenue || 0) - Number(a?.revenue || 0);
  });
});

// THUẬT TOÁN 1: Làm sạch tên sản phẩm, thay thế từ "đã xóa" thành "ngừng kinh doanh" cho lịch sự

const getProductName = (item: any) => {
  const rawName = String(item.productName || item.name || "").trim();

  if (
    !rawName ||
    rawName.toLowerCase().includes("đã xóa") ||
    rawName === "Sản phẩm đã xóa"
  ) {
    return "Sản phẩm ngừng kinh doanh";
  }

  return rawName;
};

// THUẬT TOÁN 2: Tự động đoán tên thương hiệu từ tên sản phẩm nếu backend trả về null/không rõ

const guessBrandFromName = (name: string) => {
  if (!name) return "Khác";

  const n = name.toLowerCase();

  if (n.includes("dior")) return "Dior";

  if (n.includes("chanel")) return "Chanel";

  if (n.includes("tom ford")) return "Tom Ford";

  if (n.includes("creed")) return "Creed";

  if (n.includes("jo malone")) return "Jo Malone";

  return "Khác";
};

const getBrandName = (item: any) => {
  const brand = String(item.brandName || "").trim();

  if (brand && brand !== "Không rõ thương hiệu" && brand !== "Khác") {
    return brand;
  }

  // Dự phòng: nội suy trực tiếp từ tên sản phẩm trong dữ liệu báo cáo.
  const productName = getProductName(item);
  const guessed = guessBrandFromName(productName);

  if (guessed !== "Khác") return guessed;

  return "Thương hiệu khác";
};

const getCapacityText = (item: any) => {
  // BE trả capacityName riêng; không trộn loại chai vào dung tích.
  if (item.capacityName && String(item.capacityName).trim() !== "") {
    return item.capacityName;
  }

  const text = item.capacity || item.variantName || item.subName;

  if (!text) return "";

  if (/^\d+(\.\d+)?$/.test(String(text).trim())) {
    return `${text} ml`;
  }

  return text;
};

const getBottleTypeText = (item: any) => {
  const text = String(item?.bottleTypeName || item?.bottleType || "").trim();

  return text;
};

const getBestSellingImageUrl = (item: BestSellingProductResponse) => {
  if (!item) return "";

  const rawUrl =
    (item as any).primaryImageUrl ||
    (item as any).PrimaryImageUrl ||
    item.imageUrl ||
    (item as any).ImageUrl ||
    (item as any).image ||
    (item as any).Image ||
    (item as any).thumbnailUrl ||
    (item as any).mainImage ||
    "";

  return getImageUrl(rawUrl);
};

// ĐÃ SỬA: Hàm lấy số cực xịn chống mọi loại nhiễu từ chuỗi tiền tệ (vd: "1.025.000 đ", "1,000.5")

const toNumber = (value: unknown) => {
  if (value === null || value === undefined) return 0;

  if (typeof value === "number") {
    return Number.isFinite(value) ? value : 0;
  }

  const textValue = String(value);

  // Cạo sạch chữ, khoảng trắng, các dấu phẩy, dấu chấm (chỉ giữ lại số)

  const cleanString = textValue.replace(/[^\d]/g, "");

  const numericValue = Number(cleanString);

  return Number.isFinite(numericValue) ? numericValue : 0;
};

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

const FALLBACK_IMAGE =
  "data:image/svg+xml;charset=UTF-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww\\.w3.org%2F2000%2Fsvg%22%20width%3D%22200%22%20height%3D%22200%22%3E%3Crect%20width%3D%22100%25%22%20height%3D%22100%25%22%20fill%3D%22%23f1f5f9%22%2F%3E%3Ctext%20x%3D%2250%25%22%20y%3D%2250%25%22%20dominant-baseline%3D%22middle%22%20text-anchor%3D%22middle%22%20fill%3D%22%2394a3b8%22%20font-family%3D%22Arial%22%20font-size%3D%2214%22%3EKh%C3%B4ng%20c%C3%B3%20%E1%BA%A3nh%3C%2Ftext%3E%3C%2Fsvg%3E";

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

  border-radius: 10px;
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
