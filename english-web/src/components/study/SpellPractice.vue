<template>
  <div class="card">
    <p class="prompt">根据释义写出单词：</p>
    <p class="meaning-hint">{{ word.meaning }}</p>
    <div class="input-wrap" :class="{ error: isError, shake: isShake }">
      <input
        ref="inputRef"
        v-model="userInput"
        type="text"
        class="spell-input"
        :placeholder="placeholderLetters"
        autocomplete="off"
        @keydown.enter="submitSpelling"
      />
    </div>
    <p v-if="correctWordShadow" class="correct-shadow">{{ correctWordShadow }}</p>
    <button v-if="!correctWordShadow" class="btn submit" @click="submitSpelling">提交</button>
    <button v-else class="btn next" @click="emit('wrong')">下一个</button>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { checkSpelling, submitFeedback } from '../../api/study'
import { useToastStore } from '../../stores/toast'

const props = defineProps({ word: { type: Object, required: true } })
const emit = defineEmits(['next', 'wrong'])

const inputRef = ref(null)
const userInput = ref('')
const isError = ref(false)
const isShake = ref(false)
const correctWordShadow = ref('')
const toast = useToastStore()

const placeholderLetters = computed(() => {
  const w = (props.word && props.word.word) || ''
  return w ? '_ '.repeat(w.length).trim() : ''
})

watch(() => props.word, () => {
  userInput.value = ''
  isError.value = false
  correctWordShadow.value = ''
})

async function submitSpelling() {
  const input = userInput.value.trim()
  if (!input) return
  try {
    const res = await checkSpelling(props.word.id, input)
    if (res.correct) {
      await submitFeedback(props.word.id, 'KNOW')
      emit('next')
      return
    }
    isError.value = true
    correctWordShadow.value = props.word.word || ''
    isShake.value = true
    toast.show()
    setTimeout(() => { isShake.value = false }, 500)
    // 后端 checkSpelling 已记录错题，无需再 submit SPELLING_ERROR
  } catch (e) {
    isError.value = true
    correctWordShadow.value = props.word.word || ''
    isShake.value = true
    toast.show()
    setTimeout(() => { isShake.value = false }, 500)
  }
}
</script>

<style scoped>
.card {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.06);
}
.prompt { margin: 0 0 8px; font-size: 14px; color: #666; }
.meaning-hint { margin: 0 0 20px; font-size: 17px; color: #333; line-height: 1.5; }
.input-wrap {
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  padding: 4px;
  margin-bottom: 12px;
  transition: border-color 0.2s;
}
.input-wrap.error { border-color: #e53e3e; }
.input-wrap.shake {
  animation: shake 0.5s ease;
}
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-8px); }
  40% { transform: translateX(8px); }
  60% { transform: translateX(-6px); }
  80% { transform: translateX(6px); }
}
.spell-input {
  width: 100%;
  padding: 14px 18px;
  border: none;
  border-radius: 8px;
  font-size: 20px;
  letter-spacing: 2px;
}
.spell-input:focus { outline: none; }
.correct-shadow {
  margin: 0 0 16px;
  font-size: 18px;
  color: #718096;
  letter-spacing: 3px;
  font-family: monospace;
  opacity: 0.85;
}
.btn.submit, .btn.next {
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
