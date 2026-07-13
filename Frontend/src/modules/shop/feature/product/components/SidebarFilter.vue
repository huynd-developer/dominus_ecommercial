<template>
  <aside class="sidebar-filter">
    <div class="filter-header">
      <h3>BỘ LỌC TÌM KIẾM</h3>

      <svg
        class="icon-filter"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
      >
        <line x1="4" y1="21" x2="4" y2="14" />
        <line x1="4" y1="10" x2="4" y2="3" />
        <line x1="12" y1="21" x2="12" y2="12" />
        <line x1="12" y1="8" x2="12" y2="3" />
        <line x1="20" y1="21" x2="20" y2="16" />
        <line x1="20" y1="12" x2="20" y2="3" />
        <line x1="1" y1="14" x2="7" y2="14" />
        <line x1="9" y1="8" x2="15" y2="8" />
        <line x1="17" y1="16" x2="23" y2="16" />
      </svg>
    </div>

    <div class="filter-group">
      <div class="group-title">Giới tính</div>

      <div class="checkbox-list row">
        <label class="custom-checkbox">
          <input
            v-model="selectedFilters.genders"
            type="checkbox"
            value="Nam"
            @change="emitFilter"
          />
          <span class="checkmark"></span>
          Nam
        </label>

        <label class="custom-checkbox">
          <input
            v-model="selectedFilters.genders"
            type="checkbox"
            value="Nữ"
            @change="emitFilter"
          />
          <span class="checkmark"></span>
          Nữ
        </label>

        <label class="custom-checkbox">
          <input
            v-model="selectedFilters.genders"
            type="checkbox"
            value="Unisex"
            @change="emitFilter"
          />
          <span class="checkmark"></span>
          Unisex
        </label>
      </div>
    </div>

    <div class="filter-group">
      <div class="group-title">
        Dung tích
        <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="18 15 12 9 6 15" />
        </svg>
      </div>

      <div class="checkbox-list col">
        <label
          v-for="item in capacities"
          :key="item.id"
          class="custom-checkbox"
        >
          <input
            v-model="selectedFilters.capacities"
            type="checkbox"
            :value="formatCapacityValue(item)"
            @change="emitFilter"
          />
          <span class="checkmark"></span>
          {{ formatCapacityValue(item) }}
        </label>
      </div>
    </div>

    <div class="filter-group">
      <div class="group-title">
        Nồng độ
        <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="18 15 12 9 6 15" />
        </svg>
      </div>

      <div class="checkbox-list col">
        <label
          v-for="item in concentrations"
          :key="item.id"
          class="custom-checkbox"
        >
          <input
            v-model="selectedFilters.concentrations"
            type="checkbox"
            :value="item.name"
            @change="emitFilter"
          />
          <span class="checkmark"></span>
          {{ item.name }}
        </label>
      </div>
    </div>

    <div class="filter-group">
      <div class="group-title">
        Nhóm hương
        <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="18 15 12 9 6 15" />
        </svg>
      </div>

      <div class="checkbox-list col">
        <label
          v-for="item in fragranceFamilies"
          :key="item.id"
          class="custom-checkbox"
        >
          <input
            v-model="selectedFilters.fragranceFamilies"
            type="checkbox"
            :value="item.name"
            @change="emitFilter"
          />
          <span class="checkmark"></span>
          {{ item.name }}
        </label>
      </div>
    </div>

    <div class="filter-group">
      <div class="group-title">
        Loại chai
        <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="18 15 12 9 6 15" />
        </svg>
      </div>

      <div class="checkbox-list col">
        <label
          v-for="item in bottleTypes"
          :key="item.id"
          class="custom-checkbox"
        >
          <input
            v-model="selectedFilters.bottleTypes"
            type="checkbox"
            :value="item.name"
            @change="emitFilter"
          />
          <span class="checkmark"></span>
          {{ item.name }}
        </label>
      </div>
    </div>

    <button
      type="button"
      class="btn-clear-filter"
      @click="clearFilters"
    >
      Xóa bộ lọc
    </button>
  </aside>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import api from "@/common/api";

interface FilterOption {
  id: number;
  name?: string;
  value?: number | string;
}

interface SelectedFilters {
  genders: string[];
  bottleTypes: string[];
  capacities: string[];
  concentrations: string[];
  fragranceFamilies: string[];
}

const emit = defineEmits<{
  (e: "filter-change", filters: SelectedFilters): void;
}>();

