-- ============================================================
-- 百词斩 SaaS Web 版 - 数据库完整脚本（唯一 SQL 文件，MySQL 8+）
-- 执行本文件即可完成建表+初始化。多租户隔离：除公共词库外，表均含 tenant_id
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 租户表
-- ----------------------------
DROP TABLE IF EXISTS `tenant`;
CREATE TABLE `tenant` (
    `id`             VARCHAR(32)  NOT NULL COMMENT '主键=手机号',
    `name`           VARCHAR(128) NOT NULL COMMENT '租户名称/机构名',
    `contact_name`   VARCHAR(64)  DEFAULT NULL COMMENT '联系人',
    `contact_phone`  VARCHAR(32)  DEFAULT NULL COMMENT '联系电话',
    `account_quota`  INT          NOT NULL DEFAULT 0 COMMENT '账号额度（可创建学生数）',
    `expire_time`    DATETIME     NOT NULL COMMENT '到期时间',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除：0-正常 1-已删除',
    `deleted_at`     DATETIME     DEFAULT NULL COMMENT '软删除时间',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_status_expire` (`status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户表';

-- ----------------------------
-- 2. 用户表
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id`      VARCHAR(32) DEFAULT NULL COMMENT '租户ID=手机号，超级管理员为 NULL',
    `username`       VARCHAR(64)  NOT NULL COMMENT '登录名',
    `password_hash`  VARCHAR(256) NOT NULL COMMENT '密码哈希',
    `password_plain` VARCHAR(128) DEFAULT NULL COMMENT '密码明文',
    `real_name`      VARCHAR(64)  DEFAULT NULL COMMENT '真实姓名',
    `phone`          VARCHAR(32)  DEFAULT NULL COMMENT '手机号',
    `student_no`     VARCHAR(64)  DEFAULT NULL COMMENT '学号/编号',
    `grade_class`    VARCHAR(64)  DEFAULT NULL COMMENT '年级/班级',
    `nickname`       VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    `avatar`         VARCHAR(512) DEFAULT NULL COMMENT '头像',
    `last_study_book_id` BIGINT   DEFAULT NULL COMMENT '最近学习课本',
    `last_study_unit_id` BIGINT   DEFAULT NULL COMMENT '最近学习单元',
    `last_study_at`   DATETIME    DEFAULT NULL COMMENT '最近学习时间',
    `must_change_pwd` TINYINT     NOT NULL DEFAULT 0 COMMENT '首次登录强制改密',
    `role`           VARCHAR(32)  NOT NULL COMMENT '角色：SUPER_ADMIN/ORG_ADMIN/STUDENT',
    `last_active_at` DATETIME     DEFAULT NULL COMMENT '最后活跃时间',
    `token_version`  INT          NOT NULL DEFAULT 0 COMMENT '登录令牌版本，每次登录递增',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '软删除：0-正常 1-已删除',
    `deleted_at`     DATETIME     DEFAULT NULL COMMENT '软删除时间',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_username` (`tenant_id`, `username`),
    UNIQUE KEY `uk_tenant_student_no` (`tenant_id`, `student_no`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- 3. 书本表
-- ----------------------------
DROP TABLE IF EXISTS `book`;
CREATE TABLE `book` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `version_name`  VARCHAR(64)  DEFAULT NULL COMMENT '版本（如人教版、外研版）',
    `grade`         VARCHAR(32)  DEFAULT NULL COMMENT '年级（如三年级）',
    `name`          VARCHAR(128) NOT NULL COMMENT '册别/书名',
    `cover_url`     VARCHAR(512) DEFAULT NULL COMMENT '封面图 URL',
    `description`   VARCHAR(1024) DEFAULT NULL COMMENT '简介',
    `version`       INT          NOT NULL DEFAULT 1 COMMENT '版本号',
    `sort_order`    INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='全局书本';

-- ----------------------------
-- 4. 单元表
-- ----------------------------
DROP TABLE IF EXISTS `unit`;
CREATE TABLE `unit` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `book_id`    BIGINT       NOT NULL COMMENT '书本ID',
    `name`       VARCHAR(128) NOT NULL COMMENT '单元名',
    `sort_order` INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_book_id` (`book_id`),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='全局单元';

-- ----------------------------
-- 5. 单词表
-- ----------------------------
DROP TABLE IF EXISTS `word`;
CREATE TABLE `word` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `unit_id`          BIGINT       NOT NULL COMMENT '单元ID',
    `word`             VARCHAR(128) NOT NULL COMMENT '单词',
    `phonetic`         VARCHAR(128) DEFAULT NULL COMMENT '音标',
    `pos`              VARCHAR(64) DEFAULT NULL COMMENT '词性',
    `meaning`          VARCHAR(512) NOT NULL COMMENT '释义',
    `example_sentence` VARCHAR(1024) DEFAULT NULL COMMENT '例句',
    `example_zh`       VARCHAR(1024) DEFAULT NULL COMMENT '例句翻译',
    `audio_url`        VARCHAR(512) DEFAULT NULL COMMENT '发音 URL',
    `image_url`        VARCHAR(512) DEFAULT NULL COMMENT '图片 URL',
    `sort_order`       INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at`       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_unit_id` (`unit_id`),
    UNIQUE KEY `uk_unit_word` (`unit_id`, `word`),
    KEY `idx_word` (`word`(32)),
    KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公共词库-单词';

-- ----------------------------
-- 6. 学生单词进度表
-- ----------------------------
DROP TABLE IF EXISTS `student_word_progress`;
CREATE TABLE `student_word_progress` (
    `id`               BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id`        VARCHAR(32) NOT NULL COMMENT '租户ID',
    `user_id`          BIGINT   NOT NULL COMMENT '学生用户ID',
    `word_id`          BIGINT   NOT NULL COMMENT '单词ID',
    `mastery_level`    TINYINT  NOT NULL DEFAULT 0 COMMENT '掌握阶段 0-5',
    `correct_count`    INT      NOT NULL DEFAULT 0 COMMENT '累计正确次数',
    `wrong_count`      INT      NOT NULL DEFAULT 0 COMMENT '累计错误次数',
    `last_error_time`  DATETIME DEFAULT NULL COMMENT '最后一次错误时间',
    `is_wrong`         TINYINT  NOT NULL DEFAULT 0 COMMENT '是否当前为错题',
    `consecutive_correct_count` INT NOT NULL DEFAULT 0 COMMENT '连续正确次数',
    `consecutive_wrong_count`   INT NOT NULL DEFAULT 0 COMMENT '连续错误次数',
    `in_reinforcement` TINYINT  NOT NULL DEFAULT 0 COMMENT '是否在错词强化池',
    `status`           TINYINT  NOT NULL DEFAULT 0 COMMENT '0:未学 1:学习中 2:已掌握',
    `last_review_time` DATETIME DEFAULT NULL COMMENT '上次复习时间',
    `next_review_time` DATETIME DEFAULT NULL COMMENT '下次复习时间',
    `last_study_at`    DATETIME DEFAULT NULL COMMENT '最后学习时间',
    `strength`         DECIMAL(3,2) DEFAULT 0 COMMENT '熟练度 0~1（教材/单元模式）',
    `repetitions`      INT      DEFAULT 0 COMMENT '间隔重复次数（教材/单元模式）',
    `created_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user_word` (`tenant_id`, `user_id`, `word_id`),
    KEY `idx_tenant_user` (`tenant_id`, `user_id`),
    KEY `idx_word_id` (`word_id`),
    KEY `idx_next_review` (`tenant_id`, `user_id`, `next_review_time`),
    KEY `idx_wrong_review` (`tenant_id`, `user_id`, `next_review_time`, `wrong_count`),
    KEY `idx_reinforcement` (`tenant_id`, `user_id`, `in_reinforcement`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生单词进度';

-- ----------------------------
-- 7. 学习会话表
-- ----------------------------
DROP TABLE IF EXISTS `study_session`;
CREATE TABLE `study_session` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id`           VARCHAR(32)  NOT NULL COMMENT '租户ID',
    `user_id`             BIGINT       NOT NULL COMMENT '用户ID',
    `unit_id`             BIGINT       NOT NULL COMMENT '单元ID',
    `stage`               VARCHAR(32)  NOT NULL COMMENT '学习阶段/模式',
    `words_json`          LONGTEXT     DEFAULT NULL COMMENT '本轮词表',
    `queue_json`          LONGTEXT     DEFAULT NULL COMMENT '待刷队列',
    `error_ids_json`      LONGTEXT     DEFAULT NULL COMMENT '错误单词ID列表',
    `user_input_json`     LONGTEXT     DEFAULT NULL COMMENT '学生输入记录',
    `first_attempt_seen_json` LONGTEXT DEFAULT NULL COMMENT '第一轮已作答ID',
    `first_attempt_correct_json` LONGTEXT DEFAULT NULL COMMENT '第一轮首次答对ID',
    `initial_count`       INT          DEFAULT NULL COMMENT '本轮总词数',
    `answered_count`      INT          DEFAULT NULL COMMENT '已推进数量',
    `first_round_correct` INT          DEFAULT NULL COMMENT '第一轮首次答对数',
    `correct_attempts`    INT          DEFAULT NULL COMMENT '答对次数',
    `wrong_attempts`      INT          DEFAULT NULL COMMENT '答错次数',
    `started_at`          DATETIME     DEFAULT NULL COMMENT '开始时间',
    `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_user_unit_stage` (`tenant_id`, `user_id`, `unit_id`, `stage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习会话断点';

-- ----------------------------
-- 8. 错题记录表
-- ----------------------------
DROP TABLE IF EXISTS `error_log`;
CREATE TABLE `error_log` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id`  VARCHAR(32) NOT NULL COMMENT '租户ID',
    `user_id`    BIGINT   NOT NULL COMMENT '用户ID',
    `word_id`    BIGINT   NOT NULL COMMENT '单词ID',
    `error_type` VARCHAR(32) NOT NULL COMMENT 'DONT_KNOW/SPELLING_ERROR',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_user` (`tenant_id`, `user_id`),
    KEY `idx_word_id` (`word_id`),
    KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='错误记录';

-- ----------------------------
-- 9. 学习记录表
-- ----------------------------
DROP TABLE IF EXISTS `study_log`;
CREATE TABLE `study_log` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id`     VARCHAR(32)  NOT NULL COMMENT '租户ID',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `word_id`       BIGINT       NOT NULL COMMENT '单词ID',
    `mode`          VARCHAR(32)  DEFAULT NULL COMMENT '学习类型：FLASHCARD/ENG_CH/CH_ENG/SPELL',
    `feedback_type` VARCHAR(32)  NOT NULL COMMENT 'KNOW/DONT_KNOW/SPELLING_ERROR',
    `user_input`    VARCHAR(500) DEFAULT NULL COMMENT '学生操作：选题/拼写内容',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_user_created` (`tenant_id`, `user_id`, `created_at`),
    KEY `idx_word_id` (`word_id`),
    KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习记录';

-- ----------------------------
-- 10. 单元完成记录
-- ----------------------------
DROP TABLE IF EXISTS `student_unit_completion`;
CREATE TABLE `student_unit_completion` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT,
    `tenant_id`    VARCHAR(32) NOT NULL,
    `user_id`      BIGINT   NOT NULL,
    `unit_id`      BIGINT   NOT NULL,
    `completed_at` DATETIME NOT NULL,
    `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user_unit` (`tenant_id`, `user_id`, `unit_id`),
    KEY `idx_tenant_user` (`tenant_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='单元完成';

-- ----------------------------
-- 11. 本单元各类型完成记录
-- ----------------------------
DROP TABLE IF EXISTS `student_unit_mode`;
CREATE TABLE `student_unit_mode` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`  VARCHAR(32)  NOT NULL,
    `user_id`    BIGINT       NOT NULL,
    `unit_id`    BIGINT       NOT NULL,
    `mode`       VARCHAR(32)  NOT NULL COMMENT 'FLASHCARD/ENG_CH/CH_ENG/SPELL',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user_unit_mode` (`tenant_id`, `user_id`, `unit_id`, `mode`),
    KEY `idx_tenant_user_unit` (`tenant_id`, `user_id`, `unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='本单元各类型完成';

-- ----------------------------
-- 12. 单词-模式进度
-- ----------------------------
DROP TABLE IF EXISTS `student_word_mode_progress`;
CREATE TABLE `student_word_mode_progress` (
    `id`                  BIGINT   NOT NULL AUTO_INCREMENT,
    `tenant_id`           VARCHAR(32) NOT NULL,
    `user_id`             BIGINT   NOT NULL,
    `word_id`             BIGINT   NOT NULL,
    `unit_id`             BIGINT   NOT NULL,
    `mode`                VARCHAR(32) NOT NULL COMMENT 'FLASHCARD/ENG_CH/CH_ENG/SPELL',
    `consecutive_correct` INT      NOT NULL DEFAULT 0 COMMENT '连续正确次数，>=3则该词该类型完成',
    `updated_at`          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user_word_mode` (`tenant_id`, `user_id`, `word_id`, `mode`),
    KEY `idx_tenant_user_unit_mode` (`tenant_id`, `user_id`, `unit_id`, `mode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='单词-模式进度';

-- ----------------------------
-- 13. 租户-书本授权
-- ----------------------------
DROP TABLE IF EXISTS `tenant_book_auth`;
CREATE TABLE `tenant_book_auth` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT,
    `tenant_id`  VARCHAR(32) NOT NULL COMMENT '租户ID',
    `book_id`    BIGINT   NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_book` (`tenant_id`, `book_id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_book_id` (`book_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户书本授权';

-- ----------------------------
-- 14. 错题复习完成记录（智能归纳完成后写入）
-- ----------------------------
DROP TABLE IF EXISTS `error_review_complete`;
CREATE TABLE `error_review_complete` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`   VARCHAR(32)  NOT NULL COMMENT '租户ID',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `review_date` DATE         NOT NULL COMMENT '完成复习的日期 yyyy-MM-dd',
    `error_type`  VARCHAR(32)  NOT NULL DEFAULT 'ALL' COMMENT '完成范围：ALL/FLASHCARD/ENG_TO_CH/CH_TO_ENG/SPELLING_ERROR',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user_date_type` (`tenant_id`, `user_id`, `review_date`, `error_type`),
    KEY `idx_tenant_user` (`tenant_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='错题复习完成记录';

-- ----------------------------
-- 15. 阶段结算结果
-- ----------------------------
DROP TABLE IF EXISTS `study_stage_result`;
CREATE TABLE `study_stage_result` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id`         VARCHAR(32)  NOT NULL COMMENT '租户ID',
    `user_id`           BIGINT       NOT NULL COMMENT '学生ID',
    `unit_id`           BIGINT       NOT NULL COMMENT '单元ID',
    `stage`             VARCHAR(32)  NOT NULL COMMENT '阶段/模式',
    `book_name`         VARCHAR(128) DEFAULT NULL COMMENT '书本名称',
    `unit_name`         VARCHAR(128) DEFAULT NULL COMMENT '单元名称',
    `total_questions`   INT          DEFAULT NULL COMMENT '本阶段总题数',
    `correct_attempts`  INT          DEFAULT NULL COMMENT '答对次数',
    `wrong_attempts`    INT          DEFAULT NULL COMMENT '答错次数',
    `first_round_correct` INT        DEFAULT NULL COMMENT '第一轮首次答对数',
    `stabilized_count`  INT          DEFAULT NULL COMMENT '稳定掌握数',
    `duration_seconds`  INT          DEFAULT NULL COMMENT '本次用时秒数',
    `star_reward`       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否触发星标奖励',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_stage_result_user` (`tenant_id`, `user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='阶段结算结果';

-- ----------------------------
-- 14. 学习会话统计（教材/单元模式）
-- ----------------------------
DROP TABLE IF EXISTS `study_session`;
CREATE TABLE `study_session` (
    `id`              BIGINT   NOT NULL AUTO_INCREMENT,
    `tenant_id`       VARCHAR(32) NOT NULL,
    `user_id`         BIGINT   NOT NULL,
    `study_date`      DATE     NOT NULL,
    `completed_count` INT      DEFAULT 0,
    `correct_count`   INT      DEFAULT 0,
    `streak_days`     INT      DEFAULT 0,
    `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_user_date` (`tenant_id`, `user_id`, `study_date`),
    KEY `idx_tenant_user` (`tenant_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习会话统计';

-- ----------------------------
-- 15. 学习任务
-- ----------------------------
DROP TABLE IF EXISTS `study_task`;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习任务';

-- ----------------------------
-- 16. 通知
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息通知';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 第二部分：初始化数据（租户 + 各角色账号）
-- 默认密码 123456
-- ============================================================
SET @pwd_hash = 'fa372fec54709802e9d0adf3b8832f7b';
SET @pwd_plain = '123456';

INSERT INTO `tenant` (`id`, `name`, `contact_name`, `contact_phone`, `account_quota`, `expire_time`, `status`) VALUES
('13800138000', '演示机构', '张校长', '13800138000', 500, DATE_ADD(NOW(), INTERVAL 1 YEAR), 1),
('13900139000', '第二演示机构', '李校长', '13900139000', 200, DATE_ADD(NOW(), INTERVAL 6 MONTH), 1);

INSERT INTO `user` (`tenant_id`, `username`, `password_hash`, `password_plain`, `real_name`, `role`, `status`) VALUES
(NULL, 'super', @pwd_hash, @pwd_plain, '超级管理员', 'SUPER_ADMIN', 1),
('13800138000', 'principal', @pwd_hash, @pwd_plain, '校长', 'ORG_ADMIN', 1),
('13800138000', 'student01', @pwd_hash, @pwd_plain, '学生小明', 'STUDENT', 1),
('13800138000', 'student02', @pwd_hash, @pwd_plain, '学生小红', 'STUDENT', 1),
('13800138000', 'student03', @pwd_hash, @pwd_plain, '学生小刚', 'STUDENT', 1),
('13900139000', 'principal2', @pwd_hash, @pwd_plain, '李校长', 'ORG_ADMIN', 1),
('13900139000', 'stu01', @pwd_hash, @pwd_plain, '学员甲', 'STUDENT', 1),
('13900139000', 'stu02', @pwd_hash, @pwd_plain, '学员乙', 'STUDENT', 1);

-- ============================================================
-- 第三部分：译林版书本/单元/单词（可选，若已有数据可跳过）
-- ============================================================
INSERT INTO `book` (`id`, `name`, `cover_url`, `description`, `version`, `sort_order`) VALUES
(1, '译林版三年级英语上册', NULL, '译林版小学英语三年级上册，8单元，86词。', 1, 1),
(2, '译林版三年级英语下册', NULL, '译林版小学英语三年级下册，8单元，58词。', 1, 2);

INSERT INTO `unit` (`id`, `book_id`, `name`, `sort_order`) VALUES
(1, 1, 'Unit 1 Hello', 1), (2, 1, 'Unit 2 I''m Liu Tao', 2), (3, 1, 'Unit 3 My friends', 3), (4, 1, 'Unit 4 My family', 4), (5, 1, 'Unit 5 Look at me!', 5), (6, 1, 'Unit 6 Colours', 6), (7, 1, 'Unit 7 Would you like a pie?', 7), (8, 1, 'Unit 8 Happy New Year!', 8),
(9, 2, 'Unit 1 In class', 1), (10, 2, 'Unit 2 In the library', 2), (11, 2, 'Unit 3', 3), (12, 2, 'Unit 4', 4), (13, 2, 'Unit 5', 5), (14, 2, 'Unit 6', 6), (15, 2, 'Unit 7', 7), (16, 2, 'Unit 8', 8);

INSERT INTO `word` (`unit_id`, `word`, `phonetic`, `meaning`, `sort_order`) VALUES
(1, 'hello', '[he''ləʊ]', '喂；你好', 1), (1, 'good morning', '[ɡʊd ''mɔːnɪŋ]', '早上好', 2), (1, 'Miss', '[mɪs]', '小姐，女士', 3), (1, 'hi', '[haɪ]', '你好；嗨', 4), (1, 'good afternoon', '[ɡʊd ɑːftə''nuːn]', '下午好', 5), (1, 'class', '[klɑːs]', '班；同学们', 6), (1, 'I', '[aɪ]', '我', 7), (1, 'I''m', '[aɪm]', '我是', 8),
(2, 'are', '[ɑː]', '是', 1), (2, 'you', '[juː]', '你；你们', 2), (2, 'yes', '[jes]', '是，是的', 3), (2, 'am', '[æm]', '是', 4), (2, 'no', '[nəʊ]', '不，没有', 5), (2, 'not', '[nɒt]', '不，没', 6), (2, 'goodbye', '[ˌɡʊd''baɪ]', '再见', 7);

-- 若需完整 14 本书 112 单元 144 词，请参考原 yilin-words-insert.sql
