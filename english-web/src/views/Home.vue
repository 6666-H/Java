<template>
  <div class="home">
    <div class="task-card" v-if="stats !== null">
      <h2>今日任务</h2>
      <p class="stats">今日待复习错题：<strong>{{ stats.reviewCount }}</strong> 个，待学习新词：<strong>{{ stats.newCount }}</strong> 个</p>
      <div class="actions">
        <button v-if="stats.reviewCount > 0" class="btn primary" @click="goStudy(true)">优先复习错题</button>
        <button class="btn" @click="goStudy(false)">开始学习</button>
      </div>
    </div>
    <div v-else-if="loading" class="loading">加载中…</div>
    <div v-else class="task-card">
      <h2>选择单元</h2>
      <p v-if="units.length === 0">请先选择书本，或暂无单元。</p>
      <select v-model="selectedUnitId" @change="loadStats">
        <option value="">请选择单元</option>
        <option v-for="u in units" :key="u.id" :value="u.id">{{ u.name }}</option>
      </select>
      <p v-if="selectedUnitId && stats" class="stats">待复习 {{ stats.reviewCount }} 个，新词 {{ stats.newCount }} 个</p>
      <button v-if="selectedUnitId" class="btn primary" @click="goStudy(stats && stats.reviewCount > 0)">进入学习</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listBooks, listUnits } from '../api/books'
import { getStudyStats } from '../api/study'

const router = useRouter()
const loading = ref(true)
const books = ref([])
const units = ref([])
const selectedUnitId = ref('')
const stats = ref(null)

async function loadBooksAndUnits() {
  loading.value = true
  try {
    books.value = await listBooks()
    if (books.value.length > 0) {
      units.value = await listUnits(books.value[0].id)
      if (units.value.length > 0 && !selectedUnitId.value) {
        selectedUnitId.value = units.value[0].id
        await loadStats()
      }
    }
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  if (!selectedUnitId.value) return
  try {
    stats.value = await getStudyStats(selectedUnitId.value)
  } catch {
    stats.value = { reviewCount: 0, newCount: 0 }
  }
}

function goStudy(prioritizeReview) {
  if (!selectedUnitId.value) return
  router.push({ name: 'Study', params: { unitId: selectedUnitId.value }, query: prioritizeReview ? { review: '1' } : {} })
}

onMounted(loadBooksAndUnits)
</script>

<style scoped>
.home { max-width: 520px; margin: 0 auto; }
.task-card {
  background: #fff;
  border-radius: 16px;
  padding: 28px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.06);
}
.task-card h2 { margin: 0 0 16px; font-size: 20px; color: #1a1a2e; }
.stats { margin: 12px 0 20px; color: #555; font-size: 15px; }
.stats strong { color: #667eea; }
.actions { display: flex; gap: 12px; flex-wrap: wrap; }
.btn {
  padding: 12px 24px;
  border-radius: 10px;
  border: 1px solid #e0e0e0;
  background: #fff;
  font-size: 15px;
  cursor: pointer;
}
.btn.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
}
.btn:hover { opacity: 0.9; }
.loading { text-align: center; padding: 40px; color: #666; }
select {
  width: 100%;
  padding: 12px 16px;
  margin-bottom: 12px;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  font-size: 15px;
}
</style>
