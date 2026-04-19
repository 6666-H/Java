<template>
  <section class="mode-body" :class="{ 'is-shake': isShake, 'is-paused': paused }">
    <div class="prompt-hero">
      <div class="prompt-main">
        <div class="prompt-badge">看中文，选英语</div>
        <h2>{{ word.meaning }}</h2>
        <p>{{ word.exampleZh || '先想英文，再从选项里确认。' }}</p>
      </div>

      <button type="button" class="audio-button" :disabled="paused" @click="playAudio(true)">
        <el-icon><Microphone /></el-icon>
        <span>听发音</span>
      </button>
    </div>

    <div class="tip-panel">
      <div class="tip-card">
        <span>词性</span>
        <strong>{{ word.pos || '常用词' }}</strong>
      </div>
      <div class="tip-card">
        <span>拼写提示</span>
        <strong>{{ spellingCue }}</strong>
      </div>
      <div class="tip-card" v-if="word.confusionWords?.length">
        <span>易混词</span>
        <strong>{{ word.confusionWords.slice(0, 2).join(' / ') }}</strong>
      </div>
    </div>

    <div class="prompt-strip">
      <span>想好英文后，再点击选项确认。</span>
      <span v-if="awaitingCorrectConfirm" class="danger-copy">请点击绿色正确答案，完成这次纠正。</span>
    </div>

    <div class="option-grid">
      <button
        v-for="(opt, index) in options"
        :key="opt.id"
        type="button"
        class="option-card"
        :class="[resolveOptionClass(opt), { 'is-focus': focusIndex === index }]"
        :disabled="paused"
        @click="handleSelect(opt)"
      >
        <span class="option-index">{{ index + 1 }}</span>
        <strong>{{ opt.word }}</strong>
      </button>
    </div>

    <div v-if="awaitingCorrectConfirm" class="feedback-panel">
      <strong>正确答案：{{ correctWord }}</strong>
      <p>{{ word.phonetic || '先听一遍，再看一遍正确拼写。' }}</p>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { Microphone } from '@element-plus/icons-vue'
import { playSuccessTone, playWordAudio, vibrateLight } from './studyPlayback'

const props = defineProps({
  word: { type: Object, default: () => ({}) },
  options: { type: Array, default: () => [] },
  audioEnabled: { type: Boolean, default: true },
  paused: { type: Boolean, default: false },
})

const emit = defineEmits(['next', 'wrong'])

const isShake = ref(false)
const selectedId = ref(null)
const awaitingCorrectConfirm = ref(false)
const focusIndex = ref(0)
const emitWrongLock = ref(false)

const correctWord = computed(() => props.options?.find(item => item.isCorrect)?.word || '')
const spellingCue = computed(() => {
  const word = String(props.word?.word || '')
  if (!word) return '先听音，再想字母'
  if (/(.)\1/i.test(word)) {
    const doubled = word.match(/(.)\1/i)?.[1] || ''
    return doubled ? `注意双写 ${doubled.toLowerCase()}` : '注意重复字母'
  }
  if (word.length >= 8) return '先分段，再记整词'
  return `${word.slice(0, 1)}...${word.slice(-1)}`
})

watch(
  () => [props.word?.id, props.options?.length],
  () => {
    resetState()
    playAudio(false)
  },
  { immediate: true }
)

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})

function resetState() {
  isShake.value = false
  selectedId.value = null
  awaitingCorrectConfirm.value = false
  focusIndex.value = 0
  emitWrongLock.value = false
}

function playAudio(force = false) {
  return playWordAudio(props.word, { enabled: props.audioEnabled, force })
}

function resolveOptionClass(opt) {
  if (awaitingCorrectConfirm.value) {
    if (opt.isCorrect) return 'is-correct'
    if (selectedId.value === opt.id) return 'is-wrong'
    return 'is-muted'
  }
  if (selectedId.value === opt.id) return 'is-selected'
  return 'is-idle'
}

function selectedWrongWord() {
  return props.options?.find(item => item.id === selectedId.value && !item.isCorrect)?.word || ''
}

function handleSelect(opt) {
  if (!opt || props.paused) return

  if (awaitingCorrectConfirm.value) {
    if (!opt.isCorrect || emitWrongLock.value) return
    emitWrongLock.value = true
    selectedId.value = opt.id
    window.setTimeout(() => {
      emit('wrong', {
        userInput: selectedWrongWord(),
        reason: `这次选成了“${selectedWrongWord() || '其他英文'}”，正确答案是“${correctWord.value}”。`,
        suggestion: '先听发音，再观察首字母和末尾字母。',
      })
    }, 120)
    return
  }

  selectedId.value = opt.id
  if (opt.isCorrect) {
    playSuccessTone(props.audioEnabled)
    window.setTimeout(() => emit('next'), 180)
    return
  }

  awaitingCorrectConfirm.value = true
  isShake.value = true
  vibrateLight(140)
  window.setTimeout(() => {
    isShake.value = false
    playAudio(false)
  }, 180)
}

