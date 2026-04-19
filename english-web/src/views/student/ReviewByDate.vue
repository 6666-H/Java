<template>
  <div class="student-page">
    <div class="back-row">
      <router-link to="/student/review" class="back-link">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回错题列表</span>
      </router-link>
    </div>
    <header class="page-header">
      <h1 class="page-title">{{ dateLabel }} 的错题</h1>
    </header>

    <div v-if="loading" class="loading-wrap">
      <el-spinner class="text-cyan-500" />
    </div>

    <el-empty v-else-if="grouped.length === 0" description="该日暂无错题记录" />

    <div v-else class="type-list">
      <el-card
        v-for="group in grouped"
        :key="group.type"
        class="type-card"
        :class="group.type"
        shadow="never"
      >
        <div
          class="type-row"
          @click="toggleDetail(group.type)"
        >
          <div class="type-left">
            <el-icon class="type-icon"><component :is="typeIcon(group.type)" /></el-icon>
            <div class="type-meta">
              <h2 class="type-title">{{ typeLabel(group.type) }}</h2>
              <span class="type-count">{{ group.items.length }} 个错题</span>
            </div>
          </div>
          <div class="type-right">
            <el-icon class="expand-icon" :class="{ expanded: expandedType === group.type }">
              <ArrowDown />
            </el-icon>
            <el-button type="primary" size="small" @click.stop="startPractice(group.type)">
              重复训练
            </el-button>
          </div>
        </div>
        <div v-show="expandedType === group.type" class="type-body">
          <el-table :data="group.items" size="small" class="word-table">
            <el-table-column prop="word" label="单词" min-width="120">
              <template #default="{ row }">
                <span class="word-text">{{ row.word }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="meaning" label="释义" min-width="180" show-overflow-tooltip />
          </el-table>
        </div>
      </el-card>
    </div>

    <div v-if="grouped.length > 0" class="bottom-action">
      <el-button type="danger" size="large" @click="startPracticeAll">
        重复训练本日全部错题
        <el-icon><ArrowRight /></el-icon>
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, ArrowDown, View, EditPen, Collection, Connection } from '@element-plus/icons-vue'
import { getErrorsByDateGrouped } from '../../api/study'

const route = useRoute()
const router = useRouter()
const date = computed(() => route.params.date || '')
const loading = ref(true)
const grouped = ref([])
const expandedType = ref(null)

const dateLabel = computed(() => {
  const d = date.value
  if (!d) return ''
  const dt = new Date(d + 'T00:00:00')
  const today = new Date()
  if (dt.toDateString() === today.toDateString()) return '今天'
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)
  if (dt.toDateString() === yesterday.toDateString()) return '昨天'
  return d
})

function typeLabel(errorType) {
  const t = (errorType || '').toUpperCase()
  if (t === 'FLASHCARD' || t === 'DONT_KNOW') return '看词识义'
  if (t === 'SPELLING_ERROR') return '看中文拼写'
  if (t === 'ENG_TO_CH' || t === 'ENG_TO_CH_ERROR') return '看英语选中文'
  if (t === 'CH_TO_ENG' || t === 'CH_TO_ENG_ERROR') return '看中文选英语'
  return errorType || '其他'
}

function typeIcon(errorType) {
  const t = (errorType || '').toUpperCase()
  if (t === 'FLASHCARD' || t === 'DONT_KNOW') return View
  if (t === 'SPELLING_ERROR') return EditPen
  if (t === 'ENG_TO_CH' || t === 'ENG_TO_CH_ERROR') return Collection
  if (t === 'CH_TO_ENG' || t === 'CH_TO_ENG_ERROR') return Connection
  return View
}

function toggleDetail(type) {
  expandedType.value = expandedType.value === type ? null : type
}

function typeToMode(errorType) {
  const t = (errorType || '').toUpperCase()
  if (t === 'FLASHCARD' || t === 'DONT_KNOW') return 'flashcard'
  if (t === 'SPELLING_ERROR') return 'spell'
  if (t === 'ENG_TO_CH' || t === 'ENG_TO_CH_ERROR') return 'eng_ch'
  if (t === 'CH_TO_ENG' || t === 'CH_TO_ENG_ERROR') return 'ch_eng'
  return 'flashcard'
}

function startPractice(type) {
  router.push({
    name: 'StudentReviewPractice',
    params: { date: date.value },
    query: { mode: typeToMode(type), errorType: type }
  })
}

function startPracticeAll() {
  router.push({
    name: 'StudentReviewPractice',
    params: { date: date.value }
  })
}

async function fetchList() {
  if (!date.value) return
  loading.value = true
  try {
    const res = await getErrorsByDateGrouped(date.value)
    grouped.value = Array.isArray(res) ? res : []
  } catch (err) {
    console.error(err)
    grouped.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)
</script>

<style scoped>
.student-page { min-height: 100%; }
.back-row { margin-bottom: 20px; }
.back-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #64748b;
  font-weight: 500;
  font-size: 0.9375rem;
  text-decoration: none;
  transition: color 0.2s;
}
.back-link:hover { color: #0ea5e9; }
.page-header { margin-bottom: 28px; }
.page-title { font-size: 1.5rem; font-weight: 800; color: #1e293b; margin: 0 0 8px 0; }
.page-desc { font-size: 0.875rem; color: #64748b; margin: 0; }
.loading-wrap { display: flex; justify-content: center; padding: 64px 0; }
.type-list { display: flex; flex-direction: column; gap: 12px; }
.type-card :deep(.el-card__body) { padding: 0; }
.type-card {
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}
.type-card.FLASHCARD .type-row,
.type-card.DONT_KNOW .type-row { background: #f0f9ff; }
.type-card.SPELLING_ERROR .type-row { background: #f0fdfa; }
.type-card.ENG_TO_CH .type-row, .type-card.ENG_TO_CH_ERROR .type-row { background: #f5f3ff; }
.type-card.CH_TO_ENG .type-row, .type-card.CH_TO_ENG_ERROR .type-row { background: #fffbeb; }
.type-row {
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  transition: background 0.2s;
}
.type-row:hover { filter: brightness(0.98); }
.type-left { display: flex; align-items: center; gap: 14px; }
.type-icon { font-size: 1.5rem; color: #475569; }
.type-meta { }
.type-title { font-size: 1.0625rem; font-weight: 700; color: #1e293b; margin: 0 0 4px 0; }
.type-count { font-size: 0.8125rem; color: #64748b; }
.type-right { display: flex; align-items: center; gap: 12px; }
.expand-icon {
  transition: transform 0.2s;
  color: #94a3b8;
}
.expand-icon.expanded { transform: rotate(180deg); }
.type-body { padding: 16px 20px; border-top: 1px solid #f1f5f9; background: #fafafa; }
.word-table :deep(.el-table__header-wrapper) { display: none; }
.word-text { font-weight: 600; color: #1e293b; }
.bottom-action { margin-top: 28px; text-align: center; }
</style>
