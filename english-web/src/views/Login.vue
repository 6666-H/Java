<template>
  <div class="login-shell">
    <div class="login-backdrop"></div>

    <section class="login-card">
      <aside class="brand-panel">
        <div class="brand-panel__glow brand-panel__glow--top"></div>
        <div class="brand-panel__glow brand-panel__glow--bottom"></div>
        <div class="brand-panel__orb"></div>

        <header class="brand-header">
          <div class="brand-logo">
            <el-icon><Reading /></el-icon>
          </div>
          <div class="brand-copy">
            <span>永升教育系统</span>
          </div>
        </header>

        <div class="brand-content">
          <p class="brand-kicker">Yongsheng Education</p>
          <h1>提升教育<br />智慧。</h1>
          <p class="brand-description">
            专业的英语语言学习与学术词汇管理空间，帮助学校、老师和学生更高效地完成日常学习任务。
          </p>
        </div>

        <div class="brand-illustration" aria-hidden="true">
          <span class="beam beam-a"></span>
          <span class="beam beam-b"></span>
          <span class="beam beam-c"></span>
          <span class="pane pane-a"></span>
          <span class="pane pane-b"></span>
          <span class="pane pane-c"></span>
        </div>

        <div class="word-card">
          <span class="word-card__eyebrow">每日一词</span>
          <h2>Eloquent</h2>
          <p>口才流利或有说服力的。</p>
          <div class="word-card__tags">
            <span class="tag tag-blue">学术</span>
            <span class="tag tag-orange">CEFR C1</span>
          </div>
        </div>
      </aside>

      <main class="form-panel">
        <div class="form-panel__inner">
          <div class="heading">
            <h2>多角色通用登录页</h2>
            <p>请选择您的身份并登录您的账户</p>
          </div>

          <div class="role-tabs" role="tablist" aria-label="登录角色">
            <button
              v-for="item in roles"
              :key="item.value"
              type="button"
              class="role-tab"
              :class="{ 'is-active': form.role === item.value }"
              @click="switchRole(item.value)"
            >
              {{ item.label }}
            </button>
          </div>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            status-icon
            class="login-form"
            @submit.prevent
          >
            <el-form-item v-if="needsTenantId" label="机构代码" prop="tenantId">
              <el-input
                v-model.trim="form.tenantId"
                placeholder="请输入机构授权代码"
                size="large"
                @keyup.enter="submitLogin"
              >
                <template #prefix>
                  <el-icon><OfficeBuilding /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item :label="usernameLabel" prop="username">
              <el-input
                v-model.trim="form.username"
                :placeholder="usernamePlaceholder"
                size="large"
                @keyup.enter="submitLogin"
              >
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <div class="password-row">
              <span>登录密码</span>
              <button type="button" class="text-link" @click="showForgotHint">忘记密码?</button>
            </div>

            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                :type="passwordVisible ? 'text' : 'password'"
                placeholder="请输入密码"
                size="large"
                @keyup.enter="submitLogin"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
                <template #suffix>
                  <button
                    type="button"
                    class="visibility-button"
                    @click="passwordVisible = !passwordVisible"
                  >
                    <el-icon>
                      <component :is="passwordVisible ? Hide : View" />
                    </el-icon>
                  </button>
                </template>
              </el-input>
            </el-form-item>

            <el-button
              type="primary"
              class="login-button"
              :loading="submitting"
              @click="submitLogin"
            >
              立即登录
            </el-button>
          </el-form>

          <div class="footer-action">
            <span>还没有机构账户？</span>
            <button type="button" class="text-link text-link--strong" @click="showApplyHint">申请入驻</button>
          </div>
        </div>
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Hide, Lock, OfficeBuilding, Reading, User, View } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref()
const passwordVisible = ref(false)
const submitting = ref(false)

const roles = [
  { value: 'STUDENT', label: '学生' },
  { value: 'ORG_ADMIN', label: '机构管理员' },
  { value: 'SUPER_ADMIN', label: '平台管理员' }
]

