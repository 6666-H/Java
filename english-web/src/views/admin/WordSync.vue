<template>
  <div class="sync-page">
    <div class="page-head">
      <h2>词库管理</h2>
      <el-button type="primary" @click="dialogVisible = true">
        <el-icon><Plus /></el-icon>
        <span>导入词汇</span>
      </el-button>
    </div>

    <section class="hero-card">
      <span class="hero-icon">
        <el-icon><CollectionTag /></el-icon>
      </span>
      <h3>词库同步与管理</h3>
      <div class="hero-actions">
        <el-button type="primary" @click="dialogVisible = true">导入词汇</el-button>
      </div>
    </section>

    <section v-if="parsedData" class="preview-card">
      <div class="preview-head">
        <h3>解析结果预览</h3>
        <span class="preview-meta">共 {{ parsedStats.wordCount }} 个词汇条目</span>
      </div>

      <div class="preview-stats">
        <article class="preview-stat">
          <span>书本总数</span>
          <strong>{{ parsedStats.bookCount }}</strong>
        </article>
        <article class="preview-stat">
          <span>单元总数</span>
          <strong>{{ parsedStats.unitCount }}</strong>
        </article>
        <article class="preview-stat">
          <span>单词总数</span>
          <strong>{{ parsedStats.wordCount }}</strong>
        </article>
      </div>

      <el-table :data="parsedData" empty-text="暂无预览数据">
        <el-table-column type="index" label="#" width="56" />
        <el-table-column prop="name" label="书本名称" min-width="240" />
        <el-table-column label="单元数" width="120" align="center">
          <template #default="{ row }">{{ row.units?.length || 0 }}</template>
        </el-table-column>
        <el-table-column label="词汇数" width="120" align="center">
          <template #default="{ row }">{{ countWords(row.units) }}</template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" title="导入词库" width="780px">
      <el-alert title="请粘贴 JSON 格式的词库数据" type="info" :closable="false" show-icon class="sync-alert" />
      <el-input
        v-model="jsonData"
        type="textarea"
        :rows="14"
        placeholder='[{"name":"人教版七年级上册","units":[{"name":"Unit 1","words":[{"word":"apple","meaning":"苹果"}]}]}]'
        class="sync-textarea"
      />
      <template #footer>
        <el-button @click="parsePreview" plain>解析预览</el-button>
        <el-button type="primary" @click="executeSync" :loading="syncLoading" :disabled="!parsedData">执行同步</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../api/request'
import { CollectionTag, Plus } from '@element-plus/icons-vue'

const jsonData = ref('')
const parsedData = ref(null)
const syncLoading = ref(false)
const dialogVisible = ref(false)

const parsedStats = computed(() => {
  if (!parsedData.value) return { bookCount: 0, unitCount: 0, wordCount: 0 }
  let units = 0
  let words = 0
  parsedData.value.forEach((book) => {
    if (book.units) {
      units += book.units.length
      book.units.forEach((unit) => {
        if (unit.words) words += unit.words.length
      })
    }
  })
  return {
    bookCount: parsedData.value.length,
    unitCount: units,
    wordCount: words
  }
})

function countWords(units) {
  return (Array.isArray(units) ? units : []).reduce((total, unit) => total + ((unit.words || []).length || 0), 0)
}

function parsePreview() {
  if (!jsonData.value.trim()) {
    ElMessage.warning('请输入 JSON 数据')
    return
  }
  try {
    const data = JSON.parse(jsonData.value)
    if (!Array.isArray(data)) {
      throw new Error('数据必须是数组格式')
    }
    parsedData.value = data
    ElMessage.success('解析成功，请查看预览')
  } catch (error) {
    parsedData.value = null
    ElMessage.error(`JSON 解析失败: ${error.message}`)
  }
}

async function executeSync() {
  if (!parsedData.value) return
  try {
    await ElMessageBox.confirm('确定执行同步吗？该操作会覆盖或新增系统词库内容。', '执行同步', {
      confirmButtonText: '确定同步',
      cancelButtonText: '取消',
      type: 'warning'
    })

    syncLoading.value = true
    const res = await request.post('/admin/sync', { books: parsedData.value })
    ElMessage.success(`同步成功，共更新 ${res?.successCount || parsedStats.value.bookCount} 本教材`)
    dialogVisible.value = false
    parsedData.value = null
    jsonData.value = ''
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '同步失败')
    }
  } finally {
    syncLoading.value = false
  }
}
</script>

<style scoped>
.sync-page {
  display: grid;
  gap: 18px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-head h2,
.hero-card h3,
.preview-head h3 {
  margin: 0;
}

.page-head h2 {
  font-size: 1.05rem;
  color: #111827;
}

.hero-card,
.preview-card {
  border: 1px solid #dbeafe;
  border-radius: 14px;
  background: #eff6ff;
}

.hero-card {
  padding: 28px 24px;
  text-align: center;
}

.hero-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 16px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #dbeafe;
  color: #2563eb;
  font-size: 1.7rem;
}

.hero-actions {
  margin-top: 16px;
}

.preview-card {
  padding: 18px;
  background: #fff;
  border-color: #e5e7eb;
}

.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.preview-meta {
  color: #6b7280;
  font-size: 0.84rem;
}

.preview-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.preview-stat {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 14px;
  background: #f8fafc;
}

.preview-stat span,
.preview-stat strong {
  display: block;
}

.preview-stat span {
  color: #6b7280;
  font-size: 0.84rem;
}

.preview-stat strong {
  margin-top: 8px;
  font-size: 1.4rem;
  color: #111827;
}

.sync-alert {
  margin-bottom: 16px;
}

:deep(.sync-textarea textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

@media (max-width: 720px) {
  .page-head,
  .preview-head {
    flex-direction: column;
    align-items: stretch;
  }

  .preview-stats {
    grid-template-columns: 1fr;
  }
}
</style>
