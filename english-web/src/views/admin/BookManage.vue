<template>
  <div class="book-page">
    <section class="hero-grid">
      <div class="hero-banner">
        <div>
          <h2>教材内容管理</h2>
          <p>维护学术资源核心库，统筹管理词汇、单元及发布状态。确保学习内容的学术严谨性。</p>
        </div>
        <button type="button" class="hero-button" @click="openBookForm()">
          <el-icon><Plus /></el-icon>
          <span>导入新教材</span>
        </button>
      </div>

      <article class="hero-side-card">
        <div class="side-head">
          <el-icon><Reading /></el-icon>
          <span>库容率 {{ overview.capacityRate || 0 }}%</span>
        </div>
        <div>
          <h3>全量词库统计</h3>
          <p>{{ formatNumber(overview.totalWordCount || 0) }} 活跃学术词汇</p>
        </div>
      </article>
    </section>

    <section class="toolbox">
      <div class="toolbox-head">
        <el-icon><Grid /></el-icon>
        <h3>批量处理工具</h3>
      </div>
      <div class="tool-grid">
        <button type="button" class="tool-card" @click="selectedBook ? exportWords('book') : ElMessage.info('请先打开一本教材')">
          <el-icon><Download /></el-icon>
          <strong>释义自动补全</strong>
          <span>导出当前教材做二次处理</span>
        </button>
        <button type="button" class="tool-card" @click="triggerImport">
          <el-icon><UploadFilled /></el-icon>
          <strong>导入自动补全</strong>
          <span>导入 AI 整理后的词单</span>
        </button>
        <button type="button" class="tool-card" @click="selectedBook ? openManageBook(selectedBook) : ElMessage.info('请先选择教材')">
          <el-icon><EditPen /></el-icon>
          <strong>词库去重清洗</strong>
          <span>清理冗余或失效条目</span>
        </button>
        <button type="button" class="tool-card" @click="exportWords()">
          <el-icon><Download /></el-icon>
          <strong>全库全量备份</strong>
          <span>导出当前所有教材词库</span>
        </button>
        <button type="button" class="tool-card" @click="triggerJsonImport">
          <el-icon><FolderOpened /></el-icon>
          <strong>JSON导入</strong>
          <span>批量导入教材、单元与单词</span>
        </button>
      </div>
    </section>

    <section class="filter-row">
      <div class="filter-panel">
        <div class="chips">
          <button
            v-for="chip in chips"
            :key="chip"
            type="button"
            class="chip"
            :class="{ active: activeChip === chip }"
            @click="activeChip = chip"
          >
            {{ chip }}
          </button>
        </div>
      </div>
      <div class="filter-meta">
        <div class="view-toggle">
          <button type="button" class="view-btn" :class="{ active: viewMode === 'list' }" @click="viewMode = 'list'">
            列表
          </button>
          <button type="button" class="view-btn" :class="{ active: viewMode === 'grid' }" @click="viewMode = 'grid'">
            网格
          </button>
        </div>
        <div class="sort-hint">默认按最近更新排序</div>
      </div>
    </section>

    <section v-if="viewMode === 'list'" class="book-list" v-loading="loadingOverview">
      <article v-for="book in displayedBooks" :key="book.id" class="book-list-row">
        <div class="book-list-main">
          <div class="book-list-head">
            <span class="list-status" :class="book.status === 'PUBLISHED' ? 'published' : 'draft'">
              {{ book.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </span>
            <div>
              <h3>{{ book.name }}</h3>
              <p>{{ book.description || `${bookLabel(book)} 学习内容库` }}</p>
            </div>
          </div>

          <div class="book-list-meta">
            <div class="list-metric">
              <span>学段</span>
              <strong>{{ bookStage(book) }}</strong>
            </div>
            <div class="list-metric">
              <span>版本</span>
              <strong>{{ book.versionName || '通用版' }}</strong>
            </div>
            <div class="list-metric">
              <span>单元数</span>
              <strong>{{ book.unitCount || 0 }}</strong>
            </div>
            <div class="list-metric">
              <span>词汇量</span>
              <strong>{{ formatNumber(book.wordCount || 0) }}</strong>
            </div>
            <div class="list-metric">
              <span>更新时间</span>
              <strong>{{ formatDate(book.updatedAt) }}</strong>
            </div>
          </div>
        </div>

        <div class="book-list-actions">
          <button type="button" @click="openBookForm(book)">编辑教材</button>
          <button type="button" @click="openManageBook(book)">内容管理</button>
          <button type="button" @click="triggerImportFromCard(book)">导入词库</button>
          <button type="button" class="danger" @click="deleteBook(book)">删除</button>
        </div>
      </article>

      <div v-if="!displayedBooks.length && !loadingOverview" class="empty-books">当前筛选下暂无教材</div>
    </section>

    <section v-else class="book-grid" v-loading="loadingOverview">
      <article v-for="book in displayedBooks" :key="book.id" class="book-card">
        <div class="book-cover" :style="coverStyle(book)">
          <img v-if="book.coverUrl" :src="book.coverUrl" :alt="book.name" />
          <span class="book-status" :class="book.status === 'PUBLISHED' ? 'published' : 'draft'">
            {{ book.status === 'PUBLISHED' ? '已发布' : '草稿' }}
          </span>
        </div>

        <div class="book-content">
          <div class="book-title-row">
            <div>
              <h3>{{ book.name }}</h3>
              <p>{{ bookLabel(book) }}</p>
            </div>
            <button type="button" class="more-btn" @click="openBookForm(book)">
              <el-icon><MoreFilled /></el-icon>
            </button>
          </div>

          <div class="metric-grid">
            <div>
              <span>单元</span>
              <strong>{{ book.unitCount || 0 }}</strong>
            </div>
            <div>
              <span>词汇</span>
              <strong>{{ formatNumber(book.wordCount || 0) }}</strong>
            </div>
          </div>

          <div class="book-updated">
            <span>{{ bookStage(book) }}</span>
            <span>{{ formatDate(book.updatedAt) }}</span>
          </div>

          <div class="book-actions">
            <button type="button" @click="openManageBook(book)">管理</button>
            <button type="button" @click="triggerImportFromCard(book)">导入</button>
            <button type="button" class="danger" @click="deleteBook(book)">删除</button>
          </div>
        </div>
      </article>

      <div v-if="!displayedBooks.length && !loadingOverview" class="empty-books">当前筛选下暂无教材</div>
    </section>

    <section class="book-pagination">
      <span>显示 {{ rangeLabel }} 条，共 {{ filteredTotal }} 条教材，当前第 {{ page }} / {{ totalPages }} 页</span>
      <el-pagination
        background
        layout="prev, pager, next, jumper"
        :current-page="page"
        :page-size="pageSize"
        :total="filteredTotal"
        :pager-count="7"
        @current-change="changePage"
      />
    </section>

    <el-dialog v-model="manageDialogVisible" :title="selectedBook ? selectedBook.name : '教材内容'" width="1120px">
      <div class="manage-toolbar">
        <div>
          <h3>{{ selectedBook?.name || '教材内容' }}</h3>
          <p>统一维护单元与单词内容，支持批量导入导出。</p>
        </div>
        <div class="manage-actions">
          <el-button @click="exportWords('book')" :disabled="!selectedBook">导出本书</el-button>
          <el-button @click="triggerImport" :disabled="!selectedBook">导入 CSV</el-button>
          <el-button type="primary" @click="openUnitForm()" :disabled="!selectedBook">新增单元</el-button>
        </div>
      </div>

      <div class="manage-grid">
        <aside class="unit-column" v-loading="loadingUnits">
          <button
            v-for="unit in units"
            :key="unit.id"
            type="button"
            class="unit-item"
            :class="{ active: selectedUnit?.id === unit.id }"
            @click="selectUnit(unit)"
          >
            <div>
              <strong>{{ unit.name }}</strong>
              <span>排序 {{ unit.sortOrder ?? 0 }}</span>
            </div>
            <div class="unit-actions">
              <button type="button" @click.stop="openUnitForm(unit)">编辑</button>
              <button type="button" class="danger" @click.stop="deleteUnit(unit)">删除</button>
            </div>
          </button>
        </aside>

        <section class="word-column">
          <div class="word-head">
            <div>
              <h4>{{ selectedUnit?.name || '请选择单元' }}</h4>
              <p>单词数 {{ words.length }}</p>
            </div>
            <div class="manage-actions">
              <el-button @click="exportWords('unit')" :disabled="!selectedUnit">导出本单元</el-button>
              <el-button type="primary" @click="openWordForm()" :disabled="!selectedUnit">新增单词</el-button>
            </div>
          </div>

          <el-table :data="words" v-loading="loadingWords" empty-text="当前单元暂无单词">
            <el-table-column prop="word" label="单词" min-width="140" />
            <el-table-column prop="phonetic" label="音标" min-width="120" />
            <el-table-column prop="meaning" label="释义" min-width="220" show-overflow-tooltip />
            <el-table-column prop="pos" label="词性" width="90" />
            <el-table-column label="操作" width="150" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click="openWordForm(row)">编辑</el-button>
                <el-button link type="danger" @click="deleteWord(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </div>
    </el-dialog>

    <el-dialog v-model="bookFormVisible" :title="editingBook ? '编辑教材' : '新增教材'" width="520px">
      <el-form :model="bookForm" label-width="84px">
        <el-form-item label="书名" required><el-input v-model="bookForm.name" placeholder="书名" /></el-form-item>
        <el-form-item label="年级"><el-input v-model="bookForm.grade" placeholder="如 CET-4 / TOEFL" /></el-form-item>
        <el-form-item label="版本"><el-input v-model="bookForm.versionName" placeholder="如 强化词汇 / 学术词汇" /></el-form-item>
        <el-form-item label="封面URL"><el-input v-model="bookForm.coverUrl" placeholder="选填" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="bookForm.description" type="textarea" rows="2" placeholder="选填" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="bookForm.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bookFormVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBook" :loading="bookFormLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="unitFormVisible" :title="editingUnit ? '编辑单元' : '新增单元'" width="420px">
      <el-form :model="unitForm" label-width="80px">
        <el-form-item label="单元名" required><el-input v-model="unitForm.name" placeholder="单元名" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="unitForm.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="unitFormVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUnit" :loading="unitFormLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="wordFormVisible" :title="editingWord ? '编辑单词' : '新增单词'" width="560px">
      <el-form :model="wordForm" label-width="92px">
        <el-form-item label="单词" required><el-input v-model="wordForm.word" placeholder="英文单词" /></el-form-item>
        <el-form-item label="音标"><el-input v-model="wordForm.phonetic" placeholder="选填" /></el-form-item>
        <el-form-item label="词性"><el-input v-model="wordForm.pos" placeholder="如 n. / v." /></el-form-item>
        <el-form-item label="释义" required><el-input v-model="wordForm.meaning" type="textarea" rows="2" placeholder="释义" /></el-form-item>
        <el-form-item label="英文例句"><el-input v-model="wordForm.exampleSentence" type="textarea" rows="2" placeholder="选填" /></el-form-item>
        <el-form-item label="例句翻译"><el-input v-model="wordForm.exampleZh" type="textarea" rows="2" placeholder="选填" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="wordForm.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="wordFormVisible = false">取消</el-button>
        <el-button type="primary" @click="submitWord" :loading="wordFormLoading">确定</el-button>
      </template>
    </el-dialog>

    <input ref="importInputRef" type="file" accept=".csv,text/csv" class="hidden-input" @change="handleImportChange" />
    <input ref="jsonImportInputRef" type="file" accept=".json,application/json" class="hidden-input" @change="handleJsonImportChange" />
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import request from '../../api/request'
import { getBookOverview } from '../../api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Download,
  EditPen,
  FolderOpened,
  Grid,
  MoreFilled,
  Plus,
  Reading,
  UploadFilled
} from '@element-plus/icons-vue'

const page = ref(1)
const pageSize = 10
const initialized = ref(false)
const route = useRoute()
const overview = ref({
  totalBookCount: 0,
  totalWordCount: 0,
  totalUnitCount: 0,
  capacityRate: 0,
  filteredCount: 0,
  page: 1,
  pageSize,
  books: []
})
const loadingOverview = ref(false)

const units = ref([])
const words = ref([])
const loadingUnits = ref(false)
const loadingWords = ref(false)
const selectedBook = ref(null)
const selectedUnit = ref(null)
const manageDialogVisible = ref(false)

const activeChip = ref('全部教材')
const chips = ['全部教材', '小学', '初中', '高中']
const viewMode = ref('list')
const searchKeyword = computed(() => String(route.query.keyword || '').trim())

const bookFormVisible = ref(false)
const bookForm = ref({ name: '', grade: '', versionName: '', coverUrl: '', description: '', sortOrder: 0 })
const editingBook = ref(null)
const bookFormLoading = ref(false)

const unitFormVisible = ref(false)
const unitForm = ref({ name: '', sortOrder: 0 })
const editingUnit = ref(null)
const unitFormLoading = ref(false)

const wordFormVisible = ref(false)
const wordForm = ref({ word: '', phonetic: '', pos: '', meaning: '', exampleSentence: '', exampleZh: '', sortOrder: 0 })
const editingWord = ref(null)
const wordFormLoading = ref(false)
const importInputRef = ref(null)
const jsonImportInputRef = ref(null)

const displayedBooks = computed(() => overview.value.books || [])
const filteredTotal = computed(() => Number(overview.value.filteredCount || 0))
const totalPages = computed(() => Math.max(1, Math.ceil(filteredTotal.value / pageSize)))
const rangeLabel = computed(() => {
  if (!filteredTotal.value || !displayedBooks.value.length) return '0-0'
  const start = (page.value - 1) * pageSize + 1
  const end = start + displayedBooks.value.length - 1
  return `${start}-${end}`
})

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function formatDate(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 10)
}

