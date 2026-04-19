<template>
  <div class="unit-detail-page" v-loading="loading">
    <header class="detail-header">
      <div class="detail-copy">
        <button type="button" class="back-link" @click="router.push({ name: 'StudentUnitList', params: { bookId }, query: { bookName } })">
          返回单元列表
        </button>
        <span class="detail-badge">B2 级 · 学术英语</span>
        <h1>{{ unitName || '单元详情' }}</h1>
        <div class="detail-meta">
          <span><el-icon><CollectionTag /></el-icon>{{ detail.totalCount || 0 }} 核心词汇</span>
          <span><el-icon><Timer /></el-icon>预计耗时 {{ estimatedMinutes }} 分钟</span>
        </div>
      </div>

      <div class="completion-panel">
        <div class="completion-visual"></div>
        <div class="completion-copy">
          <span>单元完成度</span>
          <strong>{{ completionPercent }}%</strong>
          <div class="completion-track">
            <span :style="{ width: `${completionPercent}%` }"></span>
          </div>
        </div>
      </div>
    </header>

    <section class="stage-grid">
      <article
        v-for="item in stageCards"
        :key="item.mode"
        class="stage-card"
        :class="{ active: item.active, locked: item.locked, completed: item.completed }"
      >
        <div class="stage-top">
          <div class="stage-icon" :class="{ active: item.active, muted: item.locked }">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <span class="stage-flag">{{ item.completed ? '已完成' : item.locked ? '未解锁' : item.active ? '当前阶段' : '进行中' }}</span>
        </div>

        <h3>{{ item.title }}</h3>
        <p>{{ item.description }}</p>

        <button v-if="item.active" type="button" class="stage-action" @click="startStage(item)">
          开始练习
          <el-icon><Right /></el-icon>
        </button>
        <div v-else class="stage-placeholder"></div>

        <div class="stage-foot" v-if="item.completed">
          <el-icon><CircleCheckFilled /></el-icon>
          <span>{{ item.doneCount }}/{{ item.totalCount }} 已完成</span>
        </div>
      </article>
    </section>

    <section class="insight-grid">
      <article class="insight-card">
        <div>
          <h2>单元洞察</h2>
          <p>你在这个单元的进度超过了 {{ peerBeat }}% 的同学。大多数学生发现“{{ activeStageTitle }}”是最难的阶段。</p>
          <div class="insight-tags">
            <span>学习达人</span>
            <span>词汇冠军</span>
          </div>
        </div>
        <div class="insight-chart"></div>
      </article>

      <article class="streak-card">
        <span>学习连胜</span>
        <strong>{{ streakDays }}</strong>
        <em>天</em>
        <div class="streak-track">
          <span v-for="n in 5" :key="n" :class="{ on: n <= streakSegments }"></span>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { CollectionTag, CircleCheckFilled, Lock, PictureRounded, Right, Timer, VideoPlay, EditPen, Select, View } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUnitProgressDetail } from '../../api/books'
import { getModeStats, getUnitModeCompletion } from '../../api/study'
import { getStudentUnitWords, getStudentStats } from '../../api/student'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref({ status: 'not_started', progress: 0, learnedCount: 0, totalCount: 0 })
const modeStats = ref({})
const completion = ref({})
const words = ref([])
const stats = ref({ consecutiveDays: 0 })

const unitId = computed(() => Number(route.params.unitId))
const unitName = computed(() => route.query.unitName || localStorage.getItem('last_unit_name') || '单元详情')
const bookId = computed(() => route.query.bookId || localStorage.getItem('last_book_id') || '')
const bookName = computed(() => route.query.bookName || localStorage.getItem('last_book_name') || '')
const completionPercent = computed(() => Math.round(Number(detail.value.progress || 0) * 100))
const estimatedMinutes = computed(() => Math.max(20, Number(detail.value.totalCount || 0) * 2 + 24))
const streakDays = computed(() => Number(stats.value?.consecutiveDays || 0))
const peerBeat = computed(() => Math.min(98, 75 + Math.round(completionPercent.value / 4)))
const streakSegments = computed(() => Math.max(1, Math.min(5, Math.ceil(streakDays.value / 4))))

