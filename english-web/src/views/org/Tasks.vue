<template>
  <div class="task-page">
    <section class="page-header">
      <div>
        <h1>任务管理</h1>
        <p>分配学习单元并跟踪机构内全体学生的实时进度。</p>
      </div>
      <el-button class="primary-btn" @click="dialogVisible = true">
        <el-icon><Plus /></el-icon>
        <span>分配新任务</span>
      </el-button>
    </section>

    <section class="summary-grid">
      <div class="summary-cards">
        <article class="summary-card">
          <div class="summary-icon blue"><el-icon><FolderOpened /></el-icon></div>
          <span>正在进行中</span>
          <strong>{{ activeTaskCount }}</strong>
          <em>任务</em>
        </article>
        <article class="summary-card">
          <div class="summary-icon orange"><el-icon><Warning /></el-icon></div>
          <span>即将截止</span>
          <strong>{{ urgentTaskCount }}</strong>
          <em>任务</em>
        </article>
        <article class="summary-card">
          <div class="summary-icon violet"><el-icon><CircleCheck /></el-icon></div>
          <span>本月已完成</span>
          <strong>{{ completedTaskCount }}</strong>
          <em>个单元</em>
        </article>
        <article class="summary-card">
          <div class="summary-icon indigo"><el-icon><DataAnalysis /></el-icon></div>
          <span>平均进度</span>
          <strong>{{ activeProgress }}%</strong>
          <em>整体任务</em>
        </article>
      </div>
      <article class="quick-card">
        <h3>快速指派</h3>
        <p>为特定班级学生快速分配本周学习目标。</p>
        <el-select v-model="quickStudentIds" multiple collapse-tags placeholder="选择学生 / 班级">
          <el-option v-for="student in students" :key="student.id" :label="studentLabel(student)" :value="student.id" />
        </el-select>
        <el-select v-model="selectedBookId" filterable placeholder="学习单元" @change="loadUnits">
          <el-option v-for="book in books" :key="book.id" :label="book.name" :value="book.id" />
        </el-select>
        <el-select v-model="quickUnitId" filterable placeholder="选择单元">
          <el-option v-for="unit in units" :key="unit.id" :label="unit.name" :value="unit.id" />
        </el-select>
        <button type="button" class="quick-submit" @click="submitQuickTask">确认分配</button>
      </article>
    </section>

    <section class="task-content-grid">
      <article class="overview-card">
        <h2>实时进度概览</h2>
        <div class="overview-row">
          <span>托福核心词汇精讲</span>
          <strong>{{ activeProgress }}%</strong>
        </div>
        <div class="overview-track"><span :style="{ width: `${activeProgress}%` }"></span></div>
        <div class="overview-row second">
          <span>雅思听力进阶练习</span>
          <strong>{{ completionProgress }}%</strong>
        </div>
        <div class="overview-track light"><span :style="{ width: `${completionProgress}%` }"></span></div>
      </article>

      <article class="list-card">
        <div class="list-head">
          <div>
            <h2>详细任务清单</h2>
          </div>
          <div class="list-actions">
            <button type="button" class="light-btn">筛选器</button>
            <button type="button" class="light-btn">导出 CSV</button>
          </div>
        </div>

        <div v-if="tasks.length === 0 && !loading" class="empty-card">当前还没有分配中的任务。</div>
        <div v-else class="task-list" v-loading="loading">
          <article v-for="task in tasks" :key="task.id" class="task-row">
            <div class="student-meta">
              <div class="student-avatar">{{ initials(task.studentName) }}</div>
              <div>
                <strong>{{ task.studentName }}</strong>
                <span>{{ task.bookName || '学习任务' }}</span>
              </div>
            </div>
            <div class="unit-meta">
              <strong>{{ task.unitName }}</strong>
              <span>词汇量要求：{{ task.totalModes || 4 }} 模式</span>
            </div>
            <div class="status-wrap">
              <span class="task-status" :class="statusClass(task.status)">{{ task.status }}</span>
            </div>
            <div class="deadline-wrap">
              <strong>{{ formatTime(task.assignedAt) }}</strong>
              <span>分配时间</span>
            </div>
            <div class="action-wrap">
              <button type="button" class="remove-btn" @click="removeTask(task)">撤销任务</button>
            </div>
          </article>
        </div>
      </article>

      <article class="insight-card">
        <div class="insight-badge">智能建议</div>
        <h3>发现 {{ urgentTaskCount }} 名学生在周期内尚未完成建议进度。</h3>
        <p>建议发起一次专题讲座或同步在线辅导。</p>
        <button type="button">查看名单并推送提醒</button>
      </article>
    </section>

    <el-dialog v-model="dialogVisible" title="创建任务" width="620px">
      <div class="form-grid">
        <el-select v-model="form.studentIds" multiple filterable placeholder="选择学生">
          <el-option v-for="student in students" :key="student.id" :label="studentLabel(student)" :value="student.id" />
        </el-select>
        <el-select v-model="selectedBookId" filterable placeholder="选择课本" @change="loadUnits">
          <el-option v-for="book in books" :key="book.id" :label="book.name" :value="book.id" />
        </el-select>
        <el-select v-model="form.unitIds" multiple filterable placeholder="选择单元">
          <el-option v-for="unit in units" :key="unit.id" :label="unit.name" :value="unit.id" />
        </el-select>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitTasks">创建任务</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '../../api/request'