function bookLabel(book) {
  return [book.grade, book.versionName].filter(Boolean).join(' / ') || '综合教材'
}

function bookStage(book) {
  const source = [book.grade, book.name, book.versionName, book.description]
    .filter(Boolean)
    .join(' ')

  if (/小学|一年级|二年级|三年级|四年级|五年级|六年级|小一|小二|小三|小四|小五|小六/.test(source)) {
    return '小学'
  }
  if (/初中|七年级|八年级|九年级|初一|初二|初三/.test(source)) {
    return '初中'
  }
  if (/高中|高一|高二|高三|十年级|十一年级|十二年级/.test(source)) {
    return '高中'
  }
  return book.grade || '综合'
}

function coverStyle(book) {
  const palette = [
    'linear-gradient(135deg, #0f6a99, #123b66)',
    'linear-gradient(135deg, #5a381d, #120e13)',
    'linear-gradient(135deg, #1f4db8, #5c80f6)',
    'linear-gradient(135deg, #0f766e, #115e59)'
  ]
  const fallback = palette[Number(book.id || 0) % palette.length]
  return book.coverUrl ? {} : { background: fallback }
}

async function fetchOverview() {
  loadingOverview.value = true
  try {
    overview.value = {
      totalBookCount: 0,
      totalWordCount: 0,
      totalUnitCount: 0,
      capacityRate: 0,
      filteredCount: 0,
      page: page.value,
      pageSize,
      books: [],
      ...(await getBookOverview({
        page: page.value,
        pageSize,
        category: activeChip.value,
        keyword: searchKeyword.value || undefined
      }))
    }
    if (selectedBook.value?.id) {
      selectedBook.value = overview.value.books.find(book => book.id === selectedBook.value.id) || selectedBook.value
    }
  } catch (error) {
    ElMessage.error(error.message || '获取教材总览失败')
  } finally {
    loadingOverview.value = false
  }
}

