<template>
  <div class="student-page">
    <section class="page-header">
      <div>
        <h1>学生管理中心</h1>
        <p>查看、添加和管理您机构中的所有注册学生信息。</p>
      </div>

      <div class="header-actions">
        <el-button class="light-btn" @click="exportCsv">
          <el-icon><Download /></el-icon>
          <span>导出数据</span>
        </el-button>
        <el-button class="primary-btn" :disabled="isQuotaFull" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          <span>新增学生</span>
        </el-button>
      </div>
    </section>

    <section class="summary-row">
      <article class="summary-card">
        <span>总学生人数</span>
        <strong>{{ mergedStudents.length }}</strong>
        <em>{{ formatDelta(stats.studentGrowthRate) }}</em>
      </article>

      <article class="summary-card">
        <span>平均学习进度</span>
        <strong>{{ averageProgress }}%</strong>
        <em>{{ averageProgressLabel }}</em>
      </article>

      <article class="quota-card">
        <div>
          <span>机构承载量</span>
          <strong>已使用 {{ usedQuota }} / {{ totalQuota || 0 }} 席位</strong>
        </div>
        <div class="quota-side">
          <span>{{ quotaPercent }}%</span>
        </div>
        <div class="quota-track">
          <span class="quota-fill" :style="{ width: `${quotaPercent}%` }"></span>
        </div>
      </article>
    </section>

    <section class="table-card">
      <div class="table-tabs">
        <button type="button" class="tab" :class="{ active: statusFilter === 'ALL' }" @click="statusFilter = 'ALL'">全部学生</button>
        <button type="button" class="tab" :class="{ active: statusFilter === 'ACTIVE' }" @click="statusFilter = 'ACTIVE'">活跃中</button>
        <button type="button" class="tab" :class="{ active: statusFilter === 'DISABLED' }" @click="statusFilter = 'DISABLED'">已停用</button>
        <button type="button" class="filter-btn">
          <el-icon><Filter /></el-icon>
        </button>
      </div>

      <el-table :data="pagedStudents" v-loading="loading" empty-text="暂无学生数据" class="student-table">
        <el-table-column label="学生姓名" min-width="220">
          <template #default="{ row }">
            <div class="student-cell">
              <div class="student-avatar">{{ initials(row.realName || row.username) }}</div>
              <div>
                <strong>{{ row.realName || row.username }}</strong>
                <span>{{ row.username }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="studentNo" label="学号（ID）" min-width="130">
          <template #default="{ row }">{{ row.studentNo || row.username }}</template>
        </el-table-column>

        <el-table-column label="学习进度" min-width="190">
          <template #default="{ row }">
            <div class="progress-cell">
              <span class="mini-track">
                <span class="mini-fill" :style="{ width: `${row.progressPercent}%` }"></span>
              </span>
              <strong>{{ row.progressPercent }}%</strong>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="lastStudyAt" label="最后在线" min-width="120">
          <template #default="{ row }">{{ row.lastStudyAt || '从未学习' }}</template>
        </el-table-column>

        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <span class="status-pill" :class="row.status === 1 ? 'active' : 'idle'">
              {{ row.status === 1 ? '活跃中' : '已停用' }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="90" align="center">
          <template #default="{ row }">
            <button type="button" class="more-btn" @click="openEditDialog(row)">⋮</button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <span>显示 {{ rangeLabel }} 共 {{ filteredStudents.length }} 名学生</span>
        <div class="pager">
          <button type="button" :disabled="page === 1" @click="page--">‹</button>
          <button
            v-for="item in visiblePages"
            :key="item"
            type="button"
            :class="{ current: item === page }"
            :disabled="item === '...'"
            @click="typeof item === 'number' && (page = item)"
          >
            {{ item }}
          </button>
          <button type="button" :disabled="page >= totalPages" @click="page++">›</button>
        </div>
      </div>
    </section>

    <section class="upgrade-card">
      <div>
        <h3>需要更多名额？</h3>
        <p>您的机构名额使用率已达 {{ quotaPercent }}%。升级到专业版可解锁无限学生名额、高级数据分析及机构专属品牌定制服务。</p>
      </div>
      <div class="upgrade-actions">
        <button type="button" class="muted-btn">当前已达到最大承载</button>
        <button type="button" class="dark-btn">立即升级</button>
      </div>
    </section>

    <el-dialog v-model="detailVisible" :title="editMode ? '学生详情' : '新增学生'" width="460px" @close="resetCreateForm">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="登录账号" required><el-input v-model="createForm.username" placeholder="学生登录用账号" /></el-form-item>
        <el-form-item label="密码" :required="!editMode"><el-input v-model="createForm.password" type="password" :placeholder="editMode ? '留空则不修改' : '登录密码'" show-password /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="createForm.realName" placeholder="真实姓名" /></el-form-item>
        <el-form-item label="学号"><el-input v-model="createForm.studentNo" placeholder="学号/编号" /></el-form-item>
        <el-form-item label="班级"><el-input v-model="createForm.gradeClass" placeholder="如 七年级" /></el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <div v-if="editMode" class="dialog-extra-actions">
            <el-button @click="resetPassword(currentStudent)">重置密码</el-button>
            <el-button :type="currentStudent?.status === 1 ? 'warning' : 'success'" @click="toggleStatus(currentStudent)">
              {{ currentStudent?.status === 1 ? '停用账号' : '启用账号' }}
            </el-button>
          </div>
          <div class="dialog-main-actions">
            <el-button @click="detailVisible = false">取消</el-button>
            <el-button type="primary" @click="submitCreate" :loading="createLoading">{{ editMode ? '保存' : '创建' }}</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import request from '../../api/request'
import { getOrgReport, resetOrgStudentPassword, updateOrgStudent } from '../../api/org'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Filter, Plus } from '@element-plus/icons-vue'

const students = ref([])
const reportRows = ref([])
const loading = ref(false)
const stats = ref({ studentGrowthRate: 0 })
const usedQuota = ref(0)
const totalQuota = ref(0)
const keyword = ref('')
const statusFilter = ref('ALL')
const page = ref(1)
const pageSize = 10
const detailVisible = ref(false)
const createLoading = ref(false)
const editMode = ref(false)
const editingId = ref(null)
const currentStudent = ref(null)
const createForm = ref({ username: '', password: '', realName: '', studentNo: '', gradeClass: '' })

const quotaPercent = computed(() => {
  if (!totalQuota.value) return 0
  return Math.min(100, Math.round((usedQuota.value / totalQuota.value) * 100))
})
const isQuotaFull = computed(() => totalQuota.value > 0 && usedQuota.value >= totalQuota.value)

const mergedStudents = computed(() => {
  const reportMap = new Map(reportRows.value.map(item => [item.studentId, item]))
  return students.value.map((student) => {
    const report = reportMap.get(student.id) || {}
    const progressPercent = Math.round((
      Number(report.flashcardRate || 0) +
      Number(report.engChRate || 0) +
      Number(report.chEngRate || 0) +
      Number(report.spellRate || 0)
    ) / 4)
    const lastStudyAt = report.lastStudyAt || formatTime(student.lastActiveAt)
    return {
      ...student,
      progressPercent,
      lastStudyAt
    }
  })
})

const filteredStudents = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  const byStatus = mergedStudents.value.filter(item => {
    if (statusFilter.value === 'ACTIVE') return Number(item.status) === 1
    if (statusFilter.value === 'DISABLED') return Number(item.status) !== 1
    return true
  })
  if (!query) return byStatus
  return byStatus.filter(item =>
    [item.username, item.realName, item.studentNo].some(value => String(value || '').toLowerCase().includes(query))
  )
})

