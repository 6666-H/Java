<template>
  <div class="tenant-page">
    <section class="stats-grid">
      <article class="stat-card">
        <div class="stat-head">
          <div class="stat-icon blue"><el-icon><OfficeBuilding /></el-icon></div>
          <span>{{ formatTrend(overview.totalCountGrowthRate, '上月') }}</span>
        </div>
        <p>机构总数</p>
        <strong>{{ overview.totalCount || 0 }}</strong>
      </article>

      <article class="stat-card">
        <div class="stat-head">
          <div class="stat-icon indigo"><el-icon><UserFilled /></el-icon></div>
          <span>{{ formatTrend(overview.activeStudentGrowthRate, '近7天') }}</span>
        </div>
        <p>活跃学生数</p>
        <strong>{{ formatNumber(overview.activeStudentCount || 0) }}</strong>
      </article>

      <article class="stat-card">
        <div class="stat-head">
          <div class="stat-icon amber"><el-icon><Warning /></el-icon></div>
          <span class="warn">需关注</span>
        </div>
        <p>即将到期</p>
        <strong>{{ overview.expiringCount || 0 }}</strong>
      </article>

      <article class="stat-card revenue">
        <div class="stat-head">
          <div class="stat-icon white"><el-icon><DataAnalysis /></el-icon></div>
          <span>年度累计</span>
        </div>
        <p>Total Revenue</p>
        <strong>{{ formatCurrency(overview.estimatedRevenue || 0) }}</strong>
      </article>
    </section>

    <section class="table-shell">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <label>
            <span>状态：</span>
            <select v-model="statusFilter">
              <option value="ALL">全部</option>
              <option value="ACTIVE">启用中</option>
              <option value="EXPIRING">即将到期</option>
              <option value="EXPIRED">已过期</option>
              <option value="DISABLED">已停用</option>
            </select>
          </label>
          <label>
            <span>套餐：</span>
            <select v-model="planFilter">
              <option value="ALL">全部</option>
              <option value="旗舰版套餐">旗舰版套餐</option>
              <option value="标准版套餐">标准版套餐</option>
              <option value="基础版套餐">基础版套餐</option>
            </select>
          </label>
        </div>

        <div class="toolbar-right">
          <button type="button" class="table-action ghost" :disabled="page === 1" @click="page--">
            <el-icon><ArrowLeft /></el-icon>
          </button>
          <button type="button" class="table-action ghost" :disabled="page >= totalPages" @click="page++">
            <el-icon><ArrowRight /></el-icon>
          </button>
          <button type="button" class="table-action primary" @click="openCreateDialog">
            <el-icon><Plus /></el-icon>
            <span>添加机构</span>
          </button>
        </div>
      </div>

      <div class="table-meta">
        <span>显示 {{ rangeLabel }} 条，共 {{ filteredRows.length }} 条</span>
      </div>

      <div class="tenant-table" v-loading="loading">
        <div class="table-row table-head">
          <div>机构名称</div>
          <div>名额使用</div>
          <div>到期状态</div>
          <div>状态</div>
          <div class="align-right">操作</div>
        </div>

        <div v-for="row in pagedRows" :key="row.id" class="table-row">
          <div class="tenant-main">
            <div class="tenant-avatar">{{ row.name?.slice(0, 1) || 'T' }}</div>
            <div>
              <strong>{{ row.name }}</strong>
              <p>{{ row.planName || '基础版套餐' }} · ID: {{ row.id }}</p>
              <span>{{ row.orgAdminUsername || row.contactName || '未设置管理员' }}</span>
            </div>
          </div>

          <div class="quota-cell">
            <div class="quota-line">
              <strong>{{ row.usedCount || 0 }} / {{ row.accountQuota || 0 }}</strong>
              <span :class="quotaClass(row.usagePercent)">{{ row.usagePercent || 0 }}%</span>
            </div>
            <div class="quota-bar">
              <div class="quota-fill" :class="quotaClass(row.usagePercent)" :style="{ width: `${Math.min(row.usagePercent || 0, 100)}%` }"></div>
            </div>
          </div>

          <div class="expire-cell">
            <template v-if="row.expireStatus === 'EXPIRING'">
              <span class="pill pill-warn">剩余 {{ Math.max(row.daysRemaining || 0, 0) }} 天</span>
            </template>
            <template v-else-if="row.expireStatus === 'EXPIRED'">
              <span class="expire-muted">已过期</span>
            </template>
            <template v-else-if="row.expireTime">
              <span>{{ formatDate(row.expireTime) }}</span>
            </template>
            <template v-else>
              <span class="expire-muted">长期有效</span>
            </template>
          </div>

          <div>
            <el-switch
              :model-value="Number(row.status) === 1"
              inline-prompt
              @change="toggleStatus(row)"
            />
          </div>

          <div class="table-actions">
            <button type="button" class="icon-link" @click="openEditDialog(row)">
              <el-icon><Setting /></el-icon>
            </button>
            <button type="button" class="icon-link danger" @click="removeTenant(row)">
              <el-icon><Delete /></el-icon>
            </button>
          </div>
        </div>
      </div>

      <div class="pagination">
        <span class="page-size">每页行数：{{ pageSize }}</span>
        <div class="pager">
          <button
            v-for="item in visiblePages"
            :key="item"
            type="button"
            class="page-btn"
            :class="{ active: item === page, dots: item === '...' }"
            :disabled="item === '...'"
            @click="typeof item === 'number' && (page = item)"
          >
            {{ item }}
          </button>
        </div>
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingTenant ? '编辑机构' : '新增机构'" width="620px" @close="resetForm">
      <div class="dialog-grid">
        <el-form :model="form" label-width="96px">
          <el-form-item label="机构ID" required>
            <el-input v-model="form.id" :disabled="!!editingTenant" placeholder="手机号或唯一标识" />
          </el-form-item>
          <el-form-item label="机构名称" required>
            <el-input v-model="form.name" placeholder="请输入机构名称" />
          </el-form-item>
          <el-form-item label="联系人">
            <el-input v-model="form.contactName" placeholder="联系人姓名" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.contactPhone" placeholder="联系电话" />
          </el-form-item>
          <el-form-item label="学生名额">
            <el-input-number v-model="form.accountQuota" :min="0" />
          </el-form-item>
          <el-form-item label="到期时间">
            <el-date-picker
              v-model="form.expireTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="请选择到期时间"
            />
          </el-form-item>
          <el-form-item v-if="editingTenant" label="启用状态">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </el-form>

        <div class="dialog-note">
          <h4>机构开通说明</h4>
          <p>保存后会自动为机构生成默认校长账号，管理员可以再到授权管理为该机构分配教材内容。</p>
          <ul>
            <li>建议 `机构ID` 直接使用手机号或统一编码，方便后续定位。</li>
            <li>套餐类型会根据名额自动归类为基础版、标准版、旗舰版。</li>
            <li>收入卡片当前使用的是签约估值，不是财务实收。</li>
          </ul>
        </div>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import request from '../../api/request'
