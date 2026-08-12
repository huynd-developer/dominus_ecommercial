<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import Swal from "sweetalert2";

import { useAuthStore } from "@/modules/auth/stores/authStore";

import InventorySummaryCards from "../components/InventorySummaryCards.vue";
import InventoryOverviewTable from "../components/InventoryOverviewTable.vue";
import InventoryLotTable from "../components/InventoryLotTable.vue";
import InventoryConfigModal from "../components/InventoryConfigModal.vue";

import { useInventoryStore } from "../stores/inventory.store.ts";

import type { InventoryStockStatus } from "../types/inventory.type.ts";

type TabType = "overview" | "near-expiry" | "expired" | "locked";

const inventoryStore = useInventoryStore();
const authStore = useAuthStore();

const activeTab = ref<TabType>("overview");

const configModalVisible = ref(false);

const role = computed(() => {
  return String(authStore.role || localStorage.getItem("role") || "")
    .toUpperCase()
    .replace("ROLE_", "")
    .trim();
});

const canEditConfig = computed(() => ["OWNER", "MANAGER"].includes(role.value));
const searchPlaceholder = computed(() => {
  if (activeTab.value === "overview") {
    return "Tìm theo SKU hoặc tên sản phẩm...";
  }

  return "Tìm theo SKU, tên sản phẩm hoặc mã lô...";
});
const currentLots = computed(() => {
  if (activeTab.value === "near-expiry") {
    return inventoryStore.nearExpiryLots;
  }

  if (activeTab.value === "expired") {
    return inventoryStore.expiredLots;
  }

  if (activeTab.value === "locked") {
    return inventoryStore.lockedLots;
  }

  return [];
});

const totalPages = computed(() => {
  const value =
    activeTab.value === "overview"
      ? inventoryStore.overviewTotalPages
      : inventoryStore.lotTotalPages;

  const parsed = Number(value);

  return Number.isFinite(parsed) && parsed > 0
    ? parsed
    : 0;
});

const currentPage = computed(() => {
  const value =
    activeTab.value === "overview"
      ? inventoryStore.overviewPage
      : inventoryStore.lotPage;

  const parsed = Number(value);

  return Number.isFinite(parsed) && parsed >= 0
    ? parsed
    : 0;
});

const totalElements = computed(() => {
  const value =
    activeTab.value === "overview"
      ? inventoryStore.overviewTotalElements
      : inventoryStore.lotTotalElements;

  const parsed = Number(value);

  return Number.isFinite(parsed) && parsed >= 0
    ? parsed
    : 0;
});

const pageSize = computed(() => {
  const value =
    activeTab.value === "overview"
      ? inventoryStore.overviewSize
      : inventoryStore.lotSize;

  const parsed = Number(value);

  return Number.isFinite(parsed) && parsed > 0
    ? parsed
    : 20;
});

const pageStart = computed(() => {
  if (totalElements.value === 0) {
    return 0;
  }

  return currentPage.value * pageSize.value + 1;
});

const pageEnd = computed(() => {
  if (totalElements.value === 0) {
    return 0;
  }

  return Math.min(
    (currentPage.value + 1) * pageSize.value,
    totalElements.value
  );
});

const pageNumbers = computed(() => {
  const total = totalPages.value;

  if (total <= 0) {
    return [];
  }

  const maxVisible = 5;
  let start = Math.max(
    0,
    currentPage.value - Math.floor(maxVisible / 2)
  );

  let end = Math.min(
    total - 1,
    start + maxVisible - 1
  );

  start = Math.max(
    0,
    end - maxVisible + 1
  );

  return Array.from(
    { length: end - start + 1 },
    (_, index) => start + index
  );
});

const loadCurrentTab = async () => {
  try {
    if (activeTab.value === "overview") {
      await inventoryStore.fetchOverview();
      return;
    }

    if (activeTab.value === "near-expiry") {
      await inventoryStore.fetchNearExpiry();
      return;
    }

    if (activeTab.value === "expired") {
      await inventoryStore.fetchExpired();
      return;
    }

    if (activeTab.value === "locked") {
      await inventoryStore.fetchLocked();
    }
  } catch (error) {
    await showLoadError(error);
  }
};

