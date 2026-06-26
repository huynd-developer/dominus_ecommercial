<template>
  <div class="countdown-timer d-flex align-items-center gap-2">
    <div class="time-box">
      <span>{{ hours }}</span>
    </div>

    <span class="time-separator">:</span>

    <div class="time-box">
      <span>{{ minutes }}</span>
    </div>

    <span class="time-separator">:</span>

    <div class="time-box">
      <span>{{ seconds }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
const hours = '02';
const minutes = '15';
const seconds = '40';
</script>

<style scoped>
.countdown-timer {
  font-family: 'Courier New', monospace;
}

.time-box {
  width: 46px;
  height: 38px;
  border-radius: 8px;
  background: #f1f3f5;
  border: 1px solid #dee2e6;
  color: #111827;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 18px;
}

.time-separator {
  font-size: 18px;
  font-weight: 800;
  color: #0a192f;
}
</style>