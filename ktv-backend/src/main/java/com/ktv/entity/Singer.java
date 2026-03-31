package com.ktv.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 歌手表
 * SQL-S3修复：继承 BaseEntity，复用 id/createTime/updateTime/deleted 公共字段
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_singer")
public class Singer extends BaseEntity {

    /**
     * 歌手名
     */
    private String name;

    /**
     * 拼音全拼
     */
    private String pinyin;

    /**
     * 拼音首字母（大写）
     */
    private String pinyinInitial;

    /**
     * 性别：0未知 1男 2女 3组合
     */
    private Integer gender;

    /**
     * 地区：内地/港台/欧美/日韩/其他
     */
    private String region;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 歌曲数量（冗余字段）
     */
    private Integer songCount;

    /**
     * 状态：0禁用 1启用
     */
    private Integer status;
}
