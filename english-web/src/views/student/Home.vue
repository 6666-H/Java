<template>
  <div class="space-y-5">
    <section class="grid grid-cols-1 lg:grid-cols-12 gap-4 items-stretch">
      <div class="lg:col-span-8 relative overflow-hidden rounded-[22px] px-5 py-4 flex flex-col justify-between min-h-[164px] bg-gradient-to-br from-[#005bbf] to-[#1a73e8] text-white shadow-lg group">
        <div class="relative z-10">
          <h1 class="font-headline text-[1.65rem] md:text-[1.95rem] font-extrabold tracking-tight mb-1.5 leading-tight">今天要学点什么？</h1>
          <p class="text-[#d8e2ff] text-[13px] max-w-md font-medium opacity-90">专注于你的每日目标，保持学习动力。</p>
        </div>
        <div class="relative z-10 flex flex-wrap items-end justify-between gap-2.5">
          <div class="bg-white/80 backdrop-blur-xl rounded-2xl p-3 border border-white/20 shadow-xl">
            <p class="text-[#414754] text-xs font-semibold mb-1">每日目标</p>
            <div class="flex items-baseline gap-2">
              <span class="text-[1.15rem] font-extrabold text-[#005bbf]">{{ goalTarget }}</span>
              <span class="text-[#727785] text-xs">需掌握单词</span>
            </div>
            <div class="w-40 h-1.5 bg-[#e0e3e5] rounded-full mt-2 overflow-hidden">
              <div class="h-full bg-[#005bbf] rounded-full" :style="{ width: `${goalProgress}%` }"></div>
            </div>
          </div>
          <button class="bg-[#9e4300] hover:bg-[#c55500] text-white px-4 h-10 rounded-xl font-bold text-sm flex items-center gap-2.5 transition-all shadow-lg" @click="continueLast">
            <span>继续上次学习</span>
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
        <div class="absolute top-0 right-0 w-1/2 h-full opacity-20 pointer-events-none mix-blend-overlay">
          <div class="absolute inset-0 bg-gradient-to-l from-[#005bbf] to-transparent"></div>
          <div class="w-full h-full bg-[radial-gradient(circle_at_30%_30%,rgba(255,255,255,0.7),transparent_10%),radial-gradient(circle_at_70%_45%,rgba(255,255,255,0.55),transparent_18%),radial-gradient(circle_at_52%_80%,rgba(255,255,255,0.35),transparent_20%)]"></div>
        </div>
      </div>

      <div class="lg:col-span-4 grid h-full grid-rows-2 gap-4">
        <div class="bg-white rounded-[22px] px-5 py-4 flex h-full flex-col justify-center border border-transparent shadow-sm relative overflow-hidden group min-h-[84px]">
          <div class="absolute top-4 right-4 text-[#9e4300] opacity-10">
            <el-icon size="48"><Opportunity /></el-icon>
          </div>
          <p class="text-[#414754] text-[11px] font-bold tracking-widest uppercase mb-1.5">连续学习</p>
          <div class="flex items-center gap-2">
            <span class="text-[1.8rem] font-extrabold text-[#191c1e] leading-none">{{ streakDays }}</span>
            <span class="text-[15px] font-semibold text-[#414754]">天</span>
          </div>
        </div>

        <div class="bg-white rounded-[22px] px-5 py-4 flex h-full flex-col justify-center border border-transparent shadow-sm min-h-[84px]">
          <p class="text-[#414754] text-[11px] font-bold tracking-widest uppercase mb-1.5">已掌握单词</p>
          <div class="flex items-center gap-3">
            <span class="text-[1.8rem] font-extrabold text-[#005bbf] leading-none">{{ masteredCountDisplay }}</span>
            <el-icon class="text-[#1a73e8]" size="22"><CircleCheckFilled /></el-icon>
          </div>
        </div>
      </div>
    </section>

    <section class="grid grid-cols-1 md:grid-cols-12 gap-4 items-stretch">
      <div class="md:col-span-7 lg:col-span-8 flex">
        <div class="bg-white rounded-[22px] p-4 shadow-sm flex h-full w-full flex-col md:flex-row gap-4 items-center border border-transparent cursor-pointer min-h-[136px]" @click="openCurrentUnit">
          <div class="w-[72px] h-24 flex-shrink-0 rounded-xl overflow-hidden shadow-lg transform -rotate-3 bg-gradient-to-br from-[#7aa4a0] to-[#527b76] text-white flex items-center justify-center">
            <span class="font-headline text-2xl font-extrabold">{{ currentCourseMark }}</span>
          </div>
          <div class="flex-1 space-y-2.5">
            <div>
              <span class="inline-block px-3 py-1 bg-[#dee0ff] text-[#00105c] text-[11px] font-bold rounded-full mb-2">进行中课程</span>
              <h3 class="text-base font-bold text-[#191c1e] leading-snug">{{ currentCourse.title }}</h3>
              <p class="text-[13px] text-[#414754]">{{ currentCourse.subtitle }}</p>
            </div>
            <div class="space-y-1.5">
              <div class="flex justify-between text-[11px] font-bold text-[#414754]">
                <span>单元进度</span>
                <span>{{ currentCourse.progress }}%</span>
              </div>
              <div class="grid grid-cols-4 gap-2 h-1.5">
                <div v-for="n in 4" :key="n" class="rounded-full" :class="n <= currentCourse.activeSegments ? 'bg-[#005bbf]' : 'bg-[#e6e8ea]'"></div>
              </div>
              <div class="flex justify-between text-[10px] text-[#727785] font-bold uppercase tracking-widest pt-0.5">
                <span>阶段 1</span>
                <span>阶段 2</span>
                <span>阶段 3</span>
                <span>测试</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="md:col-span-5 lg:col-span-4 h-full">