import { getTenantOverview } from '../../api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  ArrowRight,
  DataAnalysis,
  Delete,
  OfficeBuilding,
  Plus,
  Setting,
  UserFilled,
  Warning
} from '@element-plus/icons-vue'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingTenant = ref(null)
const overview = ref({
  totalCount: 0,
  activeStudentCount: 0,
  expiringCount: 0,
  estimatedRevenue: 0,
  totalCountGrowthRate: 0,
  activeStudentGrowthRate: 0,
  records: []
})
const page = ref(1)
const pageSize = 10
const statusFilter = ref('ALL')
const planFilter = ref('ALL')
const form = ref(createEmptyForm())

const filteredRows = computed(() => {
  return (overview.value.records || []).filter((item) => {
    const matchStatus = statusFilter.value === 'ALL' || item.expireStatus === statusFilter.value || (statusFilter.value === 'ACTIVE' && Number(item.status) === 1 && item.expireStatus === 'ACTIVE')
    const matchPlan = planFilter.value === 'ALL' || item.planName === planFilter.value
    return matchStatus && matchPlan
  })
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / pageSize)))
const pagedRows = computed(() => filteredRows.value.slice((page.value - 1) * pageSize, page.value * pageSize))
const rangeLabel = computed(() => {
  if (!filteredRows.value.length) return '0-0'
  const start = (page.value - 1) * pageSize + 1
  const end = Math.min(page.value * pageSize, filteredRows.value.length)
  return `${start}-${end}`
})

const visiblePages = computed(() => {
  if (totalPages.value <= 5) return Array.from({ length: totalPages.value }, (_, index) => index + 1)
  if (page.value <= 3) return [1, 2, 3, '...', totalPages.value]
  if (page.value >= totalPages.value - 2) return [1, '...', totalPages.value - 2, totalPages.value - 1, totalPages.value]
  return [1, '...', page.value, '...', totalPages.value]
})