const showLoadError = async (error: any) => {
  await Swal.fire({
    icon: "error",
    title: "Không thể tải dữ liệu",
    text:
      error?.response?.data?.message || "Đã xảy ra lỗi khi tải dữ liệu kho.",
    confirmButtonText: "Đóng",
  });
};

const changeTab = async (tab: TabType) => {
  activeTab.value = tab;

  inventoryStore.overviewPage = 0;
  inventoryStore.lotPage = 0;

  await loadCurrentTab();
};

const search = async () => {
  inventoryStore.overviewPage = 0;
  inventoryStore.lotPage = 0;

  await loadCurrentTab();
};

const resetFilters = async () => {
  inventoryStore.resetOverviewFilters();
  inventoryStore.resetLotPage();

  await loadCurrentTab();
};

const changeStockStatus = async (event: Event) => {
  const target = event.target as HTMLSelectElement;

  inventoryStore.stockStatus = target.value as InventoryStockStatus;

  inventoryStore.overviewPage = 0;

  await inventoryStore.fetchOverview();
};

const toggleNearExpiry = async (event: Event) => {
  const checked = (event.target as HTMLInputElement).checked;

  inventoryStore.nearExpiryFilter = checked ? true : undefined;

  inventoryStore.overviewPage = 0;

  await inventoryStore.fetchOverview();
};

const toggleExpired = async (event: Event) => {
  const checked = (event.target as HTMLInputElement).checked;

  inventoryStore.expiredFilter = checked ? true : undefined;

  inventoryStore.overviewPage = 0;

  await inventoryStore.fetchOverview();
};

const toggleLocked = async (event: Event) => {
  const checked = (event.target as HTMLInputElement).checked;

  inventoryStore.lockedFilter = checked ? true : undefined;

  inventoryStore.overviewPage = 0;

  await inventoryStore.fetchOverview();
};

const previousPage = async () => {
  if (currentPage.value <= 0) {
    return;
  }

  if (activeTab.value === "overview") {
    inventoryStore.overviewPage--;
  } else {
    inventoryStore.lotPage--;
  }

  await loadCurrentTab();
};

const nextPage = async () => {
  if (currentPage.value + 1 >= totalPages.value) {
    return;
  }

  if (activeTab.value === "overview") {
    inventoryStore.overviewPage++;
  } else {
    inventoryStore.lotPage++;
  }

  await loadCurrentTab();
};

const goToPage = async (page: number) => {
  if (
    page < 0 ||
    page >= totalPages.value ||
    page === currentPage.value
  ) {
    return;
  }

  if (activeTab.value === "overview") {
    inventoryStore.overviewPage = page;
  } else {
    inventoryStore.lotPage = page;
  }

  await loadCurrentTab();
};

const openConfig = () => {
  configModalVisible.value = true;
};

const closeConfig = () => {
  if (inventoryStore.savingConfig) {
    return;
  }

  configModalVisible.value = false;
};

const saveConfig = async (value: number) => {
  if (!canEditConfig.value) {
    await Swal.fire({
      icon: "error",
      title: "Không có quyền",
      text: "Bạn không có quyền thay đổi cấu hình kho.",
    });

    return;
  }

  try {
    await inventoryStore.updateConfig({
      expiryWarningDays: value,
    });

    configModalVisible.value = false;

    await Swal.fire({
      icon: "success",
      title: "Cập nhật thành công",
      text: `Cảnh báo hạn sử dụng được thiết lập trước ${value} ngày.`,
      timer: 1600,
      showConfirmButton: false,
    });

    if (activeTab.value !== "overview") {
      await loadCurrentTab();
    }
  } catch (error: any) {
    if (error?.response?.status === 403) {
      await Swal.fire({
        icon: "error",
        title: "Không có quyền",
        text: "Chỉ OWNER hoặc MANAGER được thay đổi cấu hình kho.",
      });

      return;
    }

    await Swal.fire({
      icon: "error",
      title: "Cập nhật thất bại",
      text:
        error?.response?.data?.message || "Không thể cập nhật cấu hình kho.",
    });
  }
};

