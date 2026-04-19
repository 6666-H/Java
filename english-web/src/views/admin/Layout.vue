<template>
  <div class="admin-shell" :class="`variant-${shellVariant}`">
    <aside class="sidebar">
      <div class="brand-block">
        <div class="brand-logo">
          <el-icon><Reading /></el-icon>
        </div>
        <div class="brand-copy">
          <strong>{{ brandTitle }}</strong>
          <span>{{ brandSubtitle }}</span>
        </div>
      </div>

      <nav class="nav-list">
        <router-link
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="nav-item"
          :class="{ active: route.path.startsWith(item.to) }"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <template v-if="shellVariant === 'studio'">
<!--        <button type="button" class="sidebar-cta" @click="routeToCreateTenant">
          <el-icon><Plus /></el-icon>
          <span>创建新机构</span>
        </button>-->

        <div class="sidebar-links">
          <button type="button" class="sidebar-link">
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </button>
          <button type="button" class="sidebar-link" @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            <span>退出登录</span>
          </button>
        </div>
      </template>

      <div v-else class="sidebar-profile">
        <div class="avatar">{{ profileInitial }}</div>
        <div class="profile-copy">
          <strong>{{ authStore.nickname || authStore.username || '平台管理员' }}</strong>
          <span>{{ authStore.username || 'admin@yongsheng.edu.cn' }}</span>
        </div>
      </div>
    </aside>

    <main class="main-panel">
      <header class="topbar">
        <div class="topbar-left">
          <div v-if="shellVariant === 'classic'" class="crumbs">
            <span>永升管理中心</span>
            <el-icon><ArrowRight /></el-icon>
            <span class="active">{{ currentPage.section }}</span>
          </div>
          <div v-else class="top-links">
            <a v-for="item in currentTopLinks" :key="item" href="javascript:void(0)">{{ item }}</a>
          </div>
          <h1>{{ currentPage.title }}</h1>
        </div>

        <div class="topbar-right">
          <label class="searchbox">
            <el-icon><Search /></el-icon>
            <input v-model="searchText" :placeholder="currentPage.searchPlaceholder" type="text" />
          </label>
          <button type="button" class="icon-btn notification-btn" aria-label="通知">
            <el-icon><Bell /></el-icon>
            <span class="dot"></span>
          </button>
          <button v-if="shellVariant === 'classic'" type="button" class="icon-btn" @click="handleLogout" aria-label="退出">
            <el-icon><SwitchButton /></el-icon>
          </button>
          <div v-else class="profile-mini">
            <div class="profile-mini-copy">
              <strong>{{ authStore.nickname || '管理员用户' }}</strong>
              <span>超级管理员</span>
            </div>
            <div class="profile-mini-avatar">{{ profileInitial }}</div>
          </div>
        </div>
      </header>

      <section class="page-slot">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowRight,
  Bell,
  Key,
  OfficeBuilding,
  Reading,
  Search,
  Setting,
  SwitchButton,
  User
} from '@element-plus/icons-vue'
import { logout as logoutApi } from '../../api/auth'
import { getSystemSettings } from '../../api/admin'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const searchText = ref('')
const settings = ref({ systemName: '永升教育系统' })
let searchSyncTimer = null

const navItems = [
  { label: '机构管理', to: '/admin/tenants', icon: OfficeBuilding },
  { label: '教材管理', to: '/admin/content', icon: Reading },
  { label: '授权管理', to: '/admin/permissions', icon: Key },
  { label: '全局设置', to: '/admin/settings', icon: Setting }
]

const shellVariant = computed(() => (route.path.startsWith('/admin/permissions') || route.path.startsWith('/admin/settings') ? 'studio' : 'classic'))

