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
      <button
        type="button"
        class="group-title"
        :aria-expanded="isGroupOpen('genders')"
        @click="toggleGroup('genders')"
      >
        <span>Giới tính</span>

        <svg
          class="chevron"
          :class="{ collapsed: !isGroupOpen('genders') }"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <polyline points="18 15 12 9 6 15" />
        </svg>
      </button>

      <Transition name="filter-collapse">
        <div v-show="isGroupOpen('genders')" class="checkbox-list col">
          <label class="custom-checkbox">
            <input
              v-model="selectedFilters.genders"
              type="checkbox"
              value="Nam"
              @change="emitFilter"
            />
            <span class="checkmark"></span>
            <span class="checkbox-text">Nam</span>
          </label>

          <label class="custom-checkbox">
            <input
              v-model="selectedFilters.genders"
              type="checkbox"
              value="Nữ"
              @change="emitFilter"
            />
            <span class="checkmark"></span>
            <span class="checkbox-text">Nữ</span>
          </label>

          <label class="custom-checkbox">
            <input
              v-model="selectedFilters.genders"
              type="checkbox"
              value="Unisex"
              @change="emitFilter"
            />
            <span class="checkmark"></span>
            <span class="checkbox-text">Unisex</span>
          </label>
        </div>
      </Transition>
    </div>

    <div class="filter-group">
      <button
        type="button"
        class="group-title"
        :aria-expanded="isGroupOpen('capacities')"
        @click="toggleGroup('capacities')"
      >
        <span>Dung tích</span>

        <svg
          class="chevron"
          :class="{ collapsed: !isGroupOpen('capacities') }"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <polyline points="18 15 12 9 6 15" />
        </svg>
      </button>

      <Transition name="filter-collapse">
        <div v-show="isGroupOpen('capacities')" class="checkbox-list col scrollable-list">
          <label
            v-for="item in capacities"
            :key="item.id || formatCapacityValue(item)"
            class="custom-checkbox"
          >
            <input
              v-model="selectedFilters.capacities"
              type="checkbox"
              :value="formatCapacityValue(item)"
              @change="emitFilter"
            />
            <span class="checkmark"></span>
            <span class="checkbox-text">{{ formatCapacityValue(item) }}</span>
          </label>
        </div>
      </Transition>
    </div>

    <div class="filter-group">
      <button
        type="button"
        class="group-title"
        :aria-expanded="isGroupOpen('concentrations')"
        @click="toggleGroup('concentrations')"
      >
        <span>Nồng độ</span>

        <svg
          class="chevron"
          :class="{ collapsed: !isGroupOpen('concentrations') }"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <polyline points="18 15 12 9 6 15" />
        </svg>
      </button>

      <Transition name="filter-collapse">
        <div v-show="isGroupOpen('concentrations')" class="checkbox-list col scrollable-list">
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
            <span class="checkbox-text">{{ item.name }}</span>
          </label>
        </div>
      </Transition>
    </div>

    <div class="filter-group">
      <button
        type="button"
        class="group-title"
        :aria-expanded="isGroupOpen('fragranceFamilies')"
        @click="toggleGroup('fragranceFamilies')"
      >
        <span>Nhóm hương</span>

        <svg
          class="chevron"
          :class="{ collapsed: !isGroupOpen('fragranceFamilies') }"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <polyline points="18 15 12 9 6 15" />
        </svg>
      </button>

      <Transition name="filter-collapse">
        <div v-show="isGroupOpen('fragranceFamilies')" class="checkbox-list col scrollable-list">
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
            <span class="checkbox-text">{{ item.name }}</span>
          </label>
        </div>
      </Transition>
    </div>

    <div class="filter-group">
      <button
        type="button"
        class="group-title"
        :aria-expanded="isGroupOpen('bottleTypes')"
        @click="toggleGroup('bottleTypes')"
      >
        <span>Loại chai</span>

        <svg
          class="chevron"
          :class="{ collapsed: !isGroupOpen('bottleTypes') }"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
        >
          <polyline points="18 15 12 9 6 15" />
        </svg>
      </button>

      <Transition name="filter-collapse">
        <div v-show="isGroupOpen('bottleTypes')" class="checkbox-list col scrollable-list">
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
            <span class="checkbox-text">{{ item.name }}</span>
          </label>
        </div>
      </Transition>
    </div>

    <button type="button" class="btn-clear-filter" @click="clearFilters">
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
  label?: number | string;
  capacityValue?: number | string;
  capacityName?: number | string;
  CapacityValue?: number | string;
  CapacityName?: number | string;
}

interface SelectedFilters {
  genders: string[];
  bottleTypes: string[];
  capacities: string[];
  concentrations: string[];
  fragranceFamilies: string[];
}

type FilterGroupKey =
  | "genders"
  | "bottleTypes"
  | "capacities"
  | "concentrations"
  | "fragranceFamilies";

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

const expandedGroups = reactive<Record<FilterGroupKey, boolean>>({
  genders: true,
  capacities: true,
  concentrations: true,
  fragranceFamilies: true,
  bottleTypes: true,
});

