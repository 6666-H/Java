<template>
  <div class="progress-page">
    <section class="page-header">
      <div>
        <h1>机构学情分析</h1>
        <p>查看全体学生在选定时间段内的学习进度与质量报告</p>
      </div>
      <div class="header-actions">
        <div class="date-chip">
          <el-icon><Calendar /></el-icon>
          <span>{{ rangeLabel }}</span>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            class="date-picker"
            @change="handleDateRangeChange"
          />
        </div>
        <button type="button" class="primary-btn" @click="exportCsv">
          <el-icon><Download /></el-icon>
          <span>导出全员报告</span>
        </button>
      </div>
    </section>

    <section class="metric-grid">
      <article class="metric-card">
        <div class="metric-title"><el-icon><Reading /></el-icon><span>人均掌握单词</span></div>
        <div class="metric-value"><strong>{{ summary.averageMastered }}</strong><em>{{ formatDelta(summary.averageMasteredDelta) }}</em></div>
      </article>
      <article class="metric-card">
        <div class="metric-title"><el-icon><CircleCheck /></el-icon><span>平均正确率</span></div>
        <div class="metric-value"><strong>{{ summary.averageAccuracy }}%</strong><em>{{ formatDelta(summary.averageAccuracyDelta) }}</em></div>
      </article>
      <article class="metric-card">
        <div class="metric-title"><el-icon><Timer /></el-icon><span>平均学习时长</span></div>
        <div class="metric-value"><strong>{{ summary.averageDurationLabel }}</strong><small>来自阶段学习结果</small></div>
      </article>
      <article class="metric-card">
        <div class="metric-title"><el-icon><TrendCharts /></el-icon><span>活跃学生总数</span></div>
        <div class="metric-value"><strong>{{ summary.activeStudentCount }}</strong><em>{{ formatDelta(summary.activeStudentDelta) }}</em></div>
      </article>
    </section>

    <section class="chart-grid">
      <article class="chart-card">
        <div class="card-head">
          <div>
            <h2>学习趋势分析</h2>
            <p>每日平均词汇增长与活跃度对比</p>
          </div>
          <div class="legend">
            <span><i class="dot blue"></i>掌握数</span>
            <span><i class="dot violet"></i>学习时长</span>
          </div>
        </div>

        <div class="chart-grid-line">
          <span></span><span></span><span></span><span></span>
        </div>
        <div class="line-chart">
          <div v-for="(item, index) in chartSeries" :key="index" class="line-col">
            <div class="line-bars">
              <span class="bar-back" :style="{ height: `${item.durationHeight}%` }"></span>
              <span class="bar-front" :style="{ height: `${item.masteredHeight}%` }"></span>
            </div>
            <span class="line-label">{{ item.label }}</span>
          </div>
        </div>
      </article>

      <article class="insight-card">
        <h3>学情洞察</h3>
        <p>{{ summary.insightSummary }}</p>
        <div class="insight-item">
          <span class="insight-icon"><el-icon><Opportunity /></el-icon></span>
          <div>
            <strong>重点关注</strong>
            <span>{{ summary.focusTopic }}</span>
          </div>
        </div>
        <div class="insight-item">
          <span class="insight-icon"><el-icon><MagicStick /></el-icon></span>
          <div>
            <strong>教学建议</strong>
            <span>{{ summary.suggestion }}</span>
          </div>
        </div>
      </article>
    </section>

    <section class="table-card">
      <div class="card-head bottom">
        <h2>学生详细表现</h2>
        <div class="table-filters">
          <button type="button" class="filter-chip active">全部学生</button>
          <button type="button" class="filter-chip">待提升</button>
        </div>
      </div>

      <el-table :data="reportRows" v-loading="reportLoading" empty-text="暂无报表数据" class="report-table">
        <el-table-column label="学生姓名" min-width="220">
          <template #default="{ row }">
            <div class="student-cell">
              <div class="avatar">{{ initials(row.realName || row.username) }}</div>
              <strong>{{ row.realName || row.username }}</strong>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="studentNo" label="学号" min-width="130" />
        <el-table-column prop="masteredCount" label="词汇量" min-width="100" />
        <el-table-column label="学习正确率" min-width="180">
          <template #default="{ row }">
            <div class="rate-cell">
              <span class="rate-track"><span class="rate-fill" :style="{ width: `${rowAccuracy(row)}%` }"></span></span>
              <strong>{{ rowAccuracy(row) }}%</strong>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="本月学时" min-width="100">
          <template #default="{ row }">{{ rowDuration(row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default>
            <button type="button" class="export-btn">导出报表</button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import request from '../../api/request'
import { ElMessage } from 'element-plus'
import { getOrgReport, getOrgReportSummary } from '../../api/org'
import {
  Calendar,
  CircleCheck,
  Download,
  MagicStick,
  Opportunity,
  Reading,
  Timer,
  TrendCharts
} from '@element-plus/icons-vue'

