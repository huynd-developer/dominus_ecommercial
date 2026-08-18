<script setup lang="ts">
import type { InventorySummary } from "../types/inventory.type";

defineProps<{
  summary: InventorySummary | null;
  loading?: boolean;
}>();

const formatNumber = (value?: number) => {
  return Number(value ?? 0).toLocaleString("vi-VN");
};
</script>

<template>
  <div class="summary-grid">
    <div class="summary-card">
      <div class="label">Tổng SKU</div>
      <div class="value">
        {{ loading ? "..." : formatNumber(summary?.totalSku) }}
      </div>
    </div>

    <div class="summary-card">
      <div class="label">Tổng tồn</div>
      <div class="value">
        {{ loading ? "..." : formatNumber(summary?.totalQuantity) }}
      </div>
    </div>

    <div class="summary-card">
      <div class="label">Có thể bán</div>
      <div class="value success">
        {{
          loading
            ? "..."
            : formatNumber(summary?.sellableQuantity)
        }}
      </div>
    </div>

    <div class="summary-card">
      <div class="label">Sắp hết hạn</div>
      <div class="value warning">
        {{
          loading
            ? "..."
            : formatNumber(summary?.nearExpiryQuantity)
        }}
      </div>
    </div>

    <div class="summary-card">
      <div class="label">Đã hết hạn</div>
      <div class="value danger">
        {{
          loading
            ? "..."
            : formatNumber(summary?.expiredQuantity)
        }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(150px, 1fr));
  gap: 16px;
}

.summary-card {
  background: #ffffff;
  border: 1px solid #e7e7e7;
  border-radius: 12px;
  padding: 18px;
}

.label {
  font-size: 13px;
  color: #777;
  margin-bottom: 8px;
}

.value {
  font-size: 25px;
  font-weight: 700;
  color: #222;
}

.success {
  color: #238636;
}

.warning {
  color: #c27c0e;
}

.danger {
  color: #c62828;
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 700px) {
  .summary-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>