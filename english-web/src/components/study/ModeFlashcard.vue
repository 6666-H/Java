<template>
  <section class="mode-body" :class="{ 'is-shake': isShake, 'is-paused': paused }">
    <div class="hero-block">
      <div class="hero-copy">
        <h2>{{ word.word }}</h2>
        <div class="hero-meta-row">
          <p class="hero-phonetic">{{ word.phonetic || '点击发音按钮，再听一遍' }}</p>
          <button type="button" class="audio-button" :disabled="paused" @click="playAudio(true)">
            <el-icon><Microphone /></el-icon>
          </button>
        </div>
      </div>
    </div>

    <div class="meaning-panel">
      <div class="meaning-row">
        <span>中文释义</span>
        <button type="button" class="meaning-toggle" :disabled="paused" @click="isMeaningVisible = !isMeaningVisible">
          {{ isMeaningVisible ? '隐藏释义' : '点击查看释义' }}
        </button>
        <strong :class="{ 'is-obscured': !isMeaningVisible }">{{ word.meaning || '暂无释义' }}</strong>
      </div>

      <div class="meta-grid">
        <div class="meta-card">
          <span>词性</span>
          <strong>{{ word.pos || '常用词' }}</strong>
        </div>
        <div class="meta-card">
          <span>学习提示</span>
          <strong>{{ memoryTip }}</strong>
        </div>
      </div>

      <div v-if="word.exampleSentence || word.exampleZh" class="example-panel">
        <span>例句</span>
        <p>{{ word.exampleSentence || '先记住词义，下一轮会进入更具体的练习。' }}</p>
        <small v-if="word.exampleZh">{{ word.exampleZh }}</small>
      </div>
    </div>
    <div class="action-row">
      <button type="button" class="action-btn primary" :disabled="paused" @click="handleKnow">
        认识
      </button>
      <button type="button" class="action-btn ghost" :disabled="paused" @click="handleNeedMore">
        不认识
      </button>
      <button type="button" class="action-btn secondary" :disabled="paused" @click="playAudio(true)">
        再听一遍
      </button>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { Microphone } from '@element-plus/icons-vue'
import { playSuccessTone, playWordAudio, vibrateLight } from './studyPlayback'

const props = defineProps({
  word: { type: Object, default: () => ({}) },
  audioEnabled: { type: Boolean, default: true },
  paused: { type: Boolean, default: false },
})

const emit = defineEmits(['next', 'wrong'])

const isShake = ref(false)
const isMeaningVisible = ref(false)

const memoryTip = computed(() => {
  const text = String(props.word?.word || '')
  if (!text) return '先听音，再记义'
  if (/(.)\1/i.test(text)) {
    const doubled = text.match(/(.)\1/i)?.[1] || ''
    return doubled ? `注意双写 ${doubled.toLowerCase()}` : '注意重复字母'
  }
  if (text.length >= 8) return '单词较长，建议分段记忆'
  if (text.includes('-') || text.includes(' ')) return '留意连接符和空格'
  return '先看整体，再记关键字母'
})

