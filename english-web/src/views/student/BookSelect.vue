<template>
  <div class="space-y-5">
    <section>
      <div class="flex flex-wrap gap-2">
        <button
          v-for="item in filterTabs"
          :key="item.key"
          class="px-4 py-2 rounded-full text-[13px] font-medium transition-colors"
          :class="activeTab === item.key ? 'bg-[#005bbf] text-white shadow-md shadow-[#005bbf]/10' : 'bg-[#e6e8ea] text-[#414754] hover:bg-[#eceef0]'"
          @click="activeTab = item.key"
        >
          {{ item.label }}
        </button>
      </div>
    </section>

    <div v-if="loading" class="p-8 bg-white rounded-2xl">教材加载中...</div>

    <div v-else class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-8">
      <div
        v-for="book in filteredBooks.slice(0, 4)"
        :key="book.id"
        class="group relative flex flex-col bg-white rounded-[22px] overflow-hidden transition-all duration-300 hover:shadow-2xl hover:shadow-on-surface/5 hover:-translate-y-1 cursor-pointer"
        @click="openBook(book)"
      >
        <div class="h-44 overflow-hidden relative" :style="{ background: coverGradient(book) }">
          <div class="absolute inset-0 bg-gradient-to-t from-black/40 to-transparent z-10"></div>
          <div class="w-full h-full flex items-center justify-center text-white/90 font-headline text-6xl font-extrabold">{{ coverMark(book) }}</div>
          <span class="absolute top-4 left-4 z-20 px-3 py-1 bg-[#4355b9] text-white text-[10px] font-bold tracking-widest rounded-full uppercase">
            {{ badgeLabel(book) }}
          </span>
        </div>

        <div class="p-5">
          <h3 class="text-[1.4rem] font-bold font-headline mb-4 group-hover:text-[#005bbf] transition-colors">{{ book.name }}</h3>
          <div class="grid grid-cols-2 gap-3 mb-5">
            <div class="bg-[#f2f4f6] p-3 rounded-[14px]">
              <p class="text-[10px] text-[#727785] uppercase font-bold mb-1">词汇总量</p>
              <p class="text-[1.25rem] font-bold text-[#191c1e]">{{ book.totalWords.toLocaleString() }}</p>
            </div>
            <div class="bg-[#f2f4f6] p-3 rounded-[14px]">
              <p class="text-[10px] text-[#727785] uppercase font-bold mb-1">已掌握</p>
              <p class="text-[1.25rem] font-bold text-[#191c1e]">{{ book.masteredCount.toLocaleString() }}</p>
            </div>
          </div>

          <div class="space-y-2">
            <div class="flex justify-between text-xs font-semibold text-[#414754]">
              <span>当前进度</span>
              <span>{{ book.progressPercent }}%</span>
            </div>
            <div class="w-full h-2 bg-[#e6e8ea] rounded-full overflow-hidden">
              <div class="h-full rounded-full transition-all duration-700" :class="progressClass(book.progressPercent)" :style="{ width: `${Math.max(4, book.progressPercent)}%` }"></div>
            </div>
          </div>
        </div>
      </div>

      <div
        v-if="featuredBook"
        class="xl:col-span-2 bg-[#005bbf] rounded-[28px] overflow-hidden p-8 flex flex-col md:flex-row items-center justify-between gap-8 cursor-pointer"
        @click="openBook(featuredBook)"
      >
        <div class="text-white">
          <span class="inline-flex px-3 py-1 rounded-full bg-white/20 text-xs font-bold">精品推荐</span>
          <h2 class="text-[2.7rem] font-extrabold font-headline mt-5 mb-3">{{ featuredBook.name }}</h2>
          <p class="text-white/75 max-w-lg leading-8">{{ featuredDescription }}</p>
          <button class="mt-7 px-7 py-3 bg-white text-[#005bbf] font-bold rounded-xl text-sm">查看详情</button>
        </div>
        <div class="w-full max-w-[180px] h-36 rounded-2xl bg-gradient-to-br from-[#231815] to-[#8a673c] text-white flex items-center justify-center text-5xl font-headline font-extrabold shadow-xl">
          {{ coverMark(featuredBook) }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listBooks, getUnitProgress } from '../../api/books'

const router = useRouter()
const loading = ref(false)
const books = ref([])
const activeTab = ref('all')

const filterTabs = computed(() => {
  const versionNames = [...new Set(books.value.map(item => item.versionName).filter(Boolean))].slice(0, 4)
  return [{ key: 'all', label: '全部' }, ...versionNames.map(item => ({ key: item, label: item }))]
})
const filteredBooks = computed(() => activeTab.value === 'all' ? books.value : books.value.filter(item => item.versionName === activeTab.value))
const featuredBook = computed(() => [...filteredBooks.value].sort((a, b) => b.progressPercent - a.progressPercent)[0] || null)
const featuredDescription = computed(() => featuredBook.value?.description || `专为 ${featuredBook.value?.grade || '进阶'} 学习者设计的词汇拓展路径。`)

async function loadBooks() {
  loading.value = true
  try {
    const res = await listBooks()
    const rows = Array.isArray(res) ? res : []
    books.value = await Promise.all(rows.map(async (book) => {
      try {
        const progress = await getUnitProgress(book.id)
        const list = Array.isArray(progress) ? progress : []
        const totalWords = list.reduce((sum, item) => sum + Number(item.totalCount || 0), 0)
        const masteredCount = list.reduce((sum, item) => sum + Number(item.masteredCount || 0), 0)
        const progressPercent = totalWords > 0 ? Math.round((masteredCount / totalWords) * 100) : 0
        return { ...book, totalWords, masteredCount, progressPercent }
      } catch (_) {
        return { ...book, totalWords: 0, masteredCount: 0, progressPercent: 0 }
      }
    }))
  } finally {
    loading.value = false
  }
}

function openBook(book) {
  localStorage.setItem('last_book_id', String(book.id))
  localStorage.setItem('last_book_name', book.name || '')
  router.push({ name: 'StudentUnitList', params: { bookId: book.id }, query: { bookName: book.name || undefined } })
}

function coverMark(book) {
  return String(book?.name || 'B').replace(/[^A-Za-z0-9]/g, '').slice(0, 2).toUpperCase() || 'BK'
}

function badgeLabel(book) {
  return (book.versionName || book.grade || 'Mastery').toUpperCase()
}

function coverGradient(book) {
  const themes = [
    'linear-gradient(135deg, #201613 0%, #a46943 100%)',
    'linear-gradient(135deg, #15181d 0%, #474b53 100%)',
    'linear-gradient(135deg, #111519 0%, #27343c 100%)',
    'linear-gradient(135deg, #2d3b3f 0%, #627d7a 100%)'
  ]
  return themes[Number(book.id || 0) % themes.length]
}

function progressClass(percent) {
  if (percent >= 85) return 'bg-[#9e4300]'
  if (percent >= 45) return 'bg-[#c55500]'
  return 'bg-[#7c8cff]'
}

onMounted(loadBooks)
</script>
