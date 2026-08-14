<script setup lang="ts">
import type { InventoryOverview } from "../types/inventory.type";

defineProps<{
  items: InventoryOverview[];
  loading?: boolean;
}>();

const formatNumber = (value: number) =>
  Number(value ?? 0).toLocaleString("vi-VN");
</script>

<template>
  <div class="table-wrapper">
    <table>
      <thead>
        <tr>
          <th>SKU</th>
          <th>Sản phẩm</th>
          <th class="number">Tổng tồn</th>
          <th class="number">Có thể bán</th>
          <th class="number">Sắp hết hạn</th>
          <th class="number">Hết hạn</th>
        </tr>
      </thead>

      <tbody>
        <tr v-if="loading">
          <td colspan="6" class="empty">
            Đang tải dữ liệu...
          </td>
        </tr>

        <tr v-else-if="items.length === 0">
          <td colspan="6" class="empty">
            Không có dữ liệu tồn kho
          </td>
        </tr>

        <tr
          v-for="item in items"
          v-else
          :key="item.productVariantId"
        >
          <td class="sku">
            {{ item.sku }}
          </td>

          <td>
            {{ item.productName }}
          </td>

          <td class="number">
            {{ formatNumber(item.totalQuantity) }}
          </td>

          <td class="number sellable">
            {{ formatNumber(item.sellableQuantity) }}
          </td>

          <td class="number warning">
            {{ formatNumber(item.nearExpiryQuantity) }}
          </td>

          <td class="number danger">
            {{ formatNumber(item.expiredQuantity) }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.table-wrapper {
  width: 100%;
  overflow-x: auto;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
}

table {
  width: 100%;
  border-collapse: collapse;
  min-width: 900px;
}

th,
td {
  padding: 13px 16px;
  border-bottom: 1px solid #eee;
  text-align: left;
}

th {
  font-size: 13px;
  color: #666;
  background: #fafafa;
  font-weight: 600;
}

td {
  font-size: 14px;
  color: #333;
}

.number {
  text-align: right;
}

.sku {
  font-weight: 600;
}

.sellable {
  color: #238636;
}

.warning {
  color: #b26a00;
}

.danger {
  color: #c62828;
}

.empty {
  padding: 40px;
  text-align: center;
  color: #999;
}
</style>