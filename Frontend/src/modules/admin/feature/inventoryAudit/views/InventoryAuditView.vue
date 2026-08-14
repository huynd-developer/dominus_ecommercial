<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";

import OpeningBalanceView from "@/modules/admin/feature/openingBalance/views/OpeningBalanceView.vue";
import StockAdjustmentView from "@/modules/admin/feature/stockAdjustment/views/StockAdjustmentView.vue";

type InventoryAuditTab = "opening" | "adjustment";

const route = useRoute();
const router = useRouter();

const activeTab = computed<InventoryAuditTab>(() =>
  route.query.tab === "adjustment" ? "adjustment" : "opening"
);

const changeTab = async (tab: InventoryAuditTab) => {
  if (activeTab.value === tab) return;

  await router.replace({
    path: "/admin/inventory-audit",
    query: {
      ...route.query,
      tab,
    },
  });
};
</script>

<template>
  <div class="inventory-audit-shell">
    <div class="audit-tabs">
      <button
        type="button"
        :class="{ active: activeTab === 'opening' }"
        @click="changeTab('opening')"
      >
        <i class="bi bi-box-arrow-in-down"></i>
        Khởi tạo tồn đầu kỳ
      </button>

      <button
        type="button"
        :class="{ active: activeTab === 'adjustment' }"
        @click="changeTab('adjustment')"
      >
        <i class="bi bi-clipboard2-check"></i>
        Kiểm kê thực tế
      </button>
    </div>

    <OpeningBalanceView v-if="activeTab === 'opening'" />
    <StockAdjustmentView v-else />
  </div>
</template>

<style scoped>
.inventory-audit-shell {
  min-height: 100%;
  background: #f7f7f8;
}

.audit-tabs {
  display: flex;
  gap: 8px;
  padding: 20px 24px 0;
  background: #f7f7f8;
}

.audit-tabs button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  background: #fff;
  color: #4b5563;
  font-weight: 700;
  cursor: pointer;
}

.audit-tabs button.active {
  border-color: #111827;
  background: #111827;
  color: #fff;
}

@media (max-width: 640px) {
  .audit-tabs {
    flex-direction: column;
  }

  .audit-tabs button {
    justify-content: center;
  }
}
</style>
