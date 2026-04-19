<template>
  <div class="card">
    <div v-if="!showMeaning" class="front">
      <p class="word">{{ word.word }}</p>
      <p v-if="word.phonetic" class="phonetic">{{ word.phonetic }}</p>
      <div class="actions">
        <button class="btn know" @click="onKnow">认识</button>
        <button class="btn dont-know" @click="onDontKnow">不认识</button>
      </div>
    </div>
    <div v-else class="back">
      <p class="word">{{ word.word }}</p>
      <p v-if="word.phonetic" class="phonetic">{{ word.phonetic }}</p>
      <div class="meaning-block">
        <p class="meaning">{{ word.meaning }}</p>
        <p v-if="word.exampleSentence" class="example">{{ word.exampleSentence }}</p>
      </div>
      <span v-if="markedReview" class="tag">待复习</span>
      <button class="btn next" @click="goNext">下一个</button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { submitFeedback } from '../../api/study'
import { useToastStore } from '../../stores/toast'

const props = defineProps({ word: { type: Object, required: true } })
const emit = defineEmits(['next', 'wrong'])

const showMeaning = ref(false)
const markedReview = ref(false)
const toast = useToastStore()

async function onKnow() {
  try {
    await submitFeedback(props.word.id, 'KNOW')
    emit('next')
  } catch (e) {
    console.error(e)
    emit('next')
  }
}

async function onDontKnow() {
  try {
    await submitFeedback(props.word.id, 'DONT_KNOW')
    markedReview.value = true
    showMeaning.value = true
    toast.show()
  } catch (e) {
    console.error(e)
    showMeaning.value = true
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
  position: relative;
}
.word { font-size: 28px; font-weight: 700; color: #1a1a2e; margin: 0 0 8px; }
.phonetic { color: #666; font-size: 16px; margin: 0 0 24px; }
.meaning-block { margin: 16px 0 20px; padding: 16px; background: #f8f9ff; border-radius: 12px; }
.meaning { font-size: 16px; color: #333; line-height: 1.6; margin: 0; }
.example { font-size: 14px; color: #666; margin: 12px 0 0; font-style: italic; }
.tag {
  display: inline-block;
  padding: 4px 10px;
  background: #fed7d7;
  color: #c53030;
  border-radius: 8px;
  font-size: 12px;
  margin-bottom: 16px;
}
.actions { display: flex; gap: 12px; margin-top: 24px; }
.btn {
  flex: 1;
  padding: 14px 20px;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
}
.btn.know { background: #48bb78; color: #fff; }
.btn.dont-know { background: #ed8936; color: #fff; }
.btn.next { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: #fff; }
.btn:hover { opacity: 0.9; }
</style>
