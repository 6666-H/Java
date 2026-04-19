<template>
  <div class="journey-page">
    <div class="journey-shell">
      <header class="journey-hero">
        <button class="ghost-btn" @click="router.push('/student/home')">返回学习中心</button>
        <div>
          <p>{{ unitInfo.bookName || '当前教材' }}</p>
          <h1>{{ unitInfo.unitName || route.query.unitName || '单元学习旅程' }}</h1>
          <span>先看今天任务，再进入背词训练。</span>
        </div>
        <button class="primary-btn" @click="startStudy">开始今天训练</button>
      </header>

      <main class="journey-grid">
        <section class="journey-card">
          <span class="kicker">今日任务</span>
          <div v-if="loading" class="loading-box">
            <el-icon class="is-loading"><Loading /></el-icon>
            <strong>正在加载学习旅程</strong>
          </div>
          <div v-else class="stats-grid">
            <div class="stats-item">
              <span>新词</span>
              <strong>{{ todayTask.newCount || 0 }}</strong>
            </div>
            <div class="stats-item">
              <span>复习</span>
              <strong>{{ todayTask.reviewCount || 0 }}</strong>
            </div>
            <div class="stats-item">
              <span>错词</span>
              <strong>{{ todayTask.errorCount || 0 }}</strong>
            </div>
            <div class="stats-item">
              <span>预计时长</span>
              <strong>{{ todayTask.estimatedMinutes || 0 }} 分钟</strong>
            </div>
          </div>
        </section>

        <section class="journey-card">
          <span class="kicker">阶段安排</span>
          <div class="plan-list">
            <article v-for="item in stagePlan" :key="item.key" class="plan-item">
              <strong>{{ item.title }}</strong>
              <p>{{ item.goal }}</p>
              <small>{{ item.minutes }} 分钟 · {{ item.questionCount }} 题</small>
            </article>
          </div>
        </section>

        <section class="journey-card">
          <span class="kicker">重点单词</span>
          <div v-if="focusWords.length" class="focus-list">
            <article v-for="item in focusWords" :key="item.wordId" class="focus-item">
              <strong>{{ item.word }}</strong>
              <p>{{ item.meaning }}</p>
            </article>
          </div>
          <p v-else class="empty-copy">这组词当前没有明显的高频错词，保持节奏就好。</p>
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import { getStudentUnitJourney } from '../../api/student'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const unitInfo = ref({})
const todayTask = ref({})
const stagePlan = ref([])
const focusWords = ref([])

async function loadJourney() {
  loading.value = true
  try {
    const result = await getStudentUnitJourney(route.params.unitId)
    unitInfo.value = result?.unit || {}
    todayTask.value = result?.todayTask || {}
    stagePlan.value = Array.isArray(result?.stagePlan) ? result.stagePlan : []
    focusWords.value = Array.isArray(result?.focusWords) ? result.focusWords : []
  } catch (_) {
    unitInfo.value = {}
    todayTask.value = {}
    stagePlan.value = []
    focusWords.value = []
  } finally {
    loading.value = false
  }
}

function startStudy() {
  router.push({
    name: 'StudentStudy',
    params: { unitId: route.params.unitId },
    query: {
      unitName: unitInfo.value.unitName || route.query.unitName || undefined,
      bookName: unitInfo.value.bookName || route.query.bookName || undefined,
    },
  })
}

onMounted(() => {
  loadJourney()
})
</script>

<style scoped>
.journey-page {
  min-height: 100vh;
  padding: 24px;
  background:
    radial-gradient(circle at top left, rgba(92, 113, 255, 0.12), transparent 26%),
    linear-gradient(180deg, #f4f7ff 0%, #eef3fb 100%);
}

.journey-shell {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  gap: 24px;
}

.journey-hero,
.journey-card {
  padding: 24px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(99, 115, 129, 0.12);
  box-shadow: 0 22px 48px rgba(20, 30, 60, 0.08);
}

.journey-hero {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
}

.journey-hero p,
.journey-hero h1,
.journey-hero span {
  margin: 0;
}

.journey-hero p {
  color: #667085;
  font-size: 0.9rem;
}

.journey-hero h1 {
  margin-top: 6px;
  color: #172033;
  font-size: clamp(1.55rem, 2vw, 2rem);
}

.journey-hero span {
  display: inline-block;
  margin-top: 10px;
  color: #566173;
}

.journey-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px;
}

.kicker {
  color: #667085;
  font-size: 0.82rem;
  font-weight: 800;
}

.stats-grid,
.plan-list,
.focus-list {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.stats-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.stats-item,
.plan-item,
.focus-item {
  padding: 16px;
  border-radius: 18px;
  background: #f7f9fc;
}

.stats-item span,
.plan-item small {
  color: #667085;
}

.stats-item strong,
.plan-item strong,
.focus-item strong {
  display: block;
  margin-top: 8px;
  color: #172033;
}

.plan-item p,
.focus-item p,
.empty-copy {
  margin: 8px 0 0;
  color: #566173;
  line-height: 1.7;
}

.ghost-btn,
.primary-btn {
  min-height: 48px;
  padding: 0 18px;
  border-radius: 16px;
  border: 1px solid rgba(99, 115, 129, 0.12);
  cursor: pointer;
  font-weight: 800;
}

.ghost-btn {
  background: #fff;
  color: #344054;
}

.primary-btn {
  background: linear-gradient(135deg, #4b5ef7 0%, #7b8cff 100%);
  color: #fff;
  border-color: transparent;
}

.loading-box {
  margin-top: 18px;
  display: grid;
  gap: 10px;
  justify-items: center;
  color: #566173;
}

@media (max-width: 980px) {
  .journey-page {
    padding: 14px;
  }

  .journey-hero,
  .journey-grid {
    grid-template-columns: 1fr;
  }
}
</style>
