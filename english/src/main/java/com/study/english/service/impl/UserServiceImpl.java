package com.study.english.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.english.dto.BatchCreateUserRequest;
import com.study.english.dto.CreateUserRequest;
import com.study.english.dto.OrgProfileDto;
import com.study.english.dto.StudentLastStudyRequest;
import com.study.english.dto.StudentProfileUpdateRequest;
import com.study.english.dto.UpdateOrgProfileRequest;
import com.study.english.dto.UpdatePasswordRequest;
import com.study.english.dto.UpdateTenantUserRequest;
import com.study.english.entity.Tenant;
import com.study.english.entity.User;
import com.study.english.exception.BusinessException;
import com.study.english.mapper.UserMapper;
import com.study.english.service.TenantService;
import com.study.english.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public List<User> listByTenantId(String tenantId, String role) {
        if (tenantId == null) return new ArrayList<>();
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<User>()
                .eq(User::getTenantId, tenantId)
                .orderByAsc(User::getId);
        if (role != null && !role.isEmpty()) q.eq(User::getRole, role);
        return list(q);
    }

    private final TenantService tenantService;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(TenantService tenantService, PasswordEncoder passwordEncoder) {
        this.tenantService = tenantService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User getByTenantAndUsername(String tenantId, String username) {
        LambdaQueryWrapper<User> q = new LambdaQueryWrapper<>();
        if (tenantId == null || tenantId.isEmpty()) {
            q.isNull(User::getTenantId);
        } else {
            q.eq(User::getTenantId, tenantId);
        }
        q.eq(User::getUsername, username);
        return getOne(q);
    }

    @Override
    public User getByUsernameAndRole(String username, String role) {
        if (username == null || role == null) return null;
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getRole, role)
                .last("LIMIT 1"));
    }

    @Override
    public User getOrgAdminByTenantId(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) return null;
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getTenantId, tenantId)
                .eq(User::getRole, User.ROLE_ORG_ADMIN)
                .last("LIMIT 1"));
    }

    @Override
    public int countByTenantId(String tenantId) {
        return baseMapper.countByTenantId(tenantId);
    }

    @Override
    public int countStudentsByTenantId(String tenantId) {
        return baseMapper.countStudentsByTenantId(tenantId);
    }

    @Override
    public void updateLastActiveAt(Long userId) {
        if (userId == null) return;
        User u = getById(userId);
        if (u != null) {
            u.setLastActiveAt(java.time.LocalDateTime.now());
            updateById(u);
        }
    }

    @Override
    public int incrementTokenVersion(Long userId) {
        if (userId == null) return 0;
        User u = getById(userId);
        if (u == null) return 0;
        int next = (u.getTokenVersion() == null ? 0 : u.getTokenVersion()) + 1;
        u.setTokenVersion(next);
        updateById(u);
        return next;
    }

    @Override
    public List<User> listInactiveStudents(String tenantId, int days) {
        if (days <= 0) days = 7;
        java.time.LocalDateTime before = java.time.LocalDateTime.now().minusDays(days);
        return baseMapper.selectInactiveStudents(tenantId, User.ROLE_STUDENT, before);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User createUser(String tenantId, CreateUserRequest req) {
        checkQuotaAndIncrement(tenantId, 1);
        String username = req.getUsername() != null ? req.getUsername().trim() : "";
        if (username.isEmpty()) throw new BusinessException("学生用户名不能为空");
        if (getByTenantAndUsername(tenantId, username) != null) {
            throw new BusinessException("该机构下学生用户名已存在，请使用其他用户名");
        }
        String studentNo = trimToNull(req.getStudentNo());
        if (studentNo != null) {
            User duplicateNo = getOne(new LambdaQueryWrapper<User>()
                    .eq(User::getTenantId, tenantId)
                    .eq(User::getRole, User.ROLE_STUDENT)
                    .eq(User::getStudentNo, studentNo)
                    .last("LIMIT 1"));
            if (duplicateNo != null) throw new BusinessException("学号/编号已存在");
        }
        User user = new User();
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setPasswordPlain(req.getPassword());
        user.setRealName(trimToNull(req.getRealName()));
        user.setStudentNo(studentNo);
        user.setGradeClass(trimToNull(req.getGradeClass()));
        user.setNickname(trimToNull(req.getRealName()));
        user.setRole(req.getRole());
        user.setStatus(User.STATUS_ENABLED);
        user.setMustChangePwd(1);
        save(user);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<User> batchCreateUsers(String tenantId, BatchCreateUserRequest req) {
        int n = req.getCount();
        checkQuotaAndIncrement(tenantId, n);
        int pad = n <= 100 ? 2 : (n <= 1000 ? 3 : 4);
        List<User> created = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            String username = req.getPrefix() + String.format("%0" + pad + "d", i);
            if (getByTenantAndUsername(tenantId, username) != null) continue;
            User user = new User();
            user.setTenantId(tenantId);
            user.setUsername(username);
            user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
            user.setPasswordPlain(req.getPassword());
            user.setRole(User.ROLE_STUDENT);
            user.setStatus(User.STATUS_ENABLED);
            user.setMustChangePwd(1);
            save(user);
            created.add(user);
        }
        return created;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void softDeleteUser(Long userId) {
        if (userId == null) return;
        LocalDateTime now = LocalDateTime.now();
        update(new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getDeleted, 1)
                .set(User::getDeletedAt, now));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void softDeleteUsersByTenantId(String tenantId) {
        if (tenantId == null) return;
        LocalDateTime now = LocalDateTime.now();
        update(new LambdaUpdateWrapper<User>()
                .eq(User::getTenantId, tenantId)
                .set(User::getDeleted, 1)
                .set(User::getDeletedAt, now));
    }

    @Override
    public OrgProfileDto toOrgProfile(User user) {
        if (user == null) return null;
        OrgProfileDto dto = new OrgProfileDto();
        dto.setUserId(user.getId());
        dto.setTenantId(user.getTenantId());
        dto.setUsername(user.getUsername());
        dto.setRealName(user.getRealName());
        dto.setPhone(user.getPhone());
        dto.setPasswordPlain(user.getPasswordPlain());
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrgProfileDto updateOrgProfile(String tenantId, Long userId, UpdateOrgProfileRequest req) {
        User user = getById(userId);
        if (user == null || !tenantId.equals(user.getTenantId())) throw new BusinessException("账号不存在");
        String username = req.getUsername() == null ? "" : req.getUsername().trim();
        if (username.isEmpty()) throw new BusinessException("账号不能为空");
        User existing = getByTenantAndUsername(tenantId, username);
        if (existing != null && !existing.getId().equals(userId)) {
            throw new BusinessException("该机构下账号已存在");
        }
        String phone = req.getPhone() == null ? "" : req.getPhone().trim();
        if (!phone.isEmpty() && !phone.matches("^1\\d{10}$")) {
            throw new BusinessException("手机号格式不正确");
        }
        user.setUsername(username);
        user.setRealName(req.getRealName() == null ? null : req.getRealName().trim());
        user.setPhone(phone.isEmpty() ? null : phone);
        updateById(user);
        return toOrgProfile(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrgPassword(String tenantId, Long userId, UpdatePasswordRequest req) {
        User user = getById(userId);
        if (user == null || !tenantId.equals(user.getTenantId())) throw new BusinessException("账号不存在");
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException("当前密码不正确");
        }
        String newPassword = req.getNewPassword() == null ? "" : req.getNewPassword().trim();
        if (newPassword.length() < 6) throw new BusinessException("新密码至少 6 位");
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordPlain(newPassword);
        user.setMustChangePwd(0);
        updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetUserPassword(String tenantId, Long operatorUserId, Long userId, String newPassword) {
        User user = getById(userId);
        if (user == null || !tenantId.equals(user.getTenantId())) throw new BusinessException("用户不存在");
        if (newPassword == null || newPassword.trim().length() < 6) throw new BusinessException("新密码至少 6 位");
        user.setPasswordHash(passwordEncoder.encode(newPassword.trim()));
        user.setPasswordPlain(newPassword.trim());
        user.setMustChangePwd(1);
        updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User updateStudent(String tenantId, Long userId, UpdateTenantUserRequest req) {
        User user = getById(userId);
        if (user == null || !tenantId.equals(user.getTenantId()) || !User.ROLE_STUDENT.equals(user.getRole())) {
            throw new BusinessException("学生账号不存在");
        }
        String username = req.getUsername() == null ? "" : req.getUsername().trim();
        if (username.isEmpty()) throw new BusinessException("用户名不能为空");
        User existing = getByTenantAndUsername(tenantId, username);
        if (existing != null && !existing.getId().equals(userId)) {
            throw new BusinessException("该机构下学生用户名已存在");
        }
        if (req.getStudentNo() != null && !req.getStudentNo().trim().isEmpty()) {
            User duplicateNo = getOne(new LambdaQueryWrapper<User>()
                    .eq(User::getTenantId, tenantId)
                    .eq(User::getRole, User.ROLE_STUDENT)
                    .eq(User::getStudentNo, req.getStudentNo().trim())
                    .ne(User::getId, userId)
                    .last("LIMIT 1"));
            if (duplicateNo != null) throw new BusinessException("学号/编号已存在");
        }
        user.setUsername(username);
        user.setRealName(trimToNull(req.getRealName()));
        user.setStudentNo(trimToNull(req.getStudentNo()));
        user.setGradeClass(trimToNull(req.getGradeClass()));
        if (req.getStatus() != null) user.setStatus(req.getStatus());
        String nextPassword = trimToNull(req.getPassword());
        if (nextPassword != null) {
            if (nextPassword.length() < 6) throw new BusinessException("密码至少 6 位");
            user.setPasswordHash(passwordEncoder.encode(nextPassword));
            user.setPasswordPlain(nextPassword);
            user.setMustChangePwd(1);
        }
        updateById(user);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User updateStudentProfile(String tenantId, Long userId, StudentProfileUpdateRequest req) {
        User user = getById(userId);
        if (user == null || !sameTenant(tenantId, user.getTenantId())) throw new BusinessException("账号不存在");
        user.setNickname(trimToNull(req.getNickname()));
        user.setAvatar(trimToNull(req.getAvatar()));
        String phone = trimToNull(req.getPhone());
        if (phone != null && !phone.matches("^1\\d{10}$")) {
            throw new BusinessException("手机号格式不正确");
        }
        user.setPhone(phone);
        updateById(user);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User updateStudentLastStudy(String tenantId, Long userId, StudentLastStudyRequest req) {
        User user = getById(userId);
        if (user == null || !sameTenant(tenantId, user.getTenantId())) throw new BusinessException("账号不存在");
        user.setLastStudyBookId(req == null ? null : req.getBookId());
        user.setLastStudyUnitId(req == null ? null : req.getUnitId());
        user.setLastStudyAt(LocalDateTime.now());
        updateById(user);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOwnPassword(String tenantId, Long userId, UpdatePasswordRequest req) {
        User user = getById(userId);
        if (user == null || !sameTenant(tenantId, user.getTenantId())) throw new BusinessException("账号不存在");
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException("当前密码不正确");
        }
        String newPassword = req.getNewPassword() == null ? "" : req.getNewPassword().trim();
        if (newPassword.length() < 6) throw new BusinessException("新密码至少 6 位");
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordPlain(newPassword);
        user.setMustChangePwd(0);
        updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User createDefaultOrgAdmin(String tenantId, String phone, String realName) {
        if (tenantId == null || tenantId.trim().isEmpty()) throw new BusinessException("租户不存在");
        if (getOrgAdminByTenantId(tenantId) != null) {
            throw new BusinessException("该机构已存在校长账号");
        }
        String username = tenantId.trim();
        String password = "123456";
        User user = new User();
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPasswordPlain(password);
        user.setRealName(realName == null || realName.trim().isEmpty() ? "校长" : realName.trim());
        user.setPhone(phone == null || phone.trim().isEmpty() ? username : phone.trim());
        user.setRole(User.ROLE_ORG_ADMIN);
        user.setStatus(User.STATUS_ENABLED);
        user.setMustChangePwd(1);
        save(user);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User updateOrgAdminByTenant(String tenantId, String username, String password, String realName, String phone) {
        User user = getOrgAdminByTenantId(tenantId);
        if (user == null) throw new BusinessException("校长账号不存在");
        String nextUsername = username == null ? "" : username.trim();
        if (nextUsername.isEmpty()) throw new BusinessException("校长账号不能为空");
        User existing = getByTenantAndUsername(tenantId, nextUsername);
        if (existing != null && !existing.getId().equals(user.getId())) {
            throw new BusinessException("该机构下账号已存在");
        }
        String nextPhone = phone == null ? "" : phone.trim();
        if (!nextPhone.isEmpty() && !nextPhone.matches("^1\\d{10}$")) {
            throw new BusinessException("手机号格式不正确");
        }
        user.setUsername(nextUsername);
        user.setRealName(realName == null || realName.trim().isEmpty() ? null : realName.trim());
        user.setPhone(nextPhone.isEmpty() ? null : nextPhone);
        if (password != null && !password.trim().isEmpty()) {
            String nextPassword = password.trim();
            if (nextPassword.length() < 6) throw new BusinessException("密码至少 6 位");
            user.setPasswordHash(passwordEncoder.encode(nextPassword));
            user.setPasswordPlain(nextPassword);
            user.setMustChangePwd(1);
        }
        updateById(user);
        return user;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean sameTenant(String expectedTenantId, String actualTenantId) {
        if (expectedTenantId == null || expectedTenantId.isBlank()) {
            return actualTenantId == null || actualTenantId.isBlank();
        }
        return expectedTenantId.equals(actualTenantId);
    }

    private void checkQuotaAndIncrement(String tenantId, int addCount) {
        Tenant tenant = tenantService.getById(tenantId);
        if (tenant == null) throw new BusinessException("租户不存在");
        int currentStudents = baseMapper.countStudentsByTenantId(tenantId);
        if (currentStudents + addCount > tenant.getAccountQuota()) {
            throw new BusinessException("名额已满");
        }
    }
}
