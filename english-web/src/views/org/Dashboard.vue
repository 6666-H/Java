<template>
  <div class="dashboard-page" v-loading="loading">
    <section class="page-header">
      <div>
        <h1>机构概况</h1>
        <p>监控平台使用情况和学生参与度指标。</p>
      </div>
    </section>

    <section class="top-grid">
      <article class="seat-card">
        <div class="seat-head">
          <div class="seat-icon"><el-icon><DataLine /></el-icon></div>
          <span class="seat-tag">容量 {{ quotaRate }}%</span>
        </div>
        <span class="label">名额使用</span>
        <div class="metric">
          <strong>{{ quota.used }}</strong>
          <span>/ {{ quota.total || 0 }}</span>
        </div>
        <div class="track"><span :style="{ width: `${quotaRate}%` }"></span></div>
      </article>

      <article class="warning-card">
        <div class="warning-head">
          <el-icon><WarningFilled /></el-icon>
          <span>名额告警</span>
        </div>
        <p>您已达到学生容量的 {{ quotaRate }}%。达到 100% 后，将禁用添加学生功能。</p>
        <button type="button">扩容名额</button>
      </article>

      <div class="mini-grid">
        <article class="mini-card">
          <div class="mini-row">
            <el-icon class="mini-icon"><UserFilled /></el-icon>
            <div class="mini-copy">
              <span class="mini-label">活跃学生</span>
              <strong>{{ stats.activeToday || 0 }}</strong>
              <em>{{ formatDelta(stats.activeStudentDelta, '较上期') }}</em>
            </div>
          </div>
        </article>
        <article class="mini-card">
          <div class="mini-row">
            <el-icon class="mini-icon"><CircleCheck /></el-icon>
            <div class="mini-copy">
              <span class="mini-label">任务完成率</span>
              <strong>{{ completionRate }}%</strong>
              <em>{{ formatDelta(stats.taskCompletionRateDelta, '较上期') }}</em>
            </div>
          </div>
        </article>
        <article class="mini-card">
          <div class="mini-row">
            <el-icon class="mini-icon"><Timer /></el-icon>
            <div class="mini-copy">
              <span class="mini-label">总累计时长</span>
              <strong>{{ totalLearningMinutes }}</strong>
              <small>来自真实学习记录</small>
            </div>
          </div>
        </article>
        <article class="mini-card add-card">
          <div class="add-circle">+</div>
          <span>添加组件</span>
        </article>
      </div>
    </section>

    <section class="content-grid">
      <article class="chart-card">
        <div class="card-head">
          <div>
            <h2>学生活跃趋势</h2>
            <p>最近14天活跃用户参与度</p>
          </div>
          <div class="chart-actions">
            <div class="legend"><span class="dot"></span><span>活跃学生</span></div>
            <button type="button" class="range-btn">最近 {{ trendData.length }} 天</button>
          </div>
        </div>

        <div class="chart-grid-line">
          <span></span><span></span><span></span><span></span>
        </div>
        <div class="bars">
          <div v-for="(item, index) in trendData" :key="index" class="bar-wrap">
            <div class="bar-shell">
              <span class="bar" :class="{ active: item.highlight }" :style="{ height: `${item.height}%` }"></span>
            </div>
          </div>
        </div>
        <div class="bar-labels">
          <span v-for="label in trendTickLabels" :key="label">{{ label }}</span>
        </div>
      </article>

      <article class="activity-card">
        <div class="card-head">
          <h2>近期活动</h2>
          <button type="button">查看全部</button>
        </div>

        <div v-if="activityItems.length" class="activity-list">
          <article v-for="item in activityItems" :key="item.key" class="activity-item">
            <div class="avatar-wrap">
              <div class="avatar">{{ item.avatar }}</div>
              <span class="avatar-badge" :class="item.color"></span>
            </div>
            <div class="activity-copy">
              <strong>{{ item.primary }}</strong>
              <span>{{ item.time }}</span>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无近期活动" />
      </article>
    </section>

    <footer class="page-footer">
      <div class="footer-links">
        <a href="javascript:void(0)">帮助中心</a>
        <a href="javascript:void(0)">接口文档</a>
      </div>
      <p>© 2024 永升教育系统. PROFESSIONAL VERSION 4.2</p>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '../../api/request'