const bottleTypes = ref<FilterOption[]>([]);
const capacities = ref<FilterOption[]>([]);
const concentrations = ref<FilterOption[]>([]);
const fragranceFamilies = ref<FilterOption[]>([]);

const selectedFilters = reactive<SelectedFilters>({
  genders: [],
  bottleTypes: [],
  capacities: [],
  concentrations: [],
  fragranceFamilies: [],
});

const extractArrayData = <T,>(data: any): T[] => {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.content)) return data.content;
  if (Array.isArray(data?.data?.content)) return data.data.content;
  if (Array.isArray(data?.data)) return data.data;

  return [];
};

const formatCapacityValue = (item: FilterOption) => {
  const raw = item.value ?? item.name ?? "";

  if (raw === "") {
    return "N/A";
  }

  const text = String(raw);

  return text.toLowerCase().includes("ml") ? text : `${text}ml`;
};

const emitFilter = () => {
  emit("filter-change", {
    genders: [...selectedFilters.genders],
    bottleTypes: [...selectedFilters.bottleTypes],
    capacities: [...selectedFilters.capacities],
    concentrations: [...selectedFilters.concentrations],
    fragranceFamilies: [...selectedFilters.fragranceFamilies],
  });
};

const clearFilters = () => {
  selectedFilters.genders = [];
  selectedFilters.bottleTypes = [];
  selectedFilters.capacities = [];
  selectedFilters.concentrations = [];
  selectedFilters.fragranceFamilies = [];

  emitFilter();
};

const fetchFilters = async () => {
  try {
    const [bottleRes, capRes, concRes, fragRes] = await Promise.all([
      api.get("/bottle-types", {
        params: {
          page: 0,
          size: 50,
        },
      }),
      api.get("/capacities", {
        params: {
          page: 0,
          size: 50,
        },
      }),
      api.get("/concentrations", {
        params: {
          page: 0,
          size: 50,
        },
      }),
      api.get("/fragrance-families", {
        params: {
          page: 0,
          size: 50,
        },
      }),
    ]);

    bottleTypes.value = extractArrayData<FilterOption>(bottleRes.data);
    capacities.value = extractArrayData<FilterOption>(capRes.data);
    concentrations.value = extractArrayData<FilterOption>(concRes.data);
    fragranceFamilies.value = extractArrayData<FilterOption>(fragRes.data);
  } catch (error) {
    console.error("Lỗi khi tải bộ lọc từ API:", error);
  }
};

onMounted(() => {
  fetchFilters();
});
</script>

<style scoped>
.sidebar-filter {
  width: 250px;
  flex-shrink: 0;
}

.filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 25px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e2e8f0;
}

.filter-header h3 {
  font-size: 16px;
  font-weight: 700;
  margin: 0;
  color: #0a142f;
  letter-spacing: 0.5px;
}

.icon-filter {
  width: 20px;
  height: 20px;
  color: #0a142f;
}

.filter-group {
  margin-bottom: 25px;
}

.group-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 15px;
  cursor: pointer;
  color: #0a142f;
}

.chevron {
  width: 16px;
  height: 16px;
  color: #0a142f;
}

.checkbox-list {
  display: flex;
  gap: 10px;
}

.checkbox-list.col {
  flex-direction: column;
}

.checkbox-list.row {
  justify-content: space-between;
}

.custom-checkbox {
  display: flex;
  align-items: center;
  position: relative;
  padding-left: 28px;
  cursor: pointer;
  font-size: 13px;
  user-select: none;
  color: #4a5568;
}

.custom-checkbox input {
  position: absolute;
  opacity: 0;
  cursor: pointer;
  height: 0;
  width: 0;
}

.checkmark {
  position: absolute;
  top: -2px;
  left: 0;
  height: 18px;
  width: 18px;
  background-color: white;
  border: 1px solid #cbd5e0;
  border-radius: 4px;
  transition: 0.2s;
}

.custom-checkbox:hover input ~ .checkmark {
  border-color: #0a142f;
}

.custom-checkbox input:checked ~ .checkmark {
  background-color: #0a142f;
  border-color: #0a142f;
}

.checkmark::after {
  content: "";
  position: absolute;
  display: none;
}

.custom-checkbox input:checked ~ .checkmark::after {
  display: block;
  left: 6px;
  top: 2px;
  width: 4px;
  height: 8px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.btn-clear-filter {
  width: 100%;
  border: 1px solid #0a142f;
  background: #ffffff;
  color: #0a142f;
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 13px;
  font-weight: 700;
}

.btn-clear-filter:hover {
  background: #0a142f;
  color: #ffffff;
}
</style>