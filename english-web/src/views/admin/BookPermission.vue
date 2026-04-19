<template>
  <div class="permission-page">
    <section class="page-head">
      <div>
        <h2>授权管理</h2>
        <p>监控与分发教育资源授权，管理合作机构权限周期。</p>
      </div>
      <div class="head-actions">
        <button type="button" class="action-btn ghost" @click="exportReport">导出报表</button>
        <button type="button" class="action-btn primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          <span>新增授权</span>
        </button>
        <button type="button" class="action-btn primary" @click="goCreateTenant">
          <el-icon><OfficeBuilding /></el-icon>
          <span>创建新机构</span>
        </button>
      </div>
    </section>

    <section class="stats-grid">
      <article class="summary-card">
        <div class="summary-head">
          <div class="summary-icon blue"><el-icon><Histogram /></el-icon></div>
          <span>{{ formatTrend(overview.growthRate, '同比') }}</span>
        </div>
        <p>总授权数</p>
        <strong>{{ overview.totalCount || 0 }}</strong>
      </article>

      <article class="summary-card">
        <div class="summary-head">
          <div class="summary-icon indigo"><el-icon><Select /></el-icon></div>
          <span class="pill">正常运行</span>
        </div>
        <p>当前活跃</p>
        <strong>{{ overview.activeCount || 0 }}</strong>
      </article>

      <article class="summary-card attention">
        <div class="summary-head">
          <div class="summary-icon amber"><el-icon><Warning /></el-icon></div>
          <span class="pill warn">需关注</span>
        </div>
        <p>即将到期（30天内）</p>
        <strong>{{ overview.expiringCount || 0 }}</strong>
      </article>
    </section>

    <section class="filter-panel">
      <div class="filter-grid">
        <label>
          <span>合作机构</span>
          <select v-model="filters.tenantId">
            <option value="">全部机构</option>
            <option v-for="tenant in tenantOptions" :key="tenant.id" :value="tenant.id">{{ tenant.name }}</option>
          </select>
        </label>

        <label>
          <span>内容类型</span>
          <select v-model="filters.bookId">
            <option value="">全部内容</option>
            <option v-for="book in bookOptions" :key="book.id" :value="String(book.id)">{{ book.name }}</option>
          </select>
        </label>

        <div>
          <span class="filter-label">授权状态</span>
          <div class="status-toggle">
            <button
              v-for="status in statusOptions"
              :key="status.value"
              type="button"
              :class="{ active: filters.status === status.value }"
              @click="filters.status = status.value"
            >
              {{ status.label }}
            </button>
          </div>
        </div>

        <div class="filter-actions">
          <button type="button" class="action-btn ghost" @click="resetFilters">重置筛选</button>
          <button type="button" class="action-btn primary" @click="applyFilters">应用筛选</button>
        </div>
      </div>
    </section>

    <section class="table-card" v-loading="loading">
      <div class="permission-table">
        <div class="permission-row table-head">
          <div>机构名称</div>
          <div>授权内容</div>
          <div>授权类型</div>
          <div>状态</div>
          <div>创建日期</div>
          <div class="align-right">操作</div>
        </div>

        <div v-for="record in records" :key="record.id" class="permission-row">
          <div class="tenant-cell">
            <div class="tenant-avatar">{{ record.tenantName?.slice(0, 2) || '机构' }}</div>
            <div>
              <strong>{{ record.tenantName }}</strong>
              <span>ID: {{ record.tenantId }}</span>
            </div>
          </div>

          <div><span class="book-tag">{{ record.bookName }}</span></div>
          <div>{{ record.authType || '年度订阅' }}</div>
          <div><span class="status-pill" :class="statusClass(record.status)">{{ statusText(record.status) }}</span></div>
          <div>{{ formatDate(record.createdAt) }}</div>
          <div class="row-actions">
            <button type="button" class="text-btn" @click="openCreateDialog(record)">调整</button>
            <button type="button" class="text-btn danger" @click="revoke(record)">撤销</button>
          </div>
        </div>
      </div>

      <div class="table-footer">
        <span>显示 {{ rangeLabel }} 条，共 {{ filteredTotal }} 条记录</span>
        <div class="footer-right">
          <button type="button" class="nav-btn" :disabled="page === 1 || loading" @click="page--">
            <el-icon><ArrowLeft /></el-icon>
          </button>
          <button type="button" class="nav-btn" :disabled="page >= totalPages || loading" @click="page++">
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
      </div>
    </section>

    <transition name="suggestion-float">
      <aside v-if="hasExpiringSuggestions && suggestionVisible" class="suggestion-card">
        <button type="button" class="suggestion-close" @click="suggestionVisible = false" aria-label="关闭智能建议">
          <el-icon><Close /></el-icon>
        </button>
        <div class="suggestion-badge"><el-icon><Opportunity /></el-icon></div>
        <div>
          <h3>智能建议</h3>
          <p>检测到 {{ overview.expiringCount || 0 }} 个机构授权即将到期，建议及时续约或重新分配教材包。</p>
        </div>
        <button type="button" class="action-btn primary suggestion-action" @click="focusExpiring">
          立即处理
        </button>
      </aside>
    </transition>

    <el-dialog v-model="dialogVisible" :title="editingRecord ? '调整授权' : '新增授权'" width="660px" @open="prepareDialog">
      <el-form :model="dialogForm" label-width="92px">
        <el-form-item label="合作机构" required>
          <el-select v-model="dialogForm.tenantId" placeholder="请选择机构" style="width: 100%">
            <el-option v-for="tenant in tenantOptions" :key="tenant.id" :label="tenant.name" :value="tenant.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="授权教材" required>
          <el-checkbox-group v-model="dialogForm.bookIds" class="book-checks">
            <el-checkbox v-for="book in bookOptions" :key="book.id" :label="book.id">{{ book.name }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveDialog">保存授权</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  getAuthorizationOverview,
  getBookOverview,
  getTenantOverview,
  getTenantBookIds,
  replaceTenantBookIds
} from '../../api/admin'
import request from '../../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  ArrowRight,
  Close,
  Histogram,
  OfficeBuilding,
  Opportunity,
  Plus,
  Select,
  Warning
} from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const page = ref(1)
const pageSize = 10
const overview = ref({ totalCount: 0, activeCount: 0, expiringCount: 0, growthRate: 0, filteredCount: 0, page: 1, pageSize, records: [] })
const tenantOptions = ref([])
const bookOptions = ref([])
const dialogVisible = ref(false)
const editingRecord = ref(null)
const dialogForm = ref({ tenantId: '', bookIds: [] })
const filters = ref({ tenantId: '', bookId: '', status: 'ACTIVE' })
const initialized = ref(false)
const suggestionVisible = ref(true)

