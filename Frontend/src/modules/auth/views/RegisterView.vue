<template>
  <div class="auth-view-wrapper">
    <RegisterForm />
  </div>
</template>

<script setup lang="ts">
import RegisterForm from '../components/RegisterForm.vue';
</script>

<style scoped>
.auth-view-wrapper {
  background: radial-gradient(circle at 50% 50%, #112240 0%, #0a1931 100%);
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
</style>