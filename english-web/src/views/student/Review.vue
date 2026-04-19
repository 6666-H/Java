<template>
  <div class="space-y-6">
    <div class="relative rounded-[22px] overflow-hidden min-h-[190px] bg-gradient-to-r from-[#ffdbcb] via-[#ffb691] to-[#ffd1b5]">
      <div class="relative z-10 p-6 flex flex-col md:flex-row justify-between items-end h-full">
        <div class="max-w-xl">
          <span class="inline-block px-3 py-1 rounded-full bg-[#9e4300] text-white text-[11px] font-bold mb-3 uppercase tracking-wider">Review Center</span>
          <h1 class="text-[2rem] md:text-[2.35rem] font-headline font-extrabold text-[#341100] mb-2 tracking-tight">复习中心</h1>
          <p class="text-[#783100] text-sm leading-relaxed max-w-md">“温故而知新，每一次复盘都是在为下一次的飞跃蓄力。你离掌握只有一步之遥。”</p>
        </div>
        <button class="mt-6 md:mt-0 flex items-center gap-3 px-5 py-3 bg-[#341100] text-white rounded-xl font-bold shadow-xl text-sm" @click="goToSmartReview">
          <el-icon><Lightning /></el-icon>
          <span>开始针对性强化</span>
        </button>
      </div>
      <div class="absolute -bottom-10 -right-10 w-64 h-64 bg-white/20 rounded-full blur-3xl"></div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
      <section class="lg:col-span-8 bg-white rounded-[22px] p-5 shadow-sm border border-[#c1c6d6]/10">
        <div class="flex justify-between items-center mb-5">
          <div class="flex items-center gap-3">
            <div class="w-9 h-9 rounded-xl bg-[#ffdbcb] flex items-center justify-center text-[#9e4300]">
              <el-icon><Connection /></el-icon>
            </div>
            <h2 class="text-[1.3rem] font-headline font-bold">弱词池</h2>
          </div>
          <span class="text-[#727785] text-sm font-medium">共计 {{ weakWords.length }} 个待突破</span>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div v-for="word in weakList" :key="word.wordId || word.id" class="p-4 rounded-[16px] bg-[#f2f4f6] transition-colors group">
            <div class="flex justify-between items-start mb-2">
              <span class="text-[1rem] font-bold font-headline">{{ word.word }}</span>
              <span class="px-2 py-1 bg-[#ffdad6] text-[#93000a] text-[10px] font-bold rounded">错误率 {{ weakRate(word) }}%</span>
            </div>
            <p class="text-xs text-[#414754] mb-3">{{ [word.pos, word.meaning].filter(Boolean).join(' · ') }}</p>
            <div class="w-full bg-[#e0e3e5] h-1.5 rounded-full overflow-hidden">
              <div class="bg-[#c55500] h-full" :style="{ width: `${weakRate(word)}%` }"></div>
            </div>
          </div>
        </div>
      </section>

      <section class="lg:col-span-4 flex flex-col gap-6">
        <div class="bg-[#4355b9] p-5 rounded-[22px] text-white relative overflow-hidden flex-1 group">
          <div class="absolute top-0 right-0 p-4 opacity-20">
            <el-icon size="76"><DataAnalysis /></el-icon>
          </div>
          <div class="relative z-10">
            <h3 class="text-[1.3rem] font-bold mb-3">AI 智能复盘</h3>
            <p class="text-white/80 text-xs mb-5 leading-relaxed">系统已根据您的遗忘曲线和错误类型，为您定制了 15 分钟的专项训练。</p>
            <div class="flex items-center gap-2 text-xs font-bold bg-white/20 w-fit px-3 py-1.5 rounded-full">
              <el-icon><Timer /></el-icon>
              <span>预计耗时 15 min</span>
            </div>
            <div class="mt-auto pt-5">
              <button class="w-full py-3 bg-white text-[#4355b9] font-black rounded-xl hover:shadow-lg transition-all text-sm" @click="goToSmartReview">立即进入</button>
            </div>
          </div>
        </div>

        <div class="bg-[#e6e8ea] p-5 rounded-[22px] border border-[#c1c6d6]/20">
          <div class="flex items-center gap-3 mb-3">
            <div class="text-[#9e4300]">
              <el-icon><TrophyBase /></el-icon>
            </div>
            <h4 class="font-bold">复习勋章进度</h4>
          </div>
          <div class="flex justify-between items-end">
            <div>
              <span class="text-[1.45rem] font-black text-[#191c1e] tracking-tighter">{{ smartCount }}/20</span>
              <span class="text-xs text-[#414754] block">连续复盘进度</span>
            </div>
            <div class="w-12 h-12 rounded-full border-4 border-[#ffdbcb] border-t-[#9e4300] flex items-center justify-center">
              <el-icon class="text-[#9e4300]"><Star /></el-icon>
            </div>
          </div>
        </div>
      </section>
    </div>

    <section class="bg-white rounded-[22px] p-5 shadow-sm border border-[#c1c6d6]/10">
      <div class="flex items-center gap-3 mb-6">
        <div class="w-9 h-9 rounded-xl bg-[#e6e8ea] flex items-center justify-center text-[#414754]">
          <el-icon><Calendar /></el-icon>
        </div>
        <h2 class="text-[1.3rem] font-headline font-bold">按日期复盘</h2>
      </div>
      <div class="relative ml-4">
        <div class="absolute left-0 top-2 bottom-0 w-0.5 bg-[#e6e8ea]"></div>
        <div v-for="(item, index) in timelineItems" :key="item.date" class="relative pl-8" :class="index !== timelineItems.length - 1 ? 'pb-8' : ''">
          <div class="absolute left-[-6px] top-2 w-3.5 h-3.5 rounded-full" :class="index === 0 ? 'bg-[#9e4300] ring-4 ring-[#ffdbcb]' : 'bg-[#e0e3e5] ring-4 ring-[#eceef0]'"></div>
          <div class="mb-2">
            <span class="text-sm font-bold" :class="index === 0 ? 'text-[#9e4300]' : 'text-[#414754]'">{{ timelineLabel(index, item.date) }}</span>
            <span class="ml-4 text-xs text-[#414754]">{{ item.totalCount }} 个错题记录</span>
          </div>
          <div v-if="index < 2" class="flex flex-wrap gap-3">
            <button v-for="tag in buildTags(item)" :key="`${item.date}-${tag}`" class="px-3 py-1.5 rounded-xl bg-[#f2f4f6] text-xs font-medium hover:border-[#ffdbcb] border border-transparent" @click="openDate(item.date)">
              <span class="font-headline font-bold">{{ tag }}</span>
            </button>
          </div>
          <button v-else class="text-[#005bbf] font-bold text-sm hover:underline flex items-center gap-1" @click="openDate(item.date)">
            回顾该日所有记录
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ArrowRight, Calendar, Connection, DataAnalysis, Lightning, Star, Timer, TrophyBase } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getErrorStatsByDate, getSmartSummaryList, getWeakWords } from '../../api/study'