function changePage(nextPage) {
  const safeNextPage = Math.min(Math.max(Number(nextPage) || 1, 1), totalPages.value)
  if (safeNextPage === page.value) return
  page.value = safeNextPage
}

async function fetchUnits() {
  if (!selectedBook.value?.id) {
    units.value = []
    selectedUnit.value = null
    words.value = []
    return
  }
  loadingUnits.value = true
  try {
    units.value = await request.get('/admin/units', { params: { bookId: selectedBook.value.id } }) || []
    if (!units.value.some(item => item.id === selectedUnit.value?.id)) {
      selectedUnit.value = units.value[0] || null
    }
    await fetchWords()
  } finally {
    loadingUnits.value = false
  }
}

async function fetchWords() {
  if (!selectedUnit.value?.id) {
    words.value = []
    return
  }
  loadingWords.value = true
  try {
    words.value = await request.get('/admin/words', { params: { unitId: selectedUnit.value.id } }) || []
  } finally {
    loadingWords.value = false
  }
}

function selectUnit(unit) {
  selectedUnit.value = unit
  fetchWords()
}

async function openManageBook(book) {
  selectedBook.value = book
  manageDialogVisible.value = true
  await fetchUnits()
}

function openBookForm(row) {
  editingBook.value = row || null
  if (row) {
    bookForm.value = {
      name: row.name || '',
      grade: row.grade || '',
      versionName: row.versionName || '',
      coverUrl: row.coverUrl || '',
      description: row.description || '',
      sortOrder: row.sortOrder ?? 0
    }
  } else {
    bookForm.value = { name: '', grade: '', versionName: '', coverUrl: '', description: '', sortOrder: (overview.value.books || []).length }
  }
  bookFormVisible.value = true
}

