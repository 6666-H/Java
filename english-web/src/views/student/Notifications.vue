<template>
  <div class="student-page">
    <section class="panel">
      <div class="panel-head">
        <div><h3>消息通知</h3></div>
        <el-button type="primary" plain @click="markAllRead">全部标为已读</el-button>
      </div>

      <div v-if="notifications.length === 0" class="empty-copy">当前没有通知。</div>
      <div v-else class="notice-list">
        <article v-for="item in notifications" :key="item.id" class="notice-card" :class="{ unread: item.isRead === 0 }">
          <strong>{{ item.type === 'TASK_ASSIGNED' ? '新任务通知' : '系统通知' }}</strong>
          <p>{{ item.content }}</p>
          <span>{{ formatTime(item.createdAt) }}</span>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getStudentNotifications, markStudentNotificationsRead } from '../../api/student'
import { ElMessage } from 'element-plus'

const notifications = ref([])

function formatTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ')
}

async function loadData() {
  try {
    notifications.value = await getStudentNotifications()
  } catch (error) {
    ElMessage.error(error.message || '获取通知失败')
  }
}

async function markAllRead() {
  await markStudentNotificationsRead()
  notifications.value = notifications.value.map(item => ({ ...item, isRead: 1 }))
  ElMessage.success('已全部标记为已读')
}

onMounted(loadData)
</script>

<style scoped>
.student-page { display: grid; gap: 14px; }
.panel { background: rgba(255,255,255,.92); border: 1px solid #efe8f4; border-radius: 14px; padding: 14px; }
:deep(.el-button--primary.is-plain) { color: #a855f7; border-color: #e9d5ff; }
.panel-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.panel-head h3 { margin: 0; color: #172033; font-size: 1rem; }
.notice-list { display: grid; gap: 8px; }
.notice-card { border-radius: 14px; background: #f8fafc; padding: 12px; border: 1px solid transparent; }
.notice-card.unread { border-color: #fdba74; background: #fff7ed; }
.notice-card strong { color: #172033; display: block; font-size: .92rem; }
.notice-card p { margin: 8px 0; color: #475569; font-size: .84rem; }
.notice-card span, .empty-copy { color: #64748b; font-size: .78rem; }
@media (max-width: 860px) { .panel-head { flex-direction: column; align-items: stretch; } }
</style>
