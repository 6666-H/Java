package com.study.english.config;

import com.study.english.entity.Tenant;
import com.study.english.entity.User;
import com.study.english.service.TenantService;
import com.study.english.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 初始化每个角色一个账号（仅当库中尚无用户时执行）。
 * 默认密码均为 123456。密码同时存哈希与明文。
 */
@Slf4j
@Component
public class DataInitializer implements ApplicationRunner {

    private final UserService userService;
    private final TenantService tenantService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(@Lazy UserService userService, TenantService tenantService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.tenantService = tenantService;
        this.passwordEncoder = passwordEncoder;
    }

    private static final String DEFAULT_PASSWORD = "123456";

    @Override
    public void run(ApplicationArguments args) {
        if (userService.count() > 0) {
            log.info("已存在用户，跳过初始化账号");
            return;
        }
        log.info("初始化各角色默认账号...");
        String hash = passwordEncoder.encode(DEFAULT_PASSWORD);

        User superAdmin = new User();
        superAdmin.setTenantId(null);
        superAdmin.setUsername("super");
        superAdmin.setPasswordHash(hash);
        superAdmin.setPasswordPlain(DEFAULT_PASSWORD);
        superAdmin.setRealName("超级管理员");
        superAdmin.setNickname("超级管理员");
        superAdmin.setRole(User.ROLE_SUPER_ADMIN);
        superAdmin.setStatus(User.STATUS_ENABLED);
        superAdmin.setMustChangePwd(1);
        userService.save(superAdmin);
        log.info("已创建超级管理员: super / {}", DEFAULT_PASSWORD);

        Tenant tenant = new Tenant();
        tenant.setId("13800138000");
        tenant.setName("演示机构");
        tenant.setContactName("张校长");
        tenant.setContactPhone("13800138000");
        tenant.setAccountQuota(500);
        tenant.setExpireTime(LocalDateTime.now().plusYears(1));
        tenant.setStatus(Tenant.STATUS_ENABLED);
        tenantService.save(tenant);
        String tenantId = tenant.getId();

        User orgAdmin = new User();
        orgAdmin.setTenantId(tenantId);
        orgAdmin.setUsername("principal");
        orgAdmin.setPasswordHash(hash);
        orgAdmin.setPasswordPlain(DEFAULT_PASSWORD);
        orgAdmin.setRealName("校长");
        orgAdmin.setNickname("校长");
        orgAdmin.setRole(User.ROLE_ORG_ADMIN);
        orgAdmin.setStatus(User.STATUS_ENABLED);
        orgAdmin.setMustChangePwd(1);
        userService.save(orgAdmin);
        log.info("已创建校长: principal / {} (租户Id={})", DEFAULT_PASSWORD, tenantId);

        saveStudent(tenantId, "student01", "学生小明", hash);
        saveStudent(tenantId, "student02", "学生小红", hash);
        saveStudent(tenantId, "student03", "学生小刚", hash);

        // 第二个租户（手机号 13900139000）
        Tenant tenant2 = new Tenant();
        tenant2.setId("13900139000");
        tenant2.setName("第二演示机构");
        tenant2.setContactName("李校长");
        tenant2.setContactPhone("13900139000");
        tenant2.setAccountQuota(200);
        tenant2.setExpireTime(LocalDateTime.now().plusMonths(6));
        tenant2.setStatus(Tenant.STATUS_ENABLED);
        tenantService.save(tenant2);
        String tenantId2 = tenant2.getId();

        User orgAdmin2 = new User();
        orgAdmin2.setTenantId(tenantId2);
        orgAdmin2.setUsername("principal2");
        orgAdmin2.setPasswordHash(hash);
        orgAdmin2.setPasswordPlain(DEFAULT_PASSWORD);
        orgAdmin2.setRealName("李校长");
        orgAdmin2.setNickname("李校长");
        orgAdmin2.setRole(User.ROLE_ORG_ADMIN);
        orgAdmin2.setStatus(User.STATUS_ENABLED);
        orgAdmin2.setMustChangePwd(1);
        userService.save(orgAdmin2);
        log.info("已创建校长: principal2 / {} (租户Id={})", DEFAULT_PASSWORD, tenantId2);

        saveStudent(tenantId2, "stu01", "学员甲", hash);
        saveStudent(tenantId2, "stu02", "学员乙", hash);
    }

    private void saveStudent(String tenantId, String username, String realName, String hash) {
        User student = new User();
        student.setTenantId(tenantId);
        student.setUsername(username);
        student.setPasswordHash(hash);
        student.setPasswordPlain(DEFAULT_PASSWORD);
        student.setRealName(realName);
        student.setNickname(realName);
        student.setRole(User.ROLE_STUDENT);
        student.setStatus(User.STATUS_ENABLED);
        student.setMustChangePwd(1);
        userService.save(student);
        log.info("已创建学生: {} / {} (租户Id={})", username, DEFAULT_PASSWORD, tenantId);
    }
}
