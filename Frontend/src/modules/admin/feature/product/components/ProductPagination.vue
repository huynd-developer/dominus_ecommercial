<template>
  <nav v-if="page.totalPages > 1">

    <ul class="pagination justify-content-center align-items-center mb-0">

      <!-- First -->
      <li
          class="page-item"
          :class="{ disabled: page.number === 0 }"
      >
        <button
            class="page-link"
            @click="change(0)"
        >
          <i class="bi bi-chevron-double-left"></i>
        </button>
      </li>

      <!-- Previous -->
      <li
          class="page-item"
          :class="{ disabled: page.number === 0 }"
      >
        <button
            class="page-link"
            @click="change(page.number - 1)"
        >
          <i class="bi bi-chevron-left"></i>
        </button>
      </li>

      <!-- Page Numbers -->
      <li
          v-for="item in pages"
          :key="item"
          class="page-item"
          :class="{ active: item === page.number }"
      >
        <button
            class="page-link"
            @click="change(item)"
        >
          {{ item + 1 }}
        </button>
      </li>

      <!-- Next -->
      <li
          class="page-item"
          :class="{ disabled: page.number === page.totalPages - 1 }"
      >
        <button
            class="page-link"
            @click="change(page.number + 1)"
        >
          <i class="bi bi-chevron-right"></i>
        </button>
      </li>

      <!-- Last -->
      <li
          class="page-item"
          :class="{ disabled: page.number === page.totalPages - 1 }"
      >
        <button
            class="page-link"
            @click="change(page.totalPages - 1)"
        >
          <i class="bi bi-chevron-double-right"></i>
        </button>
      </li>

    </ul>

    <div class="text-center text-muted mt-2 small">
      Trang {{ page.number + 1 }} / {{ page.totalPages }}
    </div>

  </nav>
</template>

<script setup lang="ts">
import { computed } from "vue";

interface Page {
  number: number;
  totalPages: number;
}

const props = defineProps<{
  page: Page;
}>();

const emit = defineEmits(["change"]);

const change = (pageNumber: number) => {

  if (
      pageNumber < 0 ||
      pageNumber >= props.page.totalPages
  ) return;

  emit("change", pageNumber);

};

const pages = computed(() => {

  const current = props.page.number;

  const total = props.page.totalPages;

  let start = Math.max(0, current - 2);

  let end = Math.min(total - 1, current + 2);

  if (end - start < 4) {

    if (start === 0) {

      end = Math.min(total - 1, start + 4);

    } else {

      start = Math.max(0, end - 4);

    }

  }

  const arr = [];

  for (let i = start; i <= end; i++) {

    arr.push(i);

  }

  return arr;

});
</script>

<style scoped>

.pagination .page-link{

    border-radius:10px;

    margin:0 3px;

    min-width:42px;

    height:42px;

    display:flex;

    align-items:center;

    justify-content:center;

    transition:.25s;

    font-weight:600;

}

.page-link:hover{

    transform:translateY(-2px);

}

.page-item.active .page-link{

    background:#0d6efd;

    border-color:#0d6efd;

    color:white;

    box-shadow:0 5px 15px rgba(13,110,253,.3);

}

.page-item.disabled .page-link{

    opacity:.5;

    cursor:not-allowed;

}

</style>