const stageDefs = [
  { stage: 1, mode: 'flashcard', title: '看词识义', description: '识别语义区域和发音暗示。', icon: View },
  { stage: 2, mode: 'eng_ch', title: '看英选中', description: '通过多选语境挑战巩固词义理解。', icon: VideoPlay },
  { stage: 3, mode: 'ch_eng', title: '看中选英', description: '根据中文概念提示回忆英文单词。', icon: Select },
  { stage: 4, mode: 'spell', title: '拼写练习', description: '无视觉辅助的自主回忆与产出。', icon: EditPen }
]

const stageCards = computed(() =>
  stageDefs.map((item, index) => {
    const stat = modeStats.value?.[item.mode] || {}
    const totalCount = Number(stat.total || detail.value.totalCount || 0)
    const incompleteCount = Number(stat.incomplete || 0)
    const doneCount = Math.max(totalCount - incompleteCount, 0)
    const completed = !!completion.value?.[item.mode]
    const prev = index === 0 ? null : stageDefs[index - 1]
    const locked = prev ? !completion.value?.[prev.mode] : false
    const active = !locked && !completed && (index === 0 || !!completion.value?.[prev.mode])
    return { ...item, totalCount, doneCount, completed, locked, active }
  })
)

const activeStageTitle = computed(() => stageCards.value.find(item => item.active)?.title || '看词识义')

function startStage(item) {
  if (item.locked) return
  localStorage.setItem('last_unit_id', String(unitId.value))
  localStorage.setItem('last_unit_name', unitName.value || '')
  if (bookId.value) localStorage.setItem('last_book_id', String(bookId.value))
  if (bookName.value) localStorage.setItem('last_book_name', bookName.value || '')
  router.push({
    name: 'StudentStudy',
    params: { unitId: unitId.value },
    query: { mode: item.mode, unitName: unitName.value || undefined }
  })
}

async function loadData() {
  loading.value = true
  try {
    const [progressDetail, progressStats, modeCompletion, unitWords, statsRes] = await Promise.all([
      getUnitProgressDetail(unitId.value),
      getModeStats(unitId.value),
      getUnitModeCompletion(unitId.value),
      getStudentUnitWords(unitId.value),
      getStudentStats()
    ])
    detail.value = progressDetail || detail.value
    modeStats.value = progressStats || {}
    completion.value = modeCompletion || {}
    words.value = Array.isArray(unitWords) ? unitWords : []
    stats.value = statsRes || stats.value
  } catch (error) {
    ElMessage.error(error.message || '获取单元详情失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.unit-detail-page {
  display: grid;
  gap: 18px;
}

.detail-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 240px;
  gap: 16px;
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

.detail-badge {
  display: block;
  margin-top: 14px;
  color: #9e4300;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-size: 0.82rem;
}

.detail-copy h1 {
  margin: 8px 0 0;
  font-family: 'Manrope', sans-serif;
  font-size: 2.25rem;
  font-weight: 800;
  line-height: 1.15;
}

.detail-meta {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.detail-meta span {
  padding: 7px 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #eceef0;
  color: #414754;
  font-weight: 700;
  font-size: 0.76rem;
}

.completion-panel {
  position: relative;
  overflow: hidden;
  min-height: 150px;
  border-radius: 18px;
  background: #f2f4f6;
}

.completion-visual {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 48% 22%, rgba(255, 255, 255, 0.76), transparent 30%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.2), transparent),
    linear-gradient(180deg, #e9edf2, #f4f6f8);
}

.completion-copy {
  position: absolute;
  right: 16px;
  bottom: 16px;
  left: 16px;
  text-align: right;
}

.completion-copy span,
.completion-copy strong {
  display: block;
}

.completion-copy span {
  color: #414754;
  font-size: 0.9rem;
  font-weight: 700;
}

.completion-copy strong {
  margin-top: 6px;
  color: #005bbf;
  font-size: 2rem;
  font-weight: 800;
}

.completion-track {
  height: 6px;
  margin-top: 10px;
  border-radius: 999px;
  background: #d8dadc;
  overflow: hidden;
}

.completion-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #005bbf;
}

.stage-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.stage-grid::before {
  content: '';
  position: absolute;
  top: 82px;
  left: 0;
  right: 0;
  height: 2px;
  background: #eceef0;
}

.stage-card {
  position: relative;
  z-index: 1;
  min-height: 220px;
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 18px 32px rgba(15, 23, 42, 0.05);
}

.stage-card.active {
  transform: translateY(-6px);
  border: 2px solid rgba(0, 91, 191, 0.14);
  box-shadow: 0 24px 44px rgba(0, 91, 191, 0.12);
}

.stage-card.locked {
  opacity: 0.5;
}

.stage-top {
  display: flex;
  justify-content: space-between;
  align-items: start;
}

.stage-icon {
  width: 38px;
  height: 38px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: #dee0ff;
  color: #293ca0;
}

.stage-icon.active {
  background: #005bbf;
  color: #fff;
}

.stage-icon.muted {
  background: #e0e3e5;
  color: #727785;
}

.stage-flag {
  padding: 5px 8px;
  border-radius: 999px;
  background: #eceef0;
  color: #727785;
  font-size: 0.64rem;
  font-weight: 800;
}

.stage-card h3 {
  margin: 16px 0 8px;
  font-size: 1.25rem;
  font-weight: 800;
}

.stage-card p {
  min-height: 40px;
  margin: 0;
  color: #727785;
  line-height: 1.55;
  font-size: 0.84rem;
}

.stage-action {
  width: 100%;
  height: 42px;
  margin-top: 16px;
  border: none;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: linear-gradient(135deg, #005bbf, #1a73e8);
  color: #fff;
  font-weight: 800;
  cursor: pointer;
}

.stage-placeholder {
  height: 42px;
  margin-top: 16px;
  border-radius: 14px;
  background: #eceef0;
}

.stage-foot {
  margin-top: 12px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #005bbf;
  font-weight: 700;
}

.insight-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) 240px;
  gap: 14px;
}

