<template>
  <div class="study-page">
    <div class="study-shell">
      <header class="topbar">
        <div class="topbar-left">
          <button class="topbar-btn back-btn" @click="confirmExit">
            <el-icon><ArrowLeft /></el-icon>
          </button>

          <div class="unit-copy">
            <p>{{ bookName || '教材' }}</p>
            <h1>{{ unitName || '背单词训练' }}</h1>
            <span>{{ currentModeMeta.shortLabel }}</span>
          </div>
        </div>

        <div class="topbar-progress" :title="`${completedCount} / ${totalCount}`">
          <div class="progress-track">
            <span :style="{ width: `${progressPercent}%` }"></span>
          </div>
        </div>

        <div class="topbar-right">
          <button class="icon-btn" :title="isFullscreen ? '退出沉浸模式' : '进入沉浸模式'" @click="toggleFullscreen">
            <el-icon><FullScreen /></el-icon>
          </button>
          <div class="settings-wrap">
            <button class="icon-btn" title="设置" @click="isSettingsOpen = !isSettingsOpen; isInsightOpen = false">
              <el-icon><Setting /></el-icon>
            </button>
            <transition name="feedback-slide">
              <div v-if="isSettingsOpen" class="settings-popover">
                <div class="settings-head">
                  <strong>学习设置</strong>
                  <span>{{ currentModeMeta.shortLabel }}</span>
                </div>
                <button class="settings-item" @click="toggleAudio">
                  <span>音效</span>
                  <strong>{{ audioEnabled ? '开启' : '关闭' }}</strong>
                </button>
                <div class="settings-note">顶部保持极简，常用开关统一收在这里。</div>
              </div>
            </transition>
          </div>
          <button class="icon-btn" :title="isPaused ? '继续训练' : '暂停训练'" @click="togglePause">
            <el-icon><component :is="isPaused ? VideoPlay : VideoPause" /></el-icon>
          </button>
        </div>
      </header>

      <main class="study-layout">
        <section class="center-panel">
          <div v-if="loading" class="state-card">
            <el-icon class="state-icon is-loading"><Loading /></el-icon>
            <h2>正在准备本轮练习</h2>
            <p>词库和题型马上就好。</p>
          </div>

          <div v-else-if="words.length === 0" class="state-card">
            <el-icon class="state-icon success"><CircleCheckFilled /></el-icon>
            <h2>当前没有待学习单词</h2>
            <p>这组词今天已经清完了，可以返回学习中心继续别的任务。</p>
            <button class="action-btn primary" @click="goBackFromStudy(true)">返回学习中心</button>
          </div>

          <div v-else-if="queue.length === 0" class="state-card summary-card">
            <h2>{{ summaryTitle }}</h2>
            <p>这一阶段已经完成，下面是本轮学习结果。</p>

            <div class="summary-grid">
              <div class="summary-item">
                <span>本轮词数</span>
                <strong>{{ totalCount }}</strong>
              </div>
              <div class="summary-item">
                <span>总答对</span>
                <strong>{{ correctAttemptsCount }}</strong>
              </div>
              <div class="summary-item">
                <span>总答错</span>
                <strong>{{ wrongAttemptsCount }}</strong>
              </div>
              <div class="summary-item">
                <span>稳定掌握</span>
                <strong>{{ stabilizedCount }}</strong>
              </div>
              <div class="summary-item">
                <span>第一轮首次答对</span>
                <strong>{{ firstRoundCorrectCount }}{{ starReward ? ' · ⭐' : '' }}</strong>
              </div>
              <div class="summary-item">
                <span>本次用时</span>
                <strong>{{ elapsedLabel }}</strong>
              </div>
            </div>

            <div class="summary-actions">
              <button
                v-if="nextModeLabel && !isReviewByDate && route.query.review !== '1' && route.query.weakOnly !== '1'"
                class="action-btn secondary"
                @click="goNextMode"
              >
                继续下一阶段：{{ nextModeLabel }}
              </button>
              <button class="action-btn primary" :disabled="submitLoading" @click="submitSessionAndExit">
                <el-icon v-if="submitLoading" class="is-loading"><Loading /></el-icon>
                <span>{{ submitLoading ? '同步数据中...' : '提交数据并返回' }}</span>
              </button>
            </div>
          </div>

          <template v-else>
            <div class="info-strip">
              <section class="sidebar-card overview-card">
                <div class="panel-head">
                  <span class="panel-kicker">任务概览</span>
                  <strong>{{ estimatedLabel }}</strong>
                </div>
                <div class="ring-row">
                  <div
                    v-for="item in overviewRings"
                    :key="item.key"
                    class="ring-card"
                  >
                    <div
                      class="ring-chart"
                      :style="{
                        '--ring-color': item.color,
                        '--ring-fill': `${item.percent}%`
                      }"
                    >
                      <span>{{ item.value }}</span>
                    </div>
                    <strong>{{ item.label }}</strong>
                  </div>
                </div>
                <div class="overview-metrics">
                  <div class="metric-chip">
                    <span>正确率</span>
                    <strong>{{ accuracyPercent }}%</strong>
                  </div>
                  <div class="metric-chip">
                    <span>连胜</span>
                    <strong>{{ currentStreak }}</strong>
                  </div>
                  <div class="metric-chip">
                    <span>奖励</span>
                    <strong>{{ rewardTitle }}</strong>
                  </div>
                </div>
                <p class="overview-copy">{{ rewardCopy }}</p>
              </section>

              <section class="sidebar-card queue-card">
                <div class="panel-head">
                  <span class="panel-kicker">本轮词表</span>
                  <strong>{{ queue.length }} 待完成</strong>
                </div>
                <div class="word-grid">
                  <div v-for="item in queuePreviewItems" :key="item.id" class="word-grid-item">
                    <span class="status-dot" :class="`is-${item.state}`"></span>
                    <strong>{{ item.word }}</strong>
                  </div>
                </div>
                <div class="queue-summary">
                  <span class="mini-tag">未学 {{ queueBuckets.pending }}</span>
                  <span class="mini-tag review">待复习 {{ queueBuckets.review }}</span>
                  <span class="mini-tag error">易错 {{ queueBuckets.error }}</span>
                  <span class="mini-tag success">已掌握 {{ queueBuckets.mastered }}</span>
                </div>
              </section>

              <section class="sidebar-card word-detail-card">
                <div class="panel-head">
                  <span class="panel-kicker">当前词信息</span>
                  <strong>{{ currentWordDetail?.word || '--' }}</strong>
                </div>
                <div v-if="currentWordDetail" class="word-info">
                  <div class="stat-row">
                    <span>中文义</span>
                    <strong>{{ currentWordDetail.meaning || '--' }}</strong>
                  </div>
                  <div class="stat-row">
                    <span>词性</span>
                    <strong>{{ currentWordDetail.pos || '常用词' }}</strong>
                  </div>
                  <div class="stat-row">
                    <span>拼写提示</span>
                    <strong>{{ spellingHint }}</strong>
                  </div>
                  <div class="word-note" v-if="currentWordDetail.exampleSentence">
                    <span>例句</span>
                    <p>{{ currentWordDetail.exampleSentence }}</p>
                  </div>
                  <div class="word-note" v-if="confusionWords.length">
                    <span>易混词</span>
                    <p>{{ confusionWords.join(' / ') }}</p>
                  </div>
                </div>
                <div class="insight-wrap">
                  <button class="insight-trigger" title="智能反馈" @click="isInsightOpen = !isInsightOpen; isSettingsOpen = false">
                    <el-icon><Opportunity /></el-icon>
                  </button>
                  <transition name="feedback-slide">
                    <div v-if="isInsightOpen" class="insight-popover">
                      <strong>{{ smartFeedback.title }}</strong>
                      <p>{{ smartFeedback.body }}</p>
                      <small>{{ smartFeedback.tip }}</small>
                    </div>
                  </transition>
                </div>
              </section>
            </div>

            <article class="training-card">
              <div class="training-head">
                <span class="training-context">{{ roundLabel }}</span>
                <strong class="training-progress">第 {{ currentWordIndex }} / {{ totalCount }} 个</strong>
              </div>

              <div v-if="questionOptionsLoading && (normalizedMode === 'eng_ch' || normalizedMode === 'ch_eng')" class="question-loading">
                <el-icon class="is-loading"><Loading /></el-icon>
                <strong>正在生成本题选项</strong>
              </div>

              <transition name="slide-left" mode="out-in">
                <div v-if="currentWord" :key="`${currentWord.id}-${normalizedMode}`">
                  <ModeFlashcard
                    v-if="normalizedMode === 'flashcard'"
                    :word="currentWordDetail"
                    :audio-enabled="audioEnabled"
                    :paused="isPaused"
                    @next="handleNext"
                    @wrong="handleWrong"
                  />
                  <ModeEngToCh
                    v-else-if="normalizedMode === 'eng_ch'"
                    :word="currentWordDetail"
                    :options="meaningOptions"
                    :audio-enabled="audioEnabled"
                    :paused="isPaused"
                    @next="handleNext"
                    @wrong="handleWrong"
                  />
                  <ModeChToEng
                    v-else-if="normalizedMode === 'ch_eng'"
                    :word="currentWordDetail"
                    :options="wordOptions"
                    :audio-enabled="audioEnabled"
                    :paused="isPaused"
                    @next="handleNext"
                    @wrong="handleWrong"
                  />
                  <ModeSpell
                    v-else
                    :word="currentWordDetail"
                    :audio-enabled="audioEnabled"
                    :paused="isPaused"
                    @next="handleNext"
                    @wrong="handleWrong"
                  />
                </div>
              </transition>

              <div v-if="isPaused" class="pause-mask">
                <strong>已暂停</strong>
                <p>休息一下没关系，准备好就继续。</p>
                <div class="pause-actions">
                  <button class="action-btn primary" @click="togglePause">继续训练</button>
                  <button class="action-btn secondary" @click="confirmExit">结束本轮</button>
                </div>
              </div>
            </article>
          </template>
        </section>
      </main>
    </div>

    <transition name="feedback-slide">
      <div v-if="feedbackState" class="feedback-toast" :class="feedbackState.type">
        <strong>{{ feedbackState.title }}</strong>
        <p>{{ feedbackState.body }}</p>
        <small v-if="feedbackState.tip">{{ feedbackState.tip }}</small>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clearStudentStudySession, getStudentStudySession, saveStudentLastStudy, saveStudentStageResult, saveStudentStudySession } from '../../api/student'
