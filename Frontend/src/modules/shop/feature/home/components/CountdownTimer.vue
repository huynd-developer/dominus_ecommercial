<template>
  <div class="countdown-timer d-flex align-items-center gap-2">
    <div class="time-box">
      <span>{{ hours }}</span>
      <small>Giờ</small>
    </div>

    <span class="time-separator">:</span>

    <div class="time-box">
      <span>{{ minutes }}</span>
      <small>Phút</small>
    </div>

    <span class="time-separator">:</span>

    <div class="time-box">
      <span>{{ seconds }}</span>
      <small>Giây</small>
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
  font-family: "Segoe UI", Arial, sans-serif;
}

.time-box {
  min-width: 54px;
  height: 46px;
  border-radius: 10px;
  background: #ffffff;
  border: 1px solid rgba(189, 154, 95, 0.26);
  color: var(--aura-black);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 22px rgba(5, 16, 36, 0.055);
}

.time-box span {
  font-family: "Consolas", "Courier New", monospace;
  font-size: 18px;
  line-height: 1;
  font-weight: 800;
}

.time-box small {
  margin-top: 3px;
  font-size: 9px;
  text-transform: uppercase;
  color: var(--aura-muted);
  font-weight: 700;
}

.time-separator {
  font-size: 18px;
  font-weight: 800;
  color: var(--aura-gold);
}
</style>