async function submitBook() {
  if (!bookForm.value.name?.trim()) {
    ElMessage.warning('请填写书名')
    return
  }
  bookFormLoading.value = true
  try {
    if (editingBook.value) {
      await request.put(`/admin/books/${editingBook.value.id}`, bookForm.value)
      ElMessage.success('教材已更新')
    } else {
      await request.post('/admin/books', bookForm.value)
      ElMessage.success('教材已新增')
    }
    bookFormVisible.value = false
    await fetchOverview()
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    bookFormLoading.value = false
  }
}

function triggerImportFromCard(book) {
  selectedBook.value = book
  selectedUnit.value = null
  triggerImport()
}

function triggerImport() {
  importInputRef.value?.click()
}

function triggerJsonImport() {
  jsonImportInputRef.value?.click()
}

async function handleImportChange(event) {
  const file = event.target.files?.[0]
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  if (selectedUnit.value?.id) formData.append('unitId', selectedUnit.value.id)
  else if (selectedBook.value?.id) formData.append('bookId', selectedBook.value.id)
  try {
    const res = await request.post('/admin/words/import', formData)
    ElMessage.success(`导入成功，共处理 ${res?.importedCount || 0} 条`)
    await Promise.all([fetchOverview(), fetchUnits()])
  } catch (error) {
    ElMessage.error(error.message || '导入失败')
  } finally {
    event.target.value = ''
  }
}

