<template>
  <div class="org-shell">
    <aside class="org-sidebar">
      <div class="brand-block">
        <div class="brand-copy">
          <strong>{{ brandTitle }}</strong>
          <span>{{ brandSubtitle }}</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <router-link
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="nav-link"
          :class="{ active: route.path.startsWith(item.to) }"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <div v-if="isDashboard" class="sidebar-profile">
        <div class="profile-avatar">{{ avatarMark }}</div>
        <div>
          <strong>{{ authStore.nickname || '管理员' }}</strong>
          <span>{{ tenantName }}</span>
        </div>
      </div>

      <div v-else class="sidebar-meta">
        <a href="javascript:void(0)" class="help-link">
          <el-icon><QuestionFilled /></el-icon>
          <span>帮助文档</span>
        </a>
      </div>
    </aside>

    <main class="org-main">
      <header class="topbar">
        <label class="search-shell">
          <el-icon><Search /></el-icon>
          <input v-model="searchText" :placeholder="searchPlaceholder" type="text" />
        </label>

        <div class="topbar-actions">
          <button type="button" class="icon-btn" aria-label="通知">
            <el-icon><Bell /></el-icon>
            <span v-if="showNoticeDot" class="notice-dot"></span>
          </button>
          <button type="button" class="icon-btn" aria-label="帮助">
            <el-icon><QuestionFilled /></el-icon>
          </button>

          <div class="account-divider"></div>

          <div class="account-chip">
            <div class="account-copy">
              <strong>{{ authStore.username || '管理员账号' }}</strong>
              <span>{{ accountSubLabel }}</span>
            </div>
            <button type="button" class="account-avatar" @click="handleLogout">{{ avatarMark }}</button>
          </div>
        </div>
      </header>

      <div v-if="showBanner" class="alert-banner">
        <div class="alert-copy">
          <el-icon><WarningFilled /></el-icon>
          <span>{{ bannerText }}</span>
        </div>
        <button type="button" class="alert-action">{{ alertActionLabel }}</button>
      </div>

      <section class="page-shell">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Bell,
  DataAnalysis,
  Document,
  DocumentCopy,
  QuestionFilled,
  Search,
  Setting,
  UserFilled,
  WarningFilled
} from '@element-plus/icons-vue'
import { logout as logoutApi } from '../../api/auth'
import request from '../../api/request'
import { useAuthStore } from '../../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const searchText = ref('')

const tenantInfo = reactive({
  name: '',
  daysRemaining: 0
})

const navItems = [
  { label: '仪表盘', to: '/org/dashboard', icon: DataAnalysis },
  { label: '学生管理', to: '/org/students', icon: UserFilled },
  { label: '任务管理', to: '/org/tasks', icon: Document },
  { label: '报表中心', to: '/org/reports', icon: DocumentCopy },
  { label: '系统设置', to: '/org/settings', icon: Setting }
]

const isDashboard = computed(() => route.path.startsWith('/org/dashboard'))
const isStudents = computed(() => route.path.startsWith('/org/students'))
const tenantName = computed(() => tenantInfo.name || authStore.nickname || '永盛教育')
const brandTitle = computed(() => '永盛教育')
const brandSubtitle = computed(() => '机构管理后台')
const avatarMark = computed(() => String(authStore.nickname || authStore.username || 'A').trim().slice(0, 1).toUpperCase())
const showBanner = computed(() => (isDashboard.value || isStudents.value) && Number(tenantInfo.daysRemaining || 0) <= 15)
const bannerText = computed(() => {
  const days = Number(tenantInfo.daysRemaining || 0)
  if (days >= 0) return `服务预警：您的机构订阅服务将于 ${days} 天后到期，请及时续费以避免影响正常使用。`
  return '服务已到期，请尽快续费恢复机构端功能。'
})
const alertActionLabel = computed(() => '立即续费')
const showNoticeDot = computed(() => isStudents.value)
const accountSubLabel = computed(() => (isDashboard.value ? '' : '系统总监'))
const searchPlaceholder = computed(() => {
  if (route.path.startsWith('/org/students')) return '搜索学生姓名、ID或手机号...'
  if (route.path.startsWith('/org/tasks')) return '搜索任务、学生或课程单元...'
  if (route.path.startsWith('/org/reports')) return '搜索报表或学生...'
  if (route.path.startsWith('/org/settings')) return '搜索功能或设置...'
  return '搜索功能或设置...'
})

async function fetchTenantInfo() {
  try {
    const res = await request.get('/tenant/info')
    tenantInfo.name = res?.name || authStore.nickname || '永盛教育'
    tenantInfo.daysRemaining = Number(res?.daysRemaining || 0)
  } catch (_error) {
    tenantInfo.name = authStore.nickname || '永盛教育'
    tenantInfo.daysRemaining = 0
  }
}

async function handleLogout() {
  try {
    await logoutApi()
  } catch (_error) {}
  authStore.logout('ORG_ADMIN')
  router.push('/login')
}

onMounted(fetchTenantInfo)
</script>