onMounted(async () => {
  try {
    await Promise.all([
      inventoryStore.fetchSummary(),
      inventoryStore.fetchOverview(),
      inventoryStore.fetchConfig(),
    ]);
  } catch (error) {
    await showLoadError(error);
  }
});
</script>

<template>
  <div class="inventory-page">
    <div class="page-header">
      <div>
        <h1>Tổng quan kho</h1>

        <p>Theo dõi tồn kho, hạn sử dụng và số lượng có thể bán theo SKU.</p>
      </div>

      <button
        v-if="canEditConfig"
        type="button"
        class="config-btn"
        @click="openConfig"
      >
        Thiết lập cảnh báo HSD
      </button>

      <div v-else-if="inventoryStore.config" class="config-readonly">
        Cảnh báo trước
        <strong>
          {{ inventoryStore.config.expiryWarningDays }}
          ngày
        </strong>
      </div>
    </div>

    <InventorySummaryCards
      :summary="inventoryStore.summary"
      :loading="inventoryStore.loadingSummary"
    />

    <div class="content-card">
      <div class="tabs">
        <button
          :class="{
            active: activeTab === 'overview',
          }"
          @click="changeTab('overview')"
        >
          Tổng quan SKU
        </button>

        <button
          :class="{
            active: activeTab === 'near-expiry',
          }"
          @click="changeTab('near-expiry')"
        >
          Sắp hết hạn
        </button>

        <button
          :class="{
            active: activeTab === 'expired',
          }"
          @click="changeTab('expired')"
        >
          Đã hết hạn
        </button>

        <button
          :class="{
            active: activeTab === 'locked',
          }"
          @click="changeTab('locked')"
        >
          Đang khóa
        </button>
      </div>

      <div class="filters">
        <div class="search-box">
          <input
            v-model="inventoryStore.keyword"
            type="text"
            maxlength="100"
            :placeholder="searchPlaceholder"
            @keyup.enter="search"
          />

          <button type="button" @click="search">Tìm kiếm</button>
        </div>

        <template v-if="activeTab === 'overview'">
          <select
            :value="inventoryStore.stockStatus"
            @change="changeStockStatus"
          >
            <option value="ALL">Tất cả tồn kho</option>

            <option value="IN_STOCK">Còn hàng</option>

            <option value="OUT_OF_STOCK">Hết hàng</option>
          </select>

          <label class="check-filter">
            <input
              type="checkbox"
              :checked="inventoryStore.nearExpiryFilter === true"
              @change="toggleNearExpiry"
            />

            Sắp hết hạn
          </label>

          <label class="check-filter">
            <input
              type="checkbox"
              :checked="inventoryStore.expiredFilter === true"
              @change="toggleExpired"
            />

            Hết hạn
          </label>

          <label class="check-filter">
            <input
              type="checkbox"
              :checked="inventoryStore.lockedFilter === true"
              @change="toggleLocked"
            />

            Lô khóa
          </label>
        </template>

        <button type="button" class="reset-btn" @click="resetFilters">
          Xóa lọc
        </button>
      </div>

      <InventoryOverviewTable
        v-if="activeTab === 'overview'"
        :items="inventoryStore.overview"
        :loading="inventoryStore.loadingOverview"
      />

      <InventoryLotTable
        v-else
        :items="currentLots"
        :loading="inventoryStore.loadingLots"
      />

      <div class="pagination">
        <div class="pagination-info">
          <span>
            Hiển thị
            <strong>{{ pageStart }}-{{ pageEnd }}</strong>
            /
            <strong>{{ totalElements }}</strong>
            bản ghi
          </span>

          <span class="page-summary">
            Trang
            <strong>{{ totalPages > 0 ? currentPage + 1 : 0 }}</strong>
            /
            <strong>{{ totalPages }}</strong>
          </span>
        </div>

        <div class="pagination-controls">
          <button
            type="button"
            class="page-nav"
            :disabled="currentPage <= 0"
            @click="previousPage"
          >
            Trước
          </button>

          <button
            v-for="page in pageNumbers"
            :key="page"
            type="button"
            class="page-number"
            :class="{ active: page === currentPage }"
            :disabled="page === currentPage"
            @click="goToPage(page)"
          >
            {{ page + 1 }}
          </button>

          <button
            type="button"
            class="page-nav"
            :disabled="
              totalPages === 0 ||
              currentPage + 1 >= totalPages
            "
            @click="nextPage"
          >
            Sau
          </button>
        </div>
      </div>
    </div>

    <InventoryConfigModal
      :visible="configModalVisible"
      :current-value="inventoryStore.config?.expiryWarningDays ?? 30"
      :saving="inventoryStore.savingConfig"
      :can-edit="canEditConfig"
      @close="closeConfig"
      @save="saveConfig"
    />
  </div>