watch([filteredRows, totalPages], () => {
  if (page.value > totalPages.value) page.value = totalPages.value
}, { deep: true })

function createEmptyForm() {
  return {
    id: '',
    name: '',
    contactName: '',
    contactPhone: '',
    accountQuota: 100,
    expireTime: '',
    status: 1
  }
}

function resetForm() {
  editingTenant.value = null
  form.value = createEmptyForm()
}

function formatDate(value) {
  if (!value) return '—'
  return String(value).replace('T', '-').slice(0, 10)
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function formatCurrency(value) {
  return `¥${(Number(value || 0) / 1000000).toFixed(2)}M`
}

function formatTrend(value, suffix) {
  const amount = Number(value || 0)
  const prefix = amount > 0 ? '+' : ''
  return `${prefix}${amount.toFixed(1)}% ${suffix}`
}

function quotaClass(percent) {
  if (Number(percent || 0) >= 90) return 'danger'
  if (Number(percent || 0) >= 80) return 'warning'
  return 'safe'
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  editingTenant.value = row
  form.value = {
    id: row.id,
    name: row.name || '',
    contactName: row.contactName || '',
    contactPhone: row.contactPhone || '',
    accountQuota: Number(row.accountQuota || 0),
    expireTime: row.expireTime ? String(row.expireTime).slice(0, 19) : '',
    status: Number(row.status) === 1 ? 1 : 0
  }
  dialogVisible.value = true
}

async function fetchOverview() {
  loading.value = true
  try {
    overview.value = { ...(await getTenantOverview()) }
  } catch (error) {
    ElMessage.error(error.message || '获取机构总览失败')
  } finally {
    loading.value = false
  }
}

async function submitForm() {
  if (!form.value.id.trim() || !form.value.name.trim()) {
    ElMessage.warning('请填写机构ID和机构名称')
    return
  }

  const payload = {
    id: form.value.id.trim(),
    name: form.value.name.trim(),
    contactName: form.value.contactName?.trim() || '',
    contactPhone: form.value.contactPhone?.trim() || '',
    accountQuota: Number(form.value.accountQuota || 0),
    expireTime: form.value.expireTime || null,
    status: Number(form.value.status) === 1 ? 1 : 0
  }

  saving.value = true
  try {
    if (editingTenant.value) {
      await request.put(`/admin/tenants/${editingTenant.value.id}`, payload)
      ElMessage.success('机构信息已更新')
    } else {
      await request.post('/admin/tenants', payload)
      ElMessage.success('机构已创建')
    }
    dialogVisible.value = false
    resetForm()
    await fetchOverview()
  } catch (error) {
    ElMessage.error(error.message || '保存机构失败')
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const nextStatus = Number(row.status) === 1 ? 0 : 1
  try {
    await request.patch(`/admin/tenants/${row.id}/status`, null, { params: { status: nextStatus } })
    ElMessage.success(nextStatus === 1 ? '机构已启用' : '机构已停用')
    await fetchOverview()
  } catch (error) {
    ElMessage.error(error.message || '更新状态失败')
  }
}

async function removeTenant(row) {
  try {
    await ElMessageBox.confirm(`确认删除机构“${row.name}”吗？删除后机构账号将不可登录。`, '删除机构', { type: 'warning' })
    await request.delete(`/admin/tenants/${row.id}`)
    ElMessage.success('机构已删除')
    await fetchOverview()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除机构失败')
    }
  }
}

onMounted(fetchOverview)
</script>

<style scoped>
.tenant-page {
  display: grid;
  gap: 16px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card,
.table-shell {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  border: 1px solid rgba(193, 198, 214, 0.35);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.05);
}

.stat-card {
  padding: 14px 18px;
}

.stat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.stat-head span {
  font-size: 11px;
  font-weight: 700;
  color: #6d7fe8;
}

.stat-head .warn {
  color: #c45a1d;
}

.stat-card p,
.stat-card strong {
  margin: 0;
}

.stat-card p {
  color: #6b7280;
  font-size: 12px;
}

.stat-card strong {
  display: block;
  margin-top: 6px;
  font-family: 'Manrope', 'PingFang SC', sans-serif;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.stat-icon {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: grid;
  place-items: center;
}

.stat-icon.blue {
  color: #0b5fd1;
  background: rgba(11, 95, 209, 0.1);
}

.stat-icon.indigo {
  color: #5364d8;
  background: rgba(83, 100, 216, 0.1);
}

.stat-icon.amber {
  color: #c45a1d;
  background: rgba(196, 90, 29, 0.12);
}

.stat-icon.white {
  color: #fff;
  background: rgba(255, 255, 255, 0.18);
}

.revenue {
  color: #fff;
  background: linear-gradient(135deg, #0b5fd1, #1a73e8);
}

.revenue p,
.revenue .stat-head span {
  color: rgba(255, 255, 255, 0.88);
}

.table-shell {
  overflow: hidden;
}

.table-toolbar,
.pagination,
.table-meta {
  padding: 20px 24px;
}

.table-toolbar,
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 28px;
}

.toolbar-left label {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #6b7280;
  font-size: 13px;
  font-weight: 700;
}

.toolbar-left select {
  border: none;
  outline: none;
  background: transparent;
  color: #111827;
  font-weight: 700;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.table-action {
  height: 40px;
  border: none;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  font-weight: 700;
  cursor: pointer;
}

.table-action.ghost {
  background: #f3f4f6;
}

.table-action.primary {
  background: #0b5fd1;
  color: #fff;
}

.table-meta {
  padding-top: 0;
  color: #6b7280;
  font-size: 12px;
}

.tenant-table {
  display: grid;
}

.table-row {
  display: grid;
  grid-template-columns: minmax(280px, 2.2fr) minmax(170px, 1.3fr) minmax(120px, 1fr) 84px 100px;
  align-items: center;
  gap: 16px;
  padding: 18px 24px;
  border-top: 1px solid rgba(241, 245, 249, 0.95);
}

.table-head {
  background: rgba(242, 244, 246, 0.65);
  color: #6b7280;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.tenant-main {
  display: flex;
  align-items: center;
  gap: 14px;
}

.tenant-avatar {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: #eef4ff;
  color: #2d5ac8;
  font-weight: 800;
}

.tenant-main strong,
.tenant-main p,
.tenant-main span {
  display: block;
}

.tenant-main strong {
  color: #111827;
}

.tenant-main p,
.tenant-main span,
.expire-muted {
  margin-top: 3px;
  color: #6b7280;
  font-size: 12px;
}

.quota-cell {
  width: 100%;
  max-width: 144px;
}

.quota-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12px;
}

.quota-line strong {
  color: #111827;
}

.quota-line span.safe,
.quota-fill.safe {
  color: #0b5fd1;
  background-color: #0b5fd1;
}

.quota-line span.warning,
.quota-fill.warning {
  color: #c45a1d;
  background-color: #c45a1d;
}

.quota-line span.danger,
.quota-fill.danger {
  color: #c2410c;
  background-color: #c2410c;
}

.quota-bar {
  height: 6px;
  overflow: hidden;
  border-radius: 999px;
  background: #e5e7eb;
}

.quota-fill {
  height: 100%;
  border-radius: inherit;
}

.expire-cell {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
}

.pill {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.pill-warn {
  color: #b45309;
  background: #ffedd5;
}

.table-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.icon-link {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: transparent;
  color: #4b5563;
  cursor: pointer;
}

.icon-link:hover {
  background: #f3f4f6;
}

.icon-link.danger {
  color: #dc2626;
}

.pagination {
  border-top: 1px solid rgba(241, 245, 249, 0.95);
}

.page-size {
  color: #6b7280;
  font-size: 13px;
}

.pager {
  display: flex;
  gap: 8px;
}

.page-btn {
  min-width: 34px;
  height: 34px;
  border: none;
  border-radius: 10px;
  background: transparent;
  font-weight: 700;
  cursor: pointer;
}

.page-btn.active {
  background: #0b5fd1;
  color: #fff;
}

.page-btn.dots {
  cursor: default;
}

.dialog-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 20px;
}

.dialog-note {
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(180deg, #f8fbff, #f3f7ff);
  color: #4b5563;
}

.dialog-note h4 {
  margin: 0 0 10px;
  color: #111827;
}

.dialog-note p {
  margin: 0 0 10px;
  line-height: 1.7;
}

.dialog-note ul {
  margin: 0;
  padding-left: 18px;
  line-height: 1.8;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .table-row {
    grid-template-columns: 1fr;
  }

  .table-head {
    display: none;
  }

  .dialog-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .table-toolbar,
  .pagination {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-left,
  .toolbar-right {
    justify-content: space-between;
  }
}
</style>