const statusOptions = [
  { value: 'ACTIVE', label: '活跃中' },
  { value: 'EXPIRING', label: '即将到期' },
  { value: 'EXPIRED', label: '已到期' },
  { value: 'ALL', label: '全部' }
]

const records = computed(() => overview.value.records || [])
const filteredTotal = computed(() => Number(overview.value.filteredCount || 0))
const totalPages = computed(() => Math.max(1, Math.ceil(filteredTotal.value / pageSize)))
const hasExpiringSuggestions = computed(() => Number(overview.value.expiringCount || 0) > 0)
const rangeLabel = computed(() => {
  if (!filteredTotal.value || !records.value.length) return '0-0'
  const start = (page.value - 1) * pageSize + 1
  const end = start + records.value.length - 1
  return `${start}-${end}`
})

watch(totalPages, () => {
  if (page.value > totalPages.value) page.value = totalPages.value
})

function formatDate(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 10)
}

function formatTrend(value, suffix) {
  const amount = Number(value || 0)
  const prefix = amount > 0 ? '+' : ''
  return `${prefix}${amount.toFixed(1)}% ${suffix}`
}

function statusText(status) {
  if (status === 'ACTIVE') return '活跃中'
  if (status === 'EXPIRING') return '即将到期'
  if (status === 'EXPIRED') return '已过期'
  return '已停用'
}

function statusClass(status) {
  return {
    active: status === 'ACTIVE',
    expiring: status === 'EXPIRING',
    expired: status === 'EXPIRED',
    disabled: status === 'DISABLED'
  }
}

function resetFilters() {
  filters.value = { tenantId: '', bookId: '', status: 'ACTIVE' }
  page.value = 1
}

function applyFilters() {
  if (page.value !== 1) {
    page.value = 1
    return
  }
  fetchOverview()
}

async function fetchAll() {
  try {
    const [tenantOverview, bookOverview] = await Promise.all([getTenantOverview(), getBookOverview()])
    tenantOptions.value = tenantOverview?.records || []
    bookOptions.value = bookOverview?.books || []
    await fetchOverview()
  } catch (error) {
    ElMessage.error(error.message || '获取授权信息失败')
  }
}