const averageProgress = computed(() => {
  if (!mergedStudents.value.length) return 0
  return Math.round(mergedStudents.value.reduce((sum, item) => sum + Number(item.progressPercent || 0), 0) / mergedStudents.value.length)
})
const averageProgressLabel = computed(() => {
  if (averageProgress.value >= 85) return '优秀'
  if (averageProgress.value >= 70) return '良好'
  if (averageProgress.value >= 50) return '稳步提升'
  return '待加强'
})
const totalPages = computed(() => Math.max(1, Math.ceil(filteredStudents.value.length / pageSize)))
const pagedStudents = computed(() => filteredStudents.value.slice((page.value - 1) * pageSize, page.value * pageSize))
const rangeLabel = computed(() => {
  if (!filteredStudents.value.length) return '0-0'
  const start = (page.value - 1) * pageSize + 1
  const end = Math.min(page.value * pageSize, filteredStudents.value.length)
  return `${start}-${end}`
})
const visiblePages = computed(() => {
  if (totalPages.value <= 5) return Array.from({ length: totalPages.value }, (_, index) => index + 1)
  if (page.value <= 3) return [1, 2, 3, '...', totalPages.value]
  if (page.value >= totalPages.value - 2) return [1, '...', totalPages.value - 2, totalPages.value - 1, totalPages.value]
  return [1, '...', page.value, '...', totalPages.value]
})