import {
  completeMode,
  getErrorsByDate,
  getIncompleteWords,
  getModeStats,
  getNextList,
  getQuestionOptions,
  getStudyStats,
  getUnitModeCompletion,
  getUnitWords,
  getWeakWords,
  getWordsByErrorDate,
  markErrorReviewComplete,
  submitFeedback,
} from '../../api/study'
import ModeFlashcard from '../../components/study/ModeFlashcard.vue'
import ModeEngToCh from '../../components/study/ModeEngToCh.vue'
import ModeChToEng from '../../components/study/ModeChToEng.vue'
import ModeSpell from '../../components/study/ModeSpell.vue'
import { useToastStore } from '../../stores/toast'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, CircleCheckFilled, FullScreen, Loading, Opportunity, Setting, VideoPause, VideoPlay } from '@element-plus/icons-vue'
import { playWordAudio } from '../../components/study/studyPlayback'

const MODE_META = {
  flashcard: {
    label: '新词学习',
    shortLabel: '看词识义',
    title: '先认识这个词',
    description: '先听发音、看释义，建立完整第一印象。',
    etaSeconds: 14,
    round: 1,
    shortcuts: [
      { key: '←', label: '还没记住' },
      { key: '→', label: '我知道了' },
      { key: 'Space', label: '再听一遍' },
      { key: 'Enter', label: '确认继续' },
    ],
  },
  eng_ch: {
    label: '识别训练',
    shortLabel: '看英语选中文',
    title: '先把英文认出来',
    description: '看到英文后，快速匹配正确中文。',
    etaSeconds: 16,
    round: 2,
    shortcuts: [
      { key: '1-4', label: '选择选项' },
      { key: '← / →', label: '切换选项' },
      { key: 'Space', label: '播放发音' },
      { key: 'Enter', label: '确认答案' },
    ],
  },
  ch_eng: {
    label: '回忆训练',
    shortLabel: '看中文选英语',
    title: '把答案从记忆里提出来',
    description: '先想英文，再从候选项里确认。',
    etaSeconds: 18,
    round: 3,
    shortcuts: [
      { key: '1-4', label: '选择选项' },
      { key: '← / →', label: '切换选项' },
      { key: 'Space', label: '播放发音' },
      { key: 'Enter', label: '确认答案' },
    ],
  },
  spell: {
    label: '拼写挑战',
    shortLabel: '看中文拼写',
    title: '把整个单词完整拼出来',
    description: '从听音和中文提示中完成拼写输出。',
    etaSeconds: 22,
    round: 4,
    shortcuts: [
      { key: 'Enter', label: '检查答案' },
      { key: 'Backspace', label: '删除字母' },
      { key: 'Space', label: '播放发音' },
      { key: 'Tab', label: '切换控件' },
    ],
  },
}

const RESULT_SUMMARY_KEY = 'student_stage_result_summary'
const STUDY_PROGRESS_LATEST_KEY = 'study_progress_latest'
const AUDIO_PREF_KEY = 'student_study_audio_enabled'
const MODE_ORDER = ['flashcard', 'eng_ch', 'ch_eng', 'spell']

const route = useRoute()
const router = useRouter()
const toast = useToastStore()

const unitId = computed(() => route.params.unitId)
const unitName = computed(() => route.query.unitName || localStorage.getItem('last_unit_name') || '')
const bookName = computed(() => route.query.bookName || localStorage.getItem('last_book_name') || '')
const isReviewByDate = computed(() => route.name === 'StudentReviewPractice')
const reviewDate = computed(() => route.params.date)
const normalizedMode = computed(() => String(studyMode.value || '').toLowerCase())
const currentModeMeta = computed(() => MODE_META[normalizedMode.value] || MODE_META.flashcard)