<!--        <h2 class="font-headline text-[1.35rem] font-bold px-1">本周重点</h2>-->
        <div class="bg-[#f2f4f6] rounded-[22px] p-3 h-full flex flex-col gap-2.5">
          <button v-for="item in focusCards" :key="item.key" class="w-full flex-1 flex items-center justify-between px-3 py-2.5 bg-white rounded-[16px] text-left min-h-[54px]" @click="item.onClick">
            <div class="flex items-center gap-4">
              <div class="w-8 h-8 rounded-xl flex items-center justify-center" :class="item.iconBg">
                <el-icon :class="item.iconColor"><component :is="item.icon" /></el-icon>
              </div>
              <div>
                <p class="text-[11px] font-bold text-[#727785] uppercase tracking-wider">{{ item.label }}</p>
                <p class="text-base font-extrabold leading-tight">{{ item.value }}</p>
              </div>
            </div>
            <el-icon class="text-[#005bbf]"><ArrowRight /></el-icon>
          </button>
        </div>
      </div>
    </section>

    <section class="space-y-3">
      <div class="flex items-center justify-between">
        <h2 class="font-headline text-[1.4rem] font-extrabold tracking-tight">复习中心</h2>
        <button class="text-sm font-bold text-[#005bbf] flex items-center gap-1 hover:underline" @click="router.push('/student/review')">
          查看所有复习任务
          <el-icon><TopRight /></el-icon>
        </button>
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-4 items-stretch">
        <div class="group cursor-pointer relative overflow-hidden bg-[#ffdbcb] p-5 rounded-[22px] min-h-[184px] transition-all hover:shadow-2xl hover:-translate-y-1" @click="startWeakPractice">
          <div class="absolute -right-12 -top-12 w-40 h-40 bg-white/20 rounded-full blur-3xl"></div>
          <div class="relative z-10 h-full flex flex-col justify-between gap-3">
            <div class="w-9 h-9 bg-white/90 rounded-2xl flex items-center justify-center text-[#9e4300] shadow-sm">
              <el-icon size="20"><Connection /></el-icon>
            </div>
            <div>
              <h3 class="text-[1.2rem] font-extrabold text-[#341100] mb-1.5">生词复习</h3>
              <p class="text-[#783100] text-[13px] leading-relaxed max-w-sm">智能间隔重复练习，专门针对你认为具有挑战性的单词。强化你的记忆。</p>
            </div>
            <div class="flex items-center gap-3 text-[#341100] font-bold">
              <span class="px-3 py-1.5 bg-white/40 rounded-full text-xs">{{ weakWords.length || 15 }} 分钟</span>
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>
        </div>

        <div class="group cursor-pointer relative overflow-hidden bg-[#e0e3e5] p-5 rounded-[22px] min-h-[184px] transition-all hover:shadow-2xl hover:-translate-y-1" @click="goReview">
          <div class="absolute -right-12 -top-12 w-40 h-40 bg-[#005bbf]/5 rounded-full blur-3xl"></div>
          <div class="relative z-10 h-full flex flex-col justify-between gap-3">
            <div class="w-9 h-9 bg-white/90 rounded-2xl flex items-center justify-center text-[#005bbf] shadow-sm">
              <el-icon size="20"><EditPen /></el-icon>
            </div>
            <div>
              <h3 class="text-[1.2rem] font-extrabold text-[#191c1e] mb-1.5">错题纠正</h3>
              <p class="text-[#414754] text-[13px] leading-relaxed max-w-sm">分析过去测验和练习中的错误，以避免在未来再次出现类似错误。</p>
            </div>
            <div class="flex items-center gap-3 text-[#191c1e] font-bold">
              <span class="px-3 py-1.5 bg-white/60 rounded-full text-xs">{{ smartCount }} 个待处理</span>
              <el-icon><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ArrowRight, CircleCheckFilled, Connection, EditPen, Opportunity, PieChart, Timer, TopRight, Warning } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getStudentHome, getStudentLastStudy } from '../../api/student'
