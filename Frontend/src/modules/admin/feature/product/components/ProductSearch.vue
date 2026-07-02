<template>
  <div class="search-toolbar mb-4">
    <div class="row align-items-center g-2">

      <!-- Search -->
      <div class="col-lg">
        <div class="search-box">

          <i class="bi bi-search search-icon"></i>

          <input
              v-model="keyword"
              type="text"
              class="search-input"
              placeholder="Nhập tên sản phẩm..."
              @input="handleSearch"
          />

          <button
              v-if="keyword"
              class="clear-btn"
              @click="clearKeyword"
          >
            <i class="bi bi-x-lg"></i>
          </button>

        </div>
      </div>

      <!-- Refresh -->
      <div class="col-lg-auto">
        <button
            class="refresh-btn"
            @click="refresh"
        >
          <i class="bi bi-arrow-clockwise me-2"></i>
          Làm mới
        </button>
      </div>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from "vue";

const emit = defineEmits<{
  (e: "search", keyword: string): void;
}>();

const keyword = ref("");

const handleSearch = () => {
  emit("search", keyword.value.trim());
};

const clearKeyword = () => {
  keyword.value = "";
  emit("search", "");
};

const refresh = () => {
  keyword.value = "";
  emit("search", "");
};
</script>

<style scoped>

/* ===========================
        TOOLBAR
=========================== */

.search-toolbar{
    width:100%;
}

/* ===========================
        SEARCH BOX
=========================== */

.search-box{

    display:flex;
    align-items:center;

    height:58px;

    background:#fff;

    border:1px solid #e5e7eb;

    border-radius:16px;

    transition:.25s;

    overflow:hidden;

    box-shadow:
            0 2px 8px rgba(15,23,42,.03);

}

.search-box:hover{

    border-color:#3b82f6;

}

.search-box:focus-within{

    border-color:#2563eb;

    box-shadow:
            0 0 0 4px rgba(37,99,235,.08);

}

/* icon */

.search-icon{

    width:58px;

    text-align:center;

    font-size:20px;

    color:#2563eb;

}

/* input */

.search-input{

    flex:1;

    height:100%;

    border:none;

    outline:none;

    background:transparent;

    font-size:15px;

    color:#1e293b;

    padding-right:10px;

}

.search-input::placeholder{

    color:#94a3b8;

}

/* clear */

.clear-btn{

    width:52px;

    height:100%;

    border:none;

    background:none;

    color:#94a3b8;

    transition:.2s;

}

.clear-btn:hover{

    color:#ef4444;

}

/* ===========================
        BUTTON
=========================== */

.refresh-btn{

    height:58px;

    min-width:155px;

    border-radius:16px;

    border:1px solid #2563eb;

    background:#fff;

    color:#2563eb;

    font-weight:600;

    transition:.25s;

}

.refresh-btn:hover{

    background:#2563eb;

    color:#fff;

    transform:translateY(-2px);

    box-shadow:0 10px 22px rgba(37,99,235,.25);

}

/* ===========================
        MOBILE
=========================== */

@media(max-width:991px){

    .refresh-btn{

        width:100%;
        margin-top:8px;

    }

}

</style>