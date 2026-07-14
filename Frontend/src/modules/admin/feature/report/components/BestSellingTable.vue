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
              <th class="text-end">Doanh thu</th>
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
                  <img
                    v-if="item.imageUrl"
                    :src="item.imageUrl"
                    class="product-img"
                    alt="Ảnh sản phẩm"
                    @error="handleImageError"
                  />

                  <div v-else class="product-img placeholder-img">
                    No Image
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

          <tfoot class="table-light">
            <tr>
              <td colspan="3" class="fw-semibold">Tổng trong bảng</td>
              <td class="text-end fw-bold">
                {{ formatNumber(totalSold) }}
              </td>
              <td class="text-end fw-bold">
                {{ formatMoney(totalRevenue) }}
              </td>
            </tr>
          </tfoot>
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

const toNumber = (value: unknown) => {
  const numberValue = Number(value ?? 0);
  return Number.isFinite(numberValue) ? numberValue : 0;
};

const safeItems = computed(() => {
  return Array.isArray(props.items) ? props.items : [];
});

const totalSold = computed(() => {
  return safeItems.value.reduce((sum, item) => {
    return sum + toNumber(item.totalSold);
  }, 0);
});

const totalRevenue = computed(() => {
  return safeItems.value.reduce((sum, item) => {
    return sum + toNumber(item.revenue);
  }, 0);
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

const handleImageError = (event: Event) => {
  const target = event.target as HTMLImageElement | null;

  if (target) {
    target.style.display = "none";
  }
};
</script>

<style scoped>
.product-img {
  width: 54px;
  height: 54px;
  object-fit: cover;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.placeholder-img {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: #9ca3af;
  background: #f3f4f6;
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