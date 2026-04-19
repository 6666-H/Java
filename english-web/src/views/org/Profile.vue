<template>
  <div class="settings-page">
    <section class="page-header">
      <div>
        <h1>系统设置</h1>
        <p>管理您的机构信息、账户安全及偏好设置</p>
      </div>
    </section>

    <section class="settings-grid">
      <article class="main-card">
        <div class="card-head">
          <h2><el-icon><OfficeBuilding /></el-icon>机构基本信息</h2>
          <button type="button" class="save-btn" :disabled="saving" @click="submitAll">保存修改</button>
        </div>

        <div class="form-grid">
          <label class="field">
            <span>机构名称</span>
            <input v-model="form.realName" type="text" placeholder="机构名称" />
          </label>
          <label class="field">
            <span>联系电话</span>
            <input v-model="form.phone" type="text" placeholder="联系电话" />
          </label>
          <label class="field full">
            <span>机构地址</span>
            <input v-model="address" type="text" placeholder="机构地址" />
          </label>
          <div class="field full">
            <span>机构 Logo</span>
            <div class="logo-row">
              <div class="logo-box">
                <el-icon><Picture /></el-icon>
              </div>
              <div class="logo-copy">
                <strong>更换图片</strong>
                <small>推荐尺寸 400x400px，支持 PNG, JPG</small>
              </div>
            </div>
          </div>
        </div>
      </article>

      <div class="side-stack">
        <article class="service-card">
          <div class="service-head">
            <span>专业版服务</span>
            <el-icon><Medal /></el-icon>
          </div>
          <p>当前服务剩余</p>
          <strong>128 <em>天</em></strong>
          <button type="button">立即续费</button>
        </article>

        <article class="security-card">
          <h3><el-icon><Lock /></el-icon>安全设置</h3>
          <button type="button" class="security-item">
            <div class="item-copy">
              <strong>修改登录密码</strong>
              <span>建议定期更换</span>
            </div>
            <em>›</em>
          </button>
          <button type="button" class="security-item">
            <div class="item-copy">
              <strong>两步验证</strong>
              <span>未开启</span>
            </div>
            <em>›</em>
          </button>
        </article>
      </div>
    </section>

    <section class="prefs-card">
      <h2>界面与偏好设置</h2>
      <div class="prefs-grid">
        <div class="pref-item">
          <span>界面语言</span>
          <div class="select-shell">
            <select>
              <option selected>简体中文 (Chinese Simplified)</option>
              <option>English (US)</option>
            </select>
          </div>
        </div>
        <div class="pref-item">
          <span>系统时区</span>
          <div class="select-shell">
            <select>
              <option selected>(GMT+08:00) 北京, 上海, 香港</option>
            </select>
          </div>
        </div>
        <div class="pref-item switch-wrap">
          <span>通知偏好</span>
          <label class="switch-row">
            <span>邮件接收系统周报</span>
            <el-switch v-model="mailWeekly" />
          </label>
          <label class="switch-row">
            <span>新学员注册 App 推送</span>
            <el-switch v-model="appPush" />
          </label>
        </div>
      </div>
      <footer class="settings-footer">
        <span>© 2024 永盛教育管理系统. 版本 2.4.0-stable</span>
        <div class="footer-links">
          <a href="javascript:void(0)">隐私协议</a>
          <a href="javascript:void(0)">服务条款</a>
        </div>
      </footer>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import request from '../../api/request'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../../stores/auth'
import { Lock, Medal, OfficeBuilding, Picture } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const saving = ref(false)
const mailWeekly = ref(true)
const appPush = ref(false)
const address = ref('北京市朝阳区建国路 88 号永盛大厦 12 层')

const form = reactive({
  tenantId: '',
  username: '',
  realName: '',
  phone: ''
})

async function fetchProfile() {
  try {
    const res = await request.get('/tenant/profile')
    form.tenantId = res?.tenantId || ''
    form.username = res?.username || ''
    form.realName = res?.realName || '永盛国际教育培训中心'
    form.phone = res?.phone || '+86 400-888-9999'
  } catch (err) {
    ElMessage.error(err.message || '获取资料失败')
  }
}

function syncAuthState(username) {
  authStore.username = username
  if (authStore.role === 'ORG_ADMIN') {
    localStorage.setItem('auth_state_ORG_ADMIN', JSON.stringify({
      userId: authStore.userId,
      tenantId: authStore.tenantId,
      username: authStore.username,
      role: authStore.role
    }))
  }
}