import { ElMessage } from 'element-plus'
import { getOrgReport, getOrgTasks } from '../../api/org'
import { CircleCheck, DataLine, Timer, UserFilled, WarningFilled } from '@element-plus/icons-vue'

const loading = ref(false)
const quota = ref({ used: 0, total: 0 })
const stats = ref({
  totalStudents: 0,
  activeToday: 0,
  activeStudentDelta: 0,
  taskCompletionRate: 0,
  taskCompletionRateDelta: 0,
  totalLearningMinutes: 0,
  trendLabels: [],
  trendValues: [],
  recentActivities: []
})
const tasks = ref([])
const reportRows = ref([])

const quotaRate = computed(() => buildPercent(quota.value.used, quota.value.total))
const completionRate = computed(() => {
  return Number(stats.value?.taskCompletionRate || 0).toFixed(1)
})
const totalLearningMinutes = computed(() => {
  return Number(stats.value?.totalLearningMinutes || 0).toLocaleString()
})

const trendData = computed(() => {
  const values = Array.isArray(stats.value?.trendValues) ? stats.value.trendValues : []
  const labels = Array.isArray(stats.value?.trendLabels) ? stats.value.trendLabels : []
  const base = values.map((value, index) => ({
    value: Number(value || 0),
    label: labels[index] || String(index + 1),
    highlight: index === values.length - 1
  }))
  const max = Math.max(...base.map(item => item.value), 1)
  return base.map(item => ({
    ...item,
    height: Math.max(26, Math.round(item.value / max * 100))
  }))
})

const trendTickLabels = computed(() => {
  if (!trendData.value.length) return []
  const indexes = Array.from(new Set([0, Math.floor((trendData.value.length - 1) / 3), Math.floor((trendData.value.length - 1) * 2 / 3), trendData.value.length - 2, trendData.value.length - 1]))
    .filter(index => index >= 0 && index < trendData.value.length)
  return indexes.map(index => shortDate(trendData.value[index].label))
})

const activityItems = computed(() => {
  if (Array.isArray(stats.value?.recentActivities) && stats.value.recentActivities.length) {
    return stats.value.recentActivities
  }
  if (tasks.value.length) {
    return tasks.value.slice(0, 4).map((task, index) => ({
      key: `task-${task.id || index}`,
      avatar: initials(task.studentName || '学生'),
      primary: `${task.studentName || '学生'} ${task.status === '已完成' ? '完成了' : '更新了'} ${task.unitName || '学习任务'}`,
      time: formatRelativeTime(task.assignedAt),
      color: task.status === '已完成' ? 'green' : 'orange'
    }))
  }
  return []
})

function averageRates(item) {
  return (
    Number(item.flashcardRate || 0) +
    Number(item.engChRate || 0) +
    Number(item.chEngRate || 0) +
    Number(item.spellRate || 0)
  ) / 4
}

function buildPercent(value, total) {
  const safeTotal = Number(total || 0)
  if (!safeTotal) return 0
  return Math.min(100, Math.round((Number(value || 0) / safeTotal) * 100))
}

function initials(value) {
  const text = String(value || 'AA').trim()
  return text.length > 2 ? text.slice(0, 2).toUpperCase() : text
}

function formatRelativeTime(value) {
  if (!value) return '刚刚'
  const date = new Date(String(value).replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 16)
  const diff = Date.now() - date.getTime()
  const minutes = Math.max(1, Math.round(diff / 60000))
  if (minutes < 60) return `${minutes} minutes ago`
  const hours = Math.round(minutes / 60)
  if (hours < 24) return `${hours} hours ago`
  return 'Yesterday'
}

function shortDate(value) {
  if (!value) return '--'
  return String(value).slice(5)
}

