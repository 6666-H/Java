<template>
  <section class="mode-body" :class="{ 'is-shake': isShake, 'is-paused': paused }">
    <div class="prompt-hero">
      <div class="prompt-main">
        <div class="prompt-badge">拼写挑战</div>
        <h2>{{ word.meaning }}</h2>
        <p>{{ promptCopy }}</p>
      </div>

      <button type="button" class="audio-button" :disabled="paused" @click="playAudio(true)">
        <el-icon><Microphone /></el-icon>
        <span>听提示</span>
      </button>
    </div>

    <div class="letter-row" :class="{ 'is-error': showInlineError }">
      <span
        v-for="(char, index) in expectedLetters"
        :key="`${char}-${index}`"
        class="letter-cell"
        :class="{
          'has-value': displayLetters[index],
          'is-hint': hintVisible && !displayLetters[index],
          'is-answer': answerVisible && !displayLetters[index],
        }"
      >
        {{ displayLetters[index] || placeholderFor(index) }}
      </span>
    </div>

    <div class="input-panel">
      <input
        ref="inputRef"
        v-model="userInput"
        type="text"
        class="spell-input"
        :disabled="paused || answerVisible"
        autocomplete="off"
        spellcheck="false"
        placeholder="在这里拼写英文"
        @keydown.enter.prevent="handleCheck"
      />

      <div class="input-tips">
        <span>支持键盘直接输入，按 `Enter` 检查答案。</span>
        <span v-if="hintVisible">提示骨架：{{ spellingPattern }}</span>
      </div>
    </div>

    <div v-if="showInlineError && !answerVisible" class="feedback-panel danger">
      <strong>{{ inlineMessage }}</strong>
      <p>{{ retryHint }}</p>
    </div>

    <div v-if="answerVisible" class="feedback-panel answer">
      <strong>{{ word.word }}</strong>
      <p>{{ word.phonetic || '再听一遍发音，然后跟着拼写。' }}</p>
    </div>

    <div class="action-row">
      <button type="button" class="action-btn ghost" :disabled="paused" @click="handleGiveUp">
        不会
      </button>
      <button type="button" class="action-btn secondary" :disabled="paused || answerVisible" @click="toggleHint">
        {{ hintVisible ? '收起提示' : '提示' }}
      </button>
      <button
        v-if="!answerVisible"
        type="button"
        class="action-btn primary"
        :disabled="paused"
        @click="handleCheck"
      >
        检查答案
      </button>
      <button
        v-else
        type="button"
        class="action-btn primary"
        :disabled="paused"
        @click="proceedNext"
      >
        记住了，下一题
      </button>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { Microphone } from '@element-plus/icons-vue'
import { playSuccessTone, playWordAudio, vibrateLight } from './studyPlayback'

const props = defineProps({
  word: { type: Object, default: () => ({}) },
  audioEnabled: { type: Boolean, default: true },
  paused: { type: Boolean, default: false },
})

const emit = defineEmits(['next', 'wrong'])

const inputRef = ref(null)
const userInput = ref('')
const hintVisible = ref(false)
const isShake = ref(false)
const showInlineError = ref(false)
const answerVisible = ref(false)
const inlineMessage = ref('')
const errorCount = ref(0)
const proceedLock = ref(false)

const expectedLetters = computed(() => String(props.word?.word || '').split(''))
const spellingPattern = computed(() => {
  if (props.word?.spellingPattern) return props.word.spellingPattern
  const target = String(props.word?.word || '')
  if (!target) return ''
  return target.split('').map((char, index) => {
    if (index === 0 || index === target.length - 1 || !/[a-z]/i.test(char)) return char
    return '_'
  }).join(' ')
})
const displayLetters = computed(() => {
  const trimmed = String(userInput.value || '').trim()
  if (!trimmed) return []
  return trimmed.split('')
})
const promptCopy = computed(() => {
  if (props.word?.exampleZh) return props.word.exampleZh
  return '先在心里默念一遍，再把英文完整拼出来。'
})
const retryHint = computed(() => {
  if (errorCount.value >= 2) return '如果还是想不出来，可以点“不会”先看答案。'
  if (hintVisible.value) return `骨架提示：${spellingPattern.value}`
  return '再试一次，先听发音，再看首字母和末尾字母。'
})

watch(
  () => props.word?.id,
  async () => {
    userInput.value = ''
    hintVisible.value = false
    isShake.value = false
    showInlineError.value = false
    answerVisible.value = false
    inlineMessage.value = ''
    errorCount.value = 0
    proceedLock.value = false
    await nextTick()
    inputRef.value?.focus()
    playAudio(false)
  },
  { immediate: true }
)

onMounted(() => {
  window.addEventListener('keydown', handleWindowKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleWindowKeydown)
})

function playAudio(force = false) {
  return playWordAudio(props.word, { enabled: props.audioEnabled, force })
}

function placeholderFor(index) {
  if (answerVisible.value) return expectedLetters.value[index] || ''
  if (hintVisible.value) {
    return spellingPattern.value.split(' ')[index] || '_'
  }
  return '_'
}

function normalizeText(value) {
  return String(value || '').trim().toLowerCase()
}

function buildSpellingMessage(expected, actual) {
  if (!actual) return '还没有输入完整答案'
  if (actual.length < expected.length) return `少了 ${expected.length - actual.length} 个字母`
  if (actual.length > expected.length) return `多输入了 ${actual.length - expected.length} 个字母`

  const mismatchIndex = expected.split('').findIndex((char, index) => char !== actual[index])
  if (mismatchIndex >= 0) {
    const letter = expected[mismatchIndex]
    return `第 ${mismatchIndex + 1} 个字母应该是 ${letter.toUpperCase()}`
  }
  return '再看一遍拼写顺序'
}

