<template>
  <section class="mode-body" :class="{ 'is-shake': isShake, 'is-paused': paused }">
    <div class="prompt-hero">
      <div class="prompt-main">
        <div class="prompt-badge">看英语，选中文</div>
        <h2>{{ word.word }}</h2>
        <p>{{ word.phonetic || '先听发音，再判断中文意思' }}</p>
      </div>

      <button type="button" class="audio-button" :disabled="paused" @click="playAudio(true)">
        <el-icon><Microphone /></el-icon>
        <span>再听一遍</span>
      </button>
    </div>

    <div v-if="word.imageUrl" class="context-image">
      <img :src="word.imageUrl" :alt="word.word || 'word image'" />
    </div>

    <div class="prompt-strip">
      <span>请选出最准确的中文释义。</span>
      <span v-if="awaitingCorrectConfirm" class="danger-copy">先点绿色正确答案，再继续下一题。</span>
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
        <strong>{{ opt.meaning }}</strong>
      </button>
    </div>

    <div v-if="awaitingCorrectConfirm" class="feedback-panel">
      <strong>正确释义：{{ correctMeaning }}</strong>
      <p>{{ word.exampleZh || word.exampleSentence || '先把正确中文和英文连起来记一遍。' }}</p>
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

const correctMeaning = computed(() => props.options?.find(item => item.isCorrect)?.meaning || '')

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

function selectedWrongMeaning() {
  return props.options?.find(item => item.id === selectedId.value && !item.isCorrect)?.meaning || ''
}

function handleSelect(opt) {
  if (!opt || props.paused) return

  if (awaitingCorrectConfirm.value) {
    if (!opt.isCorrect || emitWrongLock.value) return
    emitWrongLock.value = true
    selectedId.value = opt.id
    window.setTimeout(() => {
      emit('wrong', {
        userInput: selectedWrongMeaning(),
        reason: `这次选成了“${selectedWrongMeaning() || '其他释义'}”，正确答案是“${correctMeaning.value}”。`,
        suggestion: '再听一遍发音，然后把英文和正确中文一起读一遍。',
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
  font-size: clamp(2.5rem, 3.8vw, 3.4rem);
  line-height: 1;
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
  background: #eef0ff;
  color: #4a57d3;
  font-size: 0.82rem;
  font-weight: 800;
}

.audio-button {
  min-height: 56px;
  padding: 0 18px;
  border: 1px solid rgba(74, 87, 211, 0.14);
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #eef3ff 100%);
  color: #4a57d3;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-weight: 700;
}

.context-image {
  min-height: 220px;
  overflow: hidden;
  border-radius: 26px;
  border: 1px solid rgba(95, 108, 132, 0.12);
  background: linear-gradient(180deg, #f6f8ff 0%, #eef4ff 100%);
}

.context-image img {
  width: 100%;
  height: 220px;
  object-fit: cover;
}

.prompt-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 18px;
  border-radius: 18px;
  background: rgba(241, 245, 255, 0.8);
  border: 1px solid rgba(95, 108, 132, 0.12);
  color: #5f6c84;
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
  min-height: 116px;
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(95, 108, 132, 0.12);
  background: #fff;
  display: grid;
  align-content: space-between;
  gap: 16px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.option-card:hover:not(:disabled),
.option-card.is-focus {
  transform: translateY(-1px);
  border-color: rgba(74, 87, 211, 0.24);
  box-shadow: 0 18px 36px rgba(74, 87, 211, 0.1);
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
  background: #eef3ff;
  color: #4454d4;
  font-weight: 800;
}

.option-card strong {
  font-size: 1.08rem;
  line-height: 1.5;
  color: #1e2b45;
}

.is-selected {
  border-color: rgba(74, 87, 211, 0.22);
  background: #f7f8ff;
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
  opacity: 0.7;
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
  .option-grid {
    grid-template-columns: 1fr;
    display: grid;
  }

  .option-grid {
    gap: 12px;
  }
}
</style>