async function handleJsonImportChange(event) {
  const file = event.target.files?.[0]
  if (!file) return
  try {
    const text = await file.text()
    const parsed = JSON.parse(text)
    const payload = Array.isArray(parsed) ? { books: parsed } : parsed
    if (!payload || !Array.isArray(payload.books) || payload.books.length === 0) {
      throw new Error('JSON 结构不正确，需包含 books 数组')
    }
    const res = await request.post('/admin/sync', payload)
    ElMessage.success(`JSON 导入成功：教材 ${res?.booksCount || 0} 本，单元 ${res?.unitsCount || 0} 个，单词 ${res?.wordsCount || 0} 条`)
    await Promise.all([fetchOverview(), fetchUnits()])
  } catch (error) {
    ElMessage.error(error.message || 'JSON 导入失败')
  } finally {
    event.target.value = ''
  }
}

function openUnitForm(row) {
  editingUnit.value = row || null
  if (row) {
    unitForm.value = { name: row.name, sortOrder: row.sortOrder ?? 0 }
  } else {
    unitForm.value = { name: '', sortOrder: units.value.length }
  }
  unitFormVisible.value = true
}

async function submitUnit() {
  if (!unitForm.value.name?.trim()) {
    ElMessage.warning('请填写单元名')
    return
  }
  if (!selectedBook.value?.id) {
    ElMessage.warning('请先选择教材')
    return
  }
  unitFormLoading.value = true
  try {
    if (editingUnit.value) {
      await request.put(`/admin/units/${editingUnit.value.id}`, unitForm.value)
    } else {
      await request.post('/admin/units', { ...unitForm.value, bookId: selectedBook.value.id })
    }
    ElMessage.success('单元已保存')
    unitFormVisible.value = false
    await Promise.all([fetchUnits(), fetchOverview()])
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    unitFormLoading.value = false
  }
}

function deleteUnit(row) {
  ElMessageBox.confirm(`确定删除单元「${row.name}」吗？`, '删除单元', { type: 'warning' })
    .then(async () => {
      await request.delete(`/admin/units/${row.id}`)
      ElMessage.success('已删除')
      if (selectedUnit.value?.id === row.id) selectedUnit.value = null
      await Promise.all([fetchUnits(), fetchOverview()])
    })
    .catch(() => {})
}