import { listBooks, getUnitProgress } from '../../api/books'
import { getSmartSummaryList, getStudyHeatmap, getWeakWords } from '../../api/study'

const router = useRouter()
const homeData = ref({ today: {}, overview: {}, tasks: [] })
const heatmapStats = ref({ consecutiveDays: 0, masteredCount: 0 })
const weakWords = ref([])
const smartSummary = ref({ list: [], total: 0, firstDate: '' })
const currentCourse = ref({ title: '当前暂无课程', subtitle: '去课程库挑选一本教材开始学习', progress: 0, unitId: '', bookId: '', activeSegments: 0 })
const lastStudy = ref({ bookId: null, bookName: '', unitId: null, unitName: '' })

const goalTarget = computed(() => Math.max(Number(homeData.value?.today?.targetCount || 50), 1))
const goalProgress = computed(() => {
  const completed = Number(homeData.value?.today?.completedCount || 0)
  return Math.min(100, Math.round((completed / goalTarget.value) * 100))
})
const streakDays = computed(() => Number(heatmapStats.value?.consecutiveDays || 0))
const masteredCountDisplay = computed(() => Number(heatmapStats.value?.masteredCount || 0).toLocaleString())
const smartCount = computed(() => Number(smartSummary.value?.total || smartSummary.value?.list?.length || 0))
const currentCourseMark = computed(() => (currentCourse.value.title || 'A').replace(/[^A-Za-z]/g, '').slice(0, 1).toUpperCase() || 'A')

const focusCards = computed(() => [
  { key: 'weak', label: '生词难词', value: `${weakWords.value.length || 0}`, icon: Warning, iconBg: 'bg-[#ffdad6]/40', iconColor: 'text-[#ba1a1a]', onClick: startWeakPractice },
  { key: 'time', label: '学习时长', value: `${Math.max(0.5, Math.round((Number(homeData.value?.today?.completedCount || 0) / 10) * 10) / 10)}h`, icon: Timer, iconBg: 'bg-[#ffdbcb]/40', iconColor: 'text-[#9e4300]', onClick: continueLast },
  { key: 'ratio', label: '掌握率', value: `${Math.round(Number(homeData.value?.overview?.masteredRatio || 0) * 100)}%`, icon: PieChart, iconBg: 'bg-[#dee0ff]/40', iconColor: 'text-[#4355b9]', onClick: openCurrentUnit }
])

