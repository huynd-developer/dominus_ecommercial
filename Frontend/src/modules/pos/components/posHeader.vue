<template>
  <header class="d-flex justify-content-between align-items-center mb-2 px-0 pt-1 pb-1 select-none header-premium w-100 overflow-hidden gap-3">
    <div class="flex-grow-1 max-w-search mx-md-4 mx-2">
      <div class="search-premium-box position-relative d-flex align-items-center w-100">
        <i class="bi bi-search text-muted-custom position-absolute start-0 ms-3 font-xs"></i>
        <input 
          ref="searchInputRef"
          type="text" 
          v-model="searchQuery" 
          placeholder="Tìm nhanh sản phẩm, mã đơn hàng... (F4)" 
          class="search-premium-input w-100 rounded-3 font-xs text-light transition-all"
          @input="handleSearch"
          @keydown.enter="handleScanSku"
        />
        <button 
          v-if="searchQuery" 
          @click="searchQuery = ''" 
          class="btn-clear-search position-absolute end-0 me-2 border-0 bg-transparent p-1 d-flex align-items-center"
          type="button"
        >
          <i class="bi bi-x-circle-fill text-muted-custom opacity-50 font-xs"></i>
        </button>
      </div>
      <p v-if="scanMessage" class="font-xs mb-0 mt-1 ps-1" :class="scanSuccess ? 'text-success' : 'text-danger'">
        {{ scanMessage }}
      </p>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { usePosStore } from '../stores/posStore'

const posStore = usePosStore()
const time = ref('')
const searchQuery = ref('')
const searchInputRef = ref(null)
const scanMessage = ref('')
const scanSuccess = ref(false)
let timer = null
let scanMessageTimeout = null

const updateTime = () => {
  const now = new Date()
  time.value = now.toLocaleTimeString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

// Gõ tay để lọc list ProductGrid (dùng tạm vì product list đang là mock).
// Việc lọc thật được làm trong posStore.searchQuery / filteredProducts.
const handleSearch = () => {
  posStore.searchQuery = searchQuery.value
}

// Bấm Enter = coi như vừa bắn mã vạch xong -> gọi API thật tìm theo SKU
// Đây là nghiệp vụ chính: máy quét mã vạch thường gửi chuỗi SKU rồi tự bắn phím Enter
const handleScanSku = async () => {
  const value = searchQuery.value.trim()
  if (!value) return

  const result = await posStore.addToCartBySku(value)

  scanSuccess.value = result.success
  scanMessage.value = result.success
    ? 'Đã thêm vào giỏ hàng'
    : (result.message || 'Có lỗi xảy ra')

  if (scanMessageTimeout) clearTimeout(scanMessageTimeout)
  scanMessageTimeout = setTimeout(() => { scanMessage.value = '' }, 2500)

  if (result.success) {
    searchQuery.value = ''
    posStore.searchQuery = ''
  }
}

// Phím tắt F4 để focus nhanh vào ô tìm kiếm, phục vụ cashier dùng bàn phím/máy quét
const handleGlobalKeydown = (e) => {
  if (e.key === 'F4') {
    e.preventDefault()
    searchInputRef.value?.focus()
  }
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  window.addEventListener('keydown', handleGlobalKeydown)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (scanMessageTimeout) clearTimeout(scanMessageTimeout)
  window.removeEventListener('keydown', handleGlobalKeydown)
})
</script>

<style scoped>
/* HỆ MÀU DARK PREMIUM */
.text-gold { color: #f3c63f !important; }
.text-light-custom { color: #f1f5f9; }
.text-muted-custom { color: #9fb0d3; }
.font-xs { font-size: 0.75rem; }
.fw-black { font-weight: 900 !important; }

/* CHỮ THƯƠNG HIỆU ĐỔ GRADIENT VÀNG KIM */
.brand-text { 
  letter-spacing: 1.5px; 
  font-size: 1.25rem; 
  font-weight: 900 !important;
  background: linear-gradient(to right, #f3c63f, #ffdf73);
  -webkit-text-fill-color: transparent;
}

/* THIẾT KẾ BADGE VÀ BUTTON */
.dark-time, .user-pill { 
  background-color: #131c31; 
  border: 1px solid #222f4f; 
}

/* KHU VỰC CHỈNH Ô TÌM KIẾM */
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
.btn-clear-search:hover i {
  color: #ef4444 !important;
}

/* Ô hiển thị đồng hồ */
.dark-time {
  color: #f3c63f;
  letter-spacing: 0.5px;
  box-shadow: inset 0 0 6px rgba(0, 0, 0, 0.2);
}

/* Viên nhộng user thông tin tài khoản */
.user-pill {
  cursor: pointer;
}
.user-pill:hover {
  border-color: #384f80;
  background-color: #17223b;
}

/* Avatar tròn chữ cái đầu */
.avatar-gold {
  background-color: #f3c63f !important;
  color: #000000 !important;
}

.transition-all {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.text-nowrap {
  white-space: nowrap !important;
}

</style>