const form = reactive({
  role: 'STUDENT',
  tenantId: '',
  username: '',
  password: ''
})

const needsTenantId = computed(() => form.role !== 'SUPER_ADMIN')
const usernameLabel = computed(() => (form.role === 'STUDENT' ? '手机号 / 用户名' : '登录账号'))
const usernamePlaceholder = computed(() => {
  if (form.role === 'STUDENT') return '请输入您的登录账号'
  if (form.role === 'ORG_ADMIN') return '请输入机构管理员账号'
  return '请输入平台管理员账号'
})

const rules = computed(() => ({
  tenantId: [
    {
      validator: (_rule, value, callback) => {
        if (needsTenantId.value && !String(value || '').trim()) {
          callback(new Error('请输入机构代码'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}))

function switchRole(role) {
  form.role = role
  form.password = ''
  passwordVisible.value = false
  if (!needsTenantId.value) {
    form.tenantId = ''
  }
  formRef.value?.clearValidate()
}

function resolveRedirect(role) {
  if (role === 'SUPER_ADMIN') return '/admin'
  if (role === 'ORG_ADMIN') return '/org'
  return '/student'
}

async function submitLogin() {
  if (submitting.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const result = await authStore.login(
      form.username,
      form.password,
      needsTenantId.value ? form.tenantId : null,
      form.role
    )

    ElMessage.success(result.mustChangePwd ? '登录成功，请尽快修改初始密码' : '登录成功')
    await router.push(resolveRedirect(result.role || form.role))
  } catch (error) {
    ElMessage.error(error?.message || '登录失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

function showForgotHint() {
  ElMessage.info('请联系机构管理员或平台管理员重置密码')
}

function showApplyHint() {
  ElMessage.info('请联系平台管理员为机构开通账号')
}
</script>

<style scoped>
.login-shell {
  position: relative;
  min-height: 100vh;
  padding: 20px 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: auto;
  background:
    radial-gradient(circle at 12% 18%, rgba(139, 190, 255, 0.24), transparent 24%),
    radial-gradient(circle at 88% 82%, rgba(88, 147, 255, 0.14), transparent 22%),
    linear-gradient(180deg, #f5f8fe 0%, #eef3fb 100%);
}

.login-backdrop {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.75), transparent 40%),
    radial-gradient(circle at bottom right, rgba(46, 111, 234, 0.08), transparent 30%);
  pointer-events: none;
}

.login-card {
  position: relative;
  z-index: 1;
  width: min(1440px, 100%);
  min-height: min(760px, calc(100vh - 40px));
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  isolation: isolate;
  border-radius: 24px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 24px 56px rgba(27, 43, 77, 0.14);
}

.brand-panel {
  position: relative;
  z-index: 0;
  overflow: hidden;
  padding: 36px 40px 28px;
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background:
    linear-gradient(150deg, #0e44b8 0%, #0d57c8 44%, #0a40b2 100%);
}

.brand-panel__glow,
.brand-panel__orb,
.brand-illustration,
.word-card {
  position: absolute;
  pointer-events: none;
}

.brand-panel__glow {
  border-radius: 999px;
  filter: blur(8px);
  opacity: 0.65;
}

.brand-panel__glow--top {
  width: 420px;
  height: 180px;
  top: -40px;
  left: 48px;
  background: linear-gradient(90deg, rgba(0, 213, 255, 0.16), rgba(255, 255, 255, 0));
  transform: rotate(-10deg);
}

.brand-panel__glow--bottom {
  width: 300px;
  height: 300px;
  right: -80px;
  bottom: -100px;
  background: radial-gradient(circle, rgba(0, 144, 255, 0.24), rgba(0, 76, 182, 0));
}

.brand-panel__orb {
  top: 28px;
  right: 32px;
  width: 132px;
  height: 132px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, rgba(92, 142, 255, 0.3), rgba(92, 142, 255, 0.08) 62%, rgba(92, 142, 255, 0) 70%);
}

.brand-header,
.brand-content {
  position: relative;
  z-index: 2;
}

.brand-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-logo {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.98);
  color: #0f57d0;
  box-shadow: 0 12px 28px rgba(8, 30, 96, 0.2);
  font-size: 22px;
}

.brand-copy span {
  display: block;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.brand-content {
  margin-top: 22px;
  max-width: 380px;
}

.brand-kicker {
  margin: 0 0 10px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: rgba(213, 229, 255, 0.84);
}

.brand-content h1 {
  margin: 0;
  font-family: 'Manrope', 'PingFang SC', sans-serif;
  font-size: clamp(36px, 4vw, 56px);
  line-height: 1.04;
  letter-spacing: -0.06em;
}

.brand-description {
  margin: 18px 0 0;
  max-width: 360px;
  font-size: 16px;
  line-height: 1.6;
  color: rgba(215, 229, 255, 0.82);
}

.brand-illustration {
  z-index: 1;
  left: 28px;
  bottom: 72px;
  width: 420px;
  height: 460px;
  opacity: 0.82;
  transform: scale(0.8);
  transform-origin: left bottom;
}

.beam,
.pane {
  position: absolute;
  display: block;
}

.beam {
  width: 12px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(67, 239, 255, 0), rgba(45, 228, 255, 0.92) 18%, rgba(30, 174, 255, 0.28) 100%);
  filter: blur(1px);
}

.beam-a {
  left: 0;
  top: 104px;
  height: 132px;
}

.beam-b {
  left: 86px;
  top: 88px;
  height: 92px;
}

.beam-c {
  left: 2px;
  bottom: 34px;
  height: 168px;
}

.pane {
  background: linear-gradient(180deg, rgba(15, 111, 235, 0.18), rgba(46, 185, 255, 0.38));
  border: 1px solid rgba(79, 204, 255, 0.14);
  box-shadow: inset 0 0 42px rgba(64, 206, 255, 0.18);
}

.pane-a {
  left: 76px;
  top: 0;
  width: 334px;
  height: 548px;
}

.pane-b {
  left: 152px;
  top: 74px;
  width: 220px;
  height: 422px;
  background: linear-gradient(180deg, rgba(18, 136, 240, 0.08), rgba(46, 185, 255, 0.25));
}

.pane-c {
  left: 228px;
  top: 150px;
  width: 156px;
  height: 316px;
  transform: skewY(-24deg);
  background: linear-gradient(180deg, rgba(57, 208, 255, 0.82), rgba(17, 128, 239, 0.32));
}

.word-card {
  z-index: 2;
  left: 40px;
  bottom: 28px;
  width: min(400px, calc(100% - 80px));
  padding: 20px 22px;
  border-radius: 18px;
  color: #101828;
  background: rgba(238, 244, 255, 0.94);
  border: 1px solid rgba(255, 255, 255, 0.36);
  box-shadow: 0 18px 32px rgba(6, 24, 73, 0.14);
  backdrop-filter: blur(16px);
}

.word-card__eyebrow {
  display: inline-block;
  margin-bottom: 10px;
  color: #1057d5;
  font-size: 13px;
  font-weight: 800;
}

.word-card h2 {
  margin: 0 0 8px;
  font-size: 38px;
  line-height: 1;
  letter-spacing: -0.05em;
}

.word-card p {
  margin: 0;
  font-size: 15px;
  color: #475467;
}

.word-card__tags {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}

.tag {
  padding: 6px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 800;
}

.tag-blue {
  color: #1747b6;
  background: #dfe8ff;
}

.tag-orange {
  color: #8a3f00;
  background: #ffe4d2;
}

.form-panel {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 28px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 255, 0.94));
}

.form-panel__inner {
  width: min(460px, 100%);
}

.heading h2 {
  margin: 0;
  font-family: 'Manrope', 'PingFang SC', sans-serif;
  font-size: clamp(28px, 2.4vw, 40px);
  line-height: 1.12;
  letter-spacing: -0.05em;
  color: #111827;
}

.heading p {
  margin: 10px 0 0;
  font-size: 15px;
  line-height: 1.5;
  color: #475467;
}

.role-tabs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 28px;
  padding: 6px;
  border-radius: 16px;
  background: #ebeff3;
  box-shadow: inset 0 2px 8px rgba(15, 23, 42, 0.05);
}

.role-tab {
  border: none;
  min-height: 48px;
  border-radius: 12px;
  background: transparent;
  color: #344054;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.24s ease;
}

.role-tab.is-active {
  color: #0f57d0;
  background: #ffffff;
  box-shadow: 0 8px 18px rgba(15, 87, 208, 0.08);
}

.login-form {
  margin-top: 24px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.login-form :deep(.el-form-item__label) {
  padding-bottom: 8px;
  color: #101828;
  font-size: 14px;
  font-weight: 800;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 52px;
  padding: 0 14px;
  border-radius: 14px !important;
  background: #f4f6fa !important;
  box-shadow: none !important;
}

.login-form :deep(.el-input__inner) {
  font-size: 15px;
  color: #111827;
}

.login-form :deep(.el-input__prefix),
.login-form :deep(.el-input__suffix) {
  color: #667085;
  font-size: 16px;
}

.password-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #101828;
  font-size: 14px;
  font-weight: 800;
}

.text-link {
  border: none;
  padding: 0;
  background: transparent;
  color: #3569e8;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.text-link--strong {
  font-size: 14px;
}

.visibility-button {
  border: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.login-button {
  width: 100%;
  min-height: 56px;
  margin-top: 6px;
  border-radius: 16px;
  font-size: 18px;
  font-weight: 800;
  background: linear-gradient(90deg, #0f60d5 0%, #1f71e3 100%);
  box-shadow: 0 12px 24px rgba(15, 96, 213, 0.18);
}

.footer-action {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid rgba(152, 162, 179, 0.24);
  color: #344054;
  font-size: 14px;
}

@media (max-width: 1180px) {
  .login-card {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .brand-panel {
    min-height: 420px;
  }

  .brand-illustration {
    opacity: 0.35;
    transform: scale(0.62);
  }

  .word-card {
    width: calc(100% - 80px);
  }
}

@media (max-width: 900px) {
  .login-shell {
    padding: 12px;
    align-items: stretch;
  }

  .login-card {
    min-height: auto;
    border-radius: 20px;
  }

  .brand-panel,
  .form-panel {
    padding: 22px 18px;
  }

  .brand-panel {
    min-height: 320px;
  }

  .brand-description {
    max-width: none;
    font-size: 14px;
  }

  .brand-illustration,
  .word-card {
    display: none;
  }
}

@media (max-width: 768px) {
  .login-shell {
    padding: 10px;
  }

  .login-card {
    border-radius: 18px;
  }

  .brand-panel,
  .form-panel {
    padding: 18px 14px;
  }

  .brand-panel {
    min-height: auto;
  }

  .brand-copy span {
    font-size: 15px;
  }

  .brand-content {
    margin-top: 16px;
  }

  .brand-content h1 {
    font-size: 30px;
  }

  .brand-description {
    font-size: 13px;
    line-height: 1.5;
  }

  .heading h2 {
    font-size: 24px;
  }

  .heading p {
    font-size: 13px;
  }

  .role-tabs {
    margin-top: 20px;
    grid-template-columns: 1fr;
  }

  .role-tab {
    min-height: 42px;
    font-size: 14px;
  }

  .login-form {
    margin-top: 18px;
  }

  .login-form :deep(.el-form-item__label),
  .password-row {
    font-size: 13px;
  }

  .login-form :deep(.el-input__wrapper) {
    min-height: 48px;
  }

  .login-form :deep(.el-input__inner) {
    font-size: 14px;
  }

  .login-button {
    min-height: 50px;
    font-size: 16px;
  }

  .footer-action {
    margin-top: 18px;
    padding-top: 16px;
    font-size: 12px;
  }

  .text-link,
  .text-link--strong {
    font-size: 12px;
  }
}
</style>