const reportRows = ref([])
const reportLoading = ref(false)
const summary = ref({
  averageMastered: 0,
  averageMasteredDelta: 0,
  averageAccuracy: 0,
  averageAccuracyDelta: 0,
  averageDurationLabel: '0 min/d',
  activeStudentCount: 0,
  activeStudentDelta: 0,
  chartLabels: [],
  chartMasteredValues: [],
  chartDurationValues: [],
  insightSummary: '当前暂无可生成洞察的学习记录。',
  focusTopic: '暂无重点薄弱项',
  suggestion: '请先选择时间范围并生成学习数据。'
})
const filters = reactive({
  startDate: '',
  endDate: ''
})
const dateRange = ref([])

const rangeLabel = computed(() => {
  if (filters.startDate && filters.endDate) return `${filters.startDate} - ${filters.endDate}`
  return '最近30天'
})

const chartSeries = computed(() => {
  const labels = Array.isArray(summary.value?.chartLabels) ? summary.value.chartLabels : []
  const mastered = Array.isArray(summary.value?.chartMasteredValues) ? summary.value.chartMasteredValues : []
  const duration = Array.isArray(summary.value?.chartDurationValues) ? summary.value.chartDurationValues : []
  const base = labels.map((label, index) => ({
    label,
    mastered: Number(mastered[index] || 0),
    duration: Number(duration[index] || 0)
  }))
  const maxMastered = Math.max(...base.map(item => item.mastered), 1)
  const maxDuration = Math.max(...base.map(item => item.duration), 1)
  return base.map(item => ({
    ...item,
    masteredHeight: Math.max(14, Math.round(item.mastered / maxMastered * 100)),
    durationHeight: Math.max(14, Math.round(item.duration / maxDuration * 100))
  }))
})

function initials(value) {
  return String(value || '学').trim().slice(0, 2)
}

function rowAccuracy(row) {
  return Math.round((
    Number(row.flashcardRate || 0) +
    Number(row.engChRate || 0) +
    Number(row.chEngRate || 0) +
    Number(row.spellRate || 0)
  ) / 4)
}

function rowDuration(row) {
  return `${(Number(row.learningDays || 0) * 1.5).toFixed(1)}h`
}

function handleDateRangeChange(value) {
  filters.startDate = value?.[0] || ''
  filters.endDate = value?.[1] || ''
  loadReport()
}

async function loadReport() {
  reportLoading.value = true
  try {
    const params = {
      startDate: filters.startDate || undefined,
      endDate: filters.endDate || undefined
    }
    const [rows, summaryRes] = await Promise.all([
      getOrgReport(params),
      getOrgReportSummary(params)
    ])
    reportRows.value = Array.isArray(rows) ? rows : []
    summary.value = {
      ...summary.value,
      ...(summaryRes || {})
    }
  } catch (error) {
    ElMessage.error(error.message || '获取报表失败')
    reportRows.value = []
  } finally {
    reportLoading.value = false
  }
}

function formatDelta(value) {
  const amount = Number(value || 0)
  const prefix = amount > 0 ? '+' : ''
  return `${prefix}${amount.toFixed(1)}%`
}

