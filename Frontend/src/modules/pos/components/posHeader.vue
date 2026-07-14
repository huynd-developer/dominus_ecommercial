<template>
  <header
    class="d-flex justify-content-between align-items-center mb-2 px-0 pt-1 pb-1 select-none header-premium w-100 overflow-hidden gap-3"
  >
    <div class="flex-grow-1 max-w-search mx-md-4 mx-2">
      <div class="search-premium-box position-relative d-flex align-items-center w-100">
        <i class="bi bi-search text-muted-custom position-absolute start-0 ms-3 font-xs"></i>

        <input
          ref="searchInputRef"
          type="text"
          v-model="scanInput"
          placeholder="Dùng súng quét mã vạch (SKU) hoặc gõ tìm kiếm (F4)..."
          class="search-premium-input w-100 rounded-3 font-xs text-light transition-all"
          :disabled="posStore.isOrderLocked"
          @input="handleSearch"
          @keydown.enter="handleScanSku"
        />

        <button
          v-if="scanInput"
          @click="clearSearch"
          class="btn-clear-search position-absolute end-0 me-2 border-0 bg-transparent p-1 d-flex align-items-center"
          type="button"
        >
          <i class="bi bi-x-circle-fill text-muted-custom opacity-50 font-xs"></i>
        </button>
      </div>

      <div
        v-if="posStore.isOrderLocked"
        class="font-xs text-muted-custom mt-1 px-1"
      >
        <i class="bi bi-lock-fill me-1"></i>
        Đơn đang bị khóa do đã nhận tiền hoặc đang mở phiếu treo, không quét thêm sản phẩm.
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from "vue";
import { usePosStore } from "../stores/posStore";

const posStore = usePosStore();

const scanInput = ref("");
const searchInputRef = ref<HTMLInputElement | null>(null);

const handleSearch = () => {
  posStore.searchQuery = scanInput.value;
};

const handleScanSku = async () => {
  const sku = scanInput.value.trim();

  if (!sku) return;

  if (posStore.isOrderLocked) {
    posStore.errorMsg =
      "Đơn đang bị khóa do đã nhận tiền hoặc đang mở phiếu treo.";
    return;
  }

  await posStore.handleBarcodeScan(sku);

  scanInput.value = "";
  posStore.searchQuery = "";
};

const clearSearch = () => {
  scanInput.value = "";
  posStore.searchQuery = "";
  searchInputRef.value?.focus();
};

const handleGlobalKeydown = (e: KeyboardEvent) => {
  if (e.key === "F4") {
    e.preventDefault();
    searchInputRef.value?.focus();
  }
};

onMounted(() => {
  window.addEventListener("keydown", handleGlobalKeydown);
  searchInputRef.value?.focus();
});

onUnmounted(() => {
  window.removeEventListener("keydown", handleGlobalKeydown);
});
</script>

<style scoped>
.text-gold {
  color: #f3c63f !important;
}

.text-muted-custom {
  color: #9fb0d3;
}

.font-xs {
  font-size: 0.75rem;
}

.max-w-search {
  max-width: 480px;
}

.search-premium-input {
  background-color: #131c31;
  border: 1px solid #222f4f;
  padding: 8px 32px 8px 36px;
  outline: none;
}

.search-premium-input:focus {
  border-color: #384f80;
  background-color: #17223b;
  box-shadow: 0 0 10px rgba(56, 79, 128, 0.2);
}

.search-premium-input::placeholder {
  color: #4b5e84;
}

.search-premium-input:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn-clear-search:hover i {
  color: #ef4444 !important;
}

.transition-all {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}
</style>