const loading = ref(true)
const words = ref([])
const queue = ref([])
const initialCount = ref(0)
const studyMode = ref(route.query.mode || 'flashcard')
const choiceOptions = ref([])
const questionOptionsLoading = ref(false)
const answeredCount = ref(0)
const localErrorList = ref(new Set())
const localUserInputMap = ref({})
const firstAttemptSeen = ref(new Set())
const firstAttemptCorrect = ref(new Set())
const correctAttemptsCount = ref(0)
const wrongAttemptsCount = ref(0)
const currentStreak = ref(0)
const wrongCountMap = ref({})
const taskStats = ref({ newCount: 0, reviewCount: 0, errorCount: 0 })
const submitLoading = ref(false)
const wrongProcessing = ref(false)
const isFullscreen = ref(false)
const isPaused = ref(false)
const audioEnabled = ref(true)
const isSettingsOpen = ref(false)
const isInsightOpen = ref(false)
const sessionStartAt = ref(Date.now())
const feedbackState = ref(null)
const slideDirection = ref('slide-left')

let saveSessionTimer = null
let feedbackTimer = null

const currentWord = computed(() => queue.value[0] || null)
const totalCount = computed(() => initialCount.value || words.value.length || 0)
const completedCount = computed(() => Math.max(totalCount.value - queue.value.length, 0))
const progressPercent = computed(() => totalCount.value ? Math.round((completedCount.value / totalCount.value) * 100) : 0)
const accuracyPercent = computed(() => {
  const total = correctAttemptsCount.value + wrongAttemptsCount.value
  return total ? Math.round((correctAttemptsCount.value / total) * 100) : 100
})
const firstRoundCorrectCount = computed(() => firstAttemptCorrect.value.size)
const starReward = computed(() => firstRoundCorrectCount.value >= 4)
const stabilizedCount = computed(() => Math.max(totalCount.value - localErrorList.value.size, 0))
const roundLabel = computed(() => `${unitName.value || '当前单元'} · 第 ${currentModeMeta.value.round} 轮`)
const currentWordIndex = computed(() => totalCount.value ? Math.min(completedCount.value + 1, totalCount.value) : 0)
const hasActivePractice = computed(() => !loading.value && !!currentWord.value && queue.value.length > 0)
const estimatedLabel = computed(() => {
  if (!queue.value.length) return '即将完成'
  const remainSeconds = queue.value.length * currentModeMeta.value.etaSeconds
  return remainSeconds < 60 ? `预计 ${remainSeconds} 秒` : `预计 ${Math.ceil(remainSeconds / 60)} 分钟`
})
const elapsedLabel = computed(() => {
  const seconds = Math.max(Math.round((Date.now() - sessionStartAt.value) / 1000), 0)
  const minutes = Math.floor(seconds / 60)
  const remainSeconds = seconds % 60
  return `${minutes}分${String(remainSeconds).padStart(2, '0')}秒`
})
const summaryTitle = computed(() => isReviewByDate.value ? '本轮巩固完成' : '本轮学习完成')
const nextModeLabel = computed(() => {
  if (normalizedMode.value === 'flashcard') return MODE_META.eng_ch.shortLabel
  if (normalizedMode.value === 'eng_ch') return MODE_META.ch_eng.shortLabel
  if (normalizedMode.value === 'ch_eng') return MODE_META.spell.shortLabel
  return ''
})
const currentWordDetail = computed(() => {
  const word = currentWord.value
  if (!word) return null
  return {
    ...word,
    exampleZh: word.exampleZh || '',
    confusionWords: buildConfusionWords(word),
    spellingPattern: buildSpellingPattern(word.word),
  }
})
const meaningOptions = computed(() => {
  const word = currentWord.value
  if (!word) return []
  if (choiceOptions.value.length > 0) {
    return choiceOptions.value.map((item) => ({
      id: item.id,
      meaning: item.text,
      isCorrect: !!item.correct,
    }))
  }
  return [{ id: word.id, meaning: word.meaning, isCorrect: true }]
})
const wordOptions = computed(() => {
  const word = currentWord.value
  if (!word) return []
  if (choiceOptions.value.length > 0) {
    return choiceOptions.value.map((item) => ({
      id: item.id,
      word: item.text,
      isCorrect: !!item.correct,
    }))
  }
  return [{ id: word.id, word: word.word, isCorrect: true }]
})
const todayTask = computed(() => {
  if (isReviewByDate.value) {
    return {
      newCount: 0,
      reviewCount: queue.value.length,
      errorCount: localErrorList.value.size || words.value.length,
    }
  }
  return {
    newCount: Number(taskStats.value.newCount || 0),
    reviewCount: Number(taskStats.value.reviewCount || 0),
    errorCount: Number(taskStats.value.errorCount || 0),
  }
})
const rewardTitle = computed(() => currentStreak.value >= 3 ? '节奏很棒' : '继续向前')
const rewardCopy = computed(() => {
  if (currentStreak.value < 3) return `再答对 ${3 - currentStreak.value} 题点亮一颗星。`
  if (nextModeLabel.value) return `本轮完成后解锁 ${nextModeLabel.value}。`
  return '本轮完成后会进入结果总结页。'
})
const overviewRings = computed(() => {
  const total = Math.max(totalCount.value, 1)
  const pendingValue = Math.max(queue.value.length - localErrorList.value.size, 0)
  return [
    {
      key: 'learned',
      label: '已学',
      value: completedCount.value,
      percent: Math.round((completedCount.value / total) * 100),
      color: '#6f63ff',
    },
    {
      key: 'pending',
      label: '待学',
      value: pendingValue,
      percent: Math.round((pendingValue / total) * 100),
      color: '#a0a8bc',
    },
    {
      key: 'error',
      label: '错误',
      value: localErrorList.value.size,
      percent: Math.round((localErrorList.value.size / total) * 100),
      color: '#f1a36c',
    },
  ]
})
const queueVisualItems = computed(() => {
  return words.value.map((item) => {
    const key = String(item.id)
    return {
      ...item,
      state: resolveQueueState(key),
      stateLabel: resolveQueueStateLabel(key),
    }
  })
})
const queueBuckets = computed(() => {
  return queueVisualItems.value.reduce((result, item) => {
    if (item.state === 'mastered') result.mastered += 1
    else if (item.state === 'error') result.error += 1
    else if (item.state === 'review') result.review += 1
    else result.pending += 1
    return result
  }, { pending: 0, review: 0, error: 0, mastered: 0 })
})
const queuePreviewItems = computed(() => queueVisualItems.value.slice(0, 9))
const confusionWords = computed(() => currentWordDetail.value?.confusionWords || [])
const spellingHint = computed(() => currentWordDetail.value?.spellingPattern || '--')
const smartFeedback = computed(() => {
  if (!currentWordDetail.value) {
    return { title: '等待开始', body: '本轮还没有激活单词。', tip: '' }
  }
  const wrongCount = Number(wrongCountMap.value[currentWordDetail.value.id] || 0)
  if (wrongCount > 0) {
    return {
      title: `这个词本轮已经错 ${wrongCount} 次`,
      body: '别急，系统已经把它重新塞回队列里，稍后还会再出现。',
      tip: '先再听一遍发音，再观察拼写骨架。',
    }
  }
  if (normalizedMode.value === 'spell') {
    return {
      title: '现在训练的是拼写输出',
      body: '先想首字母和末尾字母，再补齐中间部分。',
      tip: `提示骨架：${spellingHint.value}`,
    }
  }
  return {
    title: '当前节奏稳定',
    body: '继续保持一屏只做一件事，答完这一题再看下一题。',
    tip: confusionWords.value.length ? `易混词：${confusionWords.value.join(' / ')}` : '如果没把握，就先再听一遍。',
  }
})