async function submitAll() {
  if (!form.username.trim()) {
    ElMessage.warning('请输入登录账号')
    return
  }
  if (form.phone && !/^([0-9+\-\s]{6,20})$/.test(form.phone)) {
    ElMessage.warning('请输入正确的联系电话')
    return
  }

  saving.value = true
  try {
    const res = await request.put('/tenant/profile', {
      username: form.username.trim(),
      realName: form.realName?.trim() || '',
      phone: form.phone?.trim() || ''
    })
    syncAuthState(res?.username || form.username.trim())
    ElMessage.success('资料已保存')
  } catch (err) {
    ElMessage.error(err.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(fetchProfile)
</script>

<style scoped>
.settings-page {
  display: grid;
  gap: 10px;
}

.page-header h1,
.card-head h2,
.prefs-card h2,
.security-card h3 {
  margin: 0;
  font-family: "Manrope", sans-serif;
  color: #191c1e;
}

.page-header h1 {
  font-size: 1.32rem;
  font-weight: 800;
  letter-spacing: -0.04em;
  line-height: 1.05;
}

.page-header p {
  margin: 4px 0 0;
  color: #667085;
  font-size: 0.86rem;
}

.settings-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(280px, 0.7fr);
  gap: 10px;
  align-items: stretch;
}

.main-card,
.service-card,
.security-card,
.prefs-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 6px 16px rgba(25, 28, 30, 0.04);
}

.main-card,
.prefs-card {
  padding: 10px 12px;
}

.main-card {
  height: 100%;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.card-head h2,
.security-card h3 {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 1rem;
}

.save-btn {
  height: 30px;
  padding: 0 11px;
  border: none;
  border-radius: 10px;
  background: #1a73e8;
  color: #fff;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
}

.form-grid,
.prefs-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 10px;
}

.field {
  display: grid;
  gap: 6px;
}

.field.full {
  grid-column: 1 / -1;
}

.field span,
.pref-item span {
  color: #5f6d82;
  font-size: 0.76rem;
  font-weight: 700;
}

.field input,
.select-shell select {
  width: 100%;
  height: 32px;
  border: none;
  border-radius: 9px;
  background: #e6e8ea;
  padding: 0 11px;
  color: #191c1e;
  font-size: 0.82rem;
  outline: none;
}

.logo-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 58px;
}

.logo-box {
  width: 58px;
  height: 58px;
  border-radius: 10px;
  border: 1.5px dashed #c1c6d6;
  background: #f2f4f6;
  color: #98a2b3;
  display: grid;
  place-items: center;
  font-size: 1.6rem;
}

.logo-copy strong,
.logo-copy small {
  display: block;
}

.logo-copy strong {
  color: #005bbf;
  font-size: 0.86rem;
  font-weight: 800;
}

.logo-copy small {
  margin-top: 4px;
  color: #727785;
  font-size: 0.74rem;
}

.side-stack {
  display: grid;
  grid-template-rows: minmax(0, 0.88fr) minmax(0, 1fr);
  gap: 10px;
  height: 100%;
}

.service-card {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  height: 100%;
  padding: 10px 12px;
  color: #fff;
  background: linear-gradient(180deg, #6673ea 0%, #5968e1 100%);
}

.service-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
  font-size: 0.72rem;
  font-weight: 800;
}

.service-card p {
  margin: 0;
  color: rgba(255, 255, 255, 0.86);
}

.service-card strong {
  display: block;
  margin-top: 4px;
  font-family: "Manrope", sans-serif;
  font-size: 1.95rem;
  font-weight: 800;
  line-height: 1;
}

.service-card em {
  font-size: 0.9rem;
  font-style: normal;
}

.service-card button {
  width: 100%;
  height: 30px;
  margin-top: 8px;
  border: none;
  border-radius: 9px;
  background: #fff;
  color: #4355b9;
  font-size: 0.8rem;
  font-weight: 800;
  cursor: pointer;
}

.security-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 10px 12px;
}

.security-item {
  width: 100%;
  padding: 8px 0;
  border: none;
  border-top: 1px solid #edf2f7;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: space-between;
  text-align: left;
  cursor: pointer;
}

.security-item:first-of-type {
  margin-top: 6px;
}

.item-copy strong,
.item-copy span {
  display: block;
}

.item-copy strong {
  color: #191c1e;
  font-weight: 700;
}

.item-copy span {
  margin-top: 2px;
  color: #727785;
  font-size: 0.7rem;
}

.security-item em {
  color: #98a2b3;
  font-style: normal;
  font-size: 1.2rem;
}

.prefs-card h2 {
  margin-bottom: 8px;
  font-size: 1rem;
}

.pref-item {
  display: grid;
  gap: 6px;
}

.switch-wrap {
  grid-column: 1 / -1;
}

.switch-row {
  margin-top: 2px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  min-height: 28px;
}

.settings-footer {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid #edf2f7;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #98a2b3;
  font-size: 0.64rem;
}

.footer-links {
  display: flex;
  align-items: center;
  gap: 14px;
}

.footer-links a {
  color: inherit;
  text-decoration: none;
}

@media (max-width: 1100px) {
  .settings-grid,
  .form-grid,
  .prefs-grid {
    grid-template-columns: 1fr;
  }

  .switch-wrap {
    grid-column: auto;
  }

  .side-stack {
    grid-template-rows: none;
    height: auto;
  }

  .settings-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