function formatDelta(value, suffix) {
  const amount = Number(value || 0)
  const prefix = amount > 0 ? '+' : ''
  return `${prefix}${amount.toFixed(1)}% ${suffix}`
}

async function fetchData() {
  loading.value = true
  try {
    const [quotaRes, statsRes, tasksRes, reportRes] = await Promise.all([
      request.get('/tenant/quota'),
      request.get('/tenant/stats'),
      getOrgTasks(),
      getOrgReport()
    ])
    quota.value = {
      used: Number(quotaRes?.usedCount || 0),
      total: Number(quotaRes?.totalQuota || 0)
    }
    stats.value = statsRes || stats.value
    tasks.value = Array.isArray(tasksRes) ? tasksRes : []
    reportRows.value = Array.isArray(reportRes) ? reportRes : []
  } catch (error) {
    ElMessage.error(error.message || '获取机构概览失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 14px;
}

.page-header h1,
.card-head h2 {
  margin: 0;
  font-family: "Manrope", sans-serif;
  color: #191c1e;
}

.page-header h1 {
  font-size: 1.6rem;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.page-header p,
.card-head p {
  margin: 8px 0 0;
  color: #667085;
  font-size: 0.88rem;
}

.top-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.8fr) minmax(0, 1.1fr);
  gap: 12px;
}

.seat-card,
.warning-card,
.mini-card,
.chart-card,
.activity-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 10px 24px rgba(25, 28, 30, 0.05);
}

.seat-card,
.warning-card,
.chart-card,
.activity-card {
  padding: 11px;
}

.seat-head,
.card-head,
.activity-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.seat-icon,
.mini-icon {
  width: 28px;
  height: 28px;
  border-radius: 12px;
  display: grid;
  place-items: center;
}

.seat-icon {
  background: #d8e2ff;
  color: #005bbf;
}

.seat-tag {
  padding: 5px 9px;
  border-radius: 999px;
  background: #d8e2ff;
  color: #005bbf;
  font-size: 0.7rem;
  font-weight: 800;
}

.label,
.mini-label {
  display: block;
  margin-top: 5px;
  color: #727785;
  font-size: 0.72rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.metric {
  margin-top: 4px;
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.metric strong,
.mini-card strong {
  font-family: "Manrope", sans-serif;
  color: #191c1e;
  font-size: 1.4rem;
  font-weight: 800;
}

.metric span {
  color: #727785;
  font-size: 1rem;
  font-weight: 600;
}

.track {
  height: 8px;
  margin-top: 10px;
  border-radius: 999px;
  background: #e6e8ea;
  overflow: hidden;
}

.track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #005bbf;
}

.warning-card {
  background: #ffdbcb;
  display: flex;
  flex-direction: column;
}

.warning-head {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #9e4300;
  font-size: 0.92rem;
  font-weight: 800;
}

.warning-card p {
  margin: 8px 0 0;
  color: #783100;
  line-height: 1.55;
  font-size: 0.84rem;
}

.warning-card button {
  margin-top: auto;
  width: fit-content;
  border: none;
  background: transparent;
  color: #9e4300;
  font-size: 0.78rem;
  font-weight: 800;
  cursor: pointer;
}

.mini-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.mini-card {
  min-height: 78px;
  padding: 10px;
}

.mini-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 100%;
}

.mini-copy {
  min-width: 0;
}

.mini-icon {
  background: #eef3ff;
  color: #4355b9;
}

.mini-card strong {
  display: block;
  margin-top: 2px;
  font-size: 1rem;
}

.mini-card em,
.mini-card small {
  display: block;
  margin-top: 2px;
  font-size: 0.68rem;
  font-style: normal;
  font-weight: 700;
}

.mini-card em {
  color: #16a34a;
}

.mini-card small {
  color: #727785;
}

.add-card {
  border: 2px dashed #c1c6d6;
  box-shadow: none;
  display: grid;
  place-items: center;
  color: #727785;
}