const toggleGroup = (group: FilterGroupKey) => {
  expandedGroups[group] = !expandedGroups[group];
};

const isGroupOpen = (group: FilterGroupKey) => {
  return expandedGroups[group];
};

const extractArrayData = <T,>(data: any): T[] => {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.content)) return data.content;
  if (Array.isArray(data?.data?.content)) return data.data.content;
  if (Array.isArray(data?.data)) return data.data;

  return [];
};

const getTotalPages = (data: any) => {
  const totalPages = Number(data?.data?.totalPages ?? data?.totalPages ?? 1);

  return Number.isFinite(totalPages) && totalPages > 0 ? totalPages : 1;
};

// ĐÃ SỬA: Thêm tham số t để chặn trình duyệt lưu cache dữ liệu cũ
const fetchAllFilterOptions = async <T,>(url: string): Promise<T[]> => {
  const size = 100;
  let page = 0;
  let totalPages = 1;
  const result: T[] = [];

  do {
    const res = await api.get(url, {
      params: {
        page,
        size,
        t: Date.now() // Tham số chống cache
      },
    });

    result.push(...extractArrayData<T>(res.data));

    totalPages = getTotalPages(res.data);
    page++;
  } while (page < totalPages);

  return result;
};

const formatCapacityValue = (item: FilterOption) => {
  const raw =
    item.value ??
    item.capacityValue ??
    item.CapacityValue ??
    item.name ??
    item.capacityName ??
    item.CapacityName ??
    item.label ??
    "";

  if (raw === "" || raw == null) {
    return "N/A";
  }

  // Dọn dẹp chữ ml và ép về số
  const text = String(raw).toLowerCase().replace(/ml/g, "").trim();
  const numeric = parseFloat(text);

  // Nếu là số thì trả về đúng chuẩn (VD: 10.0 -> 10ml)
  if (!Number.isNaN(numeric)) {
    return `${numeric}ml`;
  }

  const rawStr = String(raw).trim();
  return rawStr.toLowerCase().includes("ml") ? rawStr : `${rawStr}ml`;
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
    const [bottleList, capacityList, concentrationList, fragranceFamilyList] =
      await Promise.all([
        fetchAllFilterOptions<FilterOption>("/bottle-types"),
        fetchAllFilterOptions<FilterOption>("/capacities"),
        fetchAllFilterOptions<FilterOption>("/concentrations"),
        fetchAllFilterOptions<FilterOption>("/fragrance-families"),
      ]);

    bottleTypes.value = bottleList;
    capacities.value = capacityList;
    concentrations.value = concentrationList;
    fragranceFamilies.value = fragranceFamilyList;
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
  margin-bottom: 26px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e2e8f0;
}

.filter-header h3 {
  font-size: 16px;
  font-weight: 800;
  margin: 0;
  color: #0a142f;
  letter-spacing: 0.5px;
}

.icon-filter {
  width: 20px;
  height: 20px;
  color: #0a142f;
  flex-shrink: 0;
}

.filter-group {
  margin-bottom: 24px;
}

.group-title {
  width: 100%;
  border: 0;
  background: transparent;
  padding: 0;
  margin: 0 0 14px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  color: #0a142f;
  text-align: left;
}

.group-title span {
  line-height: 1.3;
}

.chevron {
  width: 16px;
  height: 16px;
  color: #0a142f;
  flex-shrink: 0;
  transition: transform 0.22s ease;
}

.chevron.collapsed {
  transform: rotate(180deg);
}

.checkbox-list {
  display: flex;
  gap: 10px;
}

.checkbox-list.col {
  flex-direction: column;
}

.scrollable-list {
  max-height: 260px;
  overflow-y: auto;
  overflow-x: hidden;
  padding-right: 6px;
}

.scrollable-list::-webkit-scrollbar {
  width: 6px;
}

.scrollable-list::-webkit-scrollbar-track {
  background: rgba(10, 20, 47, 0.05);
  border-radius: 999px;
}

.scrollable-list::-webkit-scrollbar-thumb {
  background: rgba(10, 20, 47, 0.22);
  border-radius: 999px;
}

.scrollable-list::-webkit-scrollbar-thumb:hover {
  background: rgba(10, 20, 47, 0.35);
}

.custom-checkbox {
  display: flex;
  align-items: center;
  position: relative;
  min-height: 24px;
  padding-left: 28px;
  cursor: pointer;
  font-size: 13px;
  user-select: none;
  color: #4a5568;
  line-height: 1.35;
}

.checkbox-text {
  display: inline-block;
  line-height: 1.35;
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
  top: 50%;
  left: 0;
  height: 18px;
  width: 18px;
  background-color: white;
  border: 1px solid #cbd5e0;
  border-radius: 4px;
  transform: translateY(-50%);
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

.filter-collapse-enter-active,
.filter-collapse-leave-active {
  transition: all 0.2s ease;
}

.filter-collapse-enter-from,
.filter-collapse-leave-to {
  opacity: 0;
  transform: translateY(-4px);
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