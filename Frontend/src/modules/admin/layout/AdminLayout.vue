<template>
  <div class="admin-layout">
    <TopHeader />

    <main class="content-wrapper">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import TopHeader from "./TopHeader.vue";
</script>

<style scoped>
.admin-layout{
    min-height:100vh;
    background:#f8fafc;
}

/* Sidebar rộng 260px */
.content-wrapper{
    margin-left:260px;

    padding:30px;

    min-height:100vh;

    overflow:auto;
}
</style>