<template>
  <div class="bg-[#f7f9fb] min-h-screen text-[#191c1e]">
    <template v-if="!isStudyMode">
      <div class="min-h-screen grid md:grid-cols-[220px_minmax(0,1fr)]">
        <aside class="hidden md:flex sticky top-0 h-screen flex-col bg-[#f2f4f6] border-r border-[#e8edf5] py-4 px-3 font-['Manrope']">
          <div class="px-2 pb-5">
            <div class="flex items-center gap-3 mb-4">
              <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-[#005bbf] to-[#1a73e8] text-white flex items-center justify-center shadow-md">
                <el-icon><School /></el-icon>
              </div>
              <div>
                <p class="font-black text-[#1A73E8] text-[15px] leading-none">学习中心</p>
                <p class="text-[11px] text-[#727785] mt-1">持续进步中</p>
              </div>
            </div>
            <button class="w-full h-10 bg-gradient-to-br from-[#005bbf] to-[#1a73e8] text-white rounded-lg text-[13px] font-semibold shadow-md" @click="startStudy">
              开始单元学习
            </button>
          </div>

          <nav class="grid gap-1">
            <button
              v-for="item in sideNavItems"
              :key="item.key"
              class="flex items-center gap-3 px-3 h-10 rounded-xl transition-all text-left text-[13px]"
              :class="activeSideNav === item.key ? 'bg-white text-[#1A73E8] shadow-sm font-semibold' : 'text-[#191c1e] hover:bg-white/60'"
              @click="router.push(item.to)"
            >
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </button>
          </nav>

          <div class="mt-auto pt-4">
            <button
              class="flex items-center gap-3 px-3 h-10 w-full rounded-xl transition-all text-left text-[13px]"
              :class="activeSideNav === 'profile' ? 'bg-white text-[#1A73E8] shadow-sm font-semibold' : 'text-[#191c1e] hover:bg-white/60'"
              @click="router.push('/student/profile')"
            >
              <el-icon><UserFilled /></el-icon>
              <span>个人中心</span>
            </button>
          </div>
        </aside>

        <main class="min-w-0 flex flex-col">
          <header class="sticky top-0 z-40 bg-[#f7f9fb]/92 backdrop-blur-xl border-b border-[#e8edf5]">
            <div class="px-4 py-3 sm:px-5 lg:px-6 space-y-3">
              <div class="flex items-center justify-between gap-4">
                <div class="flex items-center min-w-0">
                  <span class="text-[1.08rem] font-extrabold tracking-tighter text-blue-800 whitespace-nowrap font-['Manrope']">
                    永升教育系统
                  </span>
                </div>

                <div class="flex items-center gap-2 sm:gap-3 shrink-0">
                  <button class="w-9 h-9 rounded-full hover:bg-[#e6e8ea] transition-colors flex items-center justify-center" @click="router.push('/student/notifications')">
                    <el-icon class="text-[#414754]"><Bell /></el-icon>
                  </button>
                  <button class="w-9 h-9 rounded-full overflow-hidden border border-[#c1c6d6] bg-gradient-to-br from-blue-600 to-amber-500 text-white text-sm font-bold" @click="router.push('/student/profile')">
                    <img v-if="authStore.avatar" :src="authStore.avatar" class="w-full h-full object-cover" alt="avatar" />
                    <span v-else>{{ brandMark }}</span>
                  </button>
                </div>
              </div>

              <div class="study-toolbar">
                <div class="study-toolbar-actions">
                  <el-select
                    v-model="selectedBookId"
                    clearable
                    filterable
                    class="study-picker"
                    placeholder="选择课本"
                    :loading="booksLoading"
                    @change="handleBookChange"
                  >
                    <el-option v-for="book in books" :key="book.id" :label="book.name" :value="book.id" />
                  </el-select>

                  <el-select
                    v-model="selectedUnitId"
                    clearable
                    filterable
                    class="study-picker"
                    placeholder="选择单元"
                    :disabled="!selectedBookId || units.length === 0"
                    :loading="unitsLoading"
                    @change="handleUnitChange"
                  >
                    <el-option v-for="unit in units" :key="unit.id" :label="unit.name" :value="unit.id" />
                  </el-select>

                  <button
                    class="h-10 px-4 rounded-xl text-sm font-bold whitespace-nowrap transition-all"
                    :class="selectedUnitId ? 'bg-[#005bbf] text-white shadow-md hover:bg-[#1a73e8]' : 'bg-[#e6e8ea] text-[#727785] cursor-not-allowed'"
                    :disabled="!selectedUnitId"
                    @click="startStudy"
                  >
                    开始单元学习
                  </button>
                </div>
              </div>
            </div>
          </header>

          <section class="flex-1 px-4 py-4 sm:px-5 lg:px-6 lg:py-5">
            <router-view v-slot="{ Component }">
              <transition name="page-fade" mode="out-in">
                <component :is="Component" />
              </transition>
            </router-view>
          </section>
        </main>
      </div>
    </template>

    <template v-else>
      <router-view />
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { ElMessage } from 'element-plus'
import { Bell, Collection, DataBoard, Document, School, UserFilled } from '@element-plus/icons-vue'
import { listBooks, listUnits } from '../../api/books'
import { getStudentLastStudy } from '../../api/student'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const books = ref([])
const units = ref([])
const booksLoading = ref(false)
const unitsLoading = ref(false)
const selectedBookId = ref(null)
const selectedUnitId = ref(null)

const sideNavItems = [
  { key: 'dashboard', label: '仪表盘', to: '/student/home', icon: DataBoard, matches: ['/student/home'] },
  { key: 'review', label: '学习进度', to: '/student/review', icon: Document, matches: ['/student/review', '/student/review-practice'] },
  { key: 'wordbook', label: '学术圈', to: '/student/wordbook', icon: Collection, matches: ['/student/wordbook'] }
]

const isStudyMode = computed(() =>
  route.name === 'StudentStudy' ||
  route.name === 'StudentReviewPractice' ||
  route.name === 'StudentJourneyStudy'
)
const activeSideNav = computed(() => {
  if (route.path.startsWith('/student/profile')) return 'profile'
  return sideNavItems.find(item => item.matches.some(prefix => route.path.startsWith(prefix)))?.key || ''
})
const brandMark = computed(() => {
  const raw = String(authStore.nickname || authStore.username || 'S').trim()
  return raw ? raw.charAt(0).toUpperCase() : 'S'
})

function normalizeId(value) {
  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null
}

function resolvePreferredBookId() {
  return normalizeId(route.query.bookId || route.params.bookId || localStorage.getItem('last_book_id'))
}

function resolvePreferredUnitId() {
  return normalizeId(route.query.unitId || route.params.unitId || localStorage.getItem('last_unit_id'))
}

function persistBookSelection(bookId) {
  if (!bookId) {
    localStorage.removeItem('last_book_id')
    localStorage.removeItem('last_book_name')
    return
  }
  const currentBook = books.value.find(item => Number(item.id) === Number(bookId))
  localStorage.setItem('last_book_id', String(bookId))
  localStorage.setItem('last_book_name', currentBook?.name || '')
}

function persistUnitSelection(unitId) {
  if (!unitId) {
    localStorage.removeItem('last_unit_id')
    localStorage.removeItem('last_unit_name')
    return
  }
  const currentUnit = units.value.find(item => Number(item.id) === Number(unitId))
  localStorage.setItem('last_unit_id', String(unitId))
  localStorage.setItem('last_unit_name', currentUnit?.name || '')
}

async function hydrateServerLastStudy() {
  try {
    const result = await getStudentLastStudy()
    const bookId = normalizeId(result?.bookId)
    const unitId = normalizeId(result?.unitId)
    if (!bookId && !unitId) return
    if (bookId) {
      localStorage.setItem('last_book_id', String(bookId))
      localStorage.setItem('last_book_name', result?.bookName || '')
    }
    if (unitId) {
      localStorage.setItem('last_unit_id', String(unitId))
      localStorage.setItem('last_unit_name', result?.unitName || '')
    }
  } catch (_) {}
}

async function loadUnitsForBook(bookId) {
  const normalizedBookId = normalizeId(bookId)
  if (!normalizedBookId) {
    units.value = []
    selectedUnitId.value = null
    persistUnitSelection(null)
    return
  }

  unitsLoading.value = true
  try {
    const res = await listUnits(normalizedBookId)
    units.value = Array.isArray(res) ? res : []
  } catch (_) {
    units.value = []
  } finally {
    unitsLoading.value = false
  }
}

async function applyBookSelection(bookId, options = {}) {
  const normalizedBookId = normalizeId(bookId)
  if (!normalizedBookId) {
    selectedBookId.value = null
    units.value = []
    selectedUnitId.value = null
    persistBookSelection(null)
    persistUnitSelection(null)
    return
  }

  selectedBookId.value = normalizedBookId
  persistBookSelection(normalizedBookId)
  await loadUnitsForBook(normalizedBookId)

  const preferredUnitId = normalizeId(options.preferredUnitId)
  const hasPreferredUnit = preferredUnitId && units.value.some(item => Number(item.id) === preferredUnitId)
  const fallbackUnitId = options.fallbackToFirstUnit === false ? null : normalizeId(units.value[0]?.id)
  const nextUnitId = hasPreferredUnit ? preferredUnitId : fallbackUnitId

  selectedUnitId.value = nextUnitId
  persistUnitSelection(nextUnitId)
}

async function loadBooksAndSelections() {
  booksLoading.value = true
  try {
    await hydrateServerLastStudy()
    const res = await listBooks()
    books.value = Array.isArray(res) ? res : []
    const preferredBookId = resolvePreferredBookId()
    const hasPreferredBook = preferredBookId && books.value.some(item => Number(item.id) === preferredBookId)

    if (!books.value.length) {
      selectedBookId.value = null
      selectedUnitId.value = null
      units.value = []
      persistBookSelection(null)
      persistUnitSelection(null)
      return
    }

    if (hasPreferredBook) {
      await applyBookSelection(preferredBookId, {
        preferredUnitId: resolvePreferredUnitId(),
        fallbackToFirstUnit: true
      })
      return
    }

    selectedBookId.value = null
    selectedUnitId.value = null
    units.value = []
    if (preferredBookId) {
      persistBookSelection(null)
      persistUnitSelection(null)
    }
  } finally {
    booksLoading.value = false
  }
}

async function handleBookChange(bookId) {
  await applyBookSelection(bookId, { fallbackToFirstUnit: true })
}

function handleUnitChange(unitId) {
  const normalizedUnitId = normalizeId(unitId)
  selectedUnitId.value = normalizedUnitId
  persistUnitSelection(normalizedUnitId)
}

function startStudy() {
  const targetUnitId = normalizeId(selectedUnitId.value || localStorage.getItem('last_unit_id'))
  if (!targetUnitId) {
    ElMessage.warning('请先选择课本和单元')
    return
  }

  const targetBookId = normalizeId(selectedBookId.value || localStorage.getItem('last_book_id'))
  if (targetBookId) persistBookSelection(targetBookId)
  persistUnitSelection(targetUnitId)

  const unitName = units.value.find(item => Number(item.id) === targetUnitId)?.name || localStorage.getItem('last_unit_name') || undefined
  const bookName = books.value.find(item => Number(item.id) === targetBookId)?.name || localStorage.getItem('last_book_name') || undefined
  router.push({
    name: 'StudentStudy',
    params: { unitId: targetUnitId },
    query: { unitName, bookName }
  })
}

watch(
  () => route.fullPath,
  async () => {
    if (!books.value.length) return

    const preferredBookId = resolvePreferredBookId()
    const preferredUnitId = resolvePreferredUnitId()

    if (preferredBookId && preferredBookId !== selectedBookId.value) {
      await applyBookSelection(preferredBookId, { preferredUnitId, fallbackToFirstUnit: true })
      return
    }

    if (preferredBookId && preferredBookId === selectedBookId.value && units.value.length === 0) {
      await loadUnitsForBook(preferredBookId)
    }

    if (preferredUnitId && units.value.some(item => Number(item.id) === preferredUnitId) && preferredUnitId !== selectedUnitId.value) {
      selectedUnitId.value = preferredUnitId
      persistUnitSelection(preferredUnitId)
    }
  }
)

onMounted(loadBooksAndSelections)
</script>

<style scoped>
.study-toolbar {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.study-toolbar-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

:deep(.study-picker) {
  width: 100%;
}

:deep(.study-picker .el-select__wrapper) {
  min-height: 40px;
  border-radius: 14px;
  box-shadow: none;
  background: #ffffff;
}

@media (min-width: 640px) {
  .study-toolbar-actions {
    flex-direction: row;
    align-items: center;
  }

  :deep(.study-picker) {
    width: 220px;
  }
}

@media (min-width: 1024px) {
  .study-toolbar {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }

  .study-toolbar-actions {
    justify-content: flex-end;
  }
}

.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity .18s ease;
}

.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
}
</style>
