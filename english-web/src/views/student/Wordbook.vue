<template>
  <div class="wordbook-page">
    <div class="page-head">
      <h2>我的单词本</h2>
      <span>共 {{ wordList.length }} 个单词</span>
    </div>

    <section class="list-card">
      <div v-if="wordList.length === 0" class="empty-copy">当前还没有收藏或已学习的单词。</div>

      <article v-for="item in wordList" :key="item.wordId" class="word-row">
        <div class="word-copy">
          <strong>{{ item.word }}</strong>
          <p>{{ [item.pos, item.meaning].filter(Boolean).join(' ') || '暂无释义' }}</p>
          <span>{{ item.bookName }} / {{ item.unitName }} / {{ statusLabel(item.status) }}</span>
        </div>
        <button type="button" class="star-btn" @click="reviewWord(item)">
          <el-icon><Star /></el-icon>
        </button>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getStudentWordbook } from '../../api/student'
import { ElMessage } from 'element-plus'
import { Star } from '@element-plus/icons-vue'

const router = useRouter()
const learning = ref([])
const mastered = ref([])

const wordList = computed(() => [...mastered.value, ...learning.value])

function statusLabel(status) {
  if (status === 'mastered') return '已掌握'
  if (status === 'learning') return '学习中'
  return '未学完'
}

async function loadData() {
  try {
    const res = await getStudentWordbook()
    learning.value = Array.isArray(res?.learning) ? res.learning : []
    mastered.value = Array.isArray(res?.mastered) ? res.mastered : []
  } catch (error) {
    ElMessage.error(error.message || '获取单词本失败')
  }
}

function reviewWord(item) {
  router.push({
    name: 'StudentStudy',
    params: { unitId: item.unitId },
    query: { review: '1', mode: 'flashcard', unitName: item.unitName }
  })
}

onMounted(loadData)
</script>

<style scoped>
.wordbook-page {
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
.page-head span {
  margin: 0;
}

.page-head h2 {
  font-size: 1.15rem;
  color: #111827;
}

.page-head span {
  color: #6b7280;
  font-size: 0.92rem;
}

.list-card {
  border: 1px solid #ece7ef;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  overflow: hidden;
}

.word-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 16px;
  border-bottom: 1px solid #ece7ef;
}

.word-row:last-child {
  border-bottom: none;
}

.word-copy strong,
.word-copy p,
.word-copy span {
  display: block;
}

.word-copy strong {
  color: #111827;
  font-size: 1.05rem;
}

.word-copy p {
  margin: 10px 0 0;
  color: #374151;
  font-size: 0.96rem;
}

.word-copy span {
  margin-top: 10px;
  color: #6b7280;
  font-size: 0.86rem;
}

.star-btn {
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #9333ea;
  cursor: pointer;
}

.empty-copy {
  padding: 36px 18px;
  color: #6b7280;
  font-size: 0.92rem;
}

@media (max-width: 640px) {
  .page-head,
  .word-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