import { createOrgTasks, deleteOrgTask, getOrgTasks } from '../../api/org'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, DataAnalysis, FolderOpened, Plus, Warning } from '@element-plus/icons-vue'

const loading = ref(false)
const saving = ref(false)
const students = ref([])
const books = ref([])
const units = ref([])
const tasks = ref([])
const selectedBookId = ref(null)
const dialogVisible = ref(false)
const quickStudentIds = ref([])
const quickUnitId = ref(null)
const form = ref({
  studentIds: [],
  unitIds: []
})

const activeTaskCount = computed(() => tasks.value.filter(item => item.status !== '已完成').length)
const completedTaskCount = computed(() => tasks.value.filter(item => item.status === '已完成').length)
const urgentTaskCount = computed(() => Math.max(0, activeTaskCount.value - completedTaskCount.value))
const activeProgress = computed(() => {
  if (!tasks.value.length) return 76
  const total = tasks.value.reduce((sum, item) => sum + progressPercent(item), 0)
  return Math.round(total / tasks.value.length)
})
const completionProgress = computed(() => Math.max(18, Math.round(completedTaskCount.value / Math.max(tasks.value.length, 1) * 100)))

function initials(value) {
  return String(value || '学').trim().slice(0, 2)
}

function studentLabel(student) {
  return student.realName ? `${student.realName}（${student.username}）` : student.username
}

function formatTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 10)
}

function progressPercent(task) {
  const total = Number(task.totalModes || 4)
  const completed = Number(task.completedModes || 0)
  if (!total) return 0
  return Math.min(100, Math.round((completed / total) * 100))
}

function statusClass(status) {
  if (status === '已完成') return 'status-done'
  if (status === '进行中') return 'status-active'
  return 'status-idle'
}

async function loadStudents() {
  const res = await request.get('/tenant/users', { params: { role: 'STUDENT' } })
  students.value = Array.isArray(res) ? res : []
}

async function loadBooks() {
  const res = await request.get('/books')
  books.value = Array.isArray(res) ? res : []
}

async function loadUnits(bookId = selectedBookId.value) {
  if (!bookId) {
    units.value = []
    return
  }
  const res = await request.get('/units', { params: { bookId } })
  units.value = Array.isArray(res) ? res : []
}

async function loadTasks() {
  loading.value = true
  try {
    const res = await getOrgTasks()
    tasks.value = Array.isArray(res) ? res : []
  } catch (error) {
    ElMessage.error(error.message || '获取任务失败')
  } finally {
    loading.value = false
  }
}

async function submitTasks() {
  if (form.value.studentIds.length === 0) return ElMessage.warning('请至少选择一个学生')
  if (form.value.unitIds.length === 0) return ElMessage.warning('请至少选择一个单元')
  saving.value = true
  try {
    await createOrgTasks(form.value)
    ElMessage.success('任务分配成功')
    dialogVisible.value = false
    form.value = { studentIds: [], unitIds: [] }
    await loadTasks()
  } catch (error) {
    ElMessage.error(error.message || '任务分配失败')
  } finally {
    saving.value = false
  }
}

async function submitQuickTask() {
  if (!quickStudentIds.value.length || !quickUnitId.value) {
    return ElMessage.warning('请先选择学生和学习单元')
  }
  saving.value = true
  try {
    await createOrgTasks({
      studentIds: quickStudentIds.value,
      unitIds: [quickUnitId.value]
    })
    ElMessage.success('快速分配成功')
    quickStudentIds.value = []
    quickUnitId.value = null
    await loadTasks()
  } catch (error) {
    ElMessage.error(error.message || '快速分配失败')
  } finally {
    saving.value = false
  }
}

async function removeTask(row) {
  await ElMessageBox.confirm(`确定撤销 ${row.studentName} 的任务「${row.unitName}」吗？`, '撤销任务', {
    type: 'warning'
  })
  await deleteOrgTask(row.id)
  ElMessage.success('已撤销')
  await loadTasks()
}

onMounted(async () => {
  await Promise.all([loadStudents(), loadBooks(), loadTasks()])
})
</script>

<style scoped>
.task-page {
  display: grid;
  gap: 18px;
}

.page-header,
.summary-grid,
.list-head,
.list-actions,
.task-row,
.student-meta,
.upgrade-actions {
  display: flex;
  align-items: center;
}

.page-header,
.list-head {
  justify-content: space-between;
}

.page-header h1,
.overview-card h2,
.list-head h2,
.quick-card h3,
.insight-card h3 {
  margin: 0;
  color: #1b2230;
}

.page-header h1 {
  font-size: 1.55rem;
  font-weight: 800;
}

.page-header p,
.quick-card p,
.insight-card p {
  margin: 8px 0 0;
  color: #76859a;
}

.primary-btn,
.quick-submit,
.light-btn,
.remove-btn,
.insight-card button {
  border: none;
  cursor: pointer;
}

