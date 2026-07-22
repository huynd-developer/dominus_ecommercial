<template>
  <a-tag :color="color">
    {{ text }}
  </a-tag>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  status: number;
}>();

const color = computed(() => {
  switch (props.status) {
    case 0:
      return "orange";

    case 1:
      return "blue";

    case 2:
      return "cyan";

    case 3:
      return "green";

    case 4:
      return "red";

    case 5:
      return "volcano";

    case 6:
      return "gold";

    case 7:
      return "purple";

    default:
      return "default";
  }
});

const text = computed(() => {
  switch (props.status) {
    case 0:
      return "Chờ xác nhận";

    case 1:
      return "Đã xác nhận";

    case 2:
      return "Đang giao";

    case 3:
      return "Hoàn thành";

    case 4:
      return "Đã hủy";

    case 5:
      return "Giao thất bại";

    case 6:
      return "Yêu cầu hoàn";

    case 7:
      return "Hoàn tất hoàn";

    default:
      return "Không xác định";
  }
});
</script>