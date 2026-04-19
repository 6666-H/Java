<template>
  <div class="space-y-7">
    <div class="mb-6 flex flex-col md:flex-row md:items-end justify-between gap-4">
      <div>
        <h1 class="text-[2rem] font-extrabold font-headline tracking-tight text-[#191c1e] mb-2">个人中心</h1>
        <p class="text-[#414754] text-sm max-w-lg">欢迎回来，{{ displayName }}。在这里管理您的学习档案与成就，每一个足迹都见证着您的成长。</p>
      </div>
      <div class="flex gap-3">
        <button class="px-4 py-2 rounded-lg border-2 border-[#c1c6d6] text-[#191c1e] text-sm font-semibold hover:bg-[#e6e8ea] transition-colors" @click="saveProfile">编辑资料</button>
        <button class="px-4 py-2 rounded-lg bg-gradient-to-br from-[#005bbf] to-[#1a73e8] text-white text-sm font-semibold shadow-lg" @click="saveProfile">分享成就</button>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
      <div class="lg:col-span-4 bg-white rounded-[18px] p-5 flex flex-col items-center text-center">
        <div class="relative mb-4">
          <div class="w-24 h-24 rounded-full border-4 border-[#005bbf]/10 p-1">
            <img v-if="profile.avatar" :src="profile.avatar" class="w-full h-full rounded-full object-cover" alt="avatar" />
            <div v-else class="w-full h-full rounded-full bg-[radial-gradient(circle_at_40%_30%,#54402b,#111827)] text-white flex items-center justify-center text-3xl font-extrabold">{{ displayName.charAt(0).toUpperCase() }}</div>
          </div>
          <div class="absolute bottom-1 right-1 bg-[#005bbf] text-white p-1.5 rounded-full border-4 border-white">
            <el-icon size="14"><CircleCheckFilled /></el-icon>
          </div>
        </div>
        <h2 class="text-[1.3rem] font-bold font-headline mb-1">{{ displayName }}</h2>
        <p class="text-[#414754] text-sm mb-4">{{ profile.gradeClass || '高级语言学者' }}<span v-if="profile.studentNo"> · {{ profile.studentNo }}</span></p>
        <div class="w-full space-y-3 text-left">
          <div class="flex items-center justify-between p-2.5 bg-[#f2f4f6] rounded-[12px]">
            <span class="text-[#414754] text-sm flex items-center gap-2"><el-icon class="text-[#005bbf]"><Message /></el-icon>{{ accountLabel }}</span>
            <span class="font-medium text-sm">{{ accountValue }}</span>
          </div>
          <div class="flex items-center justify-between p-2.5 bg-[#f2f4f6] rounded-[12px]">
            <span class="text-[#414754] text-sm flex items-center gap-2"><el-icon class="text-[#005bbf]"><Iphone /></el-icon>手机号码</span>
            <span class="font-medium text-sm">{{ profile.phone || '未设置' }}</span>
          </div>
          <div class="flex items-center justify-between p-2.5 bg-[#f2f4f6] rounded-[12px]">
            <span class="text-[#414754] text-sm flex items-center gap-2"><el-icon class="text-[#005bbf]"><Calendar /></el-icon>加入时间</span>
            <span class="font-medium text-sm">{{ joinDate }}</span>
          </div>
        </div>
      </div>

      <div class="lg:col-span-8 grid grid-cols-1 md:grid-cols-3 gap-5">
        <div class="bg-[#005bbf] p-4 rounded-[18px] text-white relative overflow-hidden group">
          <div class="absolute -right-4 -bottom-4 opacity-10">
            <el-icon size="96"><DataAnalysis /></el-icon>
          </div>
          <p class="text-[#d8e2ff] text-xs font-medium mb-1">累计学习</p>
          <h3 class="text-[1.7rem] font-extrabold font-headline mb-2">{{ stats.consecutiveDays || 0 }}<span class="text-sm font-normal ml-1">天</span></h3>
          <div class="h-1.5 bg-white/20 rounded-full mt-3 overflow-hidden">
            <div class="h-full bg-white w-3/4"></div>
          </div>
          <p class="text-xs text-[#d8e2ff] mt-2">已连续坚持打卡 {{ stats.learningDays || 0 }} 天</p>
        </div>

        <div class="bg-white p-4 rounded-[18px] relative overflow-hidden border-2 border-transparent">
          <p class="text-[#414754] text-xs font-medium mb-1">词汇储备量</p>
          <h3 class="text-[1.7rem] font-extrabold font-headline text-[#191c1e] mb-2">{{ (stats.masteredCount || 0).toLocaleString() }}</h3>
          <div class="flex items-center gap-2 text-[#9e4300] font-bold text-xs">
            <el-icon><TrendCharts /></el-icon>
            <span>本月新增 +{{ monthlyGain }}</span>
          </div>
        </div>

        <div class="bg-white p-4 rounded-[18px] relative overflow-hidden border-2 border-transparent">
          <p class="text-[#414754] text-xs font-medium mb-1">超越全国用户</p>
          <h3 class="text-[1.7rem] font-extrabold font-headline text-[#005bbf] mb-2">{{ percentile }}%</h3>
          <p class="text-xs text-[#414754] mt-2 italic">“你是卓越的领跑者”</p>
        </div>

        <div class="md:col-span-3 bg-white rounded-[18px] p-5">
          <div class="flex justify-between items-center mb-5">
            <h3 class="text-[1.2rem] font-bold font-headline flex items-center gap-2">
              <el-icon class="text-[#9e4300]"><TrophyBase /></el-icon>
              成就奖章展示
            </h3>
            <button class="text-[#005bbf] text-sm font-bold flex items-center">查看全部</button>
          </div>
          <div class="flex flex-wrap gap-5 justify-around">
            <div v-for="badge in badges" :key="badge.label" class="flex flex-col items-center gap-3" :class="badge.muted ? 'opacity-40 grayscale' : ''">
              <div class="w-12 h-12 rounded-full flex items-center justify-center border-4 border-white shadow-lg" :class="badge.bg">
                <span class="text-lg">{{ badge.icon }}</span>
              </div>
              <span class="text-xs font-bold text-[#191c1e]">{{ badge.label }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
      <div class="lg:col-span-8 bg-white rounded-[18px] p-5">
        <h3 class="text-[1.1rem] font-bold font-headline mb-5 flex items-center gap-2">
          <el-icon class="text-[#005bbf]"><Setting /></el-icon>
          账号安全与设置
        </h3>
        <div class="space-y-4">
          <div class="grid grid-cols-1 md:grid-cols-[180px_1fr] gap-4 items-center">
            <div>
              <p class="font-bold text-[#191c1e]">昵称</p>
              <p class="text-xs text-[#414754]">编辑你在平台中的显示名称</p>
            </div>
            <el-input v-model="form.nickname" placeholder="请输入昵称" />
          </div>
          <div class="grid grid-cols-1 md:grid-cols-[180px_1fr] gap-4 items-center">
            <div>
              <p class="font-bold text-[#191c1e]">头像链接</p>
              <p class="text-xs text-[#414754]">可粘贴个人头像地址</p>
            </div>
            <el-input v-model="form.avatar" placeholder="https://..." />
          </div>
          <div class="grid grid-cols-1 md:grid-cols-[180px_1fr] gap-4 items-center">
            <div>
              <p class="font-bold text-[#191c1e]">手机号码</p>
              <p class="text-xs text-[#414754]">开启通知时会用于接收提醒</p>
            </div>
            <el-input v-model="form.phone" placeholder="请输入手机号" />
          </div>
          <div class="flex items-center justify-between py-3">
            <div>
              <p class="font-bold text-[#191c1e]">学习通知提醒</p>
              <p class="text-xs text-[#414754]">开启后将在学习计划开始前提醒您</p>
            </div>
            <el-switch v-model="notifyEnabled" />
          </div>
        </div>
        <div class="mt-6">
          <button class="px-5 py-2.5 rounded-lg bg-gradient-to-br from-[#005bbf] to-[#1a73e8] text-white font-semibold text-sm" @click="saveProfile">保存资料</button>
        </div>
      </div>

      <div class="lg:col-span-4 flex flex-col gap-6">
        <div class="bg-white rounded-[18px] p-5 flex-1 flex flex-col items-center justify-center text-center">
          <div class="w-14 h-14 bg-[#ffdad6] text-[#ba1a1a] rounded-full flex items-center justify-center mb-4">
            <el-icon size="30"><SwitchButton /></el-icon>
          </div>
          <h4 class="text-lg font-bold mb-2">退出登录</h4>
          <p class="text-sm text-[#414754] mb-6">确定要离开永升教育系统吗？您的学习记录已实时同步。</p>
          <button class="w-full py-2.5 bg-[#e6e8ea] text-[#ba1a1a] font-bold rounded-xl hover:bg-[#ffdad6]/50 transition-all text-sm" @click="handleLogout">确认退出</button>
        </div>

        <div class="bg-[#c55500] text-white rounded-[18px] p-4 relative overflow-hidden">
          <div class="relative z-10">
            <p class="font-bold text-base mb-1">专业客服</p>
            <p class="text-xs opacity-90 mb-4">学习遇到困难？我们随时为您服务</p>
            <button class="px-4 py-2 bg-white text-[#9e4300] text-xs font-black rounded-lg">联系我们</button>
          </div>
        </div>
      </div>
    </div>

    <section class="bg-white rounded-[18px] p-5">
      <div class="flex justify-between items-center mb-6">
        <h3 class="text-[1.1rem] font-bold font-headline">最近阶段成绩</h3>
      </div>
      <div v-if="recentResults.length === 0" class="text-[#414754]">暂时还没有阶段结算记录。</div>
      <div v-else class="grid gap-3">
        <article v-for="item in recentResults" :key="item.id" class="flex justify-between items-center gap-4 p-3 rounded-xl bg-[#f2f4f6]">
          <div>
            <strong class="block text-[#191c1e]">{{ item.unitName }} · {{ stageLabel(item.stage) }}</strong>
            <span class="block mt-1 text-xs text-[#727785]">{{ item.bookName }} · {{ item.createdAt }}</span>
          </div>
          <em class="not-italic font-bold text-[#005bbf]">{{ item.firstRoundCorrect }}/{{ item.totalQuestions }}{{ item.starReward ? ' ⭐' : '' }}</em>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { Calendar, CircleCheckFilled, DataAnalysis, Iphone, Message, Setting, SwitchButton, TrendCharts, TrophyBase } from '@element-plus/icons-vue'
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { logout as logoutApi } from '../../api/auth'
import { getStudentProfile, getStudentStats, getStudentStageResults, updateStudentProfile } from '../../api/student'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const profile = ref({ nickname: '', avatar: '', username: '', realName: '', studentNo: '', gradeClass: '', phone: '', createdAt: '' })
const stats = ref({ consecutiveDays: 0, learningDays: 0, masteredCount: 0 })
const form = ref({ nickname: '', avatar: '', phone: '' })
const recentResults = ref([])
const notifyEnabled = ref(true)

const displayName = computed(() => profile.value.nickname || profile.value.realName || profile.value.username || authStore.username || '学生')
const accountLabel = computed(() => profile.value.username?.includes('@') ? '邮箱地址' : '账号')
const accountValue = computed(() => profile.value.username || '未设置')
const joinDate = computed(() => {
  if (!profile.value.createdAt) return '未记录'
  return String(profile.value.createdAt).slice(0, 10).replace(/-/g, '.')
})
const monthlyGain = computed(() => Math.max(20, Math.round((stats.value.masteredCount || 0) / 12)))
const percentile = computed(() => Math.min(99.8, 80 + Number(stats.value.consecutiveDays || 0) * 0.7).toFixed(1))
const badges = computed(() => [
  { label: '学识渊博', icon: '✦', bg: 'bg-[#ffdbcb] text-[#9e4300]' },
  { label: '坚持不懈', icon: '◔', bg: 'bg-[#dee0ff] text-[#4355b9]' },
  { label: '乐于分享', icon: '◉', bg: 'bg-[#d8e2ff] text-[#005bbf]' },
  { label: '全国冠位', icon: '🏆', bg: 'bg-[#e6e8ea] text-[#727785]', muted: true },
  { label: '创作达人', icon: '⌁', bg: 'bg-[#e6e8ea] text-[#727785]', muted: true }
])

async function loadData() {
  const [profileRes, statsRes, resultRes] = await Promise.all([getStudentProfile(), getStudentStats(), getStudentStageResults(8)])
  profile.value = profileRes || profile.value
  stats.value = statsRes || stats.value
  recentResults.value = Array.isArray(resultRes) ? resultRes : []
  form.value = { nickname: profile.value.nickname || '', avatar: profile.value.avatar || '', phone: profile.value.phone || '' }
}

function stageLabel(stage) {
  if (stage === 'flashcard') return '阶段 1'
  if (stage === 'eng_ch') return '阶段 2'
  if (stage === 'ch_eng') return '阶段 3'
  if (stage === 'spell') return '阶段 4'
  return stage || '阶段'
}

async function saveProfile() {
  try {
    const res = await updateStudentProfile(form.value)
    profile.value.nickname = res?.nickname || form.value.nickname
    profile.value.avatar = res?.avatar || form.value.avatar
    profile.value.phone = res?.phone || form.value.phone
    authStore.nickname = profile.value.nickname || ''
    authStore.avatar = profile.value.avatar || ''
    ElMessage.success('资料已保存')
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  }
}

async function handleLogout() {
  try {
    await logoutApi()
  } catch (_) {}
  authStore.logout('STUDENT')
  router.push('/login')
}

onMounted(loadData)
</script>
