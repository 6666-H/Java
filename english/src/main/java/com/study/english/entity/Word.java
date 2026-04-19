package com.study.english.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公共词库-单词（无 tenant_id）
 */
@Data
@TableName("word")
public class Word {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 单元ID */
    @TableField("unit_id")
    private Long unitId;

    /** 单词 */
    @TableField("word")
    private String word;

    /** 音标 */
    @TableField("phonetic")
    private String phonetic;

    /** 词性 */
    @TableField("pos")
    private String pos;

    /** 释义 */
    @TableField("meaning")
    private String meaning;

    /** 例句 */
    @TableField("example_sentence")
    private String exampleSentence;

    /** 例句翻译 */
    @TableField("example_zh")
    private String exampleZh;

    /** 发音 URL */
    @TableField("audio_url")
    private String audioUrl;

    /** 图片 URL */
    @TableField("image_url")
    private String imageUrl;

    /** 排序 */
    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