<style scoped>
.org-shell {
  --org-sidebar-width: clamp(13rem, 16vw, 16rem);
  --org-page-padding: clamp(0.875rem, 0.7rem + 0.8vw, 1.5rem);
  min-height: 100vh;
  display: grid;
  grid-template-columns: var(--org-sidebar-width) minmax(0, 1fr);
  background: #f7f9fb;
}

.org-sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 1.5rem 0;
  background: #f8fafc;
  border-right: 1px solid #e8edf5;
}

.brand-block,
.nav-link,
.help-link,
.sidebar-profile,
.topbar,
.topbar-actions,
.account-chip,
.alert-copy {
  display: flex;
  align-items: center;
}

.brand-block {
  padding: 0 var(--org-page-padding);
  margin-bottom: 1.5rem;
}

.brand-copy strong,
.brand-copy span,
.account-copy strong,
.account-copy span,
.sidebar-profile strong,
.sidebar-profile span {
  display: block;
}

.brand-copy strong {
  color: #1d4fb8;
  font-family: "Manrope", sans-serif;
  font-size: 1.08rem;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.brand-copy span {
  margin-top: 4px;
  color: #8d9ab0;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.sidebar-nav {
  display: grid;
  gap: 2px;
  padding: 0 12px;
}

.nav-link {
  position: relative;
  gap: 12px;
  min-height: 42px;
  padding: 0 12px;
  border-radius: 12px;
  color: #5f6d82;
  text-decoration: none;
  font-size: 0.92rem;
  font-weight: 700;
  transition: all 0.2s ease;
}

.nav-link:hover {
  color: #2667d7;
  background: #eef3fb;
}

.nav-link.active {
  color: #1c5fd6;
  font-weight: 800;
  background: rgba(239, 245, 255, 0.8);
}

.nav-link.active::after {
  content: '';
  position: absolute;
  top: 8px;
  right: -12px;
  width: 4px;
  height: calc(100% - 16px);
  border-radius: 999px;
  background: #2469df;
}

.sidebar-meta {
  margin-top: auto;
  padding: 0 var(--org-page-padding);
}

.help-link {
  gap: 12px;
  color: #5f6d82;
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 700;
}

.sidebar-profile {
  gap: 12px;
  margin-top: auto;
  padding: 1.5rem var(--org-page-padding);
  border-top: 1px solid #e8edf5;
}

.profile-avatar,
.account-avatar {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #1f3143 0%, #0d2232 100%);
  color: #fff;
  font-size: 0.85rem;
  font-weight: 800;
  cursor: pointer;
}

.sidebar-profile strong,
.account-copy strong {
  color: #1f2735;
  font-size: 0.9rem;
  font-weight: 700;
}

.sidebar-profile span,
.account-copy span {
  margin-top: 4px;
  color: #8e9cb0;
  font-size: 0.7rem;
  font-weight: 600;
}

.org-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 40;
  justify-content: space-between;
  min-height: 3.5rem;
  padding: 0 var(--org-page-padding);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid #edf2f8;
  box-shadow: 0 3px 16px rgba(15, 23, 42, 0.03);
}

.search-shell {
  position: relative;
  width: min(26rem, 100%);
  min-height: 2.25rem;
  padding: 0 0.875rem;
  display: flex;
  align-items: center;
  gap: 10px;
  border-radius: 999px;
  background: #eceff3;
  color: #98a4b6;
}

.search-shell input {
  width: 100%;
  border: none;
  outline: none;
  background: transparent;
  color: #2a3445;
  font-size: 0.9rem;
}

.topbar-actions {
  gap: 10px;
}

.icon-btn {
  position: relative;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #6b778c;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.icon-btn:hover {
  color: #1c67dd;
  background: #f2f5fb;
}

.notice-dot {
  position: absolute;
  top: 6px;
  right: 7px;
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #e64c4c;
  border: 2px solid #fff;
}

.account-divider {
  width: 1px;
  height: 30px;
  margin: 0 6px;
  background: #e4e9f1;
}

.account-chip {
  gap: 10px;
}

.account-copy {
  text-align: right;
}

.alert-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0.75rem var(--org-page-padding);
  background: #ffdacc;
  color: #87451b;
  box-shadow: 0 3px 14px rgba(158, 67, 0, 0.05);
}

.alert-copy {
  gap: 10px;
  font-size: 0.88rem;
  font-weight: 700;
}

.alert-action {
  border: none;
  padding: 7px 14px;
  border-radius: 10px;
  background: #9e4300;
  color: #fff;
  font-size: 0.76rem;
  font-weight: 800;
  cursor: pointer;
}

.page-shell {
  padding: 0.875rem var(--org-page-padding);
}

@media (max-width: 1100px) {
  .org-shell {
    grid-template-columns: 1fr;
  }

  .org-sidebar {
    position: static;
    height: auto;
  }
}

@media (max-width: 720px) {
  .topbar,
  .alert-banner {
    flex-direction: column;
    align-items: stretch;
    height: auto;
    padding: 0.875rem var(--org-page-padding);
  }

  .page-shell {
    padding: 1rem var(--org-page-padding);
  }

  .search-shell {
    width: 100%;
  }
}
</style>