async function fetchOverview() {
  loading.value = true
  try {
    const authOverview = await getAuthorizationOverview({
      tenantId: filters.value.tenantId || undefined,
      bookId: filters.value.bookId || undefined,
      status: filters.value.status || 'ALL',
      page: page.value,
      pageSize
    })
    overview.value = {
      totalCount: 0,
      activeCount: 0,
      expiringCount: 0,
      growthRate: 0,
      filteredCount: 0,
      page: page.value,
      pageSize,
      records: [],
      ...(authOverview || {})
    }
  } catch (error) {
    ElMessage.error(error.message || '获取授权信息失败')
  } finally {
    loading.value = false
  }
}

function openCreateDialog(record = null) {
  editingRecord.value = record
  dialogVisible.value = true
}

async function prepareDialog() {
  if (editingRecord.value?.tenantId) {
    dialogForm.value = {
      tenantId: editingRecord.value.tenantId,
      bookIds: await getTenantBookIds(editingRecord.value.tenantId)
    }
    return
  }
  dialogForm.value = { tenantId: '', bookIds: [] }
}

async function saveDialog() {
  if (!dialogForm.value.tenantId || !(dialogForm.value.bookIds || []).length) {
    ElMessage.warning('请选择机构和教材')
    return
  }
  saving.value = true
  try {
    await replaceTenantBookIds({
      tenantId: dialogForm.value.tenantId,
      bookIds: dialogForm.value.bookIds
    })
    ElMessage.success('授权已保存')
    dialogVisible.value = false
    await fetchAll()
  } catch (error) {
    ElMessage.error(error.message || '保存授权失败')
  } finally {
    saving.value = false
  }
}

async function revoke(record) {
  try {
    await ElMessageBox.confirm(`确认撤销「${record.tenantName} - ${record.bookName}」授权吗？`, '撤销授权', { type: 'warning' })
    await request.delete('/admin/tenant-book-auth', {
      params: { tenantId: record.tenantId, bookId: record.bookId }
    })
    ElMessage.success('授权已撤销')
    await fetchAll()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '撤销授权失败')
    }
  }
}