watch(
  () => props.word?.id,
  () => {
    isShake.value = false
    isMeaningVisible.value = false
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

function playAudio(force = false) {
  return playWordAudio(props.word, { enabled: props.audioEnabled, force })
}

function handleKnow() {
  if (props.paused) return
  playSuccessTone(props.audioEnabled)
  emit('next')
}

function handleNeedMore() {
  if (props.paused) return
  isShake.value = true
  vibrateLight(120)
  window.setTimeout(() => {
    isShake.value = false
    emit('wrong', {
      userInput: '还没记住',
      reason: `这个词先回到队列里，稍后再练一次：${props.word?.meaning || '请先记住中文释义'}`,
      suggestion: '先点“再听一遍”，再把中文和英文连起来读一遍。',
    })
  }, 180)
}

function handleKeydown(event) {
  if (props.paused) return

  const active = document.activeElement
  const tag = active?.tagName?.toLowerCase()
  if (tag === 'input' || tag === 'textarea') return

  if (event.code === 'Space') {
    event.preventDefault()
    playAudio(true)
    return
  }

  if (event.key === 'ArrowLeft') {
    event.preventDefault()
    handleNeedMore()
    return
  }

  if (event.key === 'ArrowRight' || event.key === 'Enter') {
    event.preventDefault()
    handleKnow()
  }
}
</script>

<style scoped>
.mode-body {
  display: grid;
  gap: 10px;
}

.is-paused {
  pointer-events: none;
}

.is-shake {
  animation: shake 0.22s linear;
}

.hero-block {
  display: grid;
  gap: 8px;
  justify-items: center;
}

.hero-copy {
  display: grid;
  gap: 6px;
  justify-items: center;
}

.hero-copy h2 {
  margin: 0;
  font-size: clamp(1.8rem, 2.4vw, 2.2rem);
  line-height: 1;
  color: #162033;
  font-weight: 800;
  text-align: center;
}

.hero-meta-row {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.hero-phonetic {
  margin: 0;
  font-size: 0.84rem;
  color: #526079;
  font-weight: 600;
  text-align: center;
}

.audio-button {
  width: 40px;
  height: 40px;
  border: 1px solid rgba(111, 99, 255, 0.14);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  color: #6659ff;
  display: grid;
  place-items: center;
  cursor: pointer;
  font-weight: 700;
  box-shadow: 0 14px 28px rgba(102, 89, 255, 0.08);
}

.meaning-panel {
  display: grid;
  gap: 8px;
  padding: 14px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(245, 247, 255, 0.86) 0%, rgba(255, 255, 255, 0.98) 100%);
  border: 1px solid rgba(103, 116, 142, 0.12);
}

.meaning-row span,
.example-panel span,
.meta-card span {
  display: block;
  font-size: 0.74rem;
  color: #6f7a8f;
}

.meaning-row strong {
  display: block;
  margin-top: 6px;
  font-size: clamp(0.98rem, 0.92rem + 0.34vw, 1.14rem);
  line-height: 1.28;
  color: #1c2940;
  transition: filter 0.18s ease, opacity 0.18s ease;
}

.meaning-row strong.is-obscured {
  filter: blur(8px);
  opacity: 0.45;
  user-select: none;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.meta-card {
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(103, 116, 142, 0.12);
}

.meta-card strong {
  display: block;
  margin-top: 4px;
  font-size: 0.88rem;
  color: #1f2a44;
}

.example-panel {
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px dashed rgba(111, 99, 255, 0.2);
}

.example-panel p {
  margin: 4px 0 0;
  font-size: 0.84rem;
  line-height: 1.45;
  color: #23304b;
}

.example-panel small {
  display: block;
  margin-top: 4px;
  font-size: 0.76rem;
  line-height: 1.45;
  color: #6b768d;
}

.action-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  align-items: center;
}

.action-btn {
  width: 100%;
  min-height: 44px;
  padding: 0 12px;
  border-radius: 14px;
  border: 1px solid transparent;
  font-size: 0.88rem;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.action-btn:hover:not(:disabled) {
  transform: translateY(-1px);
}

.action-btn:disabled,
.audio-button:disabled {
  opacity: 0.56;
  cursor: not-allowed;
}

.action-btn.primary {
  background: linear-gradient(135deg, #6f63ff 0%, #8b7dff 100%);
  color: #fff;
  box-shadow: 0 18px 32px rgba(111, 99, 255, 0.18);
}

.action-btn.secondary {
  background: #f3f5ff;
  border-color: rgba(111, 99, 255, 0.14);
  color: #5d52da;
}

.action-btn.ghost {
  background: #fff3eb;
  border-color: rgba(241, 163, 108, 0.18);
  color: #c97d46;
}

.meaning-toggle {
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid rgba(111, 99, 255, 0.14);
  background: #ffffff;
  color: #6659ff;
  font-size: 0.74rem;
  font-weight: 700;
  cursor: pointer;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-6px); }
  75% { transform: translateX(6px); }
}

@media (max-width: 860px) {
  .hero-block {
    grid-template-columns: 1fr;
  }

  .audio-button {
    width: 40px;
    min-height: 40px;
    border-radius: 50%;
  }

  .action-row,
  .meta-grid {
    grid-template-columns: 1fr;
    display: grid;
  }

  .hero-copy h2,
  .hero-phonetic {
    text-align: center;
  }
}
</style>