function buildSpellingPattern(word) {
  if (!word) return '--'
  const chars = String(word).split('')
  return chars.map((char, index) => {
    if (index === 0 || index === chars.length - 1 || !/[a-z]/i.test(char)) return char
    return '_'
  }).join(' ')
}

function buildConfusionWords(word) {
  const text = String(word?.word || '').toLowerCase()
  return words.value
    .filter((item) => item.id !== word.id)
    .filter((item) => String(item.word || '').toLowerCase().charAt(0) === text.charAt(0))
    .slice(0, 3)
    .map((item) => item.word)
}

function resolveQueueState(wordId) {
  if (!queue.value.some((item) => String(item.id) === wordId)) return 'mastered'
  if (currentWord.value && String(currentWord.value.id) === wordId) return localErrorList.value.has(wordId) ? 'error' : 'pending'
  if (localErrorList.value.has(wordId)) return 'error'
  if (firstAttemptSeen.value.has(wordId)) return 'review'
  return 'pending'
}

function resolveQueueStateLabel(wordId) {
  const state = resolveQueueState(wordId)
  if (state === 'mastered') return '已掌握'
  if (state === 'review') return '待复习'
  if (state === 'error') return '易错'
  return currentWord.value && String(currentWord.value.id) === wordId ? '当前' : '未学'
}

function showFeedback(type, title, body, tip = '') {
  if (feedbackTimer) clearTimeout(feedbackTimer)
  feedbackState.value = { type, title, body, tip }
  feedbackTimer = setTimeout(() => {
    feedbackState.value = null
    feedbackTimer = null
  }, 2200)
}

function shuffleArray(list) {
  const cloned = [...list]
  for (let index = cloned.length - 1; index > 0; index -= 1) {
    const randomIndex = Math.floor(Math.random() * (index + 1))
    ;[cloned[index], cloned[randomIndex]] = [cloned[randomIndex], cloned[index]]
  }
  return cloned
}

async function loadTaskStats() {
  if (!unitId.value || isReviewByDate.value) return
  try {
    const result = await getStudyStats(unitId.value)
    taskStats.value = {
      newCount: Number(result?.newCount || 0),
      reviewCount: Number(result?.reviewCount || 0),
      errorCount: Number(result?.reviewCount || 0),
    }
  } catch (_) {
    taskStats.value = { newCount: 0, reviewCount: 0, errorCount: 0 }
  }
}

async function loadQuestionOptions() {
  if (!currentWord.value || (normalizedMode.value !== 'eng_ch' && normalizedMode.value !== 'ch_eng')) {
    choiceOptions.value = []
    questionOptionsLoading.value = false
    return
  }
  questionOptionsLoading.value = true
  const wordId = currentWord.value.id
  try {
    const result = await getQuestionOptions(wordId, normalizedMode.value)
    if (currentWord.value?.id !== wordId) return
    choiceOptions.value = Array.isArray(result?.options) ? result.options : []
  } catch (_) {
    choiceOptions.value = []
  } finally {
    if (currentWord.value?.id === wordId) questionOptionsLoading.value = false
  }
}

function buildProgressKey() {
  if (!unitId.value) return ''
  return `study_progress_${unitId.value}_${normalizedMode.value}`
}

function normalizeStudyMode(mode) {
  const normalized = String(mode || '').trim().toLowerCase()
  return MODE_ORDER.includes(normalized) ? normalized : ''
}

function getLatestPendingLocalMode() {
  if (isReviewByDate.value || !unitId.value) return ''
  try {
    const raw = localStorage.getItem(STUDY_PROGRESS_LATEST_KEY)
    if (!raw) return ''
    const payload = JSON.parse(raw)
    if (String(payload?.unitId || '') !== String(unitId.value)) return ''
    if (Number(payload?.pendingCount || 0) <= 0) return ''
    if (Date.now() - Number(payload?.timestamp || 0) > 24 * 60 * 60 * 1000) return ''
    return normalizeStudyMode(payload?.mode)
  } catch (_) {
    return ''
  }
}

async function getRemotePendingMode() {
  if (isReviewByDate.value || !unitId.value) return ''
  for (const mode of MODE_ORDER) {
    try {
      const payload = await getStudentStudySession(unitId.value, mode)
      if (!Array.isArray(payload?.words) || payload.words.length === 0) continue
      if (Array.isArray(payload?.queue) && payload.queue.length > 0) return mode
    } catch (_) {}
  }
  return ''
}

function resolveNextIncompleteMode(completion, stats) {
  for (const mode of MODE_ORDER) {
    const stat = stats?.[mode] || {}
    const total = Number(stat?.total || 0)
    const incomplete = Number(stat?.incomplete || 0)
    const completed = Boolean(completion?.[mode]) || (total > 0 && incomplete <= 0)
    if (!completed) return mode
  }
  return MODE_ORDER[MODE_ORDER.length - 1]
}

async function resolveEntryMode() {
  if (isReviewByDate.value) return normalizeStudyMode(route.query.mode) || 'flashcard'
  if (route.query.review === '1' || route.query.weakOnly === '1') {
    return normalizeStudyMode(route.query.mode) || 'flashcard'
  }

  const explicitMode = normalizeStudyMode(route.query.mode)
  if (route.query.restart === '1') return explicitMode || 'flashcard'
  if (explicitMode) return explicitMode

  const localPendingMode = getLatestPendingLocalMode()
  if (localPendingMode) return localPendingMode

  const remotePendingMode = await getRemotePendingMode()
  if (remotePendingMode) return remotePendingMode

  try {
    const [completion, stats] = await Promise.all([
      getUnitModeCompletion(unitId.value),
      getModeStats(unitId.value),
    ])
    return resolveNextIncompleteMode(completion, stats)
  } catch (_) {
    try {
      const stats = await getModeStats(unitId.value)
      return resolveNextIncompleteMode(null, stats)
    } catch (_) {
      return 'flashcard'
    }
  }
}

