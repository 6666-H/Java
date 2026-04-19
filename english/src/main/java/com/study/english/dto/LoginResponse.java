package com.study.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应：含 JWT token 及用户信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /** JWT 令牌，请求头需带 Authorization: Bearer {token} */
    private String token;
    /** 用户 ID */
    private Long userId;
    /** 租户 ID（手机号），超级管理员为 null */
    private String tenantId;
    /** 用户名 */
    private String username;
    /** 角色：SUPER_ADMIN / ORG_ADMIN / STUDENT */
    private String role;
    /** 是否首次登录强制修改密码 */
    private Boolean mustChangePwd;
    /** 昵称 */
    private String nickname;
    /** 头像 */
    private String avatar;
}