watch([statusFilter, totalPages], () => {
  if (page.value > totalPages.value) {
    page.value = totalPages.value
    return
  }
  if (page.value !== 1) {
    page.value = 1
  }
})

function initials(value) {
  return String(value || '学').trim().slice(0, 2)
}

function formatTime(value) {
  if (!value) return ''
  return typeof value === 'string' ? value.replace('T', ' ').slice(0, 16) : value
}

function formatDelta(value) {
  const amount = Number(value || 0)
  const prefix = amount > 0 ? '+' : ''
  return `${prefix}${amount.toFixed(1)}%`
}

function openCreateDialog() {
  if (isQuotaFull.value) return
  editMode.value = false
  editingId.value = null
  currentStudent.value = null
  createForm.value = { username: '', password: '', realName: '', studentNo: '', gradeClass: '' }
  detailVisible.value = true
}

function openEditDialog(row) {
  editMode.value = true
  editingId.value = row.id
  currentStudent.value = row
  createForm.value = {
    username: row.username || '',
    password: '',
    realName: row.realName || '',
    studentNo: row.studentNo || '',
    gradeClass: row.gradeClass || ''
  }
  detailVisible.value = true
}

function resetCreateForm() {
  createForm.value = { username: '', password: '', realName: '', studentNo: '', gradeClass: '' }
}

async function submitCreate() {
  if (!createForm.value.username?.trim()) return ElMessage.warning('请填写登录账号')
  if (!editMode.value && !createForm.value.password) return ElMessage.warning('请填写密码')
  createLoading.value = true
  try {
    const payload = {
      username: createForm.value.username.trim(),
      password: createForm.value.password,
      realName: createForm.value.realName?.trim() || undefined,
      studentNo: createForm.value.studentNo?.trim() || undefined,
      gradeClass: createForm.value.gradeClass?.trim() || undefined,
      role: 'STUDENT'
    }
    if (editMode.value) {
      await updateOrgStudent(editingId.value, payload)
      ElMessage.success('学生账号已更新')
    } else {
      await request.post('/tenant/users', payload)
      ElMessage.success('学生账号创建成功')
    }
    detailVisible.value = false
    await fetchStudents()
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    createLoading.value = false
  }
}

async function toggleStatus(row) {
  if (!row) return
  const nextStatus = row.status === 1 ? 0 : 1
  await updateOrgStudent(row.id, {
    username: row.username,
    password: '',
    realName: row.realName || '',
    studentNo: row.studentNo || '',
    gradeClass: row.gradeClass || '',
    role: 'STUDENT',
    status: nextStatus
  })
  ElMessage.success(nextStatus === 1 ? '学生已启用' : '学生已停用')
  await fetchStudents()
}