function openWordForm(row) {
  editingWord.value = row || null
  if (row) {
    wordForm.value = {
      word: row.word,
      phonetic: row.phonetic || '',
      pos: row.pos || '',
      meaning: row.meaning || '',
      exampleSentence: row.exampleSentence || '',
      exampleZh: row.exampleZh || '',
      sortOrder: row.sortOrder ?? 0
    }
  } else {
    wordForm.value = {
      word: '',
      phonetic: '',
      pos: '',
      meaning: '',
      exampleSentence: '',
      exampleZh: '',
      sortOrder: words.value.length
    }
  }
  wordFormVisible.value = true
}

async function submitWord() {
  if (!wordForm.value.word?.trim()) {
    ElMessage.warning('请填写单词')
    return
  }
  if (!selectedUnit.value?.id) {
    ElMessage.warning('请先选择单元')
    return
  }
  wordFormLoading.value = true
  try {
    if (editingWord.value) {
      await request.put(`/admin/words/${editingWord.value.id}`, wordForm.value)
    } else {
      await request.post('/admin/words', { ...wordForm.value, unitId: selectedUnit.value.id })
    }
    ElMessage.success('单词已保存')
    wordFormVisible.value = false
    await Promise.all([fetchWords(), fetchOverview()])
  } catch (error) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    wordFormLoading.value = false
  }
}

function deleteWord(row) {
  ElMessageBox.confirm(`确定删除单词「${row.word}」吗？`, '删除单词', { type: 'warning' })
    .then(async () => {
      await request.delete(`/admin/words/${row.id}`)
      ElMessage.success('已删除')
      await Promise.all([fetchWords(), fetchOverview()])
    })
    .catch(() => {})
}

function deleteBook(row) {
  ElMessageBox.confirm(`确定删除教材「${row.name}」吗？其下单元与单词将一并删除。`, '删除教材', { type: 'warning' })
    .then(async () => {
      await request.delete(`/admin/books/${row.id}`)
      ElMessage.success('教材已删除')
      await fetchOverview()
    })
    .catch(() => {})
}

async function exportWords(scope) {
  try {
    const params = new URLSearchParams()
    if (scope === 'unit' && selectedUnit.value?.id) params.set('unitId', selectedUnit.value.id)
    if (scope === 'book' && selectedBook.value?.id) params.set('bookId', selectedBook.value.id)
    const token = localStorage.getItem('token_SUPER_ADMIN') || ''
    const url = `${(import.meta.env.VITE_API_BASE || '/api')}/admin/words/export${params.toString() ? `?${params.toString()}` : ''}`
    const response = await fetch(url, {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    })
    if (!response.ok) throw new Error('导出失败')
    const blob = await response.blob()
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = scope === 'unit' ? 'unit-words.csv' : scope === 'book' ? 'book-words.csv' : 'all-words.csv'
    link.click()
    URL.revokeObjectURL(link.href)
  } catch (error) {
    ElMessage.error(error.message || '导出失败')
  }
}

watch(totalPages, () => {
  if (page.value > totalPages.value) {
    page.value = totalPages.value
  }
})

watch(page, () => {
  if (initialized.value) {
    fetchOverview()
  }
})

watch(activeChip, () => {
  if (!initialized.value) return
  if (page.value !== 1) {
    page.value = 1
    return
  }
  fetchOverview()
})

watch(searchKeyword, () => {
  if (!initialized.value) return
  if (page.value !== 1) {
    page.value = 1
    return
  }
  fetchOverview()
})

fetchOverview().then(() => {
  initialized.value = true
})
</script>

<style scoped>
.book-page {
  display: grid;
  gap: 16px;
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(280px, 1fr);
  gap: 14px;
}

.hero-banner,
.hero-side-card,
.toolbox,
.book-card,
.add-card {
  border-radius: 18px;
}

.hero-banner {
  position: relative;
  overflow: hidden;
  padding: 18px 20px;
  display: flex;
  justify-content: space-between;
  gap: 14px;
  background: linear-gradient(135deg, #0b5fd1, #1a73e8);
  color: #fff;
}

.hero-banner::after {
  content: '';
  position: absolute;
  right: -42px;
  bottom: -52px;
  width: 180px;
  height: 180px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  filter: blur(4px);
}

.hero-banner h2,
.hero-side-card h3 {
  margin: 0;
  font-family: 'Manrope', 'PingFang SC', sans-serif;
  font-weight: 800;
}

.hero-banner h2 {
  font-size: 28px;
  letter-spacing: -0.04em;
}

.hero-banner p {
  max-width: 540px;
  margin: 6px 0 0;
  color: rgba(255, 255, 255, 0.86);
  line-height: 1.55;
  font-size: 13px;
}

.hero-button {
  position: relative;
  z-index: 1;
  height: 44px;
  padding: 0 14px;
  border: none;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  background: #fff;
  color: #0b5fd1;
  font-weight: 800;
  cursor: pointer;
}

.hero-side-card {
  padding: 18px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background: #ffd8c4;
}

.side-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #a74600;
}

