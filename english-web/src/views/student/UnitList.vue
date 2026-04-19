<template>
  <div class="unit-list-page">
    <header class="book-header">
      <div>
        <button type="button" class="back-link" @click="router.push('/student/books')">返回教材库</button>
        <h1>{{ bookName || '教材单元' }}</h1>
      </div>

      <div class="book-summary">
        <div>
          <strong>{{ completedUnits }}</strong>
          <span>已解锁单元</span>
        </div>
        <div>
          <strong>{{ overallProgress }}%</strong>
          <span>总进度</span>
        </div>
      </div>
    </header>

    <div v-if="loading" class="loading-box">单元加载中...</div>

    <section v-else class="unit-grid">
      <article
        v-for="item in visualUnits"
        :key="item.unitId"
        class="unit-card"
        :class="{ active: item.active, locked: item.locked, completed: item.completed }"
        @click="!item.locked && goUnit(item)"
      >
        <div class="unit-card-top">
          <span class="unit-chip">UNIT {{ String(item.displayIndex).padStart(2, '0') }}</span>
          <el-icon v-if="item.completed" class="unit-state complete"><CircleCheckFilled /></el-icon>
          <el-icon v-else-if="item.locked" class="unit-state"><Lock /></el-icon>
          <el-icon v-else class="unit-state active-icon"><VideoPlay /></el-icon>
        </div>

        <h3>{{ item.unitName }}</h3>
        <p>{{ item.subtitle }}</p>

        <div class="stage-icons">
          <span v-for="stage in 4" :key="stage" :class="{ on: stage <= item.activeSegments }"></span>
        </div>
      </article>
    </section>

    <aside class="quote-card">
      <div class="quote-icon">
        <el-icon><Opportunity /></el-icon>
      </div>
      <h3>今日金句</h3>
      <p>"The beautiful thing about learning is that no one can take it away from you."</p>
      <strong>- B. B. King</strong>
    </aside>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CircleCheckFilled, Lock, Opportunity, VideoPlay } from '@element-plus/icons-vue'
import { listUnits, getUnitProgress } from '../../api/books'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const progressList = ref([])
const units = ref([])
const bookName = ref('')

const bookId = computed(() => Number(route.params.bookId))
const completedUnits = computed(() => visualUnits.value.filter(item => !item.locked).length)
const overallProgress = computed(() => {
  if (visualUnits.value.length === 0) return 0
  const total = visualUnits.value.reduce((sum, item) => sum + Number(item.completionPercent || 0), 0)
  return Math.round(total / visualUnits.value.length)
})

const visualUnits = computed(() => {
  const merged = progressList.value.map((row, index) => {
    const prev = progressList.value[index - 1]
    const locked = index > 0 && !prev?.completed && Number(prev?.completionPercent || 0) === 0
    return {
      ...row,
      displayIndex: index + 1,
      locked,
      active: Number(row.completionPercent || 0) > 0 && !row.completed,
      subtitle: unitSubtitle(row.unitName, index),
      activeSegments: Math.max(0, Math.min(4, Math.ceil(Number(row.completionPercent || 0) / 25)))
    }
  })
  const firstLearning = merged.find(item => item.active)
  if (!firstLearning && merged[0]) merged[0].locked = false
  return merged
})

async function loadData() {
  loading.value = true
  try {
    const [unitRes, progressRes] = await Promise.all([listUnits(bookId.value), getUnitProgress(bookId.value)])
    units.value = Array.isArray(unitRes) ? unitRes : []
    const progress = Array.isArray(progressRes) ? progressRes : []
    const unitMap = new Map(units.value.map(item => [item.id, item]))
    progressList.value = progress.map(item => ({
      ...item,
      unitName: item.unitName || unitMap.get(item.unitId)?.name || `单元 ${item.unitId}`
    }))
    bookName.value = String(route.query.bookName || localStorage.getItem('last_book_name') || '剑桥英语核心词汇')
  } finally {
    loading.value = false
  }
}

function unitSubtitle(name, index) {
  const map = [
    '掌握家庭成员及日常社交用语',
    '探讨现代科技对日常的影响',
    '环境保护及气候变化术语',
    '鉴赏不同的艺术流派',
    '职场与国际贸易表达',
    '运动项目及健康管理',
    '基础法律常识英语',
    '新闻与自媒体术语'
  ]
  return map[index] || `${name || '本单元'} 主题学习`
}

