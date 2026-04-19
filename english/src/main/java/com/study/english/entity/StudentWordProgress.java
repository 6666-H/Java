package com.study.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生单词进度（多租户，含 tenant_id）
 */
@Data
@TableName("student_word_progress")
public class StudentWordProgress {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID=手机号 */
    @TableField("tenant_id")
    private String tenantId;

    /** 学生用户ID */
    @TableField("user_id")
    private Long userId;

    /** 单词ID */
    @TableField("word_id")
    private Long wordId;

    /** 掌握阶段 0-5（艾宾浩斯） */
    @TableField("mastery_level")
    private Integer masteryLevel;

    /** 累计正确次数 */
    @TableField("correct_count")
    private Integer correctCount;

    /** 累计错误次数 */
    @TableField("wrong_count")
    private Integer wrongCount;

    /** 最后一次错误时间 */
    @TableField("last_error_time")
    private LocalDateTime lastErrorTime;

    /** 是否当前为错题 0否 1是 */
    @TableField("is_wrong")
    private Integer isWrong;

    /** 连续正确次数，强化池内满2移出 */
    @TableField("consecutive_correct_count")
    private Integer consecutiveCorrectCount;

    /** 连续错误次数，>=2加入强化池 */
    @TableField("consecutive_wrong_count")
    private Integer consecutiveWrongCount;

    /** 是否在错词强化池 0否 1是 */
    @TableField("in_reinforcement")
    private Integer inReinforcement;

    /** 0:未学 1:学习中 2:已掌握 */
    @TableField("status")
    private Integer status;

    /** 上次复习时间 */
    @TableField("last_review_time")
    private LocalDateTime lastReviewTime;

    /** 下次复习时间 */
    @TableField("next_review_time")
    private LocalDateTime nextReviewTime;

    /** 最后学习时间 */
    @TableField("last_study_at")
    private LocalDateTime lastStudyAt;

    /** 熟练度 0~1（教材/单元产品模式） */
    @TableField("strength")
    private BigDecimal strength;

    /** 间隔重复次数（教材/单元产品模式：1=10min,2=1d,3=3d,4=7d,5=15d,6=30d,7+=60d） */
    @TableField("repetitions")
    private Integer repetitions;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /** 掌握阶段上限 */
    public static final int MASTERY_MAX = 5;

    /** status 常量 */
    public static final int STATUS_UNLEARNED = 0;
    public static final int STATUS_LEARNING = 1;
    public static final int STATUS_MASTERED = 2;

    public static final int IS_WRONG_NO = 0;
    public static final int IS_WRONG_YES = 1;
    /** 强化池内连续正确2次移出 */
    public static final int REINFORCEMENT_CORRECT_TO_EXIT = 2;
    /** 连续错误2次加入强化池 */
    public static final int REINFORCEMENT_WRONG_THRESHOLD = 2;
    /** 简单反馈流：连续正确3次后晋级（兼容旧 API） */
    public static final int CONSECUTIVE_CORRECT_TO_MASTER = 3;
}
