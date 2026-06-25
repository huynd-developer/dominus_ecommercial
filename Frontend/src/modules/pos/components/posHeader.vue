<template>
  <header class="d-flex justify-content-between align-items-center mb-3 px-2">
    <div class="d-flex align-items-center gap-4">
      <h3 class="mb-0 fw-bold text-warning letter-spacing-1">DOMINUS</h3>
      <div class="text-secondary d-flex align-items-center gap-2 font-sm">
        <i class="bi bi-geo-alt"></i>
        <span>Chi nhánh Quận 1</span>
      </div>
    </div>
    <div class="d-flex align-items-center gap-4 text-light">
      <div class="d-flex align-items-center gap-2">
        <i class="bi bi-clock"></i>
        <span>{{ currentTime }}</span>
      </div>
      <div class="user-profile d-flex align-items-center gap-2 rounded-pill px-3 py-1 bg-dark-subtle border-secondary">
        <el-avatar :size="32" class="bg-warning text-dark fw-bold">C</el-avatar>
        <div class="d-flex flex-column line-height-1">
          <span class="fw-bold font-sm">Cashier 01</span>
          <span class="text-secondary font-xs">Ca Sáng</span>
        </div>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const currentTime = ref('')
let timer = null

onMounted(() => {
  timer = setInterval(() => {
    const now = new Date()
    currentTime.value = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`
  }, 1000)
})

onUnmounted(() => clearInterval(timer))
</script>