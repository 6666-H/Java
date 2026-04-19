# 功能检查清单

本文档对照需求逐项说明后端实现位置与使用方式。  
**命名对齐**：需求中的 `account_limit` 对应库表字段 `account_quota`，`end_date` 对应 `expire_time`；接口 DTO 使用 `accountQuota`、`expireTime`。

---

## 一、超级管理员（平台方）

### 1. 资源同步：JSON 批量导入 单词-例句-图片-音频 URL

| 项目     | 说明 |
|----------|------|
| **接口** | `POST /admin/sync` |
| **权限** | 仅超级管理员（JWT 中 tenantId 为 null） |
| **实现** | `AdminSyncController`、`SyncService`、`SyncRequest` |
| **JSON 结构** | `books[]` → `units[]` → `words[]`；单词项含：`word`、`phonetic`、`meaning`、`exampleSentence`、`imageUrl`、`audioUrl`、`sortOrder` |
| **逻辑** | 按 (unit_id, word) 幂等：已存在则更新释义/例句/图片/音频，不存在则新增 |

### 2. 租户管控：设置账号上限与到期日期

| 项目     | 说明 |
|----------|------|
| **接口** | `GET /admin/tenants` 列表；`GET /admin/tenants/{id}` 详情；`PUT /admin/tenants/{id}` 更新 |
| **权限** | 仅超级管理员 |
| **实现** | `AdminTenantController`、`TenantUpdateRequest` |
| **Body (PUT)** | `accountQuota`（账号上限）、`expireTime`（到期日期，ISO 格式），可选 `name`、`contactName`、`contactPhone`、`status` |
| **库表** | `tenant.account_quota`、`tenant.expire_time` |

---

## 二、租户管理员（购买人）

### 1. 名额管理：实时“已用/总名额”，名额满时无法创建学生

| 项目     | 说明 |
|----------|------|
| **已用/总名额** | `GET /tenant/quota` → `{ usedCount, totalQuota }`，前端可展示“已用/总名额”并据此禁用“创建学生”按钮 |
| **实现** | `TenantDashboardController.quota()`、`UserService.countByTenantId()`、`TenantQuotaDto` |
| **名额满时创建** | `POST /tenant/users`、`POST /tenant/users/batch` 在事务内校验 `count(users) < account_quota`，若已满抛 `BusinessException("名额已满")` 并回滚 |
| **实现** | `TenantUserController`、`UserServiceImpl.checkQuotaAndIncrement()` |

### 2. 有效期自查：剩余 7 天时后台显著提示“服务即将到期”

| 项目     | 说明 |
|----------|------|
| **接口** | `GET /tenant/info` |
| **返回** | `TenantInfoDto`：`expireTime`、`daysRemaining`（剩余天数）、**`expiringSoon`**（剩余 ≤7 天为 true） |
| **实现** | `TenantDashboardController.info()`、`TenantInfoDto` |
| **前端** | 当 `expiringSoon === true` 时在后台显著展示“服务即将到期”等提示 |

---

## 三、学生端（最终用户）

### 1. 智能推送：每天打开自动看到昨天的错题

| 项目     | 说明 |
|----------|------|
| **接口** | `GET /api/study/next_list?unitId=1&limit=20` |
| **实现** | `StudyController.nextList()`、`StudentWordProgressService.getNextList()` |
| **策略** | 优先拉取 `next_review_time <= NOW()` 且 `wrong_count > 0` 的单词（昨日错题），再拉取今日计划复习，最后新词；学生端每日打开即调用该接口即可“自动看到昨天的错题” |

### 2. 学习闭环：新词学习 → 错题收集 → 隔日强制复习 → 掌握度提升

| 环节         | 接口/表 | 说明 |
|--------------|---------|------|
| **新词学习** | `GET /api/word/next?unitId=` 或 `GET /api/study/next_list` | 新词在 next_list 的“补充策略”中按顺序下发 |
| **错题收集** | `POST /api/study/submit`，`feedbackType: DONT_KNOW` / `SPELLING_ERROR` | 入错题队，`next_review_time = now + 24h`，写入 `error_log` |
| **隔日强制复习** | `GET /api/study/next_list` 优先策略 | `next_review_time <= NOW()` 且 `wrong_count > 0` 的单词优先推送 |
| **掌握度提升** | `POST /api/study/submit`，`feedbackType: KNOW` | 艾宾浩斯 `mastery_level` 递进，`next_review_time` 按 1/2/4/7/15 天间隔；`status` 可至“已掌握” |

拼写校验：`POST /api/study/check_spelling` 传 `wordId`、`userInput`，不一致则增加错误权重并记入 `error_log`，与上述闭环一致。

---

## 四、接口汇总（按角色）

| 角色       | 接口 | 说明 |
|------------|------|------|
| 超管       | `POST /admin/sync` | JSON 批量同步 书/单元/单词（含例句、图片、音频） |
| 超管       | `GET/PUT /admin/tenants`, `GET/PUT /admin/tenants/{id}` | 租户列表、详情、更新（account_quota、expire_time） |
| 超管       | `POST /admin/tenant-book-auth`，`GET /admin/tenant-book-auth/list` | 租户书本授权 |
| 租户管理员 | `GET /tenant/quota` | 已用/总名额 |
| 租户管理员 | `GET /tenant/info` | 当前租户信息、到期日、expiringSoon（≤7 天） |
| 租户管理员 | `POST /tenant/users`，`POST /tenant/users/batch` | 创建学生（名额满时接口报错） |
| 学生       | `GET /api/books` | 当前租户可用的书本列表 |
| 学生       | `GET /api/study/next_list?unitId=&limit=` | 智能复习列表（错题优先） |
| 学生       | `POST /api/study/submit` | 学习反馈（认识/不认识/拼写错误） |
| 学生       | `POST /api/study/check_spelling` | 拼写校验并增加错误权重 |
