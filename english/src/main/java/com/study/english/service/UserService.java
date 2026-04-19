package com.study.english.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.english.entity.User;
import com.study.english.dto.BatchCreateUserRequest;
import com.study.english.dto.CreateUserRequest;
import com.study.english.dto.OrgProfileDto;
import com.study.english.dto.StudentLastStudyRequest;
import com.study.english.dto.StudentProfileUpdateRequest;
import com.study.english.dto.UpdateOrgProfileRequest;
import com.study.english.dto.UpdatePasswordRequest;
import com.study.english.dto.UpdateTenantUserRequest;

import java.util.List;

public interface UserService extends IService<User> {

    User getByTenantAndUsername(String tenantId, String username);

    /** 按用户名+角色查找（用于校长/管理员登录，无 tenantId 时） */
    User getByUsernameAndRole(String username, String role);

    /** 获取指定机构下的校长账号（ORG_ADMIN），若无则返回 null */
    User getOrgAdminByTenantId(String tenantId);

    /** 该租户下已创建用户数（用于名额展示与校验） */
    int countByTenantId(String tenantId);

    /** 该租户下学生账号数（校长不计入名额，用于名额展示与校验） */
    int countStudentsByTenantId(String tenantId);

    /** 更新学生最后活跃时间（打卡/学习时调用） */
    void updateLastActiveAt(Long userId);

    /** 递增 token_version 并返回新值（登录时调用，实现单设备登录） */
    int incrementTokenVersion(Long userId);

    /** 当前租户下的用户列表。role 可选，如 STUDENT 仅返回学生 */
    List<User> listByTenantId(String tenantId, String role);

    /** 连续 N 天未打卡的学生（last_active_at 为空或早于 (now - days)） */
    java.util.List<com.study.english.entity.User> listInactiveStudents(String tenantId, int days);

    /**
     * 在事务内校验名额后创建用户（BCrypt 加密密码）。若 count >= quota 抛 BusinessException("名额已满")。
     */
    User createUser(String tenantId, CreateUserRequest req);

    /**
     * 批量生成账号：prefix+01, prefix+02, ... 同一事务内名额校验。
     */
    List<User> batchCreateUsers(String tenantId, BatchCreateUserRequest req);

    /** 软删除用户（设置 deleted=1, deleted_at=NOW()） */
    void softDeleteUser(Long userId);

    /** 软删除指定租户下所有用户 */
    void softDeleteUsersByTenantId(String tenantId);

    OrgProfileDto toOrgProfile(User user);

    OrgProfileDto updateOrgProfile(String tenantId, Long userId, UpdateOrgProfileRequest req);

    void updateOrgPassword(String tenantId, Long userId, UpdatePasswordRequest req);

    void resetUserPassword(String tenantId, Long operatorUserId, Long userId, String newPassword);

    User updateStudent(String tenantId, Long userId, UpdateTenantUserRequest req);

    User updateStudentProfile(String tenantId, Long userId, StudentProfileUpdateRequest req);

    User updateStudentLastStudy(String tenantId, Long userId, StudentLastStudyRequest req);

    void updateOwnPassword(String tenantId, Long userId, UpdatePasswordRequest req);

    User createDefaultOrgAdmin(String tenantId, String phone, String realName);

    User updateOrgAdminByTenant(String tenantId, String username, String password, String realName, String phone);
}
