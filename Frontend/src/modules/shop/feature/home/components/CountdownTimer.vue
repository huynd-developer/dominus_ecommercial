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
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";

const props = defineProps<{
  targetDate?: string | null;
}>();

const emit = defineEmits<{
  (e: "expired"): void;
}>();

const remainingMs = ref(0);
let timer: ReturnType<typeof window.setInterval> | null = null;
let expiredEmitted = false;

const pad = (value: number) => {
  return String(Math.max(0, value)).padStart(2, "0");
};

const updateRemaining = () => {
  if (!props.targetDate) {
    remainingMs.value = 0;
    return;
  }

  const targetTime = new Date(props.targetDate).getTime();
  const now = Date.now();
  const diff = Math.max(0, targetTime - now);

  remainingMs.value = diff;

  if (diff <= 0 && !expiredEmitted) {
    expiredEmitted = true;
    emit("expired");
  }
};

const totalSeconds = computed(() => {
  return Math.floor(remainingMs.value / 1000);
});

const hours = computed(() => {
  return pad(Math.floor(totalSeconds.value / 3600));
});

const minutes = computed(() => {
  return pad(Math.floor((totalSeconds.value % 3600) / 60));
});

const seconds = computed(() => {
  return pad(totalSeconds.value % 60);
});

const startTimer = () => {
  stopTimer();
  expiredEmitted = false;
  updateRemaining();

  timer = window.setInterval(() => {
    updateRemaining();
  }, 1000);
};

const stopTimer = () => {
  if (timer) {
    window.clearInterval(timer);
    timer = null;
  }
};

watch(
  () => props.targetDate,
  () => {
    startTimer();
  }
);

onMounted(() => {
  startTimer();
});

onBeforeUnmount(() => {
  stopTimer();
});
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