</template>

<style scoped>
.inventory-page {
  padding: 24px;
  background: #f7f7f8;
  min-height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0 0 6px;
  font-size: 26px;
  color: #222;
}

.page-header p {
  margin: 0;
  color: #777;
}

.config-btn {
  background: #222;
  color: white;
  border: 0;
  border-radius: 8px;
  padding: 11px 16px;
  cursor: pointer;
}

.config-readonly {
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: white;
  color: #555;
}

.content-card {
  margin-top: 20px;
  background: white;
  border-radius: 12px;
  padding: 18px;
  border: 1px solid #e8e8e8;
}

.tabs {
  display: flex;
  gap: 4px;
  border-bottom: 1px solid #eee;
  margin-bottom: 18px;
}

.tabs button {
  padding: 10px 14px;
  border: 0;
  background: transparent;
  cursor: pointer;
  color: #666;
  border-bottom: 2px solid transparent;
}

.tabs button.active {
  color: #111;
  border-bottom-color: #222;
  font-weight: 600;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 16px;
}

.search-box {
  display: flex;
  flex: 1;
  min-width: 300px;
}

.search-box input {
  flex: 1;
  min-width: 0;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #ddd;
  border-radius: 8px 0 0 8px;
  outline: none;
}

.search-box button {
  border: 0;
  padding: 0 16px;
  background: #222;
  color: white;
  border-radius: 0 8px 8px 0;
  cursor: pointer;
}

.filters select {
  height: 40px;
  padding: 0 10px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: white;
}

.check-filter {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  white-space: nowrap;
}

.reset-btn {
  height: 40px;
  padding: 0 14px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 16px;
  color: #666;
}

.pagination-info {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
  font-size: 14px;
}

.page-summary {
  color: #777;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 6px;
}

.pagination button {
  min-width: 38px;
  height: 38px;
  padding: 0 12px;
  border: 1px solid #ddd;
  background: white;
  color: #444;
  border-radius: 7px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.pagination button:hover:not(:disabled) {
  border-color: #222;
  color: #111;
}

.pagination .page-number.active {
  background: #222;
  border-color: #222;
  color: white;
  font-weight: 600;
}

.pagination button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.pagination .page-number.active:disabled {
  opacity: 1;
}

@media (max-width: 800px) {
  .inventory-page {
    padding: 14px;
  }

  .page-header {
    flex-direction: column;
  }

  .search-box {
    width: 100%;
    min-width: 100%;
  }

  .pagination {
    align-items: flex-start;
    flex-direction: column;
  }

  .pagination-controls {
    width: 100%;
    overflow-x: auto;
    padding-bottom: 2px;
  }
}
</style>
