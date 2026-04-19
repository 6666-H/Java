<template>
  <div class="settings-page">
    <header class="page-header">
      <div class="system-chip">
        <el-icon><Setting /></el-icon>
        <span>系统管理</span>
      </div>
      <h2>全局设置</h2>
      <p>配置永升教育系统的核心运行参数与安全策略。</p>
    </header>

    <section class="section-block">
      <div class="section-title">
        <span class="title-icon blue"><el-icon><Tools /></el-icon></span>
        <h3>基础设置</h3>
      </div>

      <div class="settings-card" v-loading="loading">
        <div class="info-grid">
          <label>
            <span>系统名称</span>
            <input v-model="form.systemName" type="text" />
          </label>
          <label>
            <span>技术支持邮箱</span>
            <input v-model="form.supportEmail" type="email" />
          </label>
          <label class="full">
            <span>版权信息文字</span>
            <textarea v-model="form.footerText" rows="3"></textarea>
          </label>
        </div>
      </div>

      <div class="settings-card logo-card">
        <div class="logo-preview">
          <img v-if="form.logoUrl" :src="form.logoUrl" alt="Logo" />
          <span v-else>{{ logoFallback }}</span>
        </div>
        <div class="logo-copy">
          <h4>品牌 Logo</h4>
          <p>上传机构的官方标志。建议使用透明背景的 PNG 或 SVG 格式，尺寸至少为 512x512 像素，文件大小不超过 2MB。</p>
          <div class="logo-actions">
            <el-upload :show-file-list="false" :auto-upload="false" accept="image/*" :on-change="handleLogoChange">
              <el-button type="primary">选择文件</el-button>
            </el-upload>
            <button type="button" class="text-link" @click="removeLogo">移除当前</button>
          </div>
        </div>
      </div>
    </section>

    <section class="section-block">
      <div class="section-title">
        <span class="title-icon amber"><el-icon><Lock /></el-icon></span>
        <h3>安全策略</h3>
      </div>

      <div class="security-grid">
        <article class="settings-card security-card">
          <div class="toggle-row">
            <div>
              <strong>强密码策略</strong>
              <p>强制要求包含大小写字母、数字及特殊字符</p>
            </div>
            <el-switch v-model="form.strongPasswordEnabled" />
          </div>

          <div class="toggle-row">
            <div>
              <strong>多因素身份验证 (MFA)</strong>
              <p>启用后，登录时需进行手机或邮箱验证</p>
            </div>
            <el-switch v-model="form.mfaEnabled" />
          </div>
        </article>

        <article class="settings-card metrics-card">
          <label>
            <span>登录失败锁定阈值</span>
            <div class="metric-input">
              <input v-model.number="form.loginFailureLimit" type="number" min="1" max="20" />
              <em>次尝试后账户锁定</em>
            </div>
          </label>

          <label>
            <span>会话超时时间</span>
            <div class="metric-input">
              <input v-model.number="form.sessionTimeoutMinutes" type="number" min="5" max="1440" />
              <em>分钟无操作后自动登出</em>
            </div>
          </label>
        </article>
      </div>
    </section>

    <footer class="sticky-footer">
      <div class="footer-meta">
        <el-icon><InfoFilled /></el-icon>
        <span>上次更新: {{ formatDateTime(form.updatedAt) }}</span>
      </div>
      <div class="footer-actions">
        <el-button @click="fetchSettings">取消更改</el-button>
        <el-button type="primary" :loading="saving" @click="saveSettingsForm">保存全局配置</el-button>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getSystemSettings, saveSystemSettings, uploadSystemLogo } from '../../api/admin'
import { InfoFilled, Lock, Setting, Tools } from '@element-plus/icons-vue'

const loading = ref(false)
const saving = ref(false)
const form = ref(createDefaultForm())

const logoFallback = computed(() => String(form.value.systemName || 'L').trim().slice(0, 1).toUpperCase())

function createDefaultForm() {
  return {
    systemName: '',
    supportEmail: '',
    footerText: '',
    logoUrl: '',
    strongPasswordEnabled: false,
    mfaEnabled: false,
    loginFailureLimit: 5,
    sessionTimeoutMinutes: 30,
    updatedAt: ''
  }
}

function formatDateTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

async function fetchSettings() {
  loading.value = true
  try {
    const res = await getSystemSettings()
    form.value = { ...createDefaultForm(), ...(res || {}) }
  } catch (error) {
    ElMessage.error(error.message || '获取系统设置失败')
  } finally {
    loading.value = false
  }
}