function exportReport() {
  const header = ['tenant_name', 'tenant_id', 'book_name', 'status', 'created_at', 'expire_time']
  const lines = records.value.map((record) =>
    [record.tenantName, record.tenantId, record.bookName, statusText(record.status), formatDate(record.createdAt), formatDate(record.expireTime)]
      .map((value) => `"${String(value || '').replace(/"/g, '""')}"`)
      .join(',')
  )
  const blob = new Blob([`\uFEFF${header.join(',')}\n${lines.join('\n')}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = 'authorization-report.csv'
  link.click()
  URL.revokeObjectURL(url)
}

function goCreateTenant() {
  router.push('/admin/tenants')
}

function focusExpiring() {
  filters.value.status = 'EXPIRING'
  if (page.value !== 1) {
    page.value = 1
    return
  }
  fetchOverview()
}

watch(page, () => {
  if (initialized.value) {
    fetchOverview()
  }
})

watch(filters, () => {
  if (!initialized.value) return
  if (page.value !== 1) {
    page.value = 1
    return
  }
  fetchOverview()
}, { deep: true })

onMounted(async () => {
  await fetchAll()
  initialized.value = true
})
</script>

<style scoped>
.permission-page {
  position: relative;
  display: grid;
  gap: 12px;
  padding-bottom: 72px;
}

.page-head,
.head-actions,
.summary-head,
.table-footer,
.footer-right,
.filter-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.page-head h2 {
  margin: 0;
  font-family: 'Manrope', 'PingFang SC', sans-serif;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.04em;
  color: #141821;
}

.page-head p {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.action-btn {
  min-height: 36px;
  padding: 0 13px;
  border: none;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.action-btn.ghost {
  background: #eceff3;
  color: #111827;
}

.action-btn.primary {
  background: #0b5fd1;
  color: #fff;
}

.action-btn.primary.subtle {
  background: #1f7ae6;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.summary-card,
.filter-panel,
.table-card,
.suggestion-card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(193, 198, 214, 0.32);
  border-radius: 16px;
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.045);
}

.summary-card {
  min-height: 100px;
  padding: 14px;
}

.summary-card.attention {
  border-left: 4px solid #c45a1d;
}

.summary-head span,
.pill {
  font-size: 12px;
  font-weight: 800;
}

.summary-card p,
.summary-card strong {
  margin: 0;
}

.summary-card p {
  margin-top: 10px;
  color: #6b7280;
  font-size: 12px;
}

.summary-card strong {
  display: block;
  margin-top: 4px;
  font-family: 'Manrope', 'PingFang SC', sans-serif;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.summary-icon {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: grid;
  place-items: center;
}

.summary-icon.blue {
  color: #0b5fd1;
  background: rgba(11, 95, 209, 0.08);
}

.summary-icon.indigo {
  color: #5762db;
  background: rgba(87, 98, 219, 0.1);
}

.summary-icon.amber {
  color: #c45a1d;
  background: rgba(196, 90, 29, 0.1);
}

.pill {
  padding: 5px 8px;
  border-radius: 999px;
  background: #ebeaff;
  color: #4f46e5;
}

.pill.warn {
  background: #ffedd5;
  color: #b45309;
}

.filter-panel {
  padding: 14px;
  background: #f2f4f6;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  align-items: end;
}

.filter-grid label,
.filter-label {
  display: block;
  color: #6b7280;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.filter-grid select {
  margin-top: 8px;
  width: 100%;
  height: 36px;
  border: none;
  border-radius: 10px;
  padding: 0 12px;
  background: #fff;
  font-weight: 700;
  font-size: 12px;
}

.status-toggle {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}

.status-toggle button {
  flex: 1;
  height: 36px;
  border: none;
  border-radius: 10px;
  background: #e2e8f0;
  color: #4b5563;
  font-weight: 800;
  font-size: 12px;
  cursor: pointer;
}

.status-toggle button.active {
  background: #0b5fd1;
  color: #fff;
}

.permission-table {
  display: grid;
}

.permission-row {
  display: grid;
  grid-template-columns: minmax(240px, 2fr) minmax(160px, 1.2fr) 120px 120px 120px 140px;
  gap: 14px;
  align-items: center;
  padding: 11px 14px;
  border-top: 1px solid rgba(241, 245, 249, 0.95);
}

.table-head {
  background: rgba(248, 250, 252, 0.8);
  color: #6b7280;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.tenant-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tenant-avatar {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  display: grid;
  place-items: center;
  background: #eef4ff;
  color: #2d5ac8;
  font-weight: 800;
  font-size: 12px;
}

.tenant-cell strong,
.tenant-cell span {
  display: block;
}

.tenant-cell span {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
}

.book-tag,
.status-pill {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 800;
}

.book-tag {
  color: #4f46e5;
  background: #ebeaff;
}

.status-pill.active {
  color: #16a34a;
  background: #dcfce7;
}

.status-pill.expiring {
  color: #b45309;
  background: #ffedd5;
}

.status-pill.expired,
.status-pill.disabled {
  color: #dc2626;
  background: #fee2e2;
}

.row-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.text-btn,
.nav-btn {
  border: none;
  background: transparent;
  font-weight: 700;
  cursor: pointer;
}

.text-btn {
  color: #0b5fd1;
}

.text-btn.danger {
  color: #dc2626;
}

.table-footer {
  padding: 10px 14px;
  border-top: 1px solid rgba(241, 245, 249, 0.95);
  color: #6b7280;
  font-size: 12px;
}

.nav-btn {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: #f3f4f6;
}

.suggestion-card {
  position: fixed;
  right: 18px;
  bottom: 18px;
  z-index: 30;
  width: 260px;
  padding: 14px;
  display: grid;
  gap: 10px;
  border-radius: 18px 18px 8px 8px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.14);
}

.suggestion-badge {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  color: #b45309;
  background: #ffedd5;
}

.suggestion-close {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 26px;
  height: 26px;
  border: none;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: #f3f4f6;
  color: #6b7280;
  cursor: pointer;
}

.suggestion-card h3,
.suggestion-card p {
  margin: 0;
}

.suggestion-card h3 {
  font-size: 14px;
}

.suggestion-card p {
  color: #6b7280;
  line-height: 1.6;
  font-size: 12px;
}

.suggestion-action {
  width: 100%;
  justify-content: center;
}

.suggestion-float-enter-active,
.suggestion-float-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.suggestion-float-enter-from,
.suggestion-float-leave-to {
  opacity: 0;
  transform: translate3d(0, 18px, 0);
}

.book-checks {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

@media (max-width: 1200px) {
  .filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .suggestion-card {
    right: 14px;
    bottom: 14px;
  }
}

@media (max-width: 900px) {
  .stats-grid,
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .permission-row {
    grid-template-columns: 1fr;
  }

  .table-head {
    display: none;
  }
}

@media (max-width: 640px) {
  .page-head,
  .head-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .book-checks {
    grid-template-columns: 1fr;
  }

  .suggestion-card {
    right: 12px;
    left: 12px;
    width: auto;
    bottom: 12px;
  }
}
</style>