.primary-btn {
  height: 36px;
  padding: 0 14px;
  border-radius: 12px;
  background: linear-gradient(135deg, #1c67dc 0%, #2d80f5 100%);
  color: #ffffff;
  font-weight: 700;
}

.summary-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(340px, 1.15fr);
  align-items: stretch;
  gap: 16px;
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.summary-card,
.quick-card,
.overview-card,
.list-card,
.insight-card {
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 10px 22px rgba(24, 53, 92, 0.07);
}

.summary-card {
  padding: 12px;
  min-height: 116px;
}

.summary-icon {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: grid;
  place-items: center;
}

.summary-icon.blue {
  background: #edf4ff;
  color: #2a67dc;
}

.summary-icon.orange {
  background: #ffe8da;
  color: #d3773a;
}

.summary-icon.violet {
  background: #efecff;
  color: #6f72ea;
}

.summary-icon.indigo {
  background: #e7efff;
  color: #2b67dc;
}

.summary-card span,
.summary-card em {
  display: block;
  color: #7e8ca1;
}

.summary-card span {
  margin-top: 18px;
}

.summary-card strong {
  display: block;
  margin-top: 8px;
  color: #1f2634;
  font-size: 1.5rem;
  font-weight: 800;
}

.summary-card em {
  margin-top: 6px;
  font-style: normal;
}

.quick-card {
  flex: 1;
  padding: 14px;
  display: grid;
  gap: 14px;
  align-self: stretch;
}

.quick-submit {
  height: 38px;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: inset 0 0 0 2px #2a6ddd;
  color: #1f66db;
  font-weight: 700;
}

.overview-card,
.list-card,
.insight-card {
  padding: 14px;
}

.task-content-grid {
  display: grid;
  grid-template-columns: minmax(260px, 0.9fr) minmax(0, 1.7fr) minmax(280px, 0.9fr);
  gap: 12px;
  align-items: stretch;
}

.overview-row {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #5b6980;
  font-size: 0.84rem;
}

.overview-row.second {
  margin-top: 12px;
}

.overview-track {
  height: 7px;
  margin-top: 8px;
  border-radius: 999px;
  background: #ebeff6;
  overflow: hidden;
}

.overview-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #1d67dd;
}

.overview-track.light span {
  background: #8c98ff;
}

.list-actions {
  gap: 10px;
}

.light-btn {
  height: 32px;
  padding: 0 12px;
  border-radius: 10px;
  background: #f3f6fb;
  color: #65758c;
  font-weight: 700;
}

.task-list {
  margin-top: 8px;
  display: grid;
  gap: 8px;
}

.task-row {
  gap: 14px;
  padding: 8px 0;
  border-top: 1px solid #eef2f8;
}

.task-row:first-child {
  border-top: none;
}

.student-meta,
.unit-meta,
.deadline-wrap {
  gap: 12px;
}

.student-meta {
  flex: 1.1;
}

.unit-meta,
.deadline-wrap {
  flex: 1;
  display: grid;
}

.student-avatar {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #ffe5c8 0%, #d8ecff 100%);
  color: #2a5ec7;
  font-weight: 800;
}

.student-meta strong,
.unit-meta strong,
.deadline-wrap strong {
  color: #273042;
}

.student-meta span,
.unit-meta span,
.deadline-wrap span {
  color: #8d9ab0;
  font-size: 0.74rem;
}

.status-wrap,
.action-wrap {
  width: 110px;
  display: flex;
  justify-content: center;
}

.task-status {
  min-width: 76px;
  min-height: 30px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 700;
}

.status-done {
  background: #ece9ff;
  color: #6972eb;
}

.status-active {
  background: #e7efff;
  color: #2b67dc;
}

.status-idle {
  background: #ffe7da;
  color: #d4763a;
}

.remove-btn {
  height: 28px;
  padding: 0 12px;
  border-radius: 10px;
  background: #171c22;
  color: #ffffff;
  font-size: 0.76rem;
}

.insight-card {
  background: #fff7f2;
}

.insight-badge {
  width: fit-content;
  padding: 6px 10px;
  border-radius: 12px;
  background: #ffe2d2;
  color: #c16831;
  font-size: 0.76rem;
  font-weight: 700;
}

.insight-card h3 {
  margin-top: 10px;
  font-size: 0.94rem;
  line-height: 1.55;
}

.insight-card p {
  font-size: 0.82rem;
  line-height: 1.6;
}

.insight-card button {
  width: 100%;
  height: 34px;
  margin-top: 10px;
  border-radius: 12px;
  background: #171c22;
  color: #ffffff;
  font-weight: 700;
}

.form-grid {
  display: grid;
  gap: 14px;
}

.empty-card {
  padding: 24px 0;
  color: #8d9ab0;
}

@media (max-width: 1120px) {
  .summary-grid,
  .page-header {
    grid-template-columns: 1fr;
  }

  .summary-cards {
    grid-template-columns: 1fr 1fr;
  }

  .task-content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .summary-cards {
    grid-template-columns: 1fr;
  }
}
</style>