function goUnit(item) {
  localStorage.setItem('last_book_id', String(bookId.value))
  localStorage.setItem('last_book_name', bookName.value || '')
  localStorage.setItem('last_unit_id', String(item.unitId))
  localStorage.setItem('last_unit_name', item.unitName || '')
  router.push({
    name: 'StudentUnitDetail',
    params: { unitId: item.unitId },
    query: {
      bookId: bookId.value,
      bookName: bookName.value || undefined,
      unitName: item.unitName || undefined
    }
  })
}

onMounted(loadData)
</script>

<style scoped>
.unit-list-page {
  display: grid;
  gap: 18px;
}

.book-header {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: start;
}

.back-link {
  border: none;
  background: transparent;
  padding: 0;
  color: #005bbf;
  cursor: pointer;
  font-weight: 700;
}

.book-header h1 {
  margin: 8px 0 0;
  font-family: 'Manrope', sans-serif;
  font-size: 2rem;
  font-weight: 800;
}

.book-header p {
  max-width: 760px;
  margin: 10px 0 0;
  color: #414754;
  line-height: 1.65;
  font-size: 0.92rem;
}

.book-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(110px, 1fr));
  background: #fff;
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 16px 30px rgba(15, 23, 42, 0.05);
}

.book-summary div {
  padding: 14px 18px;
}

.book-summary strong,
.book-summary span {
  display: block;
}

.book-summary strong {
  color: #005bbf;
  font-size: 1.65rem;
  font-weight: 800;
}

.book-summary span {
  margin-top: 4px;
  color: #727785;
  font-size: 0.8rem;
  font-weight: 700;
}

.loading-box {
  padding: 20px;
  border-radius: 18px;
  background: #fff;
  color: #727785;
}

.unit-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.unit-card {
  min-height: 168px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 12px 22px rgba(15, 23, 42, 0.04);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.unit-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 20px 28px rgba(15, 23, 42, 0.08);
}

.unit-card.active {
  border: 2px solid #005bbf;
}

.unit-card.locked {
  opacity: 0.42;
  cursor: not-allowed;
}

.unit-card.completed {
  background: #fff;
}

.unit-card-top {
  display: flex;
  justify-content: space-between;
  align-items: start;
}

.unit-chip {
  padding: 5px 9px;
  border-radius: 999px;
  background: #eceef0;
  color: #727785;
  font-size: 0.64rem;
  font-weight: 800;
}

.unit-state {
  color: #727785;
  font-size: 1.15rem;
}

.unit-state.complete,
.unit-state.active-icon {
  color: #005bbf;
}

.unit-card h3 {
  margin: 18px 0 8px;
  font-size: 1.35rem;
  font-weight: 800;
}

.unit-card p {
  margin: 0;
  color: #727785;
  line-height: 1.55;
  font-size: 0.82rem;
}

.stage-icons {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.stage-icons span {
  height: 4px;
  border-radius: 999px;
  background: #d8dadc;
}

.stage-icons span.on {
  background: #005bbf;
}

.quote-card {
  justify-self: end;
  width: 300px;
  margin-top: 2px;
  padding: 18px 20px;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 18px 34px rgba(15, 23, 42, 0.05);
}

.quote-icon {
  width: 38px;
  height: 38px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: #ffdbcb;
  color: #9e4300;
}

.quote-card h3 {
  margin: 10px 0 8px;
  font-size: 1.1rem;
  font-weight: 800;
}

.quote-card p {
  margin: 0;
  color: #414754;
  line-height: 1.6;
  font-size: 0.88rem;
  font-style: italic;
}

.quote-card strong {
  display: block;
  margin-top: 14px;
  color: #c55500;
}

@media (max-width: 1200px) {
  .unit-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .quote-card {
    justify-self: stretch;
    width: auto;
  }
}

@media (max-width: 820px) {
  .book-header,
  .unit-grid {
    grid-template-columns: 1fr;
  }

  .book-header {
    flex-direction: column;
  }
}
</style>
