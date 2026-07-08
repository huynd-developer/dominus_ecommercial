<template>
  <div :class="['order-result-card', modeClass]">
    <div v-if="mode === 'processing'" class="loading-circle"></div>

    <div v-else class="result-icon">
      <i v-if="mode === 'success'" class="bi bi-check-lg"></i>
      <i v-else class="bi bi-x-lg"></i>
    </div>

    <h2>{{ title }}</h2>

    <p class="result-message">
      {{ message }}
    </p>

    <div v-if="details.length > 0" class="result-details">
      <div
        v-for="item in details"
        :key="item.label"
        class="detail-line"
      >
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </div>

    <button
      v-if="primaryText"
      type="button"
      class="btn-primary-action"
      @click="$emit('primary')"
    >
      {{ primaryText }}
      <i v-if="primaryIcon" :class="primaryIcon"></i>
    </button>

    <button
      v-if="secondaryText"
      type="button"
      class="btn-secondary-action"
      @click="$emit('secondary')"
    >
      {{ secondaryText }}
    </button>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

type ResultMode = "success" | "failed" | "processing";

const props = withDefaults(
  defineProps<{
    mode: ResultMode;
    title: string;
    message: string;
    details?: {
      label: string;
      value: string | number;
    }[];
    primaryText?: string;
    secondaryText?: string;
    primaryIcon?: string;
  }>(),
  {
    details: () => [],
    primaryText: "",
    secondaryText: "",
    primaryIcon: "",
  }
);

defineEmits<{
  (e: "primary"): void;
  (e: "secondary"): void;
}>();

const modeClass = computed(() => {
  return {
    success: "success-card",
    failed: "failed-card",
    processing: "processing-card",
  }[props.mode];
});
</script>

<style scoped>
.order-result-card {
  background: #ffffff;
  padding: 44px 38px;
  border-radius: 16px;
  box-shadow: 0 18px 60px rgba(0, 0, 0, 0.12);
  text-align: center;
  max-width: 480px;
  width: 100%;
  border-top: 4px solid #b78d52;
}

.success-card {
  border-top-color: #10b981;
}

.failed-card {
  border-top-color: #ef4444;
}

.processing-card {
  border-top-color: #b78d52;
}

.result-icon {
  width: 74px;
  height: 74px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  font-size: 36px;
  font-weight: 800;
}

.success-card .result-icon {
  background: #dcfce7;
  color: #16a34a;
}

.failed-card .result-icon {
  background: #fee2e2;
  color: #dc2626;
}

h2 {
  color: #06132b;
  margin: 0 0 12px;
  font-size: 26px;
  font-weight: 800;
}

.result-message {
  color: #64748b;
  font-size: 15px;
  margin-bottom: 24px;
  line-height: 1.6;
}

.result-details {
  background: #f8fafc;
  border: 1px dashed #cbd5e0;
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 24px;
  text-align: left;
}

.detail-line {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  font-size: 14px;
  color: #64748b;
  margin-bottom: 8px;
}

.detail-line:last-child {
  margin-bottom: 0;
}

.detail-line strong {
  color: #06132b;
  text-align: right;
  word-break: break-word;
}

.btn-primary-action,
.btn-secondary-action {
  width: 100%;
  padding: 15px;
  border-radius: 8px;
  font-weight: 800;
  cursor: pointer;
  transition: 0.2s;
  font-size: 15px;
  margin-top: 10px;
}

.btn-primary-action {
  background: #06132b;
  color: white;
  border: none;
}

.btn-primary-action:hover {
  background: #b78d52;
  transform: translateY(-1px);
}

.btn-secondary-action {
  background: transparent;
  color: #06132b;
  border: 1px solid #06132b;
}

.btn-secondary-action:hover {
  background: #f8fafc;
  color: #b78d52;
  border-color: #b78d52;
}

.loading-circle {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  border: 5px solid #e5e7eb;
  border-top-color: #b78d52;
  margin: 0 auto 24px;
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>