<script setup lang="ts">
import type { InventoryLotStatus } from "../types/inventory.type";

defineProps<{
  items: InventoryLotStatus[];
  loading?: boolean;
}>();

const formatNumber = (value: number) =>
  Number(value ?? 0).toLocaleString("vi-VN");

const formatDate = (value?: string | null) => {
  if (!value) return "-";

  const date = new Date(`${value}T00:00:00`);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleDateString("vi-VN");
};
</script>

<template>
  <div class="table-wrapper">
    <table>
      <thead>
        <tr>
          <th>SKU</th>
          <th>Sản phẩm</th>
          <th>Mã lô</th>
          <th class="number">Tồn</th>
          <th>Hạn sử dụng</th>
          <th class="number">Còn lại</th>
          <th>Trạng thái</th>
        </tr>
      </thead>

      <tbody>
        <!-- LOADING -->
        <tr v-if="loading">
          <td colspan="7" class="empty">
            Đang tải dữ liệu...
          </td>
        </tr>

        <!-- EMPTY -->
        <tr v-else-if="items.length === 0">
          <td colspan="7" class="empty">
            Không có dữ liệu
          </td>
        </tr>

        <!-- DATA -->
        <tr
          v-for="item in items"
          v-else
          :key="item.inventoryLotId"
        >
          <!-- SKU -->
          <td class="sku">
            {{ item.sku }}
          </td>

          <!-- PRODUCT -->
          <td>
            {{ item.productName }}
          </td>

          <!-- LOT -->
          <td>
            {{ item.lotCode }}
          </td>

          <!-- QUANTITY -->
          <td class="number">
            {{ formatNumber(item.quantityOnHand) }}
          </td>

          <!-- EXPIRATION DATE -->
          <td>
            {{ formatDate(item.expirationDate) }}
          </td>

          <!-- DAYS TO EXPIRY -->
          <td class="number">
            <span
              :class="{
                danger: item.daysToExpiry < 0,
                warning:
                  item.daysToExpiry >= 0 &&
                  item.nearExpiry,
              }"
            >
              {{
                item.daysToExpiry < 0
                  ? `Quá hạn ${Math.abs(item.daysToExpiry)} ngày`
                  : `${item.daysToExpiry} ngày`
              }}
            </span>
          </td>

          <!-- STATUS -->
          <td>
            <div class="status-badges">
              <!-- HẾT HẠN -->
              <span
                v-if="item.expired"
                class="badge badge-danger"
              >
                Hết hạn
              </span>

              <!-- SẮP HẾT HẠN -->
              <span
                v-if="item.nearExpiry && !item.expired"
                class="badge badge-warning"
              >
                Sắp hết hạn
              </span>

              <!-- ĐANG KHÓA -->
              <span
                v-if="item.locked"
                class="badge badge-locked"
              >
                Đang khóa
              </span>

              <!-- BÌNH THƯỜNG -->
              <span
                v-if="
                  !item.expired &&
                  !item.nearExpiry &&
                  !item.locked
                "
                class="badge badge-success"
              >
                Bình thường
              </span>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.table-wrapper {
  overflow-x: auto;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  background: white;
}

table {
  width: 100%;
  min-width: 900px;
  border-collapse: collapse;
}

th,
td {
  padding: 13px 16px;
  border-bottom: 1px solid #eee;
}

th {
  background: #fafafa;
  color: #666;
  font-size: 13px;
  text-align: left;
}

td {
  font-size: 14px;
}

.number {
  text-align: right;
}

.sku {
  font-weight: 600;
}

/* =========================
   STATUS BADGES
   ========================= */

.status-badges {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.badge {
  display: inline-block;
  border-radius: 999px;
  padding: 4px 9px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.badge-danger {
  background: #ffebee;
  color: #c62828;
}

.badge-warning {
  background: #fff8e1;
  color: #b26a00;
}

.badge-locked {
  background: #fff3e0;
  color: #9a3412;
}

.badge-success {
  background: #e8f5e9;
  color: #238636;
}

/* =========================
   DAYS TO EXPIRY
   ========================= */

.warning {
  color: #b26a00;
}

.danger {
  color: #c62828;
}

/* =========================
   EMPTY / LOADING
   ========================= */

.empty {
  text-align: center;
  color: #999;
  padding: 40px;
}
</style>