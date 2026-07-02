<template>
  <aside class="sidebar-filter">
    <div class="filter-header">
      <h3>BỘ LỌC TÌM KIẾM</h3>
      <svg class="icon-filter" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
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
      <input type="checkbox" value="Nam" v-model="selectedFilters.genders" @change="emitFilter">
      <span class="checkmark"></span> Nam
    </label>
    
    <label class="custom-checkbox">
      <input type="checkbox" value="Nữ" v-model="selectedFilters.genders" @change="emitFilter">
      <span class="checkmark"></span> Nữ
    </label>
    
    <label class="custom-checkbox">
      <input type="checkbox" value="Unisex" v-model="selectedFilters.genders" @change="emitFilter">
      <span class="checkmark"></span> Unisex
    </label>
  </div>
</div>

<div class="filter-group">
      <div class="group-title">Dung tích <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="18 15 12 9 6 15" /></svg></div>
      <div class="checkbox-list col">
        <label class="custom-checkbox" v-for="item in capacities" :key="item.id">
          <input type="checkbox" :value="item.value + 'ml'" v-model="selectedFilters.capacities" @change="emitFilter" />
          <span class="checkmark"></span> {{ item.value }}ml
        </label>
      </div>
    </div>

    <div class="filter-group">
      <div class="group-title">Nồng độ <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="18 15 12 9 6 15" /></svg></div>
      <div class="checkbox-list col">
        <label class="custom-checkbox" v-for="item in concentrations" :key="item.id">
          <input type="checkbox" :value="item.name" v-model="selectedFilters.concentrations" @change="emitFilter" />
          <span class="checkmark"></span> {{ item.name }}
        </label>
      </div>
    </div>

    <div class="filter-group">
      <div class="group-title">Nhóm hương <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="18 15 12 9 6 15" /></svg></div>
      <div class="checkbox-list col">
        <label class="custom-checkbox" v-for="item in fragranceFamilies" :key="item.id">
          <input type="checkbox" :value="item.name" v-model="selectedFilters.fragranceFamilies" @change="emitFilter" />
          <span class="checkmark"></span> {{ item.name }}
        </label>
      </div>
    </div>

    <div class="filter-group">
      <div class="group-title">Loại chai <svg class="chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="18 15 12 9 6 15" /></svg></div>
      <div class="checkbox-list col">
        <label class="custom-checkbox" v-for="item in bottleTypes" :key="item.id">
          <input type="checkbox" :value="item.name" v-model="selectedFilters.bottleTypes" @change="emitFilter" />
          <span class="checkmark"></span> {{ item.name }}
        </label>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';

const emit = defineEmits(['filter-change']);

const bottleTypes = ref([]);
const capacities = ref([]);
const concentrations = ref([]);
const fragranceFamilies = ref([]);

const filters = ref({
  genders: []
});

// Biến lưu trữ các mục người dùng đã tích chọn
const selectedFilters = ref({
  genders: [],          // 1. THÊM DÒNG NÀY VÀO
  bottleTypes: [],
  capacities: [],
  concentrations: [],
  fragranceFamilies: []
});


const emitFilter = () => {
  emit('filter-change', {
    genders: selectedFilters.value.genders, // Đổi từ filters -> selectedFilters
    bottleTypes: selectedFilters.value.bottleTypes,
    capacities: selectedFilters.value.capacities,
    concentrations: selectedFilters.value.concentrations,
    fragranceFamilies: selectedFilters.value.fragranceFamilies
  });
};

const fetchFilters = async () => {
  try {
    const [bottleRes, capRes, concRes, fragRes] = await Promise.all([
      axios.get('http://localhost:8080/api/bottle-types?size=50'),
      axios.get('http://localhost:8080/api/capacities?size=50'),
      axios.get('http://localhost:8080/api/concentrations?size=50'),
      axios.get('http://localhost:8080/api/fragrance-families?size=50')
    ]);

    // Bật F12 tab Console lên xem 4 dòng này để biết chính xác cấu trúc Object nhé!
    console.log('Bottle Data:', bottleRes.data);
    console.log('Capacity Data:', capRes.data);

    // Ép mảng an toàn: Thử lấy .content, nếu không có thì lấy trực tiếp .data, cuối cùng là mảng rỗng
    bottleTypes.value = bottleRes.data?.content || bottleRes.data || [];
    capacities.value = capRes.data?.content || capRes.data || [];
    concentrations.value = concRes.data?.content || concRes.data || [];
    fragranceFamilies.value = fragRes.data?.content || fragRes.data || [];

  } catch (error) {
    console.error('Lỗi khi tải bộ lọc từ API:', error);
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

.custom-checkbox:hover input~.checkmark {
  border-color: #0a142f;
}

.custom-checkbox input:checked~.checkmark {
  background-color: #0a142f;
  border-color: #0a142f;
}

.checkmark:after {
  content: "";
  position: absolute;
  display: none;
}

.custom-checkbox input:checked~.checkmark:after {
  display: block;
  left: 6px;
  top: 2px;
  width: 4px;
  height: 8px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}
</style>