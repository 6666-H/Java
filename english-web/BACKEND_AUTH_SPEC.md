# 后端鉴权需求说明（多角色 + 单设备登录）

前端已支持「同一账号多角色登录」和「按角色单设备登录」。后端需配合实现以下逻辑。

---

## 一、同一账号可登录不同端口

**需求**：同一用户名密码，可根据选择的身份（学生端 / 校长端 / 管理端）登录到对应页面。

**实现**：

1. 用户可拥有**多个角色**（如同时为 SUPER_ADMIN 和 ORG_ADMIN）
2. 登录时前端会传递 `role` 参数（STUDENT | ORG_ADMIN | SUPER_ADMIN）
3. 后端校验：用户是否**拥有**该角色（而不是只能匹配单一角色）
4. 若用户拥有该角色，则签发对应角色的 Token 并返回

**示例**：

- 用户 A 拥有 [SUPER_ADMIN, ORG_ADMIN]
- 选择「管理端」登录 → 返回 SUPER_ADMIN 的 Token → 进入 /admin
- 选择「校长端」登录 → 返回 ORG_ADMIN 的 Token → 进入 /org
- 同一账号可多次登录不同角色，每次返回对应角色的 Token

---

## 二、每个角色单设备登录

**需求**：同一用户同一角色，仅允许一台设备在线；新设备登录会使旧设备 Token 失效。

**实现**：

1. 在用户 + 角色维度维护「当前有效 Token」或「会话 ID」
2. 每次登录（同一 userId + role）时：
   - 使该维度下的**旧 Token / 旧会话**失效
   - 只保留本次登录的 Token 为有效
3. 鉴权时仅接受当前有效的 Token，被挤下线的 Token 返回 401

**存储示例**（可按实现选择）：

- Redis：`session:{userId}:{role}` → 当前有效 tokenId 或 jti
- 或在 Token 中加入 `jti`，服务端维护「有效 jti 集合」，登录时移除旧的 jti

---

## 三、登录接口约定

**POST** `/api/auth/login`

**请求体**：

```json
{
  "username": "string",
  "password": "string",
  "tenantId": "string?",   // 学生端必填
  "role": "STUDENT" | "ORG_ADMIN" | "SUPER_ADMIN"   // 期望登录的角色
}
```

**响应**（成功）：

```json
{
  "code": "0",
  "data": {
    "token": "jwt...",
    "userId": 1,
    "tenantId": "xxx",
    "username": "admin",
    "role": "SUPER_ADMIN"   // 实际登录成功的角色
  }
}
```

**错误**：用户不具备该角色时返回相应错误码与提示。

---

## 四、Token  payload 建议

JWT payload 中建议包含：

- `sub`：userId
- `role`：当前角色（STUDENT / ORG_ADMIN / SUPER_ADMIN）
- `tenantId`：机构 ID（学生端必填）
- `username`：用户名
- `jti`：Token 唯一 ID（用于单设备失效时作废）

---

## 五、前端配合说明

- 前端按**角色**分别存储 Token：`token_SUPER_ADMIN`、`token_ORG_ADMIN`、`token_STUDENT`
- 请求时根据当前路由（/admin、/org、/student）选择对应 Token 放在 Authorization
- 收到 401 时，仅清除该角色的 Token，并跳转登录页