function revealAnswer(message) {
  showInlineError.value = true
  answerVisible.value = true
  inlineMessage.value = message
  playAudio(false)
}

function handleCheck() {
  if (props.paused || answerVisible.value) return

  const expected = normalizeText(props.word?.word)
  const actual = normalizeText(userInput.value)
  if (!expected) return

  if (expected === actual) {
    showInlineError.value = false
    answerVisible.value = false
    inlineMessage.value = ''
    playSuccessTone(props.audioEnabled)
    emit('next')
    return
  }

  errorCount.value += 1
  showInlineError.value = true
  inlineMessage.value = buildSpellingMessage(expected, actual)
  isShake.value = true
  vibrateLight(120)
  window.setTimeout(() => {
    isShake.value = false
  }, 180)

  if (errorCount.value >= 2) {
    revealAnswer(`正确拼写是 ${props.word?.word || ''}`)
  }
}

function toggleHint() {
  if (props.paused || answerVisible.value) return
  hintVisible.value = !hintVisible.value
  if (hintVisible.value) {
    inputRef.value?.focus()
  }
}

function handleGiveUp() {
  if (props.paused) return
  revealAnswer(`这次先记住 ${props.word?.word || ''}`)
}

function proceedNext() {
  if (proceedLock.value || props.paused) return
  proceedLock.value = true
  emit('wrong', {
    userInput: String(userInput.value || '').trim(),
    reason: inlineMessage.value || `正确答案是 ${props.word?.word || ''}`,
    suggestion: hintVisible.value
      ? `下次先想骨架：${spellingPattern.value}`
      : '先听发音，再看首尾字母，最后完整拼一遍。',
  })
}

function handleWindowKeydown(event) {
  if (props.paused) return

  const active = document.activeElement
  const tag = active?.tagName?.toLowerCase()

  if (event.code === 'Space' && tag !== 'input' && tag !== 'textarea') {
    event.preventDefault()
    playAudio(true)
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
  background: #eafaf5;
  color: #17735e;
  font-size: 0.82rem;
  font-weight: 800;
}

.audio-button {
  min-height: 56px;
  padding: 0 18px;
  border: 1px solid rgba(23, 115, 94, 0.16);
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #eefcf7 100%);
  color: #17735e;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-weight: 700;
}

.letter-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(52px, 1fr));
  gap: 12px;
  padding: 18px;
  border-radius: 24px;
  border: 1px solid rgba(95, 108, 132, 0.12);
  background: linear-gradient(180deg, #f7fbff 0%, #ffffff 100%);
}

.letter-row.is-error {
  border-color: rgba(234, 88, 12, 0.2);
  background: #fff8f3;
}

.letter-cell {
  min-height: 64px;
  border-radius: 18px;
  border: 1px dashed rgba(95, 108, 132, 0.18);
  background: #fff;
  display: grid;
  place-items: center;
  font-size: 1.28rem;
  font-weight: 800;
  color: #9aa3b3;
  text-transform: lowercase;
}

.letter-cell.has-value {
  border-style: solid;
  border-color: rgba(74, 87, 211, 0.18);
  color: #1d2a42;
}

.letter-cell.is-hint,
.letter-cell.is-answer {
  color: #4b5565;
}

.input-panel {
  display: grid;
  gap: 12px;
}

.spell-input {
  width: 100%;
  min-height: 56px;
  padding: 0 20px;
  border-radius: 18px;
  border: 1px solid rgba(95, 108, 132, 0.16);
  background: #fff;
  font-size: 1.05rem;
  font-weight: 600;
  color: #162033;
}

.spell-input:focus {
  outline: none;
  border-color: rgba(74, 87, 211, 0.28);
  box-shadow: 0 0 0 4px rgba(74, 87, 211, 0.08);
}

.input-tips {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  color: #5f6c84;
  font-size: 0.88rem;
}

.feedback-panel {
  padding: 18px 20px;
  border-radius: 20px;
  border: 1px solid transparent;
}

.feedback-panel strong {
  display: block;
  font-size: 1.02rem;
}

.feedback-panel p {
  margin: 8px 0 0;
  line-height: 1.65;
  font-size: 0.92rem;
}

.feedback-panel.danger {
  background: #fff7ed;
  border-color: rgba(234, 88, 12, 0.16);
}

.feedback-panel.danger strong {
  color: #9a3412;
}

.feedback-panel.danger p {
  color: #a16207;
}

.feedback-panel.answer {
  background: #eefcf7;
  border-color: rgba(22, 163, 74, 0.14);
}

.feedback-panel.answer strong {
  color: #166534;
}

.feedback-panel.answer p {
  color: #17735e;
}

.action-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.action-btn {
  min-height: 56px;
  padding: 0 18px;
  border-radius: 18px;
  border: 1px solid transparent;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
}

.action-btn:disabled,
.audio-button:disabled,
.spell-input:disabled {
  opacity: 0.56;
  cursor: not-allowed;
}

.action-btn.primary {
  background: linear-gradient(135deg, #17735e 0%, #26a27f 100%);
  color: #fff;
  box-shadow: 0 18px 32px rgba(23, 115, 94, 0.18);
}

.action-btn.secondary {
  background: #eefcf7;
  border-color: rgba(23, 115, 94, 0.14);
  color: #17735e;
}

.action-btn.ghost {
  background: #fff7ed;
  border-color: rgba(234, 88, 12, 0.16);
  color: #c56a13;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-6px); }
  75% { transform: translateX(6px); }
}

@media (max-width: 860px) {
  .prompt-hero,
  .input-tips,
  .action-row {
    grid-template-columns: 1fr;
    display: grid;
  }
}
</style>
