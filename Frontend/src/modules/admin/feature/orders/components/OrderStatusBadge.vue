<template>
  <span class="badge" :class="badgeClass">
    {{ label }}
  </span>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  status: number;
  statusText?: string | null;
}>();

const label = computed(() => {
  switch (props.status) {
    case 0:
      return "Chờ xác nhận";
    case 1:
      return "Đã xác nhận";
    case 2:
      return "Đang giao hàng";
    case 3:
      return "Hoàn thành";
    case 4:
      return "Đã hủy";
    case 5:
      return "Giao hàng thất bại";
    case 6:
      return "Yêu cầu hoàn hàng";
    case 7:
      return "Hoàn hàng hoàn tất";
    default:
      return props.statusText || "Không xác định";
  }
});

const badgeClass = computed(() => {
  switch (props.status) {
    case 0:
      return "text-bg-warning";
    case 1:
      return "text-bg-primary";
    case 2:
      return "text-bg-info";
    case 3:
      return "text-bg-success";
    case 4:
      return "text-bg-danger";
    case 5:
      return "text-bg-dark";
    case 6:
      return "text-bg-secondary";
    case 7:
      return "text-bg-success";
    default:
      return "text-bg-light text-dark";
  }
});
</script>