.side-head span {
  padding: 5px 9px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.55);
  font-size: 11px;
  font-weight: 800;
}

.hero-side-card p {
  margin: 6px 0 0;
  color: #7c5130;
  font-size: 13px;
}

.filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.filter-panel,
.filter-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.chip {
  min-height: 34px;
  padding: 0 13px;
  border: none;
  border-radius: 999px;
  background: #e5e7eb;
  color: #4b5563;
  font-weight: 700;
  cursor: pointer;
}

.chip.active {
  background: #0b5fd1;
  color: #fff;
}

.view-toggle {
  padding: 4px;
  border-radius: 999px;
  display: inline-flex;
  gap: 4px;
  background: rgba(226, 232, 240, 0.9);
}

.view-btn {
  min-width: 64px;
  height: 34px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #4b5563;
  font-weight: 700;
  cursor: pointer;
}

.view-btn.active {
  background: #0b5fd1;
  color: #fff;
}

.sort-hint {
  color: #7b8190;
  font-size: 12px;
  font-weight: 600;
}

.book-list {
  display: grid;
  gap: 12px;
}

.book-list-row {
  padding: 18px 20px;
  border-radius: 18px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(193, 198, 214, 0.3);
  box-shadow: 0 14px 24px rgba(15, 23, 42, 0.04);
}

.book-list-main {
  display: grid;
  gap: 14px;
}

.book-list-head {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.book-list-head h3,
.book-list-head p {
  margin: 0;
}

.book-list-head h3 {
  font-size: 18px;
  font-family: 'Manrope', 'PingFang SC', sans-serif;
  font-weight: 800;
  color: #111827;
}

.book-list-head p {
  margin-top: 6px;
  color: #7b8190;
  font-size: 13px;
  line-height: 1.5;
}

.list-status {
  min-width: 52px;
  padding: 6px 10px;
  border-radius: 999px;
  display: inline-flex;
  justify-content: center;
  background: #eef2ff;
  font-size: 11px;
  font-weight: 800;
}

.list-status.published {
  color: #0b5fd1;
}

.list-status.draft {
  background: #fff3e8;
  color: #c45a1d;
}

.book-list-meta {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.list-metric {
  padding: 12px 14px;
  border-radius: 14px;
  background: #f4f7fb;
}

.list-metric span,
.list-metric strong {
  display: block;
}

.list-metric span {
  color: #7b8190;
  font-size: 12px;
  font-weight: 700;
}

.list-metric strong {
  margin-top: 6px;
  color: #1f2a44;
  font-size: 15px;
  font-weight: 800;
}

.book-list-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(112px, 1fr));
  gap: 10px;
}

.book-list-actions button,
.book-actions button,
.tool-card {
  border: none;
  border-radius: 14px;
  background: #f4f6f8;
  color: #4b5563;
  font-weight: 700;
  cursor: pointer;
}

.book-list-actions button {
  min-height: 42px;
  padding: 0 16px;
  font-size: 13px;
}

.book-list-actions .danger,
.book-actions .danger {
  background: #fff1f2;
  color: #dc2626;
}

.book-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.book-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  color: #6b7280;
  font-size: 12px;
}

.book-card,
.add-card,
.toolbox {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(193, 198, 214, 0.3);
  box-shadow: 0 14px 24px rgba(15, 23, 42, 0.045);
}

.book-cover {
  position: relative;
  height: 110px;
  overflow: hidden;
  border-radius: 18px 18px 0 0;
}

.book-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.book-status {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 5px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 800;
  background: rgba(255, 255, 255, 0.85);
}

.book-status.published {
  color: #0b5fd1;
}

.book-status.draft {
  color: #c45a1d;
}

.book-content {
  padding: 12px;
}

