<template>
  <div class="layout">
    <header class="header">
      <router-link to="/" class="logo">英语单词</router-link>
      <span class="user">{{ auth.username }}</span>
      <button class="logout" @click="logout">退出</button>
    </header>
    <main class="main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

function logout() {
  auth.logout()
  router.push({ name: 'Login' })
}
</script>

<style scoped>
.layout { min-height: 100vh; display: flex; flex-direction: column; }
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem clamp(1rem, 0.8rem + 1vw, 1.5rem);
  background: #fff;
  box-shadow: 0 1px 0 rgba(0,0,0,0.06);
}
.logo { font-size: 1.125rem; font-weight: 700; color: #1a1a2e; text-decoration: none; }
.user { color: #666; font-size: 0.875rem; }
.logout { padding: 0.375rem 0.875rem; border: 1px solid #e0e0e0; border-radius: 0.5rem; background: #fff; cursor: pointer; font-size: 0.875rem; }
.logout:hover { background: #f5f5f5; }
.main { flex: 1; padding: clamp(1rem, 0.8rem + 1vw, 1.5rem); }
</style>