const currentPage = computed(() => {
  if (route.path.startsWith('/admin/content')) {
    return {
      title: `${settings.value.systemName || '永升教育系统'} - 教材内容管理`,
      section: '教材管理',
      searchPlaceholder: '搜索教材...'
    }
  }
  if (route.path.startsWith('/admin/permissions')) {
    return {
      title: '授权管理',
      section: '授权管理',
      searchPlaceholder: '搜索授权、机构...'
    }
  }
  if (route.path.startsWith('/admin/settings')) {
    return {
      title: '全局设置',
      section: '系统管理',
      searchPlaceholder: '搜索功能或参数...'
    }
  }
  return {
    title: `${settings.value.systemName || '永升教育系统'} - 机构管理`,
    section: '机构管理',
    searchPlaceholder: '搜索机构名称...'
  }
})

const brandTitle = computed(() => settings.value.systemName || '永升教育系统')
const brandSubtitle = computed(() => '全球管理中心')
const currentTopLinks = computed(() => (shellVariant.value === 'studio' ? ['仪表盘', '系统报告', '帮助中心'] : ['控制台', '教材库', '学习进度']))
const profileInitial = computed(() => String(authStore.nickname || authStore.username || 'A').trim().slice(0, 1).toUpperCase())
const isBookContentPage = computed(() => route.path.startsWith('/admin/content'))

async function handleLogout() {
  try {
    await logoutApi()
  } catch (_) {}
  authStore.logout('SUPER_ADMIN')
  router.push('/login')
}

function syncSearchTextFromRoute() {
  const routeKeyword = isBookContentPage.value ? String(route.query.keyword || '') : ''
  if (searchText.value !== routeKeyword) {
    searchText.value = routeKeyword
  }
}

function updateRouteKeyword(keyword) {
  if (!isBookContentPage.value) return
  const trimmedKeyword = keyword.trim()
  const currentKeyword = String(route.query.keyword || '')
  if (trimmedKeyword === currentKeyword) return

  const nextQuery = { ...route.query }
  if (trimmedKeyword) nextQuery.keyword = trimmedKeyword
  else delete nextQuery.keyword

  router.replace({ path: route.path, query: nextQuery })
}

watch(() => route.fullPath, () => {
  syncSearchTextFromRoute()
}, { immediate: true })

watch(searchText, (value) => {
  if (!isBookContentPage.value) return
  if (searchSyncTimer) window.clearTimeout(searchSyncTimer)
  searchSyncTimer = window.setTimeout(() => updateRouteKeyword(value), 240)
})

onMounted(async () => {
  try {
    settings.value = { ...(await getSystemSettings()) }
  } catch (_) {}
})