.book-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.book-title-row h3,
.book-title-row p {
  margin: 0;
}

.book-title-row h3 {
  font-size: 15px;
  font-family: 'Manrope', 'PingFang SC', sans-serif;
  font-weight: 800;
  color: #111827;
  letter-spacing: -0.03em;
}

.book-title-row p,
.book-updated {
  color: #7b8190;
  font-size: 12px;
}

.more-btn {
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: #6b7280;
  cursor: pointer;
}

.metric-grid {
  margin: 10px 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.metric-grid div {
  padding: 8px 10px;
  border-radius: 12px;
  background: #f2f4f6;
}

.metric-grid span,
.metric-grid strong {
  display: block;
}

.metric-grid span {
  color: #7b8190;
  font-size: 12px;
  font-weight: 700;
}

.metric-grid strong {
  margin-top: 4px;
  color: #4f67d9;
  font-size: 16px;
  font-family: 'Manrope', 'PingFang SC', sans-serif;
  font-weight: 800;
}

.book-updated {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.book-actions {
  margin-top: 10px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.book-actions button {
  min-height: 38px;
  padding: 0 8px;
  font-size: 12px;
}

.empty-books {
  padding: 28px 18px;
  border-radius: 18px;
  text-align: center;
  color: #7b8190;
  background: rgba(255, 255, 255, 0.7);
  border: 1px dashed rgba(193, 198, 214, 0.5);
}

.add-card {
  min-height: 100%;
  padding: 24px 16px;
  border: 2px dashed rgba(96, 112, 146, 0.25);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(255, 255, 255, 0.45);
  color: #5b6478;
}

.add-icon {
  width: 52px;
  height: 52px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: rgba(11, 95, 209, 0.12);
  color: #0b5fd1;
  font-size: 18px;
}

.toolbox {
  padding: 20px;
}

.toolbox-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.toolbox-head h3 {
  margin: 0;
  font-family: 'Manrope', 'PingFang SC', sans-serif;
  font-size: 22px;
  font-weight: 800;
}

.tool-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.tool-card {
  min-height: 88px;
  padding: 14px;
  display: grid;
  align-content: start;
  gap: 6px;
  text-align: left;
}

.tool-card strong,
.tool-card span {
  display: block;
}

.tool-card span {
  color: #7b8190;
  font-size: 12px;
}

.manage-toolbar,
.word-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.manage-toolbar h3,
.word-head h4,
.manage-toolbar p,
.word-head p {
  margin: 0;
}

.manage-toolbar p,
.word-head p {
  margin-top: 6px;
  color: #7b8190;
}

.manage-actions {
  display: flex;
  gap: 12px;
}

.manage-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
}

.unit-column {
  display: grid;
  gap: 12px;
}

.unit-item {
  padding: 16px;
  border: 1px solid rgba(193, 198, 214, 0.35);
  border-radius: 18px;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.unit-item.active {
  border-color: rgba(11, 95, 209, 0.35);
  box-shadow: inset 0 0 0 1px rgba(11, 95, 209, 0.18);
  background: #f6faff;
}

.unit-item strong,
.unit-item span {
  display: block;
}

.unit-item span {
  margin-top: 6px;
  color: #7b8190;
  font-size: 12px;
}

.unit-actions {
  margin-top: 12px;
  display: flex;
  gap: 12px;
}

.unit-actions button {
  border: none;
  background: transparent;
  padding: 0;
  color: #0b5fd1;
  font-weight: 700;
  cursor: pointer;
}

.unit-actions .danger {
  color: #dc2626;
}

.word-column {
  display: grid;
  gap: 16px;
}

.hidden-input {
  display: none;
}

@media (max-width: 1200px) {
  .book-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .book-list-meta,
  .tool-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .hero-grid,
  .manage-grid {
    grid-template-columns: 1fr;
  }

  .book-list-row {
    grid-template-columns: 1fr;
  }

  .book-list-actions {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .book-grid,
  .tool-grid,
  .book-actions,
  .book-list-meta,
  .book-list-actions {
    grid-template-columns: 1fr;
  }

  .hero-banner,
  .filter-row,
  .filter-meta,
  .manage-toolbar,
  .word-head {
    flex-direction: column;
    align-items: stretch;
  }

  .book-updated {
    flex-direction: column;
  }

  .book-pagination {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
