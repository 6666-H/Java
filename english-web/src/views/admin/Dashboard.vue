<template>
  <div class="admin-dashboard" v-loading="loading">
    <section class="metric-grid" v-if="dashboard">
      <article class="metric-card">
        <span>学校总数</span>
        <strong>{{ dashboard.totalTenants }}</strong>
        <em>平台当前管理的全部租户</em>
      </article>
      <article class="metric-card warm">
        <span>近 7 天活跃学校</span>
        <strong>{{ dashboard.activeTenants }}</strong>
        <em>有学生学习记录的学校</em>
      </article>
      <article class="metric-card cool">
        <span>今日学习人数</span>
        <strong>{{ dashboard.todayActiveStudents }}</strong>
        <em>全平台当日活跃学生</em>
      </article>
      <article class="metric-card">
        <span>累计掌握单词</span>
        <strong>{{ dashboard.totalMasteredWords }}</strong>
        <em>已达到掌握状态的单词数</em>
      </article>
    </section>

    <section class="dashboard-grid" v-if="dashboard">
      <div class="panel">
        <div class="panel-head">
          <div>
            <h3>近 7 天学习趋势</h3>
            <p>按天统计全平台活跃学生人数</p>
          </div>
        </div>
        <div class="trend-chart">
          <div v-for="(value, index) in dashboard.trendValues" :key="dashboard.trendLabels[index]" class="trend-item">
            <div class="trend-bar-wrap">
              <div class="trend-bar" :style="{ height: barHeight(value) }"></div>
            </div>
            <strong>{{ value }}</strong>
            <span>{{ shortDate(dashboard.trendLabels[index]) }}</span>
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-head">
          <div>
            <h3>学校活跃排行</h3>
            <p>按近 7 天活跃学生数排序</p>
          </div>
        </div>
        <div v-if="dashboard.tenantRanks?.length" class="rank-list">
          <article v-for="(item, index) in dashboard.tenantRanks" :key="item.tenantId" class="rank-row">
            <div class="rank-index">{{ index + 1 }}</div>
            <div class="rank-copy">
              <strong>{{ item.tenantName }}</strong>
              <span>{{ item.tenantId }}</span>
            </div>
            <em>{{ item.activeStudents }} 人</em>
          </article>
        </div>
        <el-empty v-else description="暂无排行数据" />
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminDashboard } from '../../api/admin'

const loading = ref(false)
const dashboard = ref(null)

function barHeight(value) {
  const values = dashboard.value?.trendValues || []
  const max = Math.max(...values, 1)
  return `${Math.max((value / max) * 100, 8)}%`
}

function shortDate(value) {
  if (!value) return '--'
  return value.slice(5)
}

async function fetchDashboard() {
  loading.value = true
  try {
    dashboard.value = await getAdminDashboard()
  } catch (error) {
    ElMessage.error(error.message || '获取平台概览失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchDashboard)
</script>

<style scoped>
.admin-dashboard {
  display: grid;
  gap: 16px;
}

.metric-grid,
.dashboard-grid {
  display: grid;
  gap: 16px;
}

.metric-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.dashboard-grid {
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.9fr);
}

.metric-card,
.panel {
  background: rgba(255, 255, 255, 0.82);
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.metric-card {
  padding: 12px 14px;
}

.metric-card span,
.metric-card strong,
.metric-card em {
  display: block;
}

.metric-card span {
  color: #64748b;
  font-size: 0.76rem;
}

.metric-card strong {
  margin-top: 6px;
  font-size: 1.18rem;
  line-height: 1;
  color: #111827;
}

.metric-card em {
  margin-top: 6px;
  color: #94a3b8;
  font-style: normal;
  font-size: 0.75rem;
}

.metric-card.warm strong { color: #ea580c; }
.metric-card.cool strong { color: #2563eb; }

.panel {
  padding: 14px;
}

.panel-head h3 {
  margin: 0;
  font-size: 0.95rem;
  color: #1e293b;
}

.panel-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 0.76rem;
}

.trend-chart {
  margin-top: 12px;
  min-height: 176px;
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
  align-items: end;
}

.trend-item {
  display: grid;
  gap: 8px;
  justify-items: center;
}

.trend-bar-wrap {
  width: 100%;
  height: 122px;
  display: flex;
  align-items: end;
  justify-content: center;
}

.trend-bar {
  width: 100%;
  max-width: 36px;
  border-radius: 14px 14px 6px 6px;
  background: linear-gradient(180deg, #fb7185 0%, #f97316 48%, #2563eb 100%);
}

.trend-item strong {
  font-size: 0.82rem;
  color: #1e293b;
}

.trend-item span {
  color: #94a3b8;
  font-size: 0.7rem;
}

.rank-list {
  margin-top: 10px;
  display: grid;
  gap: 6px;
}

.rank-row {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 12px;
  align-items: center;
  padding: 8px 10px;
  border-radius: 14px;
  background: #f8fafc;
}

.rank-index {
  width: 28px;
  height: 28px;
  border-radius: 10px;
  background: #111827;
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 800;
  font-size: 0.74rem;
}

.rank-copy strong,
.rank-copy span {
  display: block;
}

.rank-copy strong {
  color: #1e293b;
  font-size: 0.86rem;
}

.rank-copy span {
  margin-top: 2px;
  color: #94a3b8;
  font-size: 0.72rem;
}

.rank-row em {
  color: #2563eb;
  font-style: normal;
  font-weight: 700;
}

@media (max-width: 1024px) {
  .metric-grid,
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
