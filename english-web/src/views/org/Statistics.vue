<template>
  <div class="org-page">
    <section class="stats-grid" v-if="stats">
      <div class="stat-card stat-blue">
        <div class="stat-icon">👥</div>
        <div class="stat-body">
          <span class="stat-label">全校学生总数</span>
          <span class="stat-value">{{ stats.totalStudents }}</span>
        </div>
      </div>
      <div class="stat-card stat-green">
        <div class="stat-icon">✨</div>
        <div class="stat-body">
          <span class="stat-label">今日活跃人数</span>
          <span class="stat-value">{{ stats.activeToday }}</span>
        </div>
      </div>
      <div class="stat-card stat-purple">
        <div class="stat-icon">📖</div>
        <div class="stat-body">
          <span class="stat-label">累计攻克单词</span>
          <span class="stat-value">{{ stats.totalWordsLearned }}</span>
        </div>
      </div>
    </section>

    <section class="chart-card" v-if="stats">
      <h3 class="chart-title">近 7 天活跃趋势</h3>
      <div class="chart-bars">
        <div v-for="(count, index) in stats.activeTrend" :key="index" class="bar-wrap">
          <div class="bar" :style="{ height: barHeight(count) }" :title="`${count} 人`">
            <span class="bar-num">{{ count }}</span>
          </div>
        </div>
      </div>
      <div class="chart-labels">
        <span v-for="i in 7" :key="i">{{ i === 7 ? '今天' : (7 - i) + '天前' }}</span>
      </div>
    </section>

    <section class="report-card">
      <div class="report-head">
        <div>
          <h3>学生学习报表</h3>
          <p>支持按班级、时间和关键词查看学习天数、连续打卡、掌握数和四阶段完成率。</p>
        </div>
        <div class="report-actions">
          <el-select v-model="filters.gradeClass" placeholder="全部班级" clearable class="filter-item">
            <el-option v-for="item in gradeClassOptions" :key="item" :label="item" :value="item" />
          </el-select>
          <el-input v-model="filters.keyword" placeholder="搜索姓名/账号/学号" clearable class="filter-item input-item" />
          <el-date-picker v-model="filters.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" clearable class="filter-item" />
          <el-date-picker v-model="filters.endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" clearable class="filter-item" />
          <el-button type="primary" :loading="reportLoading" @click="loadReport">查询</el-button>
          <el-button @click="exportCsv" :disabled="reportRows.length === 0">导出 CSV</el-button>
        </div>
      </div>

      <el-table :data="reportRows" v-loading="reportLoading" stripe empty-text="暂无报表数据">
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column prop="username" label="账号" min-width="120" />
        <el-table-column prop="studentNo" label="学号" min-width="120" />
        <el-table-column prop="gradeClass" label="年级/班级" min-width="120" />
        <el-table-column prop="learningDays" label="学习天数" width="100" align="center" />
        <el-table-column prop="streakDays" label="连续打卡" width="100" align="center" />
        <el-table-column prop="masteredCount" label="掌握单词" width="110" align="center" />
        <el-table-column prop="totalErrors" label="错误次数" width="100" align="center" />
        <el-table-column label="阶段完成率" min-width="280">
          <template #default="scope">
            <div class="rate-list">
              <span>① {{ scope.row.flashcardRate }}%</span>
              <span>② {{ scope.row.engChRate }}%</span>
              <span>③ {{ scope.row.chEngRate }}%</span>
              <span>④ {{ scope.row.spellRate }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="lastStudyAt" label="最近学习时间" min-width="160" />
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import request from '../../api/request'
import { ElMessage } from 'element-plus'
import { getOrgReport } from '../../api/org'

const loading = ref(true)
const reportLoading = ref(false)
const stats = ref(null)
const students = ref([])
const reportRows = ref([])
const filters = reactive({
  gradeClass: '',
  keyword: '',
  startDate: '',
  endDate: ''
})

const gradeClassOptions = computed(() =>
  [...new Set((students.value || []).map(item => item.gradeClass).filter(Boolean))]
)

function barHeight(count) {
  if (!stats.value || !stats.value.totalStudents) return '4px'
  const max = Math.max(...(stats.value.activeTrend || [1]), 1)
  const pct = (count / max) * 100
  return `${Math.max(8, pct)}%`
}

async function fetchStudents() {
  try {
    const res = await request.get('/tenant/users', { params: { role: 'STUDENT' } })
    students.value = Array.isArray(res) ? res : []
  } catch (_error) {
    students.value = []
  }
}

async function fetchStats() {
  loading.value = true
  try {
    const res = await request.get('/tenant/stats')
    stats.value = res || null
  } catch (err) {
    ElMessage.error(err.message || '获取统计数据失败')
  } finally {
    loading.value = false
  }
}

async function loadReport() {
  reportLoading.value = true
  try {
    reportRows.value = await getOrgReport({
      gradeClass: filters.gradeClass || undefined,
      keyword: filters.keyword || undefined,
      startDate: filters.startDate || undefined,
      endDate: filters.endDate || undefined
    })
  } catch (error) {
    ElMessage.error(error.message || '获取报表失败')
    reportRows.value = []
  } finally {
    reportLoading.value = false
  }
}

function exportCsv() {
  if (reportRows.value.length === 0) return
  const header = ['姓名', '账号', '学号', '年级班级', '学习天数', '连续打卡', '掌握单词', '错误次数', '阶段1完成率', '阶段2完成率', '阶段3完成率', '阶段4完成率', '最近学习时间']
  const rows = reportRows.value.map(item => [
    item.realName || '',
    item.username || '',
    item.studentNo || '',
    item.gradeClass || '',
    item.learningDays ?? 0,
    item.streakDays ?? 0,
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
  link.download = `school-report-${Date.now()}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
}

onMounted(async () => {
  await Promise.all([fetchStats(), fetchStudents(), loadReport()])
})
</script>

<style scoped>
.org-page { min-height: 100%; }
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}
.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 6px rgba(15, 23, 42, 0.06);
}
.stat-icon { font-size: 1.2rem; line-height: 1; }
.stat-blue { border-left: 4px solid #3b82f6; }
.stat-green { border-left: 4px solid #22c55e; }
.stat-purple { border-left: 4px solid #8b5cf6; }
.stat-label { display: block; font-size: 0.75rem; color: #64748b; margin-bottom: 4px; }
.stat-value { font-size: 1.15rem; font-weight: 800; color: #1e293b; }
.chart-card {
  background: #fff;
  border-radius: 14px;
  padding: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06);
  margin-bottom: 14px;
}
.chart-title { font-size: 0.94rem; font-weight: 700; color: #334155; margin: 0 0 14px 0; }
.chart-bars {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  height: 180px;
}
.bar-wrap { flex: 1; min-width: 0; display: flex; flex-direction: column; align-items: center; }
.bar {
  width: 100%;
  max-width: 48px;
  min-height: 8px;
  background: linear-gradient(180deg, #7c3aed 0%, #6d28d9 100%);
  border-radius: 8px 8px 0 0;
  transition: height 0.3s;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 6px;
}
.bar:hover { opacity: 0.9; }
.bar-num { font-size: 0.6875rem; color: rgba(255,255,255,0.95); font-weight: 600; }
.chart-labels {
  display: flex;
  justify-content: space-around;
  padding: 10px 0 0;
  font-size: 0.6875rem;
  color: #94a3b8;
}
.report-card {
  background: #fff;
  border-radius: 16px;
  padding: 14px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06);
}

.report-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 12px;
}

.report-head h3 {
  margin: 0;
  font-size: 1rem;
  color: #1e293b;
}

.report-head p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 0.84rem;
}

.report-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.filter-item {
  width: 140px;
}

.input-item {
  width: 180px;
}

.rate-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  color: #475569;
  font-size: 0.82rem;
}

@media (max-width: 980px) {
  .report-head {
    flex-direction: column;
  }

  .report-actions {
    justify-content: flex-start;
  }
}
</style>
