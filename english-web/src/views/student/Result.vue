<template>
  <div class="result-page">
    <section class="result-card" v-if="summary">
      <div class="result-hero">
        <h1>{{ summary.title || '本阶段完成' }}</h1>
        <p>{{ summary.unitName || '当前单元' }} · {{ summary.modeLabel || '' }}</p>
      </div>

      <div class="result-grid">
        <article class="result-item">
          <span>本阶段总题数</span>
          <strong>{{ summary.initialCount || 0 }}</strong>
        </article>
        <article class="result-item">
          <span>总答对次数</span>
          <strong>{{ summary.correctAttemptsCount || 0 }}</strong>
        </article>
        <article class="result-item">
          <span>总答错次数</span>
          <strong>{{ summary.wrongAttemptsCount || 0 }}</strong>
        </article>
        <article class="result-item">
          <span>第一轮首次答对</span>
          <strong>{{ summary.firstRoundCorrectCount || 0 }}{{ summary.starReward ? ' ⭐' : '' }}</strong>
        </article>
        <article class="result-item">
          <span>稳定掌握</span>
          <strong>{{ summary.stabilizedCount || 0 }}</strong>
        </article>
        <article class="result-item">
          <span>本次用时</span>
          <strong>{{ summary.elapsedLabel || '0分00秒' }}</strong>
        </article>
      </div>

      <div class="result-actions">
        <button
          v-if="summary.nextMode"
          type="button"
          class="result-btn secondary"
          @click="goNextMode"
        >
          继续下一阶段
        </button>
        <button type="button" class="result-btn" @click="goBackUnit">
          {{ summary.nextMode ? '稍后再学，返回单元' : '返回单元' }}
        </button>
      </div>
    </section>

    <section v-else class="result-card empty-card">
      <h2>没有找到本次结算数据</h2>
      <p>可能是页面已刷新，或本次学习结算已过期。</p>
      <button type="button" class="result-btn" @click="goBackUnit">返回单元详情</button>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const RESULT_SUMMARY_KEY = 'student_stage_result_summary'

const summary = computed(() => {
  try {
    const raw = sessionStorage.getItem(RESULT_SUMMARY_KEY)
    if (!raw) return null
    const data = JSON.parse(raw)
    if (String(data.unitId) !== String(route.params.unitId)) return null
    if (String(data.stage) !== String(route.params.stage)) return null
    return data
  } catch (_) {
    return null
  }
})

function goBackUnit() {
  router.push({
    name: 'StudentUnitDetail',
    params: { unitId: route.params.unitId },
    query: {
      bookId: summary.value?.bookId || localStorage.getItem('last_book_id') || undefined,
      bookName: summary.value?.bookName || localStorage.getItem('last_book_name') || undefined,
      unitName: summary.value?.unitName || localStorage.getItem('last_unit_name') || undefined
    }
  })
}

function goNextMode() {
  if (!summary.value?.nextMode) return
  router.push({
    name: 'StudentStudy',
    params: { unitId: route.params.unitId },
    query: {
      mode: summary.value.nextMode,
      unitName: summary.value.unitName || undefined
    }
  })
}
</script>

<style scoped>
.result-page {
  display: block;
  min-height: auto;
  padding: 8px 0 0;
}

.result-card {
  width: 100%;
  max-width: 680px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #efe8f4;
  border-radius: 14px;
  padding: 18px;
}

.result-hero {
  text-align: center;
}

.result-hero h1 {
  margin: 0;
  color: #111827;
  font-size: 1.35rem;
}

.result-hero p {
  margin: 10px 0 0;
  color: #64748b;
  font-size: 0.9rem;
}

.result-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.result-item {
  background: #f8fafc;
  border-radius: 14px;
  padding: 12px;
}

.result-item span,
.result-item strong {
  display: block;
}

.result-item span {
  color: #64748b;
  font-size: 0.78rem;
}

.result-item strong {
  margin-top: 8px;
  color: #111827;
  font-size: 1.1rem;
}

.result-actions {
  margin-top: 16px;
  display: grid;
  gap: 8px;
}

.result-btn {
  min-height: 46px;
  border: none;
  border-radius: 12px;
  background: #a855f7;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.result-btn.secondary {
  background: #fbf5ff;
  color: #a855f7;
}

.empty-card {
  text-align: center;
}

.empty-card h2 {
  margin: 0;
  color: #111827;
  font-size: 1.2rem;
}

.empty-card p {
  margin: 10px 0 20px;
  color: #64748b;
}

@media (max-width: 860px) {
  .result-grid {
    grid-template-columns: 1fr;
  }

  .result-card {
    padding: 22px 16px;
  }
}
</style>