function exportCsv() {
  if (reportRows.value.length === 0) return
  const header = ['姓名', '账号', '学号', '年级班级', '学习天数', '掌握单词', '错误次数', '阶段1完成率', '阶段2完成率', '阶段3完成率', '阶段4完成率', '最近学习时间']
  const rows = reportRows.value.map(item => [
    item.realName || '',
    item.username || '',
    item.studentNo || '',
    item.gradeClass || '',
    item.learningDays ?? 0,
    item.masteredCount ?? 0,
    item.totalErrors ?? 0,
    `${item.flashcardRate ?? 0}%`,
    `${item.engChRate ?? 0}%`,
    `${item.chEngRate ?? 0}%`,
    `${item.spellRate ?? 0}%`,
    item.lastStudyAt || ''
  ])
  const csv = [header, ...rows].map(line =>
    line.map(value => `"${String(value ?? '').replace(/"/g, '""')}"`).join(',')
  ).join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `learning-report-${Date.now()}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
}

onMounted(async () => {
  await request.get('/tenant/users', { params: { role: 'STUDENT' } }).catch(() => [])
  await loadReport()
})
</script>

<style scoped>
.progress-page {
  display: grid;
  gap: 24px;
}

.page-header,
.header-actions,
.metric-grid,
.chart-grid,
.card-head,
.legend,
.line-chart,
.line-bars,
.insight-item,
.student-cell,
.rate-cell,
.table-filters {
  display: flex;
  align-items: center;
}

.page-header,
.card-head {
  justify-content: space-between;
}

.page-header h1,
.card-head h2,
.insight-card h3 {
  margin: 0;
  font-family: "Manrope", sans-serif;
  color: #191c1e;
}

.page-header h1 {
  font-size: 1.55rem;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.page-header p,
.card-head p {
  margin: 8px 0 0;
  color: #667085;
}

.header-actions,
.legend,
.table-filters {
  gap: 12px;
}

.date-chip,
.primary-btn,
.filter-chip,
.export-btn {
  border: none;
}

.date-chip {
  position: relative;
  height: 36px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-radius: 12px;
  background: #f2f4f6;
  color: #5f6d82;
  font-size: 0.86rem;
  font-weight: 600;
}

.date-picker {
  position: absolute;
  inset: 0;
  opacity: 0;
}

.primary-btn {
  height: 36px;
  padding: 0 14px;
  border-radius: 12px;
  background: #005bbf;
  color: #fff;
  font-size: 0.86rem;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.metric-grid {
  gap: 16px;
  align-items: stretch;
}

.metric-card,
.chart-card,
.insight-card,
.table-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 10px 22px rgba(25, 28, 30, 0.05);
}

.metric-card {
  flex: 1;
  min-height: 88px;
  padding: 12px;
  display: grid;
}

.metric-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #5f6d82;
  font-size: 0.8rem;
  font-weight: 700;
}

.metric-value {
  margin-top: auto;
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.metric-value strong {
  font-family: "Manrope", sans-serif;
  color: #191c1e;
  font-size: 1.4rem;
  font-weight: 800;
}

.metric-value em,
.metric-value small {
  color: #16a34a;
  font-size: 0.74rem;
  font-style: normal;
  font-weight: 700;
}

.metric-value small {
  color: #667085;
}

.chart-grid {
  align-items: stretch;
  gap: 16px;
}

.chart-card {
  position: relative;
  flex: 1.7;
  padding: 14px;
  overflow: hidden;
}

.legend span {
  color: #5f6d82;
  font-size: 0.78rem;
  font-weight: 700;
}

.dot {
  width: 10px;
  height: 10px;
  margin-right: 6px;
  display: inline-block;
  border-radius: 999px;
}

.dot.blue {
  background: #005bbf;
}

.dot.violet {
  background: #4355b9;
}

.chart-grid-line {
  position: absolute;
  inset: 66px 14px 28px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  opacity: 0.18;
}

.chart-grid-line span {
  border-bottom: 1px solid #727785;
}

.line-chart {
  position: relative;
  z-index: 1;
  height: 180px;
  margin-top: 10px;
  justify-content: space-between;
  align-items: end;
}

.line-col {
  flex: 1;
  display: grid;
  gap: 12px;
}

.line-bars {
  height: 150px;
  justify-content: center;
  align-items: end;
  position: relative;
}

.bar-back,
.bar-front {
  position: absolute;
  bottom: 0;
  border-radius: 8px 8px 0 0;
}

.bar-back {
  width: 22px;
  background: rgba(0, 91, 191, 0.15);
}

.bar-front {
  width: 10px;
  background: #005bbf;
}

.line-label {
  text-align: center;
  color: #727785;
  font-size: 0.7rem;
  font-weight: 700;
}

.insight-card {
  width: 300px;
  padding: 14px;
  color: #fff;
  background: #005bbf;
  position: relative;
  overflow: hidden;
}

.insight-card::before {
  content: '';
  position: absolute;
  right: -46px;
  bottom: -46px;
  width: 160px;
  height: 160px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  filter: blur(20px);
}

.insight-card p {
  position: relative;
  z-index: 1;
  margin: 10px 0 0;
  color: #dbeafe;
  line-height: 1.8;
  font-size: 0.92rem;
}

.insight-item {
  position: relative;
  z-index: 1;
  gap: 12px;
  margin-top: 14px;
}

.insight-icon {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.18);
}

.insight-item strong,
.insight-item span {
  display: block;
  color: #fff;
}

.insight-item strong {
  font-size: 0.8rem;
}

.insight-item span {
  margin-top: 4px;
  color: #dbeafe;
  font-size: 0.86rem;
}

.table-card {
  padding: 0;
  overflow: hidden;
}

.card-head.bottom {
  padding: 14px;
  margin: 0;
}

.filter-chip {
  height: 30px;
  padding: 0 14px;
  border-radius: 999px;
  background: transparent;
  color: #667085;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
}

.filter-chip.active {
  background: #f2f4f6;
}

:deep(.report-table .el-table__inner-wrapper::before) {
  display: none;
}

:deep(.report-table th.el-table__cell) {
  background: rgba(242, 244, 246, 0.5);
  color: #667085;
  font-weight: 800;
}

.student-cell,
.rate-cell {
  gap: 12px;
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #d8e2ff 0%, #ffdbcb 100%);
  color: #005bbf;
  font-size: 0.8rem;
  font-weight: 800;
}

.rate-track {
  flex: 1;
  height: 6px;
  border-radius: 999px;
  background: #e6e8ea;
  overflow: hidden;
}

.rate-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #005bbf;
}

.export-btn {
  background: transparent;
  color: #005bbf;
  font-size: 0.82rem;
  font-weight: 800;
  cursor: pointer;
}

@media (max-width: 1120px) {
  .page-header,
  .metric-grid,
  .chart-grid {
    flex-direction: column;
    align-items: stretch;
  }

  .insight-card {
    width: auto;
  }
}
</style>
