<template>
  <div class="auth-view-wrapper">
    <LoginForm :isAdminMode="true" />
  </div>
</template>

<script setup lang="ts">
import LoginForm from '../components/LoginForm.vue';
</script>

<style scoped>
.auth-view-wrapper {
  background: radial-gradient(circle at 50% 50%, #172a45 0%, #020c1b 100%);
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
</style>