.insight-card,
.streak-card {
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 18px 34px rgba(15, 23, 42, 0.05);
}

.insight-card {
  padding: 18px 20px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 140px;
  gap: 14px;
  align-items: center;
}

.insight-card h2 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 800;
}

.insight-card p {
  margin: 10px 0 0;
  color: #414754;
  line-height: 1.6;
  font-size: 0.9rem;
}

.insight-tags {
  margin-top: 12px;
  display: flex;
  gap: 10px;
}

.insight-tags span {
  padding: 6px 10px;
  border-radius: 999px;
  background: #ffebe7;
  color: #9e4300;
  font-weight: 700;
  font-size: 0.72rem;
}

.insight-chart {
  min-height: 100px;
  border-radius: 14px;
  background:
    linear-gradient(180deg, rgba(0, 91, 191, 0.04), rgba(0, 91, 191, 0.12)),
    linear-gradient(90deg, transparent 0 20%, rgba(255, 255, 255, 0.3) 20% 22%, transparent 22% 42%, rgba(255, 255, 255, 0.3) 42% 44%, transparent 44% 64%, rgba(255, 255, 255, 0.3) 64% 66%, transparent 66%);
}

.streak-card {
  padding: 18px 20px;
}

.streak-card span,
.streak-card strong,
.streak-card em {
  display: block;
}

.streak-card span {
  color: #414754;
  font-weight: 700;
}

.streak-card strong {
  margin-top: 10px;
  color: #9e4300;
  font-size: 2.25rem;
  font-weight: 800;
}

.streak-card em {
  color: #414754;
  font-style: normal;
  font-size: 1.1rem;
}

.streak-track {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.streak-track span {
  height: 6px;
  border-radius: 999px;
  background: #d8dadc;
}

.streak-track span.on {
  background: #9e4300;
}

@media (max-width: 1200px) {
  .detail-header,
  .stage-grid,
  .insight-grid,
  .insight-card {
    grid-template-columns: 1fr;
  }

  .stage-grid::before {
    display: none;
  }
}
</style>