function saveProgress() {
  if (isReviewByDate.value || !unitId.value || words.value.length === 0) return
  const payload = {
    unitId: unitId.value,
    words: words.value,
    queue: queue.value,
    initialCount: initialCount.value,
    answeredCount: answeredCount.value,
    errorIds: [...localErrorList.value],
    userInputMap: { ...localUserInputMap.value },
    firstAttemptSeenIds: [...firstAttemptSeen.value],
    firstAttemptCorrectIds: [...firstAttemptCorrect.value],
    correctAttemptsCount: correctAttemptsCount.value,
    wrongAttemptsCount: wrongAttemptsCount.value,
    currentStreak: currentStreak.value,
    wrongCountMap: { ...wrongCountMap.value },
    startedAt: new Date(sessionStartAt.value).toISOString().slice(0, 19),
    timestamp: Date.now(),
  }
  try {
    localStorage.setItem(buildProgressKey(), JSON.stringify(payload))
    localStorage.setItem(STUDY_PROGRESS_LATEST_KEY, JSON.stringify({
      key: buildProgressKey(),
      unitId: unitId.value,
      unitName: unitName.value,
      mode: normalizedMode.value,
      pendingCount: queue.value.length,
      timestamp: payload.timestamp,
    }))
  } catch (_) {}

  if (saveSessionTimer) clearTimeout(saveSessionTimer)
  saveSessionTimer = setTimeout(async () => {
    try {
      await saveStudentStudySession(unitId.value, normalizedMode.value, payload)
    } catch (_) {}
  }, 250)
}

function loadSavedProgress() {
  try {
    const raw = localStorage.getItem(buildProgressKey())
    if (!raw) return false
    const payload = JSON.parse(raw)
    if (String(payload.unitId) !== String(unitId.value)) return false
    if (Date.now() - Number(payload.timestamp || 0) > 24 * 60 * 60 * 1000) return false
    words.value = Array.isArray(payload.words) ? payload.words : []
    queue.value = Array.isArray(payload.queue) ? payload.queue : []
    initialCount.value = Number(payload.initialCount || words.value.length || 0)
    answeredCount.value = Number(payload.answeredCount || 0)
    localErrorList.value = new Set((payload.errorIds || []).map((item) => String(item)))
    localUserInputMap.value = payload.userInputMap || {}
    firstAttemptSeen.value = new Set((payload.firstAttemptSeenIds || []).map((item) => String(item)))
    firstAttemptCorrect.value = new Set((payload.firstAttemptCorrectIds || []).map((item) => String(item)))
    correctAttemptsCount.value = Number(payload.correctAttemptsCount || 0)
    wrongAttemptsCount.value = Number(payload.wrongAttemptsCount || 0)
    currentStreak.value = Number(payload.currentStreak || 0)
    wrongCountMap.value = payload.wrongCountMap || {}
    sessionStartAt.value = payload.startedAt ? new Date(payload.startedAt).getTime() : Date.now()
    return words.value.length > 0
  } catch (_) {
    return false
  }
}

async function loadRemoteSession() {
  if (isReviewByDate.value || !unitId.value) return false
  try {
    const payload = await getStudentStudySession(unitId.value, normalizedMode.value)
    if (!Array.isArray(payload?.words) || payload.words.length === 0) return false
    words.value = payload.words
    queue.value = Array.isArray(payload.queue) ? payload.queue : [...payload.words]
    initialCount.value = Number(payload.initialCount || payload.words.length || 0)
    answeredCount.value = Number(payload.answeredCount || 0)
    localErrorList.value = new Set((payload.errorIds || []).map((item) => String(item)))
    localUserInputMap.value = payload.userInputMap || {}
    firstAttemptSeen.value = new Set((payload.firstAttemptSeenIds || []).map((item) => String(item)))
    firstAttemptCorrect.value = new Set((payload.firstAttemptCorrectIds || []).map((item) => String(item)))
    correctAttemptsCount.value = Number(payload.correctAttemptsCount || 0)
    wrongAttemptsCount.value = Number(payload.wrongAttemptsCount || 0)
    currentStreak.value = 0
    wrongCountMap.value = {}
    sessionStartAt.value = payload.startedAt ? new Date(payload.startedAt).getTime() : Date.now()
    return true
  } catch (_) {
    return false
  }
}

function clearProgress() {
  try {
    localStorage.removeItem(buildProgressKey())
  } catch (_) {}
  if (!isReviewByDate.value && unitId.value) {
    clearStudentStudySession(unitId.value, normalizedMode.value).catch(() => {})
  }
}

async function fetchWords() {
  loading.value = true
  studyMode.value = await resolveEntryMode()
  try {
    if (!isReviewByDate.value && loadSavedProgress()) {
      loading.value = false
      saveProgress()
      return
    }
    if (!isReviewByDate.value && await loadRemoteSession()) {
      loading.value = false
      return
    }

    let list = []
    if (isReviewByDate.value && reviewDate.value) {
      list = await getWordsByErrorDate(reviewDate.value)
      const errorType = route.query.errorType
      if (errorType && Array.isArray(list) && list.length > 0) {
        const errorList = await getErrorsByDate(reviewDate.value)
        const ids = (errorList || [])
          .filter((item) => String(item.errorType || item.error_type || '').toUpperCase() === String(errorType).toUpperCase())
          .map((item) => item.wordId ?? item.word_id)
        list = list.filter((item) => ids.includes(item.id))
      }
    } else if (unitId.value && route.query.weakOnly === '1') {
      list = await getWeakWords(unitId.value, 50)
    } else if (unitId.value && route.query.review === '1') {
      list = await getUnitWords(unitId.value)
      if (!Array.isArray(list) || list.length === 0) {
        list = await getNextList(unitId.value, 500, normalizedMode.value)
      }
      list = shuffleArray(Array.isArray(list) ? list : [])
    } else if (unitId.value) {
      try {
        const stats = await getModeStats(unitId.value)
        if (stats?.[normalizedMode.value]?.incomplete > 0) {
          list = await getIncompleteWords(unitId.value, normalizedMode.value, Math.min(stats[normalizedMode.value].incomplete, 100))
        }
      } catch (_) {}
      if (!Array.isArray(list) || list.length === 0) {
        list = await getNextList(unitId.value, 20, normalizedMode.value)
      }
    }

    words.value = Array.isArray(list) ? list : []
    queue.value = [...words.value]
    initialCount.value = words.value.length
    answeredCount.value = 0
    localErrorList.value = new Set()
    localUserInputMap.value = {}
    firstAttemptSeen.value = new Set()
    firstAttemptCorrect.value = new Set()
    correctAttemptsCount.value = 0
    wrongAttemptsCount.value = 0
    currentStreak.value = 0
    wrongCountMap.value = {}
    sessionStartAt.value = Date.now()
    saveProgress()
  } catch (error) {
    console.error(error)
    words.value = []
    queue.value = []
  } finally {
    loading.value = false
  }
}

function advanceQueue(removeCurrent) {
  const head = queue.value[0]
  if (!head) return
  answeredCount.value += 1
  if (removeCurrent) {
    queue.value = queue.value.slice(1)
  } else {
    queue.value = [...queue.value.slice(1), head]
  }
  saveProgress()
}

function advanceQueueWrong() {
  const head = queue.value[0]
  if (!head) return
  answeredCount.value += 1
  const rest = queue.value.slice(1)
  queue.value = [rest[0], head, ...rest.slice(1)].filter(Boolean)
  saveProgress()
}