onBeforeUnmount(() => {
  if (searchSyncTimer) window.clearTimeout(searchSyncTimer)
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=Manrope:wght@500;600;700;800&display=swap');

.admin-shell {
  --admin-sidebar-width: clamp(12.5rem, 15vw, 13.5rem);
  --admin-page-padding: clamp(0.875rem, 0.7rem + 0.8vw, 1.5rem);
  min-height: 100vh;
  display: grid;
  grid-template-columns: var(--admin-sidebar-width) minmax(0, 1fr);
  background:
    radial-gradient(circle at top left, rgba(216, 226, 255, 0.9), transparent 22%),
    linear-gradient(180deg, #f7f9fb 0%, #f3f6fb 100%);
}

.sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  padding: 1.25rem 0.75rem 1.125rem;
  display: flex;
  flex-direction: column;
  background: rgba(248, 250, 252, 0.95);
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 2px 8px 18px;
}

.brand-logo {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(135deg, #005bbf, #1a73e8);
  box-shadow: 0 14px 28px rgba(0, 91, 191, 0.22);
}

.brand-copy strong,
.profile-copy strong,
.profile-mini-copy strong {
  display: block;
  font-family: 'Manrope', 'PingFang SC', sans-serif;
  font-weight: 800;
}

.brand-copy strong {
  font-size: 18px;
  line-height: 1;
  letter-spacing: -0.03em;
  color: #1e3a8a;
}

.brand-copy span,
.profile-copy span,
.profile-mini-copy span {
  display: block;
  margin-top: 4px;
  color: #74809a;
  font-size: 11px;
}

.nav-list {
  display: grid;
  gap: 4px;
}

.nav-item {
  position: relative;
  min-height: 48px;
  padding: 0 16px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #5b6478;
  text-decoration: none;
  font-size: 14px;
  font-weight: 700;
  transition: all 0.2s ease;
}

.nav-item:hover,
.nav-item.active {
  color: #0f5fd0;
  background: rgba(255, 255, 255, 0.88);
}

.nav-item.active::after {
  content: '';
  position: absolute;
  top: 8px;
  right: -12px;
  width: 4px;
  height: calc(100% - 16px);
  border-radius: 999px;
  background: #0f5fd0;
}

.sidebar-profile,
.sidebar-links {
  margin-top: auto;
}

.sidebar-profile {
  padding: 14px 8px 6px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar,
.profile-mini-avatar {
  width: 42px;
  height: 42px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(135deg, #0b5fd1, #173c91);
  font-weight: 800;
}

.profile-copy strong {
  color: #111827;
  font-size: 14px;
}

.main-panel {
  min-width: 0;
  padding: 0.5rem var(--admin-page-padding) var(--admin-page-padding);
}

.topbar {
  min-height: 74px;
  padding: 0.5rem 0.25rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.topbar-left h1 {
  margin: 0;
  font-family: 'Manrope', 'PingFang SC', sans-serif;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.03em;
  color: #181c23;
}

.crumbs,
.top-links {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #7b8190;
  font-size: 12px;
  font-weight: 600;
}

.crumbs .active {
  color: #0b5fd1;
}

.top-links a {
  color: #7b8190;
  text-decoration: none;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.searchbox {
  width: min(20rem, 32vw);
  min-height: 2.75rem;
  padding: 0 0.875rem;
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: 14px;
  background: rgba(230, 232, 234, 0.95);
  color: #727785;
}

.searchbox input {
  width: 100%;
  border: none;
  outline: none;
  background: transparent;
  color: #1f2937;
}

.icon-btn {
  position: relative;
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.82);
  color: #4b5563;
  cursor: pointer;
}

.notification-btn .dot {
  position: absolute;
  top: 8px;
  right: 9px;
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #ef4444;
}

.profile-mini {
  padding-left: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-left: 1px solid rgba(193, 198, 214, 0.5);
}

.profile-mini-copy strong {
  font-size: 12px;
  color: #111827;
}

.profile-mini-avatar {
  width: 36px;
  height: 36px;
  font-size: 13px;
}

.page-slot {
  min-width: 0;
}

.sidebar-cta {
  margin-top: auto;
  margin-bottom: 12px;
  width: 100%;
  height: 46px;
  border: none;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: #0b5fd1;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.sidebar-links {
  padding-top: 14px;
  border-top: 1px solid rgba(203, 213, 225, 0.55);
  display: grid;
  gap: 4px;
}

.sidebar-link {
  height: 42px;
  border: none;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  background: transparent;
  color: #5b6478;
  font-weight: 600;
  cursor: pointer;
}

@media (max-width: 1024px) {
  .admin-shell {
    grid-template-columns: 6rem minmax(0, 1fr);
  }

  .brand-copy,
  .nav-item span,
  .profile-copy,
  .sidebar-link span,
  .sidebar-cta span {
    display: none;
  }

  .sidebar-cta,
  .sidebar-link,
  .nav-item {
    justify-content: center;
    padding: 0;
  }

  .searchbox {
    width: 13.75rem;
  }
}

@media (max-width: 768px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: static;
    height: auto;
    border-bottom: 1px solid rgba(203, 213, 225, 0.55);
  }

  .nav-list {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .main-panel {
    padding: 12px;
  }

  .topbar {
    flex-direction: column;
    align-items: stretch;
  }

  .topbar-right {
    justify-content: space-between;
  }

  .searchbox {
    flex: 1;
    width: auto;
  }
}
</style>
