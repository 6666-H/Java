package com.study.english.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaUpgradeRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        upgradeUserTable();
        upgradeWordTable();
        createStudyTaskTable();
        createNotificationTable();
        createStudySessionTable();
        createStudyStageResultTable();
    }

    private void upgradeUserTable() {
        execute("ALTER TABLE `user` ADD COLUMN `student_no` VARCHAR(64) NULL COMMENT '学号/编号'");
        execute("ALTER TABLE `user` ADD COLUMN `grade_class` VARCHAR(64) NULL COMMENT '年级/班级'");
        execute("ALTER TABLE `user` ADD COLUMN `nickname` VARCHAR(64) NULL COMMENT '昵称'");
        execute("ALTER TABLE `user` ADD COLUMN `avatar` VARCHAR(512) NULL COMMENT '头像'");
        execute("ALTER TABLE `user` ADD COLUMN `last_study_book_id` BIGINT NULL COMMENT '最近学习课本'");
        execute("ALTER TABLE `user` ADD COLUMN `last_study_unit_id` BIGINT NULL COMMENT '最近学习单元'");
        execute("ALTER TABLE `user` ADD COLUMN `last_study_at` DATETIME NULL COMMENT '最近学习时间'");
        execute("ALTER TABLE `user` ADD COLUMN `must_change_pwd` TINYINT NOT NULL DEFAULT 0 COMMENT '首次登录强制改密'");
        execute("CREATE UNIQUE INDEX `uk_tenant_student_no` ON `user` (`tenant_id`, `student_no`)");
    }

    private void createStudyTaskTable() {
        execute("""
            CREATE TABLE `study_task` (
                `id` BIGINT NOT NULL AUTO_INCREMENT,
                `tenant_id` VARCHAR(32) NOT NULL,
                `student_id` BIGINT NOT NULL,
                `unit_id` BIGINT NOT NULL,
                `assigned_by` BIGINT DEFAULT NULL,
                `status` VARCHAR(32) NOT NULL DEFAULT 'active',
                `assigned_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (`id`),
                KEY `idx_task_tenant_student` (`tenant_id`, `student_id`),
                KEY `idx_task_tenant_unit` (`tenant_id`, `unit_id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    private void createNotificationTable() {
        execute("""
            CREATE TABLE `notification` (
                `id` BIGINT NOT NULL AUTO_INCREMENT,
                `tenant_id` VARCHAR(32) NOT NULL,
                `user_id` BIGINT NOT NULL,
                `type` VARCHAR(32) NOT NULL,
                `content` VARCHAR(500) NOT NULL,
                `is_read` TINYINT NOT NULL DEFAULT 0,
                `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (`id`),
                KEY `idx_notice_user` (`tenant_id`, `user_id`, `is_read`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    private void upgradeWordTable() {
        execute("ALTER TABLE `word` ADD COLUMN `pos` VARCHAR(64) NULL COMMENT '词性'");
        execute("ALTER TABLE `word` ADD COLUMN `example_zh` VARCHAR(1024) NULL COMMENT '例句翻译'");
    }

    private void createStudySessionTable() {
        execute("""
            CREATE TABLE `study_session` (
                `id` BIGINT NOT NULL AUTO_INCREMENT,
                `tenant_id` VARCHAR(32) NOT NULL,
                `user_id` BIGINT NOT NULL,
                `unit_id` BIGINT NOT NULL,
                `stage` VARCHAR(32) NOT NULL,
                `words_json` LONGTEXT NULL,
                `queue_json` LONGTEXT NULL,
                `error_ids_json` LONGTEXT NULL,
                `user_input_json` LONGTEXT NULL,
                `first_attempt_seen_json` LONGTEXT NULL,
                `first_attempt_correct_json` LONGTEXT NULL,
                `initial_count` INT DEFAULT NULL,
                `answered_count` INT DEFAULT NULL,
                `first_round_correct` INT DEFAULT NULL,
                `correct_attempts` INT DEFAULT NULL,
                `wrong_attempts` INT DEFAULT NULL,
                `started_at` DATETIME DEFAULT NULL,
                `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (`id`),
                UNIQUE KEY `uk_session_user_unit_stage` (`tenant_id`, `user_id`, `unit_id`, `stage`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
        execute("ALTER TABLE `study_session` ADD COLUMN `unit_id` BIGINT NOT NULL DEFAULT 0");
        execute("ALTER TABLE `study_session` ADD COLUMN `stage` VARCHAR(32) NULL");
        execute("ALTER TABLE `study_session` ADD COLUMN `words_json` LONGTEXT NULL");
        execute("ALTER TABLE `study_session` ADD COLUMN `queue_json` LONGTEXT NULL");
        execute("ALTER TABLE `study_session` ADD COLUMN `error_ids_json` LONGTEXT NULL");
        execute("ALTER TABLE `study_session` ADD COLUMN `user_input_json` LONGTEXT NULL");
        execute("ALTER TABLE `study_session` ADD COLUMN `first_attempt_seen_json` LONGTEXT NULL");
        execute("ALTER TABLE `study_session` ADD COLUMN `first_attempt_correct_json` LONGTEXT NULL");
        execute("ALTER TABLE `study_session` ADD COLUMN `initial_count` INT DEFAULT NULL");
        execute("ALTER TABLE `study_session` ADD COLUMN `answered_count` INT DEFAULT NULL");
        execute("ALTER TABLE `study_session` ADD COLUMN `first_round_correct` INT DEFAULT NULL");
        execute("ALTER TABLE `study_session` ADD COLUMN `correct_attempts` INT DEFAULT NULL");
        execute("ALTER TABLE `study_session` ADD COLUMN `wrong_attempts` INT DEFAULT NULL");
        execute("ALTER TABLE `study_session` ADD COLUMN `started_at` DATETIME DEFAULT NULL");
        execute("CREATE UNIQUE INDEX `uk_session_user_unit_stage` ON `study_session` (`tenant_id`, `user_id`, `unit_id`, `stage`)");
    }

    private void createStudyStageResultTable() {
        execute("""
            CREATE TABLE `study_stage_result` (
                `id` BIGINT NOT NULL AUTO_INCREMENT,
                `tenant_id` VARCHAR(32) NOT NULL,
                `user_id` BIGINT NOT NULL,
                `unit_id` BIGINT NOT NULL,
                `stage` VARCHAR(32) NOT NULL,
                `book_name` VARCHAR(128) NULL,
                `unit_name` VARCHAR(128) NULL,
                `total_questions` INT DEFAULT NULL,
                `correct_attempts` INT DEFAULT NULL,
                `wrong_attempts` INT DEFAULT NULL,
                `first_round_correct` INT DEFAULT NULL,
                `stabilized_count` INT DEFAULT NULL,
                `duration_seconds` INT DEFAULT NULL,
                `star_reward` TINYINT NOT NULL DEFAULT 0,
                `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (`id`),
                KEY `idx_stage_result_user` (`tenant_id`, `user_id`, `created_at`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """);
    }

    private void execute(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ex) {
            log.debug("Skip schema upgrade sql: {}", ex.getMessage());
        }
    }
}