async function loadDashboard() {
  try {
    const [homeRes, heatmapRes, weakRes, smartRes, lastStudyRes] = await Promise.all([
      getStudentHome(),
      getStudyHeatmap(),
      getWeakWords(null, 20),
      getSmartSummaryList({ days: 45, page: 1, pageSize: 3 }),
      getStudentLastStudy().catch(() => null),
    ])
    homeData.value = homeRes || homeData.value
    heatmapStats.value = heatmapRes || heatmapStats.value
    weakWords.value = Array.isArray(weakRes) ? weakRes : []
    smartSummary.value = smartRes || smartSummary.value
    lastStudy.value = lastStudyRes || lastStudy.value
    if (lastStudyRes?.bookId) {
      localStorage.setItem('last_book_id', String(lastStudyRes.bookId))
      localStorage.setItem('last_book_name', lastStudyRes.bookName || '')
    }
    if (lastStudyRes?.unitId) {
      localStorage.setItem('last_unit_id', String(lastStudyRes.unitId))
      localStorage.setItem('last_unit_name', lastStudyRes.unitName || '')
    }
  } catch (_) {}
  await hydrateCurrentCourse()
}

async function hydrateCurrentCourse() {
  const cachedBookId = Number(lastStudy.value?.bookId || localStorage.getItem('last_book_id') || 0)
  const cachedUnitId = Number(lastStudy.value?.unitId || localStorage.getItem('last_unit_id') || 0)
  try {
    const books = await listBooks()
    const bookList = Array.isArray(books) ? books : []
    const bookId = cachedBookId || bookList[0]?.id
    const book = bookList.find(item => item.id === bookId) || bookList[0]
    if (!book?.id) return
    const progress = await getUnitProgress(book.id)
    const rows = Array.isArray(progress) ? progress : []
    const active = rows.find(item => item.unitId === cachedUnitId) || rows.find(item => item.completionPercent > 0 && !item.completed) || rows[0]
    if (!active) return
    currentCourse.value = {
      title: book.name || '教材课程',
      subtitle: active.unitName ? `${active.unitName}` : '开始你的学习',
      progress: Number(active.completionPercent || 0),
      unitId: active.unitId,
      bookId: book.id,
      activeSegments: Math.max(1, Math.min(4, Math.ceil(Number(active.completionPercent || 0) / 25)))
    }
  } catch (_) {}
}

function continueLast() {
  const lastUnitId = lastStudy.value?.unitId || localStorage.getItem('last_unit_id') || currentCourse.value.unitId
  if (!lastUnitId) return router.push('/student/books')
  router.push({
    name: 'StudentStudy',
    params: { unitId: lastUnitId },
    query: {
      unitName: lastStudy.value?.unitName || localStorage.getItem('last_unit_name') || currentCourse.value.subtitle,
      bookName: lastStudy.value?.bookName || localStorage.getItem('last_book_name') || currentCourse.value.title,
    }
  })
}

function openCurrentUnit() {
  if (!currentCourse.value.unitId) return router.push('/student/books')
  router.push({ name: 'StudentUnitDetail', params: { unitId: currentCourse.value.unitId }, query: { bookId: currentCourse.value.bookId || undefined, unitName: currentCourse.value.subtitle || undefined } })
}

function startWeakPractice() {
  const firstWeakWord = weakWords.value[0]
  if (!firstWeakWord?.unitId) return router.push('/student/review')
  router.push({ name: 'StudentStudy', params: { unitId: firstWeakWord.unitId }, query: { weakOnly: '1', mode: 'flashcard', unitName: firstWeakWord.unitName || undefined } })
}

function goReview() {
  const date = smartSummary.value?.firstDate || smartSummary.value?.list?.[0]?.date
  if (!date) return router.push('/student/review')
  router.push({ name: 'StudentReviewByDate', params: { date } })
}

onMounted(loadDashboard)
</script>