const router = useRouter()
const weakWords = ref([])
const statsList = ref([])
const smartSummary = ref({ list: [], total: 0, firstDate: '' })

const weakList = computed(() => weakWords.value.slice(0, 4))
const smartCount = computed(() => Number(smartSummary.value?.total || smartSummary.value?.list?.length || 0))
const timelineItems = computed(() => (statsList.value.length ? statsList.value.slice(0, 3) : [{ date: smartSummary.value.firstDate || '最近 7 天', totalCount: smartCount.value || 0, typeCounts: {} }]))

async function fetchData() {
  const [weakRes, statsRes, smartRes] = await Promise.allSettled([
    getWeakWords(null, 18),
    getErrorStatsByDate({ days: 90 }),
    getSmartSummaryList({ days: 45, page: 1, pageSize: 5 })
  ])
  weakWords.value = weakRes.status === 'fulfilled' && Array.isArray(weakRes.value) ? weakRes.value : []
  statsList.value = statsRes.status === 'fulfilled' && Array.isArray(statsRes.value) ? statsRes.value : []
  smartSummary.value = smartRes.status === 'fulfilled' ? (smartRes.value || smartSummary.value) : smartSummary.value
}

function weakRate(word) {
  const wrongCount = Number(word?.wrongCount || word?.errorCount || 0)
  const total = Math.max(1, wrongCount + Number(word?.correctCount || 1))
  return Math.max(25, Math.min(95, Math.round((wrongCount / total) * 100) || 35))
}

function buildTags(item) {
  const entries = Object.entries(item?.typeCounts || {})
  if (entries.length === 0) return ['Hierarchy', 'Aesthetic', 'Implement']
  return entries.slice(0, 3).map(([key]) => key)
}

function timelineLabel(index, date) {
  if (index === 0) return `今天, ${date}`
  if (index === 1) return `昨天, ${date}`
  return date
}

function openDate(date) {
  if (!date) return router.push('/student/review')
  router.push({ name: 'StudentReviewByDate', params: { date } })
}

function goToSmartReview() {
  const date = smartSummary.value?.firstDate || statsList.value?.[0]?.date
  openDate(date)
}

onMounted(fetchData)
</script>
