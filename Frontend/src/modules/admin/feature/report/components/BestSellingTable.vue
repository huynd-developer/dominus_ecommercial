<template>
  <div class="card border-0 shadow-sm">
    <div class="card-header bg-white border-0 py-3">
      <h5 class="mb-0 fw-bold">Sản phẩm bán chạy nhất</h5>
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
            <tr v-for="(item, index) in items" :key="item.productId">
              <td>
                <span class="rank-badge">#{{ index + 1 }}</span>
              </td>

              <td>
                <div class="d-flex align-items-center gap-3">
                  <img
                    v-if="item.imageUrl"
                    :src="item.imageUrl"
                    class="product-img"
                    alt="product"
                  />

                  <div v-else class="product-img placeholder-img">
                    No Image
                  </div>

                  <div>
                    <div class="fw-semibold">
                      {{ item.productName }}
                    </div>
                    <div class="text-muted small">
                      ID: {{ item.productId }}
                    </div>
                  </div>
                </div>
              </td>

              <td>{{ item.brandName }}</td>

              <td class="text-end fw-semibold">
                {{ item.totalSold }}
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
import type { BestSellingProductResponse } from "../types/report.type";

defineProps<{
  items: BestSellingProductResponse[];
}>();

const formatMoney = (value: number) => {
  return Number(value || 0).toLocaleString("vi-VN", {
    style: "currency",
    currency: "VND",
  });
};
</script>

<style scoped>
.product-img {
  width: 54px;
  height: 54px;
  object-fit: cover;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.placeholder-img {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: #9ca3af;
  background: #f3f4f6;
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

.empty-box {
  padding: 50px;
  text-align: center;
  color: #6b7280;
  background: #f9fafb;
}
</style>