.add-circle {
  width: 26px;
  height: 26px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: #eceef0;
  font-size: 1rem;
  font-weight: 700;
}

.add-card span {
  margin-top: 8px;
  font-size: 0.72rem;
  font-weight: 800;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.8fr) minmax(320px, 1.1fr);
  gap: 12px;
}

.chart-card {
  grid-column: 1 / span 2;
}

.activity-card {
  grid-column: 3;
}

.chart-actions,
.legend {
  display: flex;
  align-items: center;
  gap: 12px;
}

.legend {
  color: #727785;
  font-size: 0.78rem;
  font-weight: 700;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: #005bbf;
}

.range-btn {
  border: none;
  padding: 6px 9px;
  border-radius: 10px;
  background: #e6e8ea;
  color: #414754;
  font-size: 0.72rem;
  font-weight: 800;
}

.chart-card {
  position: relative;
  overflow: hidden;
}

.chart-grid-line {
  position: absolute;
  left: 18px;
  right: 18px;
  top: 56px;
  bottom: 30px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  pointer-events: none;
  opacity: 0.25;
}

.chart-grid-line span {
  border-bottom: 1px solid #727785;
}

.bars {
  height: 132px;
  margin-top: 10px;
  padding: 0 8px;
  display: grid;
  grid-template-columns: repeat(14, minmax(0, 1fr));
  gap: 8px;
  align-items: end;
  position: relative;
  z-index: 1;
}

.bar-wrap,
.bar-shell {
  height: 100%;
  display: flex;
  align-items: end;
  justify-content: center;
}

.bar {
  width: 100%;
  max-width: 14px;
  border-radius: 8px 8px 0 0;
  background: #adc7ff;
}

.bar.active {
  background: #005bbf;
  box-shadow: 0 -4px 12px rgba(0, 91, 191, 0.3);
}

.bar-labels {
  margin-top: 4px;
  padding: 0 8px;
  display: flex;
  justify-content: space-between;
  color: #727785;
  font-size: 0.68rem;
  font-weight: 800;
  text-transform: uppercase;
}

.activity-card button,
.footer-links a {
  border: none;
  background: transparent;
  color: #005bbf;
  font-size: 0.76rem;
  font-weight: 800;
  text-decoration: none;
  cursor: pointer;
}

.activity-list {
  margin-top: 10px;
  display: grid;
  gap: 10px;
}

.avatar-wrap {
  position: relative;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #dee0ff 0%, #d8e2ff 100%);
  color: #293ca0;
  font-size: 0.82rem;
  font-weight: 800;
}

.avatar-badge {
  position: absolute;
  right: -2px;
  bottom: -2px;
  width: 14px;
  height: 14px;
  border-radius: 999px;
  border: 2px solid #fff;
}

.avatar-badge.green {
  background: #22c55e;
}

.avatar-badge.blue {
  background: #005bbf;
}

.avatar-badge.orange {
  background: #9e4300;
}

.activity-copy {
  flex: 1;
}

.activity-copy strong,
.activity-copy span {
  display: block;
}

.activity-copy strong {
  color: #191c1e;
  font-size: 0.78rem;
  line-height: 1.4;
}

.activity-copy span {
  margin-top: 4px;
  color: #98a2b3;
  font-size: 0.68rem;
}

.page-footer {
  padding-top: 8px;
  border-top: 1px solid #e6e8ea;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.footer-links {
  display: flex;
  align-items: center;
  gap: 32px;
}

.page-footer p {
  margin: 0;
  color: #98a2b3;
  font-size: 0.62rem;
  font-weight: 800;
  letter-spacing: 0.12em;
}

@media (max-width: 1180px) {
  .top-grid,
  .content-grid {
    grid-template-columns: 1fr;
  }

  .chart-card,
  .activity-card {
    grid-column: auto;
  }
}

@media (max-width: 720px) {
  .mini-grid {
    grid-template-columns: 1fr;
  }

  .page-header h1 {
    font-size: 2.2rem;
  }

  .page-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