async function resetPassword(row) {
  if (!row) return
  const { value } = await ElMessageBox.prompt(`为 ${row.realName || row.username} 输入新密码`, '重置密码', {
    inputType: 'password',
    inputPlaceholder: '至少 6 位'
  })
  await resetOrgStudentPassword(row.id, value)
  ElMessage.success('密码已重置')
}

function exportCsv() {
  if (filteredStudents.value.length === 0) return
  const header = ['学号', '姓名', '年级', '学习进度', '最近活跃', '状态']
  const rows = filteredStudents.value.map(item => [
    item.studentNo || item.username,
    item.realName || item.username,
    item.gradeClass || '',
    `${item.progressPercent}%`,
    item.lastStudyAt || '',
    item.status === 1 ? '正常' : '停用'
  ])
  const csv = [header, ...rows].map(line =>
    line.map(value => `"${String(value ?? '').replace(/"/g, '""')}"`).join(',')
  ).join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `students-${Date.now()}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
}

async function fetchStudents() {
  loading.value = true
  try {
    const [quotaRes, usersRes, reportRes, statsRes] = await Promise.all([
      request.get('/tenant/quota').catch(() => null),
      request.get('/tenant/users', { params: { role: 'STUDENT' } }).catch(() => []),
      getOrgReport().catch(() => []),
      request.get('/tenant/stats').catch(() => null)
    ])
    if (quotaRes) {
      usedQuota.value = quotaRes.usedCount ?? quotaRes.used ?? 0
      totalQuota.value = quotaRes.totalQuota ?? quotaRes.totalCount ?? quotaRes.total ?? 0
    }
    students.value = Array.isArray(usersRes) ? usersRes : []
    reportRows.value = Array.isArray(reportRes) ? reportRes : []
    stats.value = statsRes || stats.value
    if (page.value > totalPages.value) page.value = totalPages.value
  } catch (error) {
    ElMessage.error(error.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchStudents)
</script>

<style scoped>
.student-page {
  display: grid;
  gap: 18px;
}

.page-header,
.summary-row,
.header-actions,
.table-tabs,
.progress-cell,
.table-footer,
.pager,
.upgrade-card,
.upgrade-actions,
.dialog-footer,
.dialog-extra-actions,
.dialog-main-actions,
.student-cell {
  display: flex;
  align-items: center;
}

.page-header,
.table-footer,
.upgrade-card,
.dialog-footer {
  justify-content: space-between;
}

.page-header h1,
.upgrade-card h3 {
  margin: 0;
  color: #1d2330;
}

.page-header h1 {
  font-size: 1.55rem;
  font-weight: 800;
}

.page-header p,
.upgrade-card p {
  margin: 8px 0 0;
  color: #77869b;
}

.header-actions,
.summary-row,
.table-tabs,
.upgrade-actions,
.dialog-extra-actions,
.dialog-main-actions,
.pager {
  gap: 12px;
}

.light-btn,
.primary-btn,
.filter-btn,
.muted-btn,
.dark-btn,
.more-btn {
  border: none;
  cursor: pointer;
}

.light-btn {
  background: #f3f6fb;
  color: #3b4658;
}

.primary-btn {
  background: linear-gradient(135deg, #1e67dd 0%, #2c7bf2 100%);
  color: #ffffff;
}

.light-btn,
.primary-btn {
  height: 36px;
  padding: 0 14px;
  border-radius: 12px;
  font-weight: 700;
}

.summary-row {
  align-items: stretch;
}

.summary-card,
.table-card,
.upgrade-card {
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 10px 22px rgba(24, 53, 92, 0.07);
}

.summary-card {
  min-width: 170px;
  padding: 12px 14px;
}

.summary-card span,
.quota-card span {
  display: block;
  color: #7d8ca3;
  font-size: 0.78rem;
}

.summary-card strong {
  display: block;
  margin-top: 8px;
  color: #1b63d8;
  font-size: 1.55rem;
  font-weight: 800;
}

.summary-card em {
  margin-left: 6px;
  color: #5d7df0;
  font-style: normal;
  font-weight: 700;
  font-size: 0.78rem;
}

.quota-card {
  flex: 1;
  min-height: 92px;
  padding: 12px 14px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: start;
  color: #ffffff;
  background: linear-gradient(135deg, #0d64d8 0%, #0d4fce 100%);
}

.quota-card strong {
  display: block;
  margin-top: 8px;
  font-size: 1.2rem;
  font-weight: 800;
}

.quota-side {
  font-size: 1.3rem;
  font-weight: 800;
}

.quota-track {
  grid-column: 1 / -1;
  height: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.24);
  overflow: hidden;
}

.quota-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #ffffff;
}

.table-card {
  padding: 12px 0 6px;
}

.table-tabs {
  padding: 0 16px 10px;
  border-bottom: 1px solid #eef2f8;
}

.tab {
  border: none;
  background: transparent;
  color: #8a97ab;
  font-size: 0.92rem;
  font-weight: 700;
  cursor: pointer;
}

.tab.active {
  color: #1d63d8;
}

.filter-btn {
  margin-left: auto;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: #f3f6fb;
  color: #66778f;
}

:deep(.student-table .el-table__inner-wrapper::before) {
  display: none;
}

:deep(.student-table th.el-table__cell) {
  background: #f7f9fc;
  color: #7c8ca3;
  font-weight: 700;
}

:deep(.student-table td.el-table__cell),
:deep(.student-table th.el-table__cell) {
  padding: 12px 0;
}

.student-cell {
  gap: 12px;
}

.student-avatar {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #dfe9ff 0%, #f5e9ff 100%);
  color: #5d70d7;
  font-size: 0.85rem;
  font-weight: 800;
}

.student-cell strong,
.student-cell span {
  display: block;
}

.student-cell strong {
  color: #283142;
  font-size: 0.94rem;
}

.student-cell span {
  margin-top: 6px;
  color: #99a4b5;
  font-size: 0.8rem;
}

.progress-cell {
  justify-content: space-between;
  gap: 10px;
}

.mini-track {
  flex: 1;
  height: 6px;
  border-radius: 999px;
  background: #ebf0f7;
  overflow: hidden;
}

.mini-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #1d67dd;
}

.progress-cell strong {
  color: #374153;
  font-size: 0.86rem;
}

.status-pill {
  min-width: 70px;
  min-height: 30px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 700;
}

.status-pill.active {
  background: #e7eaff;
  color: #6673ea;
}

.status-pill.idle {
  background: #eef1f5;
  color: #7f8da2;
}

.more-btn {
  width: 30px;
  height: 30px;
  border-radius: 999px;
  background: transparent;
  color: #8e9ab0;
  font-size: 1.2rem;
}

.table-footer {
  padding: 10px 16px 6px;
  color: #8c99ad;
  font-size: 0.84rem;
}

.pager button {
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: #7f8ea4;
  cursor: pointer;
}

.pager .current {
  background: #1c67dc;
  color: #ffffff;
}

.upgrade-card {
  padding: 16px 18px;
  background: #f2f4f7;
}

.muted-btn {
  height: 38px;
  padding: 0 14px;
  border-radius: 12px;
  background: #e7ebf2;
  color: #9ca7b7;
  font-weight: 700;
}

.dark-btn {
  height: 38px;
  padding: 0 16px;
  border-radius: 12px;
  background: #171b22;
  color: #ffffff;
  font-weight: 700;
}

@media (max-width: 1080px) {
  .page-header,
  .summary-row,
  .upgrade-card,
  .dialog-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .quota-card {
    grid-template-columns: 1fr;
  }
}
</style>