async function saveSettingsForm() {
  saving.value = true
  try {
    const res = await saveSystemSettings(form.value)
    form.value = { ...createDefaultForm(), ...(res || form.value) }
    ElMessage.success('系统设置已保存')
  } catch (error) {
    ElMessage.error(error.message || '保存系统设置失败')
  } finally {
    saving.value = false
  }
}

async function handleLogoChange(uploadFile) {
  const file = uploadFile?.raw
  if (!file) return
  saving.value = true
  try {
    const res = await uploadSystemLogo(file)
    form.value = { ...form.value, ...(res || {}) }
    ElMessage.success('Logo 已更新')
  } catch (error) {
    ElMessage.error(error.message || '上传 Logo 失败')
  } finally {
    saving.value = false
  }
}

function removeLogo() {
  form.value = { ...form.value, logoUrl: '' }
}

onMounted(fetchSettings)
</script>

<style scoped>
.settings-page {
  display: grid;
  gap: 18px;
  padding-bottom: 88px;
}

.page-header h2,
.section-title h3,
.logo-copy h4 {
  margin: 0;
  font-family: 'Manrope', 'PingFang SC', sans-serif;
  font-weight: 800;
}

.page-header h2 {
  margin-top: 4px;
  font-size: 36px;
  letter-spacing: -0.05em;
  color: #181c23;
}

.page-header p {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 15px;
}

.system-chip,
.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.system-chip {
  color: #0b5fd1;
  font-weight: 800;
  font-size: 12px;
}

.section-block {
  display: grid;
  gap: 14px;
}

.title-icon {
  width: 30px;
  height: 30px;
  border-radius: 10px;
  display: grid;
  place-items: center;
}

.title-icon.blue {
  color: #0b5fd1;
  background: #dbeafe;
}

.title-icon.amber {
  color: #c45a1d;
  background: #ffedd5;
}

.settings-card {
  padding: 20px;
  border-radius: 18px;
  border: 1px solid rgba(193, 198, 214, 0.3);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.045);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px 20px;
}

.info-grid label,
.metrics-card label {
  display: block;
}

.info-grid label span,
.metrics-card span {
  display: block;
  margin-bottom: 8px;
  color: #6b7280;
  font-size: 12px;
  font-weight: 700;
}

.info-grid input,
.info-grid textarea,
.metric-input input {
  width: 100%;
  border: none;
  border-radius: 12px;
  padding: 12px 14px;
  background: #e9ecef;
  font-size: 14px;
  outline: none;
}

.info-grid textarea {
  resize: vertical;
}

.info-grid .full {
  grid-column: 1 / -1;
}

.logo-card {
  display: flex;
  align-items: center;
  gap: 20px;
}

.logo-preview {
  width: 100px;
  height: 100px;
  border-radius: 20px;
  display: grid;
  place-items: center;
  overflow: hidden;
  background: #f3f4f6;
  color: #111827;
  font-size: 34px;
  font-weight: 800;
}

.logo-preview img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 10px;
}

.logo-copy {
  flex: 1;
}

.logo-copy p {
  margin: 8px 0 0;
  color: #6b7280;
  line-height: 1.55;
  font-size: 13px;
}

.logo-actions {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.text-link {
  border: none;
  background: transparent;
  color: #0b5fd1;
  font-weight: 700;
  cursor: pointer;
}

.security-grid {
  display: grid;
  grid-template-columns: 1fr 0.9fr;
  gap: 14px;
}

.security-card,
.metrics-card {
  display: grid;
  gap: 18px;
}

.toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.toggle-row strong,
.toggle-row p {
  display: block;
}

.toggle-row strong {
  color: #111827;
}

.toggle-row p,
.metric-input em {
  margin: 4px 0 0;
  color: #6b7280;
  font-size: 12px;
  font-style: normal;
}

.metric-input {
  display: flex;
  align-items: center;
  gap: 10px;
}

.metric-input input {
  width: 84px;
}

.sticky-footer {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 216px;
  padding: 12px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 -8px 20px rgba(15, 23, 42, 0.04);
  backdrop-filter: blur(14px);
}

.footer-meta,
.footer-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.footer-meta {
  color: #6b7280;
}

@media (max-width: 1024px) {
  .sticky-footer {
    left: 96px;
  }
}

@media (max-width: 900px) {
  .info-grid,
  .security-grid {
    grid-template-columns: 1fr;
  }

  .logo-card {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 768px) {
  .sticky-footer {
    left: 0;
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
