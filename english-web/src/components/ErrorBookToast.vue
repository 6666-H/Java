<template>
  <Transition name="toast">
    <div v-if="visible" class="error-book-toast">
      <span class="icon">📕</span>
      <span>已加入今日错题本</span>
    </div>
  </Transition>
</template>

<script setup>
import { storeToRefs } from 'pinia'
import { useToastStore } from '../stores/toast'

const { visible } = storeToRefs(useToastStore())
</script>

<style scoped>
.error-book-toast {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
  font-size: 14px;
  font-weight: 500;
}
.icon { font-size: 18px; }
.toast-enter-active, .toast-leave-active { transition: opacity 0.3s ease, transform 0.3s ease; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateX(20px); }
</style>