function normalizeWrongPayload(payload) {
  if (payload && typeof payload === 'object') return payload
  if (typeof payload === 'string') return { userInput: payload }
  return {}
}

function feedbackTypeForWrong(mode) {
  return mode === 'spell' ? 'SPELLING_ERROR' : 'DONT_KNOW'
}

function errorTypeForWrong(mode) {
  if (mode === 'flashcard') return 'FLASHCARD'
  if (mode === 'eng_ch') return 'ENG_TO_CH'
  if (mode === 'ch_eng') return 'CH_TO_ENG'
  if (mode === 'spell') return 'SPELLING_ERROR'
  return undefined
}

async function handleNext() {
  const word = currentWord.value
  if (!word) return
  const id = String(word.id)
  if (!firstAttemptSeen.value.has(id)) {
    firstAttemptSeen.value = new Set([...firstAttemptSeen.value, id])
    firstAttemptCorrect.value = new Set([...firstAttemptCorrect.value, id])
  }
  try {
    const result = await submitFeedback(word.id, 'KNOW', normalizedMode.value, undefined, '')
    correctAttemptsCount.value += 1
    currentStreak.value += 1
    const removeCurrent = !!result?.wordCompleted
    showFeedback(
      'success',
      currentStreak.value > 1 ? `太棒了，连续答对 ${currentStreak.value} 题` : '答对了',
      removeCurrent ? `${word.word} 已进入已掌握。` : `${word.word} 还会在本轮再出现一次，继续巩固。`,
      removeCurrent ? '进度条已经向前推进了一步。' : '下一次见到它时会更容易想起来。'
    )
    advanceQueue(removeCurrent)
  } catch (error) {
    console.error(error)
    ElMessage.error(error?.message || '提交学习结果失败')
  }
}

function handleWrong(payload) {
  if (wrongProcessing.value || !currentWord.value) return
  wrongProcessing.value = true

  const word = currentWord.value
  const id = String(word.id)
  const wrongPayload = normalizeWrongPayload(payload)
  const sessionWrongCount = Number(wrongCountMap.value[id] || 0) + 1

  if (!firstAttemptSeen.value.has(id)) {
    firstAttemptSeen.value = new Set([...firstAttemptSeen.value, id])
  }

  wrongCountMap.value = { ...wrongCountMap.value, [id]: sessionWrongCount }
  localErrorList.value = new Set([...localErrorList.value, id])
  wrongAttemptsCount.value += 1
  currentStreak.value = 0

  if (wrongPayload.userInput) {
    localUserInputMap.value = { ...localUserInputMap.value, [id]: String(wrongPayload.userInput) }
  }

  showFeedback(
    'error',
    `${word.word} 先回队列再练一次`,
    wrongPayload.reason || `正确答案：${normalizedMode.value === 'eng_ch' ? word.meaning : word.word}`,
    wrongPayload.suggestion || '别急，这个词稍后会再次出现。'
  )

  const queueSnapshot = [...queue.value]
  advanceQueueWrong()
  toast.show()
  wrongProcessing.value = false

  submitFeedback(
    word.id,
    feedbackTypeForWrong(normalizedMode.value),
    normalizedMode.value,
    errorTypeForWrong(normalizedMode.value),
    localUserInputMap.value[id] || '',
    sessionWrongCount
  ).catch((error) => {
    console.error(error)
    queue.value = queueSnapshot
    saveProgress()
    ElMessage.error(error?.message || '提交学习结果失败')
  })
}

function goBackFromStudy(clearSaved = false) {
  if (clearSaved) clearProgress()
  else if (!isReviewByDate.value) saveProgress()

  if (isReviewByDate.value) {
    router.push(reviewDate.value ? { name: 'StudentReviewByDate', params: { date: reviewDate.value } } : '/student/review')
    return
  }
  router.push('/student/home')
}

function confirmExit() {
  if (queue.value.length === 0 || words.value.length === 0) {
    goBackFromStudy()
    return
  }
  ElMessageBox.confirm('当前学习进度还未提交，确定要中途退出吗？', '提示', {
    confirmButtonText: '强制退出',
    cancelButtonText: '继续学习',
    type: 'warning',
  }).then(() => {
    goBackFromStudy()
  }).catch(() => {})
}

async function submitSessionAndExit() {
  submitLoading.value = true
  try {
    if (isReviewByDate.value && reviewDate.value) {
      await markErrorReviewComplete(reviewDate.value, route.query.errorType)
      clearProgress()
      router.push({ name: 'StudentReviewByDate', params: { date: reviewDate.value } })
      return
    }
    if (!isReviewByDate.value && route.query.review !== '1' && route.query.weakOnly !== '1' && unitId.value) {
      await completeMode(unitId.value, normalizedMode.value)
      await persistStageResult()
      clearProgress()
      router.push({ name: 'StudentResult', params: { unitId: unitId.value, stage: normalizedMode.value } })
      return
    }
    goBackFromStudy(true)
  } catch (error) {
    ElMessage.error(error?.message || '提交完成状态失败')
  } finally {
    submitLoading.value = false
  }
}

async function goNextMode() {
  const nextMode = normalizedMode.value === 'flashcard'
    ? 'eng_ch'
    : normalizedMode.value === 'eng_ch'
      ? 'ch_eng'
      : normalizedMode.value === 'ch_eng'
        ? 'spell'
        : ''
  if (!nextMode || !unitId.value) return

  submitLoading.value = true
  try {
    await completeMode(unitId.value, normalizedMode.value)
    clearProgress()
    router.push({
      name: 'StudentStudy',
      params: { unitId: unitId.value },
      query: { mode: nextMode, unitName: unitName.value || undefined },
    })
  } catch (error) {
    ElMessage.error(error?.message || '切换下一阶段失败')
  } finally {
    submitLoading.value = false
  }
}

async function persistStageResult() {
  if (!unitId.value) return
  const payload = {
    unitId: unitId.value,
    stage: normalizedMode.value,
    title: summaryTitle.value,
    unitName: unitName.value || '',
    bookName: bookName.value || '',
    modeLabel: currentModeMeta.value.shortLabel,
    initialCount: totalCount.value,
    correctAttemptsCount: correctAttemptsCount.value,
    wrongAttemptsCount: wrongAttemptsCount.value,
    firstRoundCorrectCount: firstRoundCorrectCount.value,
    stabilizedCount: stabilizedCount.value,
    elapsedLabel: elapsedLabel.value,
    starReward: starReward.value,
    nextMode: normalizedMode.value === 'flashcard' ? 'eng_ch' : normalizedMode.value === 'eng_ch' ? 'ch_eng' : normalizedMode.value === 'ch_eng' ? 'spell' : '',
    durationSeconds: Math.max(Math.round((Date.now() - sessionStartAt.value) / 1000), 0),
  }
  sessionStorage.setItem(RESULT_SUMMARY_KEY, JSON.stringify(payload))
  await saveStudentStageResult({
    unitId: payload.unitId,
    stage: payload.stage,
    bookName: payload.bookName,
    unitName: payload.unitName,
    totalQuestions: payload.initialCount,
    correctAttempts: payload.correctAttemptsCount,
    wrongAttempts: payload.wrongAttemptsCount,
    firstRoundCorrect: payload.firstRoundCorrectCount,
    stabilizedCount: payload.stabilizedCount,
    durationSeconds: payload.durationSeconds,
    starReward: payload.starReward,
  })
}

