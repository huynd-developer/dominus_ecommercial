<template>
  <header class="d-flex justify-content-between align-items-center mb-2 px-0 pt-1 pb-1 select-none header-premium w-100 overflow-hidden gap-3">
    <div class="flex-grow-1 max-w-search mx-md-4 mx-2">
      <div class="search-premium-box position-relative d-flex align-items-center w-100">
        <i class="bi bi-search text-muted-custom position-absolute start-0 ms-3 font-xs"></i>
        <input 
          ref="searchInputRef"
          type="text" 
          v-model="scanInput" 
          placeholder="Dùng súng quét mã vạch (SKU) hoặc gõ tìm kiếm (F4)..." 
          class="search-premium-input w-100 rounded-3 font-xs text-light transition-all"
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
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { usePosStore } from '../stores/posStore'

const posStore = usePosStore()
const time = ref('')
const scanInput = ref('')
const searchInputRef = ref(null)
let timer = null

const updateTime = () => {
  const now = new Date()
  time.value = now.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}

// Cập nhật text tìm kiếm cho lưới sản phẩm
const handleSearch = () => {
  posStore.searchQuery = scanInput.value
}

// Xử lý súng quét mã vạch (súng tự động bắn phím Enter sau khi scan)
const handleScanSku = async () => {
  const sku = scanInput.value.trim()
  if (!sku) return

  await posStore.handleBarcodeScan(sku)
  
  // Quét xong thì xóa trắng ô input để chuẩn bị quét món tiếp theo
  scanInput.value = ''
  posStore.searchQuery = ''
}

const clearSearch = () => {
  scanInput.value = ''
  posStore.searchQuery = ''
  searchInputRef.value?.focus()
}

// Phím tắt F4 để focus nhanh vào ô tìm kiếm
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
  // Tự động focus vào ô quét mã khi vừa mở app
  searchInputRef.value?.focus()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('keydown', handleGlobalKeydown)
})
</script>

<style scoped>
.text-gold { color: #f3c63f !important; }
.text-muted-custom { color: #9fb0d3; }
.font-xs { font-size: 0.75rem; }
.dark-time { background-color: #131c31; border: 1px solid #222f4f; color: #f3c63f; letter-spacing: 0.5px; }
.max-w-search { max-width: 480px; }
.search-premium-input { background-color: #131c31; border: 1px solid #222f4f; padding: 8px 32px 8px 36px; outline: none; }
.search-premium-input:focus { border-color: #384f80; background-color: #17223b; box-shadow: 0 0 10px rgba(56, 79, 128, 0.2); }
.search-premium-input::placeholder { color: #4b5e84; }
.btn-clear-search:hover i { color: #ef4444 !important; }
.transition-all { transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1); }
</style>