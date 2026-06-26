<template>
  <header class="d-flex justify-content-between align-items-center mb-2 px-1 pt-1">
    <div class="d-flex align-items-center gap-3">
      <h4 class="mb-0 fw-bold text-warning brand-text">DOMINUS POS</h4>
      <span class="badge-branch font-xs px-2 py-0-5 rounded text-muted-custom">
        <i class="bi bi-shop text-warning me-1"></i>Hệ thống Quận 1
      </span>
    </div>
    
    <div class="d-flex align-items-center gap-3 text-light">
      <div class="font-xs dark-time px-2 py-1 rounded">
        <i class="bi bi-clock-fill text-secondary me-1"></i>20:39
      </div>
      <div class="user-pill d-flex align-items-center gap-2 rounded-pill px-3 py-1">
        <el-avatar :size="22" class="bg-warning text-dark fw-bold font-xs">B</el-avatar>
        <span class="fw-bold font-xs text-light">Ngô Boss</span>
      </div>
    </div>
  </header>
</template>

<style scoped>
.brand-text { letter-spacing: 1px; font-size: 1.25rem; color: #facc15 !important; }
.text-muted-custom { color: #94a3b8; }
.font-xs { font-size: 0.75rem; }

.badge-branch { background-color: #111726; border: 1px solid #1e293b; }
.dark-time { background-color: #111726; border: 1px solid #1e293b; color: #cbd5e1; }
.user-pill { background-color: #111726; border: 1px solid #1e293b; }
</style>