function toggleAudio() {
  audioEnabled.value = !audioEnabled.value
  localStorage.setItem(AUDIO_PREF_KEY, audioEnabled.value ? '1' : '0')
}

function syncFullscreenState() {
  isFullscreen.value = !!document.fullscreenElement
}

async function toggleFullscreen() {
  try {
    if (document.fullscreenElement) await document.exitFullscreen()
    else await document.documentElement.requestFullscreen()
    syncFullscreenState()
  } catch (_) {}
}

function togglePause() {
  isPaused.value = !isPaused.value
  if (!isPaused.value && currentWordDetail.value) {
    playWordAudio(currentWordDetail.value, { enabled: audioEnabled.value, force: false })
  }
}

async function initializePage() {
  isPaused.value = false
  isSettingsOpen.value = false
  isInsightOpen.value = false
  audioEnabled.value = localStorage.getItem(AUDIO_PREF_KEY) !== '0'
  if (!isReviewByDate.value && unitId.value) {
    saveStudentLastStudy({ unitId: Number(unitId.value) }).catch(() => {})
  }
  await Promise.all([loadTaskStats(), fetchWords()])
}

watch(
  () => [currentWord.value?.id, normalizedMode.value],
  () => { loadQuestionOptions() },
  { immediate: true }
)

watch(
  () => [route.name, route.params.unitId, route.params.date, route.query.review, route.query.mode, route.query.errorType],
  () => { initializePage() },
  { immediate: false }
)

onMounted(() => {
  initializePage()
  window.addEventListener('beforeunload', saveProgress)
  document.addEventListener('fullscreenchange', syncFullscreenState)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', saveProgress)
  document.removeEventListener('fullscreenchange', syncFullscreenState)
  if (saveSessionTimer) clearTimeout(saveSessionTimer)
  if (feedbackTimer) clearTimeout(feedbackTimer)
  saveProgress()
})
</script>

<style scoped>
.slide-left-enter-active,
.slide-left-leave-active {
  transition: all 0.22s ease;
}

.slide-left-enter-from {
  opacity: 0;
  transform: translateX(28px);
}

.slide-left-leave-to {
  opacity: 0;
  transform: translateX(-28px);
}

.feedback-slide-enter-active,
.feedback-slide-leave-active {
  transition: all 0.2s ease;
}

.feedback-slide-enter-from,
.feedback-slide-leave-to {
  opacity: 0;
  transform: translateY(12px);
}

.study-page {
  min-height: 100vh;
  background: #f8f9fe;
  padding: 20px 22px;
  font-family: Inter, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
}

.study-shell {
  width: 100%;
  display: grid;
  gap: 18px;
  min-height: calc(100vh - 40px);
  grid-template-rows: auto minmax(0, 1fr);
}

.topbar {
  display: grid;
  grid-template-columns: minmax(240px, auto) minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
  padding: 14px 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(99, 115, 129, 0.12);
  box-shadow: 0 18px 42px rgba(34, 47, 78, 0.08);
}

.topbar-left,
.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.settings-wrap {
  position: relative;
}

.topbar-progress {
  display: flex;
  align-items: center;
}

.topbar-btn,
.icon-btn {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  border: 1px solid rgba(99, 115, 129, 0.12);
  background: #f7f8fe;
  color: #4c5566;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: background 0.18s ease, transform 0.18s ease;
}

.topbar-btn:hover,
.icon-btn:hover {
  background: #edf1ff;
  transform: translateY(-1px);
}

.back-btn {
  background: #eef2ff;
}

.unit-copy p,
.unit-copy h1,
.unit-copy span {
  margin: 0;
}

.unit-copy p {
  color: #6b7280;
  font-size: 0.78rem;
}

.unit-copy h1 {
  margin-top: 3px;
  font-size: clamp(1.34rem, 1.8vw, 1.68rem);
  color: #172033;
  line-height: 1.1;
}

.unit-copy span {
  display: inline-block;
  margin-top: 3px;
  color: #4f5d75;
  font-weight: 600;
  font-size: 0.82rem;
}

.progress-track {
  height: 6px;
  width: 100%;
  border-radius: 999px;
  background: #e7eaf5;
  overflow: hidden;
}

.progress-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #6659ff 0%, #8b7dff 100%);
}

.study-layout {
  display: grid;
  grid-template-columns: 1fr;
  gap: 18px;
  align-items: start;
  min-height: 0;
}

.center-panel {
  min-width: 0;
  display: grid;
  gap: 18px;
}

.training-card,
.state-card,
.sidebar-card {
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(99, 115, 129, 0.12);
  box-shadow: 0 24px 60px rgba(34, 47, 78, 0.08);
}

.training-card {
  position: relative;
  overflow: hidden;
  padding: 16px;
}

.sidebar-card {
  padding: 12px;
}

.info-strip {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(0, 1.2fr) minmax(0, 1fr);
  gap: 10px;
  align-items: stretch;
}

.info-strip .sidebar-card {
  height: 100%;
}

.panel-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 6px;
}

.panel-kicker {
  color: #667085;
  font-size: 0.66rem;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.panel-head strong {
  color: #172033;
  font-size: 0.8rem;
}

.stat-row,
.shortcut-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 24px;
}

.stat-row span,
.shortcut-row span {
  color: #5f6b80;
  font-size: 0.74rem;
}

.stat-row strong {
  color: #1d2739;
  font-size: 0.78rem;
}

.training-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.training-context {
  color: #667085;
  font-size: 0.72rem;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
}

.training-progress {
  color: #172033;
  font-size: 0.86rem;
  line-height: 1;
  white-space: nowrap;
}

.question-loading,
.state-card {
  text-align: center;
}

.question-loading {
  padding: 20px 18px;
  border-radius: 18px;
  background: #f5f7fe;
  margin-bottom: 16px;
  color: #556070;
  display: grid;
  gap: 8px;
  font-size: 0.9rem;
}

.pause-mask {
  position: absolute;
  inset: 0;
  background: rgba(248, 249, 254, 0.92);
  display: grid;
  place-items: center;
  text-align: center;
  gap: 12px;
  padding: 28px;
}

.pause-mask strong {
  font-size: 1.4rem;
  color: #172033;
}

.pause-mask p {
  margin: 0;
  color: #667085;
}

