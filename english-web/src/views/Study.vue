<template>
  <div class="study-page">
    <div class="top-bar">
      <router-link to="/" class="back">← 返回</router-link>
      <span class="progress">{{ currentIndex + 1 }} / {{ words.length }}</span>
    </div>
    <template v-if="currentWord">
      <div class="mode-tabs">
        <button :class="{ active: mode === 'meaning' }" @click="mode = 'meaning'">看词识义</button>
        <button :class="{ active: mode === 'listen' }" @click="mode = 'listen'">听音辨义</button>
        <button :class="{ active: mode === 'spell' }" @click="mode = 'spell'">拼写练习</button>
      </div>
      <KeepAlive>
        <WordToMeaning v-if="mode === 'meaning'" :word="currentWord" @next="onNext" @wrong="onWrong" />
        <ListenToMeaning v-else-if="mode === 'listen'" :word="currentWord" :options="listenOptions" @next="onNext" @wrong="onWrong" />
        <SpellPractice v-else :word="currentWord" @next="onNext" @wrong="onWrong" />
      </KeepAlive>
    </template>
    <div v-else-if="loading" class="loading">加载中…</div>
    <div v-else class="empty">
      <p>本单元暂无待学习内容</p>
      <router-link to="/">返回首页</router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getNextList } from '../api/study'
import WordToMeaning from '../components/study/WordToMeaning.vue'
import ListenToMeaning from '../components/study/ListenToMeaning.vue'
import SpellPractice from '../components/study/SpellPractice.vue'

const route = useRoute()
const unitId = route.params.unitId
const prioritizeReview = !!route.query.review
const loading = ref(true)
const words = ref([])
const currentIndex = ref(0)
const mode = ref('meaning')

const currentWord = computed(() => words.value[currentIndex.value] ?? null)

const listenOptions = computed(() => {
  const w = currentWord.value
  if (!w) return []
  const others = words.value.filter((x) => x.id !== w.id)
  const shuffled = [...others].sort(() => Math.random() - 0.5).slice(0, 3)
  const options = [...shuffled.map((x) => ({ id: x.id, meaning: x.meaning, correct: false })), { id: w.id, meaning: w.meaning, correct: true }]
  return options.length >= 2 ? options.sort(() => Math.random() - 0.5) : [{ id: w.id, meaning: w.meaning, correct: true }]
})

async function loadWords() {
  loading.value = true
  try {
    const list = await getNextList(unitId, 50)
    words.value = list || []
    currentIndex.value = 0
  } finally {
    loading.value = false
  }
}

function onNext() {
  if (currentIndex.value < words.value.length - 1) {
    currentIndex.value++
  } else {
    loadWords()
  }
}

function onWrong() {
  if (currentIndex.value < words.value.length - 1) {
    currentIndex.value++
  } else {
    loadWords()
  }
}

onMounted(loadWords)
</script>

<style scoped>
.study-page { max-width: 560px; margin: 0 auto; }
.top-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.back { color: #667eea; text-decoration: none; font-size: 14px; }
.progress { color: #666; font-size: 14px; }
.mode-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}
.mode-tabs button {
  flex: 1;
  padding: 10px;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  font-size: 14px;
}
.mode-tabs button.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-color: transparent;
}
.loading, .empty { text-align: center; padding: 60px 20px; color: #666; }
.empty a { color: #667eea; }
</style>