function handleKeydown(event) {
  if (props.paused || !props.options?.length) return

  const active = document.activeElement
  const tag = active?.tagName?.toLowerCase()
  if (tag === 'input' || tag === 'textarea') return

  if (event.code === 'Space') {
    event.preventDefault()
    playAudio(true)
    return
  }

  if (/^[1-4]$/.test(event.key)) {
    const index = Number(event.key) - 1
    if (props.options[index]) {
      focusIndex.value = index
      handleSelect(props.options[index])
    }
    return
  }

  if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
    event.preventDefault()
    focusIndex.value = (focusIndex.value + 1) % props.options.length
    return
  }

  if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
    event.preventDefault()
    focusIndex.value = (focusIndex.value - 1 + props.options.length) % props.options.length
    return
  }

  if (event.key === 'Enter') {
    event.preventDefault()
    handleSelect(props.options[focusIndex.value])
  }
}
</script>

<style scoped>
.mode-body {
  display: grid;
  gap: 22px;
}

.is-paused {
  pointer-events: none;
}

.is-shake {
  animation: shake 0.22s linear;
}

.prompt-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.prompt-main h2 {
  margin: 10px 0 0;
  font-size: clamp(2rem, 3vw, 2.8rem);
  line-height: 1.15;
  color: #162033;
}

.prompt-main p {
  margin: 10px 0 0;
  color: #5f6c84;
  font-size: 1rem;
  font-weight: 600;
}

.prompt-badge {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  background: #fff3e8;
  color: #c56a13;
  font-size: 0.82rem;
  font-weight: 800;
}

.audio-button {
  min-height: 56px;
  padding: 0 18px;
  border: 1px solid rgba(197, 106, 19, 0.16);
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #fff8f0 100%);
  color: #c56a13;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-weight: 700;
}

.tip-panel {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.tip-card {
  padding: 16px 18px;
  border-radius: 18px;
  background: #fffaf5;
  border: 1px solid rgba(95, 108, 132, 0.12);
}

.tip-card span {
  display: block;
  color: #7a6f61;
  font-size: 0.8rem;
}

.tip-card strong {
  display: block;
  margin-top: 8px;
  color: #2b3140;
  font-size: 1rem;
  line-height: 1.45;
}

.prompt-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 18px;
  border-radius: 18px;
  background: rgba(255, 247, 237, 0.86);
  border: 1px solid rgba(95, 108, 132, 0.12);
  color: #745f48;
  font-size: 0.9rem;
}

.danger-copy {
  color: #be6b16;
  font-weight: 700;
}

.option-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.option-card {
  min-height: 104px;
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(95, 108, 132, 0.12);
  background: #fff;
  display: grid;
  align-content: space-between;
  gap: 14px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.option-card:hover:not(:disabled),
.option-card.is-focus {
  transform: translateY(-1px);
  border-color: rgba(197, 106, 19, 0.24);
  box-shadow: 0 18px 36px rgba(197, 106, 19, 0.1);
}

.option-card:disabled,
.audio-button:disabled {
  opacity: 0.56;
  cursor: not-allowed;
}

.option-index {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: #fff3e8;
  color: #c56a13;
  font-weight: 800;
}

.option-card strong {
  font-size: 1.08rem;
  line-height: 1.45;
  color: #1e2b45;
}

.is-selected {
  border-color: rgba(197, 106, 19, 0.22);
  background: #fffaf5;
}

.is-correct {
  border-color: rgba(22, 163, 74, 0.24);
  background: #f0fdf4;
}

.is-correct .option-index {
  background: rgba(22, 163, 74, 0.12);
  color: #15803d;
}

.is-wrong {
  border-color: rgba(234, 88, 12, 0.24);
  background: #fff7ed;
}

.is-wrong .option-index {
  background: rgba(234, 88, 12, 0.12);
  color: #c55a11;
}

.is-muted {
  opacity: 0.72;
}

.feedback-panel {
  padding: 18px 20px;
  border-radius: 20px;
  background: #fff7ed;
  border: 1px solid rgba(234, 88, 12, 0.16);
}

.feedback-panel strong {
  display: block;
  color: #9a3412;
  font-size: 1.02rem;
}

.feedback-panel p {
  margin: 8px 0 0;
  color: #a16207;
  line-height: 1.65;
  font-size: 0.92rem;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-6px); }
  75% { transform: translateX(6px); }
}

@media (max-width: 860px) {
  .prompt-hero,
  .prompt-strip,
  .option-grid,
  .tip-panel {
    grid-template-columns: 1fr;
    display: grid;
  }
}
</style>