.pause-actions,
.summary-actions {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.state-card {
  display: grid;
  gap: 12px;
  justify-items: center;
  padding: 40px 24px;
}

.state-card h2 {
  margin: 0;
  color: #172033;
}

.state-card p {
  margin: 0;
  color: #667085;
  line-height: 1.7;
  font-size: 0.94rem;
}

.state-icon {
  font-size: 1.8rem;
  color: #6659ff;
}

.state-icon.success {
  color: #16a34a;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.summary-item {
  padding: 14px;
  border-radius: 18px;
  background: #f7f9fc;
  text-align: left;
}

.summary-item span {
  display: block;
  color: #667085;
  font-size: 0.78rem;
}

.summary-item strong {
  display: block;
  margin-top: 6px;
  color: #172033;
  font-size: 0.98rem;
}

.action-btn {
  min-height: 48px;
  padding: 0 18px;
  border-radius: 16px;
  border: 1px solid transparent;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  font-weight: 800;
  font-size: 0.96rem;
}

.action-btn.primary {
  background: linear-gradient(135deg, #6659ff 0%, #8b7dff 100%);
  color: #fff;
}

.action-btn.secondary {
  background: #f5f6fb;
  color: #344054;
  border-color: rgba(99, 115, 129, 0.12);
}

.overview-card {
  display: grid;
  gap: 10px;
}

.queue-card,
.word-detail-card {
  display: grid;
  gap: 8px;
}

.ring-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.ring-card {
  display: grid;
  justify-items: center;
  gap: 4px;
  padding: 6px 4px;
  border-radius: 14px;
  background: #f7f8fe;
}

.ring-chart {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background:
    radial-gradient(circle at center, #ffffff 0 56%, transparent 57%),
    conic-gradient(var(--ring-color) 0 var(--ring-fill), #e7eaf2 var(--ring-fill) 100%);
  display: grid;
  place-items: center;
}

.ring-chart span {
  font-size: 0.76rem;
  font-weight: 800;
  color: #1f2937;
}

.ring-card strong {
  font-size: 0.68rem;
  color: #4f5b6d;
}

.overview-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
}

.metric-chip {
  padding: 7px 8px;
  border-radius: 12px;
  background: #f5f6fb;
}

.metric-chip span {
  display: block;
  color: #707b90;
  font-size: 0.62rem;
}

.metric-chip strong {
  display: block;
  margin-top: 3px;
  color: #1c2435;
  font-size: 0.8rem;
}

.overview-copy {
  margin: 0;
  color: #5c6475;
  line-height: 1.35;
  font-size: 0.74rem;
}

.word-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  margin-top: 8px;
}

.word-grid-item {
  position: relative;
  min-height: 40px;
  padding: 12px 7px 7px;
  border-radius: 12px;
  background: #f7f8fe;
  display: grid;
  place-items: center;
  text-align: center;
}

.word-grid-item strong {
  font-size: 0.72rem;
  color: #1d2739;
  line-height: 1.25;
  text-align: center;
}

.status-dot {
  position: absolute;
  left: 8px;
  top: 5px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #a0a8bc;
}

.status-dot.is-pending {
  background: #7d88a3;
}

.status-dot.is-review {
  background: #8a7cff;
}

.status-dot.is-error {
  background: #f1a36c;
}

.status-dot.is-mastered {
  background: #5fc78d;
}

.queue-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 6px;
}

.mini-tag {
  min-height: 20px;
  padding: 0 7px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  background: #f1f4fb;
  color: #576277;
  font-size: 0.64rem;
  font-weight: 700;
}

.mini-tag.review {
  background: #f1edff;
  color: #6f63ff;
}

.mini-tag.error {
  background: #fff4ea;
  color: #d17b3f;
}

.mini-tag.success {
  background: #edf9f2;
  color: #35a86a;
}

.word-info {
  display: grid;
  gap: 6px;
}

.word-note {
  padding: 8px 10px;
  border-radius: 12px;
  background: #f7f8fe;
}

.word-note span {
  display: block;
  color: #667085;
  font-size: 0.64rem;
}

.word-note p {
  margin: 4px 0 0;
  color: #344054;
  line-height: 1.35;
  font-size: 0.72rem;
}

.insight-wrap {
  position: relative;
  justify-self: end;
  margin-top: 2px;
}

.insight-trigger {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid rgba(99, 115, 129, 0.12);
  background: #f6f7fb;
  color: #4c5566;
  display: grid;
  place-items: center;
  cursor: pointer;
  font-weight: 700;
}

.settings-popover,
.insight-popover {
  width: min(280px, 100%);
  padding: 16px;
  border-radius: 18px;
  background: #ffffff;
  border: 1px solid rgba(99, 115, 129, 0.12);
  box-shadow: 0 18px 40px rgba(34, 47, 78, 0.12);
  z-index: 20;
}

.settings-popover {
  position: absolute;
  right: 0;
  top: calc(100% + 10px);
}

.settings-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.settings-head strong {
  color: #172033;
  font-size: 0.92rem;
}

.settings-head span {
  color: #667085;
  font-size: 0.76rem;
}

.settings-item {
  width: 100%;
  margin-top: 14px;
  min-height: 44px;
  padding: 0 14px;
  border-radius: 14px;
  border: 1px solid rgba(99, 115, 129, 0.12);
  background: #f7f8fe;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  cursor: pointer;
}

.settings-item span {
  color: #4f5b6d;
  font-size: 0.84rem;
}

.settings-item strong {
  color: #6659ff;
  font-size: 0.84rem;
}

.settings-note {
  margin-top: 12px;
  color: #6f7a8f;
  font-size: 0.76rem;
  line-height: 1.6;
}

.insight-popover {
  position: absolute;
  right: 0;
  bottom: calc(100% + 10px);
}

.insight-popover strong,
.insight-popover p,
.insight-popover small {
  display: block;
}

.insight-popover p {
  margin: 8px 0 0;
  color: #566173;
  line-height: 1.6;
  font-size: 0.84rem;
}

.insight-popover small {
  margin-top: 8px;
  color: #778196;
  line-height: 1.5;
  font-size: 0.76rem;
}

.feedback-toast {
  position: fixed;
  left: 50%;
  bottom: 24px;
  transform: translateX(-50%);
  width: min(640px, calc(100vw - 32px));
  padding: 18px 20px;
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(20, 30, 60, 0.16);
  z-index: 60;
}

.feedback-toast strong,
.feedback-toast p,
.feedback-toast small {
  display: block;
}

.feedback-toast p {
  margin: 8px 0 0;
  line-height: 1.7;
}

.feedback-toast small {
  margin-top: 8px;
  opacity: 0.9;
}

.feedback-toast.success {
  background: #edf7f1;
  color: #21653f;
}

.feedback-toast.error {
  background: #fff4ea;
  color: #9a3412;
}

@media (max-width: 1180px) {
  .study-shell {
    min-height: auto;
    grid-template-rows: none;
  }

  .info-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .word-detail-card {
    grid-column: 1 / -1;
  }
}

@media (max-width: 900px) {
  .study-page {
    padding: 14px;
  }

  .topbar {
    grid-template-columns: 1fr;
  }

  .topbar-left,
  .topbar-right,
  .topbar-progress,
  .training-head,
  .info-strip,
  .summary-grid,
  .overview-metrics {
    grid-template-columns: 1fr;
    display: grid;
  }

  .training-progress {
    text-align: left;
  }

  .word-grid,
  .ring-row {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .insight-popover {
    position: static;
    width: 100%;
  }

  .settings-popover {
    position: static;
    width: 100%;
    margin-top: 10px;
  }
}
</style>
