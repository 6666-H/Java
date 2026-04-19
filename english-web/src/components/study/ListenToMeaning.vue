<template>
  <div class="card">
    <div class="audio-row">
      <button class="play-btn" @click="playAudio">🔊 播放</button>
    </div>
    <p class="hint">听发音，选择正确释义</p>
    <div class="options">
      <button
        v-for="opt in options"
        :key="opt.id"
        class="option"
        :class="{ chosen: chosenId === opt.id, correct: showResult && opt.correct, wrong: showResult && chosenId === opt.id && !opt.correct }"
        @click="choose(opt)"
      >
        {{ opt.meaning }}
      </button>
    </div>
    <button v-if="showResult" class="btn next" @click="goNext">下一个</button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { submitFeedback } from '../../api/study'
import { useToastStore } from '../../stores/toast'

const props = defineProps({
  word: { type: Object, required: true },
  options: { type: Array, required: true },
})
const emit = defineEmits(['next', 'wrong'])

const chosenId = ref(null)
const showResult = ref(false)
const toast = useToastStore()

function playAudio() {
  if (!props.word.audioUrl) {
    const u = new SpeechSynthesisUtterance(props.word.word)
    u.lang = 'en-US'
    speechSynthesis.speak(u)
    return
  }
  const a = new Audio(props.word.audioUrl)
  a.play().catch(() => {
    const u = new SpeechSynthesisUtterance(props.word.word)
    u.lang = 'en-US'
    speechSynthesis.speak(u)
  })
}

function choose(opt) {
  if (showResult.value) return
  chosenId.value = opt.id
  showResult.value = true
  if (opt.correct) {
    submitFeedback(props.word.id, 'KNOW').catch(() => {})
    setTimeout(() => emit('next'), 800)
  } else {
    submitFeedback(props.word.id, 'DONT_KNOW').catch(() => {})
    toast.show()
  }
}

function goNext() {
  emit('wrong')
}
</script>

<style scoped>
.card {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.06);
}
.audio-row { margin-bottom: 20px; }
.play-btn {
  padding: 12px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  cursor: pointer;
}
.hint { color: #666; font-size: 14px; margin: 0 0 16px; }
.options { display: flex; flex-direction: column; gap: 10px; }
.option {
  padding: 14px 18px;
  text-align: left;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  background: #fff;
  font-size: 15px;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}
.option:hover { border-color: #667eea; background: #f8f9ff; }
.option.chosen { border-color: #667eea; background: #f0f4ff; }
.option.correct { border-color: #48bb78; background: #f0fff4; }
.option.wrong { border-color: #e53e3e; background: #fff5f5; }
.btn.next {
  margin-top: 20px;
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  cursor: pointer;
}
</style>
