<template>
  <div class="app">
    <ErrorBookToast v-if="toastVisible" />
    <router-view />
  </div>
</template>

<script setup>
import { storeToRefs } from 'pinia'
import { useToastStore } from './stores/toast'
import ErrorBookToast from './components/ErrorBookToast.vue'

const { visible: toastVisible } = storeToRefs(useToastStore())
</script>

<style>
:root {
  font-size: clamp(13px, 0.18vw + 12px, 16px);
  --app-bg: #f7f9fb;
  --panel-bg: #ffffff;
  --panel-bg-strong: rgba(255, 255, 255, 0.95);
  --panel-border: rgba(114, 119, 133, 0.12);
  --panel-shadow: 0 28px 60px rgba(25, 28, 30, 0.06);
  --panel-radius: 24px;
  --panel-radius-sm: 18px;
  --page-gutter: clamp(0.875rem, 0.7rem + 0.9vw, 2rem);
  --section-gap: clamp(1rem, 0.8rem + 1vw, 2rem);
  --control-height: clamp(2.5rem, 2.35rem + 0.5vw, 3rem);
  --control-radius: clamp(0.875rem, 0.8rem + 0.3vw, 1rem);
  --text-primary: #191c1e;
  --text-secondary: #414754;
  --text-muted: #727785;
  --brand-blue: #005bbf;
  --brand-green: #2f8f5b;
  --brand-purple: #4355b9;
  --brand-orange: #9e4300;
}

* {
  box-sizing: border-box;
}

html {
  background: var(--app-bg);
}

body {
  margin: 0;
  color: var(--text-primary);
  font-family: 'Inter', 'PingFang SC', 'SF Pro Text', 'Helvetica Neue', Arial, sans-serif;
  background: var(--app-bg);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

a {
  color: inherit;
}

button,
input,
select,
textarea {
  font: inherit;
}

#app,
.app {
  min-height: 100vh;
}

.app {
  background: transparent;
}

.el-button {
  border-radius: var(--control-radius);
  font-weight: 700;
  min-height: var(--control-height);
  box-shadow: none;
  border: none;
}

.el-button--primary {
  --el-button-bg-color: #005bbf;
  --el-button-border-color: transparent;
  --el-button-hover-bg-color: #1a73e8;
  --el-button-hover-border-color: transparent;
  --el-button-active-bg-color: #0051a8;
  box-shadow: 0 18px 32px rgba(0, 91, 191, 0.18);
}

.el-input__wrapper,
.el-textarea__inner,
.el-date-editor.el-input__wrapper {
  border-radius: var(--control-radius) !important;
  box-shadow: none !important;
  border: 1px solid transparent !important;
  background: #f2f4f6 !important;
  min-height: var(--control-height);
}

.el-input__wrapper.is-focus,
.el-textarea__inner:focus,
.el-date-editor.el-input__wrapper.is-focus {
  border-color: rgba(0, 91, 191, 0.2) !important;
  box-shadow: 0 0 0 4px rgba(0, 91, 191, 0.08) !important;
}

.el-dialog {
  overflow: hidden;
  border-radius: clamp(1.25rem, 1rem + 0.8vw, 1.75rem);
  border: 1px solid rgba(255, 255, 255, 0.75);
  box-shadow: var(--panel-shadow);
  background: var(--panel-bg-strong);
  backdrop-filter: blur(24px);
}

.el-dialog__header,
.el-dialog__body,
.el-dialog__footer {
  padding-left: var(--page-gutter);
  padding-right: var(--page-gutter);
}

.el-table {
  --el-table-border-color: transparent;
  --el-table-header-bg-color: #f2f4f6;
  --el-table-row-hover-bg-color: #f8fafc;
  --el-table-tr-bg-color: transparent;
  border-radius: 22px;
  overflow: hidden;
  background: transparent;
}

.el-table th.el-table__cell {
  color: var(--text-secondary);
  font-weight: 700;
  border-bottom: none;
}

.el-table td.el-table__cell,
.el-table--border::before,
.el-table::before {
  border-bottom: none;
}

.el-tag,
.el-pagination .btn-next,
.el-pagination .btn-prev,
.el-pagination .el-pager li {
  border-radius: 999px;
  font-weight: 700;
}

.el-alert,
.el-empty {
  border-radius: 20px;
}

.el-switch.is-checked .el-switch__core {
  background: #005bbf;
  border-color: #005bbf;
}

.el-input__inner::placeholder,
.el-textarea__inner::placeholder {
  color: #9aa1b3;
}

::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.5);
}

@media (max-width: 768px) {
  :root {
    font-size: clamp(12px, 0.9vw + 10px, 14px);
  }
}
</style>
